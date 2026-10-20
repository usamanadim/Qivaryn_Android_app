package com.qivaryn.app.update

import java.io.File
import java.security.MessageDigest

/** Lowercase hex SHA-256 of [file], streamed so a large APK is never buffered. */
internal fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
}

/** Lowercase hex SHA-256 of an in-memory [bytes] buffer. */
internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { byte -> "%02x".format(byte) }
