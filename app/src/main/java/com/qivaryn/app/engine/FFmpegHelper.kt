package com.qivaryn.app.engine

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Extracts and validates the bundled FFmpeg binary. The native library and the
 * fallback asset are arm64 only, so other ABIs skip audio merging instead of
 * crashing.
 */
object FFmpegHelper {
    private const val TAG = "FFmpegHelper"
    private const val BINARY_NAME = "ffmpeg"
    private const val ASSET_PATH = "bin/" + BINARY_NAME
    private const val MIN_BINARY_SIZE = 1_000_000L

    @Volatile
    private var binary: File? = null

    @Volatile
    private var prepared = false

    /** Prepares FFmpeg once and reports whether an executable is available. */
    fun prepare(context: Context): Boolean {
        binary?.let { if (it.exists() && it.canExecute()) return true }
        if (prepared) return binary != null
        synchronized(this) {
            if (prepared) return binary != null
            binary = prepareLocked(context)
            prepared = true
            return binary != null
        }
    }

    /** Returns the prepared executable, or null when FFmpeg cannot run here. */
    fun getBinary(context: Context): File? = if (prepare(context)) binary else null

    private fun prepareLocked(context: Context): File? {
        val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        if (abi != "arm64-v8a") {
            Log.w(TAG, "Bundled FFmpeg is arm64-only; skipping audio merge on " + abi)
            return null
        }

        // The packaged native library is executable in place, so run it
        // directly and avoid copying 50 MB into the app sandbox.
        val native = File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")
        if (native.exists() && native.canExecute()) {
            val version = runVersionCheck(native)
            if (version != null) {
                Log.d(TAG, "Native FFmpeg ready: " + version.lineSequence().firstOrNull())
                return native
            }
            Log.w(TAG, "Native FFmpeg could not be executed; using asset fallback")
        }

        // Fallback for builds that ship the binary only as an asset.
        val target = File(File(context.filesDir, "bin"), BINARY_NAME)
        return try {
            if (!target.exists() || target.length() < MIN_BINARY_SIZE) {
                Log.d(TAG, "Extracting FFmpeg to " + target.absolutePath)
                target.parentFile?.mkdirs()
                context.assets.open(ASSET_PATH).use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
            if (!makeExecutable(target)) {
                Log.e(TAG, "FFmpeg is not executable")
                return null
            }
            val version = runVersionCheck(target)
            if (version == null) {
                Log.e(TAG, "FFmpeg executable did not run")
                return null
            }
            Log.d(TAG, "FFmpeg ready: " + version.lineSequence().firstOrNull())
            target
        } catch (throwable: Throwable) {
            Log.e(TAG, "Could not prepare FFmpeg", throwable)
            null
        }
    }

    private fun makeExecutable(file: File): Boolean {
        file.setExecutable(true, false)
        if (!file.canExecute()) {
            runCatching {
                ProcessBuilder("chmod", "755", file.absolutePath)
                    .redirectErrorStream(true)
                    .start()
                    .waitFor(15, TimeUnit.SECONDS)
            }
        }
        return file.canExecute()
    }

    private fun runVersionCheck(file: File): String? {
        return try {
            val process = ProcessBuilder(file.absolutePath, "-version")
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val finished = process.waitFor(20, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return null
            }
            if (process.exitValue() == 0 && output.contains("ffmpeg", ignoreCase = true)) {
                output
            } else {
                null
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "FFmpeg version check failed", throwable)
            null
        }
    }
}
