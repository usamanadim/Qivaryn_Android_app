package com.qivaryn.app.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Owns the public download destination: the standard Downloads directory,
 * inside a folder named Qivaryn. On Android 10+ this is written through
 * MediaStore so it works with scoped storage; older releases use the public
 * Downloads directory directly.
 */
object DownloadDestination {
    private const val TAG = "QivarynDownloads"
    const val FOLDER_NAME = "Qivaryn"
    const val RELATIVE_PATH = "Download/$FOLDER_NAME"

    val MEDIA_EXTENSIONS = setOf("mp4", "mov", "webm", "jpg", "jpeg", "png", "webp")

    data class PublishedFile(
        val name: String,
        val size: Long,
        val localPath: String?,
        val contentUri: String?,
        val mimeType: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /** Hidden, temporary workspace used only while the Python engine downloads. */
    fun stagingDir(context: Context): File {
        val dir = File(context.cacheDir, "qivaryn_staging")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Standard Downloads/Qivaryn folder, resolved through the OS API. */
    fun publicDir(): File =
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            FOLDER_NAME
        )

    fun ensurePublicDir(): File {
        val dir = publicDir()
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun publishFile(
        context: Context,
        source: File,
        preferredName: String,
        mimeType: String
    ): PublishedFile? {
        if (!source.exists() || source.length() <= 0L) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishViaMediaStore(context, source, preferredName, mimeType)
        } else {
            publishViaFile(context, source, preferredName, mimeType)
        }
    }

    private fun publishViaMediaStore(
        context: Context,
        source: File,
        preferredName: String,
        mimeType: String
    ): PublishedFile? {
        val resolver = context.contentResolver
        val uniqueName = makeUniqueName(preferredName, queryExistingNames(context))
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, uniqueName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return null
        var copied = false
        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            }
            copied = true
        } catch (throwable: Throwable) {
            Log.e(TAG, "Could not write " + uniqueName + " to Downloads", throwable)
        }
        if (!copied) {
            resolver.delete(uri, null, null)
            return null
        }
        resolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) },
            null,
            null
        )
        return PublishedFile(
            name = uniqueName,
            size = source.length(),
            localPath = queryLocalPath(context, uri),
            contentUri = uri.toString(),
            mimeType = mimeType,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun publishViaFile(
        context: Context,
        source: File,
        preferredName: String,
        mimeType: String
    ): PublishedFile? {
        val dir = ensurePublicDir()
        val existing = dir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
        val uniqueName = makeUniqueName(preferredName, existing)
        val destination = File(dir, uniqueName)
        return try {
            source.inputStream().use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destination.absolutePath),
                arrayOf(mimeType),
                null
            )
            PublishedFile(
                name = uniqueName,
                size = destination.length(),
                localPath = destination.absolutePath,
                contentUri = null,
                mimeType = mimeType,
                timestamp = System.currentTimeMillis()
            )
        } catch (throwable: Throwable) {
            Log.e(TAG, "Could not copy " + uniqueName + " to Downloads", throwable)
            null
        }
    }

    fun listPublicFiles(context: Context): List<PublishedFile> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listViaMediaStore(context)
        } else {
            listViaFile()
        }

    private fun listViaMediaStore(context: Context): List<PublishedFile> {
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.SIZE,
            MediaStore.Downloads.DATE_ADDED,
            MediaStore.Downloads.DATA,
            MediaStore.Downloads.MIME_TYPE
        )
        val selection = MediaStore.Downloads.RELATIVE_PATH + " LIKE ?"
        val args = arrayOf(RELATIVE_PATH + "/%")
        val result = mutableListOf<PublishedFile>()
        try {
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                args,
                MediaStore.Downloads.DATE_ADDED + " DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
                val dataColumn = cursor.getColumnIndex(MediaStore.Downloads.DATA)
                val mimeColumn = cursor.getColumnIndex(MediaStore.Downloads.MIME_TYPE)
                val dateColumn = cursor.getColumnIndex(MediaStore.Downloads.DATE_ADDED)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn) ?: continue
                    if (name.substringAfterLast('.', "").lowercase() !in MEDIA_EXTENSIONS) continue
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val data = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                    val mime = if (mimeColumn >= 0) {
                        cursor.getString(mimeColumn)
                    } else {
                        mimeForName(name)
                    }
                    result.add(
                        PublishedFile(
                            name = name,
                            size = cursor.getLong(sizeColumn),
                            localPath = data,
                            contentUri = uri.toString(),
                            mimeType = mime ?: mimeForName(name),
                            timestamp = if (dateColumn >= 0) cursor.getLong(dateColumn) * 1000L else System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "Could not read Qivaryn downloads", throwable)
        }
        return result
    }

    private fun listViaFile(): List<PublishedFile> {
        val dir = publicDir()
        if (!dir.exists()) return emptyList()
        return dir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in MEDIA_EXTENSIONS }
            ?.map {
                PublishedFile(
                    name = it.name,
                    size = it.length(),
                    localPath = it.absolutePath,
                    contentUri = null,
                    mimeType = mimeForName(it.name),
                    timestamp = it.lastModified()
                )
            }
            ?.sortedByDescending { it.name }
            ?: emptyList()
    }

    private fun queryExistingNames(context: Context): Set<String> {
        val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME)
        val selection = MediaStore.Downloads.RELATIVE_PATH + " LIKE ?"
        val args = arrayOf(RELATIVE_PATH + "/%")
        val names = mutableSetOf<String>()
        try {
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                args,
                null
            )?.use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    cursor.getString(nameColumn)?.let { names.add(it) }
                }
            }
        } catch (throwable: Throwable) {
            Log.e(TAG, "Could not inspect existing downloads", throwable)
        }
        return names
    }

    private fun queryLocalPath(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Downloads.DATA),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun makeUniqueName(name: String, existing: Set<String>): String {
        if (name !in existing) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var index = 1
        var candidate = "$base ($index)$extension"
        while (candidate in existing) {
            index++
            candidate = "$base ($index)$extension"
        }
        return candidate
    }

    fun mimeForName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "mp4" -> "video/mp4"
        "mov" -> "video/quicktime"
        "webm" -> "video/webm"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "jpg", "jpeg" -> "image/jpeg"
        else -> "application/octet-stream"
    }

    fun isMediaFile(file: File): Boolean =
        file.isFile &&
            file.extension.lowercase() in MEDIA_EXTENSIONS &&
            !file.name.startsWith(".") &&
            !file.name.contains(".temp", ignoreCase = true)
}