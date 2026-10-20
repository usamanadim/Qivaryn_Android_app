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

class QivarynEngineBridge(private val context: Context) {
    private val TAG = "QivarynEngineBridge"

    /** MediaFormat has no public constant for the codec-specific data keys. */
    private val KEY_CSD_0 = "csd-0"
    private val KEY_CSD_1 = "csd-1"

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
        setupModule.callAttr("setup_path", outputDir.absolutePath)

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
        // Expose this Kotlin bridge to Python so it can request media muxing.
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
     * file. H.264 and H.265 are copied with MediaMuxer, which is lossless and
     * instant. Other codecs (VP9, AV1) need Media3, which understands them.
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

        if (isMuxableVideoFile(video)) {
            Log.d(TAG, "Trying MediaMuxer audio merge for " + video.name)
            if (mergeWithMediaMuxer(video, audio, output, audioStartMs)) {
                Log.d(TAG, "Merged audio with MediaMuxer: " + output.name)
                return true
            }
            Log.w(TAG, "MediaMuxer merge failed")
            output.delete()
        } else {
            // Android's own MP4 writer only stores H.264 and H.265, so this
            // stream has to go through Media3.
            Log.i(TAG, "Video codec needs Media3: " + video.name)
        }

        if (Media3Merger.merge(context, video, audio, output, audioStartMs)) {
            Log.d(TAG, "Merged audio with Media3: " + output.name)
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
        var videoMime = ""
        var audioMime = ""
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
            videoMime = videoFormat.getString(MediaFormat.KEY_MIME).orEmpty()
            audioMime = audioFormat.getString(MediaFormat.KEY_MIME).orEmpty()

            if (!isMuxableVideoMime(videoMime)) {
                Log.w(
                    TAG,
                    "MediaMuxer cannot store " + videoMime +
                        " in an MP4; the caller falls back to the audio-bearing stream"
                )
                return false
            }
            if (!isMuxableAudioMime(audioMime)) {
                Log.w(TAG, "MediaMuxer cannot store " + audioMime + " in an MP4")
                return false
            }

            // MediaExtractor returns track formats that carry extra entries the
            // MP4 writer rejects ("Failed to add the track to the muxer"), so
            // rebuild a minimal format with only the keys the writer accepts.
            val muxVideoFormat = muxableFormat(videoFormat)
            if (muxVideoFormat == null) {
                Log.e(TAG, "MediaMuxer: no usable video track format (" + videoMime + ")")
                return false
            }
            val muxAudioFormat = muxableFormat(audioFormat)
            if (muxAudioFormat == null) {
                Log.e(TAG, "MediaMuxer: no usable audio track format (" + audioMime + ")")
                return false
            }

            muxer = MediaMuxer(
                output.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val muxVideoTrack = muxer.addTrack(muxVideoFormat)
            val muxAudioTrack = muxer.addTrack(muxAudioFormat)
            muxer.start()

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
            val ok = output.exists() && output.length() > 0L
            Log.d(TAG, "MediaMuxer finished, size=" + output.length() + ", ok=" + ok)
            ok
        } catch (throwable: Throwable) {
            Log.e(
                TAG,
                "MediaMuxer merge failed (video=" + videoMime + ", audio=" + audioMime + ")",
                throwable
            )
            false
        } finally {
            videoExtractor.release()
            audioExtractor.release()
            if (muxer != null) {
                runCatching { muxer.stop() }
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

    /** Android's MP4 writer only stores H.264 and H.265 video tracks. */
    private fun isMuxableVideoMime(mime: String): Boolean =
        mime.startsWith("video/avc") || mime.startsWith("video/hevc")

    /** Reads the video track of a file and reports whether it fits an MP4. */
    private fun isMuxableVideoFile(file: File): Boolean {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            val track = findTrack(extractor, "video/")
            if (track < 0) {
                Log.e(TAG, "No video track in " + file.name)
                return false
            }
            val mime = extractor.getTrackFormat(track).getString(MediaFormat.KEY_MIME).orEmpty()
            val muxable = isMuxableVideoMime(mime)
            if (!muxable) {
                Log.w(TAG, "Video track " + mime + " in " + file.name + " is not MP4-compatible")
            }
            muxable
        } catch (throwable: Throwable) {
            Log.e(TAG, "Could not inspect the video track of " + file.name, throwable)
            false
        } finally {
            runCatching { extractor.release() }
        }
    }

    /** Android's MP4 writer stores AAC, AMR, Opus and uncompressed audio. */
    private fun isMuxableAudioMime(mime: String): Boolean =
        mime.startsWith("audio/mp4a") || mime.startsWith("audio/aac") ||
            mime.startsWith("audio/amr") || mime.startsWith("audio/opus") ||
            mime.startsWith("audio/3gpp") || mime.startsWith("audio/raw") ||
            mime.startsWith("audio/mpeg")

    /**
     * Rebuilds a track format with only the keys the MP4 writer understands.
     * Returns null when a required key is missing.
     */
    private fun muxableFormat(source: MediaFormat): MediaFormat? {
        val mime = source.getString(MediaFormat.KEY_MIME) ?: return null
        val clean = MediaFormat()
        clean.setString(MediaFormat.KEY_MIME, mime)
        return try {
            if (mime.startsWith("video/")) {
                clean.setInteger(MediaFormat.KEY_WIDTH, source.getInteger(MediaFormat.KEY_WIDTH))
                clean.setInteger(MediaFormat.KEY_HEIGHT, source.getInteger(MediaFormat.KEY_HEIGHT))
                if (source.containsKey(MediaFormat.KEY_ROTATION)) {
                    clean.setInteger(
                        MediaFormat.KEY_ROTATION,
                        source.getInteger(MediaFormat.KEY_ROTATION)
                    )
                }
            } else {
                clean.setInteger(
                    MediaFormat.KEY_SAMPLE_RATE,
                    source.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                )
                clean.setInteger(
                    MediaFormat.KEY_CHANNEL_COUNT,
                    source.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                )
            }
            copyByteBuffer(source, clean, KEY_CSD_0)
            copyByteBuffer(source, clean, KEY_CSD_1)
            if (!clean.containsKey(KEY_CSD_0)) {
                copyParameterSets(source, clean)
            }
            if (source.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                clean.setInteger(
                    MediaFormat.KEY_MAX_INPUT_SIZE,
                    source.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                )
            }
            clean
        } catch (throwable: Throwable) {
            Log.e(TAG, "Cannot build a muxable track format for " + mime, throwable)
            null
        }
    }

    private fun copyByteBuffer(source: MediaFormat, target: MediaFormat, key: String) {
        if (!source.containsKey(key)) return
        val buffer = source.getByteBuffer(key)?.duplicate() ?: return
        if (buffer.remaining() <= 0) return
        val copy = ByteBuffer.allocateDirect(buffer.remaining())
        copy.put(buffer)
        copy.position(0)
        target.setByteBuffer(key, copy)
    }

    /**
     * Some extractors expose the H.264 parameter sets separately instead of
     * the combined codec-specific data the MP4 writer needs.
     */
    private fun copyParameterSets(source: MediaFormat, target: MediaFormat) {
        val sps = source.getByteBuffer("sps")?.duplicate() ?: return
        val pps = source.getByteBuffer("pps")?.duplicate() ?: return
        if (sps.remaining() <= 0 || pps.remaining() <= 0) return
        val combined = ByteBuffer.allocate(sps.remaining() + pps.remaining())
        combined.put(sps)
        combined.put(pps)
        combined.position(0)
        target.setByteBuffer(KEY_CSD_0, combined)
    }
}