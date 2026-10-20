package com.qivaryn.app.engine

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Muxes a video file and a separate audio file into a single MP4 with Media3.
 *
 * Android's own [android.media.MediaMuxer] only stores H.264 and H.265 video,
 * so the VP9 and AV1 streams Instagram serves for Reels could not be muxed and
 * the reel was saved without sound. Media3's MP4 writer understands those
 * codecs, so the tracks can be copied into one file without re-encoding.
 *
 * The audio file may carry a video track of its own (Instagram's progressive
 * stream does); that track is dropped so the result holds exactly one video
 * and one audio track. [audioStartMs] delays the audio so it lines up with the
 * video.
 */
object Media3Merger {
    private const val TAG = "Media3Merger"
    private const val TIMEOUT_SECONDS = 240L

    fun merge(
        context: Context,
        video: File,
        audio: File,
        output: File,
        audioStartMs: Long
    ): Boolean {
        if (!video.exists() || !audio.exists()) {
            Log.e(TAG, "Cannot merge: missing input file")
            return false
        }
        output.parentFile?.mkdirs()
        output.delete()

        val videoItem = EditedMediaItem.Builder(
            MediaItem.Builder().setUri(Uri.fromFile(video)).build()
        ).build()

        val audioItem = EditedMediaItem.Builder(
            MediaItem.Builder().setUri(Uri.fromFile(audio)).build()
        ).build()

        // The track types tell the composition which track each sequence
        // contributes. The audio file may carry a video track of its own
        // (Instagram's progressive stream does); it is simply not used.
        val videoSequence = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_VIDEO))
            .addItems(listOf(videoItem))
            .build()

        val audioBuilder = EditedMediaItemSequence.Builder(setOf(C.TRACK_TYPE_AUDIO))
            .addItems(listOf(audioItem))
        if (audioStartMs > 0L) {
            // Media3 expresses a delay as a gap in front of the sequence.
            audioBuilder.addGap(audioStartMs * 1000L)
        }
        val audioSequence = audioBuilder.build()

        val composition = Composition.Builder(
            listOf(videoSequence, audioSequence)
        )
            .setTransmuxVideo(true)
            .setTransmuxAudio(true)
            .build()

        return export(context, composition, output)
    }

    /**
     * Runs the export on a dedicated thread with a Looper, because Media3
     * requires one, and blocks until it finishes. The Python engine calls this
     * from a worker thread and expects a plain boolean back.
     */
    private fun export(context: Context, composition: Composition, output: File): Boolean {
        val thread = HandlerThread("QivarynMedia3").apply { start() }
        val handler = Handler(thread.looper)
        val latch = CountDownLatch(1)
        val failure = AtomicReference<String?>(null)
        val transformerRef = AtomicReference<Transformer?>(null)

        try {
            handler.post {
                try {
                    val transformer = Transformer.Builder(context)
                        .setLooper(thread.looper)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(
                                composition: Composition,
                                exportResult: ExportResult
                            ) {
                                latch.countDown()
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException
                            ) {
                                failure.set(
                                    exportException.errorCodeName + ": " +
                                        (exportException.message ?: "unknown error")
                                )
                                latch.countDown()
                            }
                        })
                        .build()
                    transformerRef.set(transformer)
                    transformer.start(composition, output.absolutePath)
                } catch (throwable: Throwable) {
                    failure.set(throwable.message ?: throwable.javaClass.simpleName)
                    latch.countDown()
                }
            }

            if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                runCatching { transformerRef.get()?.cancel() }
                Log.e(TAG, "Media3 merge timed out")
                return false
            }

            val error = failure.get()
            if (error != null) {
                Log.e(TAG, "Media3 merge failed: " + error)
                return false
            }

            val ok = output.exists() && output.length() > 0L
            Log.d(TAG, "Media3 merge finished, output size=" + output.length())
            return ok
        } finally {
            runCatching { transformerRef.get()?.cancel() }
            thread.quitSafely()
        }
    }
}
