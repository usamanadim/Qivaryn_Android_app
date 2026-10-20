package com.qivaryn.app.engine

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.qivaryn.app.data.DownloadDestination
import java.io.File
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.TimeUnit

class QivarynEngineBridge(private val context: Context) {
    private val TAG = "QivarynEngineBridge"
    private var engine: PyObject? = null

    /**
     * Receives throttled byte counts from the Python download loop. The UI sets
     * this before starting a download and clears it once the run finishes.
     */
    @Volatile
    var progressListener: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null

    /** Called from Python (`bridge.onProgress(done, total)`) while downloading. */
    fun onProgress(downloadedBytes: Long, totalBytes: Long) {
        progressListener?.invoke(downloadedBytes, totalBytes)
    }

    /**
     * Names of media already published to Downloads/Qivaryn. The Python engine
     * validates its persistent state database against this index, because the
     * working folder it downloads into is emptied after every run.
     */
    @Volatile
    private var publishedNames: Set<String> = emptySet()

    /**
     * Returns true when [shortcode] already has a published file. Carousel
     * children are published as `shortcode_1.ext`, `shortcode_2.ext`, ...
     */
    @Suppress("unused")
    fun isPublished(shortcode: String): Boolean {
        if (shortcode.isBlank()) return false
        return publishedNames.any { name ->
            val base = name.substringBeforeLast('.', name)
            if (base == shortcode) return@any true
            val prefix = shortcode + "_"
            if (!base.startsWith(prefix)) return@any false
            val suffix = base.substring(prefix.length)
            suffix.isNotEmpty() && suffix.all { it.isDigit() }
        }
    }

    fun initEngine(outputDir: File, dbPath: File, cookiesFile: File? = null, sessionId: String? = null) {
        val py = Python.getInstance()
        val setupModule = py.getModule("chaquopy_setup")
        setupModule.callAttr("setup_path", null, outputDir.absolutePath)

        // Build the published-file index before the engine starts so its
        // resume checks can see files saved by previous runs.
        publishedNames = DownloadDestination.listPublicFiles(context)
            .map { it.name }
            .toSet()

        val engineModule = py.getModule("downloader")
        engine = engineModule.callAttr(
            "QivarynEngine",
            outputDir.absolutePath,
            dbPath.absolutePath,
            sessionId,
            cookiesFile?.absolutePath,
            3
        )
        // Expose this Kotlin bridge to Python so it can request FFmpeg muxing.
        engine?.put("bridge", this)
        Log.d(TAG, "QivarynEngine initialized")
    }

    /**
     * Checks the imported cookies against Instagram without starting a
     * download. Returns `valid:<username>`, `invalid:<detail>`, or
     * `error:<detail>`. Call from a background thread.
     */
    fun validateSession(cookiesFile: File): String {
        val py = Python.getInstance()
        val clientModule = py.getModule("client")
        return clientModule.callAttr("validate_session", cookiesFile.absolutePath).toString()
    }

    /**
     * Runs the engine's existing download_url entry point. The Python method
     * returns a bool for single items and an int count for account feeds, so
     * this helper normalizes both shapes into a count (0 = nothing/failed).
     */
    fun downloadUrlCount(
        url: String,
        maxPages: Int? = null,
        force: Boolean = false,
        count: Int? = null,
        mediaType: String? = null
    ): Int {
        val engineObj = engine ?: throw IllegalStateException("Engine not initialized")
        val result = engineObj.callAttr(
            "download_url_count",
            url,
            maxPages,
            force,
            count,
            mediaType
        )
        return result.toInt()
    }

    fun downloadUrl(
        url: String,
        maxPages: Int? = null,
        force: Boolean = false,
        count: Int? = null,
        mediaType: String? = null
    ): Boolean = downloadUrlCount(url, maxPages, force, count, mediaType) > 0

    /**
     * Returns processed:downloaded:skipped:failed counters from the last engine
     * operation. Empty when the engine did not run an account feed.
     */
    fun lastDownloadStats(): IntArray {
        val engineObj = engine ?: return intArrayOf()
        val text = runCatching {
            engineObj.callAttr("get_last_stats").toString()
        }.getOrNull() ?: return intArrayOf()
        return text.split(":")
            .mapNotNull { it.toIntOrNull() }
            .toIntArray()
    }

    /**
     * Returns the Python engine's reason for the most recent single-item
     * failure. Empty when the run succeeded or no error was recorded.
     */
    fun lastError(): String {
        val engineObj = engine ?: return ""
        return runCatching {
            engineObj.callAttr("get_last_error").toString()
        }.getOrDefault("")
    }

    /**
     * Muxes the separately downloaded Instagram audio track into the video
     * file. Video is stream-copied to preserve the original quality; audio is
     * stream-copied when possible and re-encoded to AAC only as a fallback.
     */
    fun mergeVideoAudio(
        videoPath: String,
        audioPath: String,
        outPath: String,
        audioStartMs: Long
    ): Boolean {
        val video = File(videoPath)
        val audio = File(audioPath)
        val output = File(outPath)
        if (!video.exists() || !audio.exists()) {
            Log.e(TAG, "Cannot merge: missing input file")
            return false
        }
        output.parentFile?.mkdirs()
        output.delete()

        Log.d(TAG, "Trying MediaMuxer audio merge for " + video.name)
        if (mergeWithMediaMuxer(video, audio, output, audioStartMs)) {
            Log.d(TAG, "Merged audio with MediaMuxer: " + output.name)
            return true
        }
        Log.w(TAG, "MediaMuxer merge failed; trying FFmpeg fallback")
        output.delete()
        val ffmpeg = FFmpegHelper.getBinary(context) ?: run {
            Log.w(TAG, "FFmpeg unavailable; audio merge skipped")
            return false
        }

        fun buildCommand(audioCodec: String, bitrate: String?): List<String> {
            val command = mutableListOf(
                ffmpeg.absolutePath,
                "-y",
                "-i", video.absolutePath
            )
            if (audioStartMs != 0L) {
                command += listOf(
                    "-itsoffset",
                    String.format(Locale.US, "%.3f", audioStartMs / 1000.0)
                )
            }
            command += listOf(
                "-i", audio.absolutePath,
                "-map", "0:v:0",
                "-map", "1:a:0",
                "-c:v", "copy",
                "-c:a", audioCodec
            )
            if (bitrate != null) {
                command += listOf("-b:a", bitrate)
            }
            command += listOf(
                "-shortest",
                "-movflags", "+faststart",
                output.absolutePath
            )
            return command
        }

        if (runFfmpeg(buildCommand("copy", null), "copy")) {
            return true
        }

        output.delete()
        if (runFfmpeg(buildCommand("aac", "192k"), "aac")) {
            return true
        }
        output.delete()

        return false
    }

    /**
     * Copies the video and audio samples into a new MP4 without re-encoding.
     * This is the primary muxer because it works inside the app sandbox on all
     * supported ABIs and preserves the original streams exactly.
     */
    private fun mergeWithMediaMuxer(
        video: File,
        audio: File,
        output: File,
        audioStartMs: Long
    ): Boolean {
        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        var started = false
        return try {
            videoExtractor.setDataSource(video.absolutePath)
            audioExtractor.setDataSource(audio.absolutePath)
            val videoTrack = findTrack(videoExtractor, "video/")
            val audioTrack = findTrack(audioExtractor, "audio/")
            if (videoTrack < 0 || audioTrack < 0) {
                Log.e(TAG, "MediaMuxer: missing video or audio track")
                return false
            }
            videoExtractor.selectTrack(videoTrack)
            audioExtractor.selectTrack(audioTrack)
            val videoFormat = videoExtractor.getTrackFormat(videoTrack)
            val audioFormat = audioExtractor.getTrackFormat(audioTrack)

            muxer = MediaMuxer(
                output.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val muxVideoTrack = muxer.addTrack(videoFormat)
            val muxAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()
            started = true

            val videoBuffer = ByteBuffer.allocateDirect(8 * 1024 * 1024)
            val audioBuffer = ByteBuffer.allocateDirect(8 * 1024 * 1024)
            val info = MediaCodec.BufferInfo()
            val offsetUs = audioStartMs * 1000L

            var videoSize = videoExtractor.readSampleData(videoBuffer, 0)
            var videoTime = videoExtractor.sampleTime
            var audioSize = audioExtractor.readSampleData(audioBuffer, 0)
            var audioTime = audioExtractor.sampleTime

            while (videoSize >= 0 || audioSize >= 0) {
                val hasVideo = videoSize >= 0
                val hasAudio = audioSize >= 0
                val adjustedAudioTime = audioTime + offsetUs
                val writeVideo = hasVideo && (!hasAudio || videoTime <= adjustedAudioTime)
                if (writeVideo) {
                    info.offset = 0
                    info.size = videoSize
                    info.presentationTimeUs = videoTime.coerceAtLeast(0L)
                    info.flags = if (
                        videoExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
                    ) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else {
                        0
                    }
                    muxer.writeSampleData(muxVideoTrack, videoBuffer, info)
                    videoExtractor.advance()
                    videoSize = videoExtractor.readSampleData(videoBuffer, 0)
                    videoTime = videoExtractor.sampleTime
                } else if (hasAudio) {
                    if (adjustedAudioTime >= 0L) {
                        info.offset = 0
                        info.size = audioSize
                        info.presentationTimeUs = adjustedAudioTime
                        info.flags = if (
                            audioExtractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC != 0
                        ) {
                            MediaCodec.BUFFER_FLAG_KEY_FRAME
                        } else {
                            0
                        }
                        muxer.writeSampleData(muxAudioTrack, audioBuffer, info)
                    }
                    audioExtractor.advance()
                    audioSize = audioExtractor.readSampleData(audioBuffer, 0)
                    audioTime = audioExtractor.sampleTime
                } else {
                    break
                }
            }

            muxer.stop()
            muxer.release()
            muxer = null
            started = false
            val ok = output.exists() && output.length() > 0L
            Log.d(TAG, "MediaMuxer finished, size=" + output.length() + ", ok=" + ok)
            ok
        } catch (throwable: Throwable) {
            Log.e(TAG, "MediaMuxer merge failed", throwable)
            false
        } finally {
            videoExtractor.release()
            audioExtractor.release()
            if (muxer != null) {
                if (started) {
                    runCatching { muxer.stop() }
                }
                runCatching { muxer.release() }
            }
            if (!output.exists() || output.length() == 0L) {
                output.delete()
            }
        }
    }

    private fun findTrack(extractor: MediaExtractor, prefix: String): Int {
        for (index in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith(prefix) == true) return index
        }
        return -1
    }

    private fun runFfmpeg(command: List<String>, mode: String): Boolean {
        var process: Process? = null
        return try {
            Log.d(TAG, "Running FFmpeg (" + mode + "): " + command.joinToString(" "))
            process = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val finished = process.waitFor(180, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                Log.e(TAG, "FFmpeg timed out (" + mode + ")")
                return false
            }
            val exit = process.exitValue()
            if (exit != 0) {
                Log.e(TAG, "FFmpeg failed (" + mode + ", exit=" + exit + "): " + output.takeLast(2000))
                false
            } else {
                val result = command.lastOrNull()?.let { File(it) }
                val ok = result != null && result.exists() && result.length() > 0L
                Log.d(TAG, "FFmpeg completed (" + mode + "), output size=" + (result?.length() ?: 0L))
                ok
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "FFmpeg execution failed (" + mode + ")", throwable)
            false
        } finally {
            if (process?.isAlive == true) process.destroy()
        }
    }
}