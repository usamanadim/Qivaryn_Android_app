package com.qivaryn.app.data

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.qivaryn.app.engine.EngineProvider
import java.io.File

enum class AppScreen {
    PRIVACY,
    HOME,
    INSTAGRAM,
    DOWNLOADS,
    HISTORY,
    SETTINGS,
    UPDATE
}

/**
 * UI state holder for navigation, cookies, downloads, and appearance.
 *
 * Downloads are staged in a private cache folder while the Python engine is
 * running, then published into the public Downloads/Qivaryn folder through
 * [DownloadDestination]. History is always read back from that public folder.
 */
class QivarynState(val context: Context) {

    private val prefs = context.getSharedPreferences("qivaryn_ui", Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _downloads = mutableStateListOf<DownloadEntry>()
    private val suppressedKeys: MutableSet<String> =
        prefs.getStringSet(KEY_SUPPRESSED, emptySet())?.toMutableSet() ?: mutableSetOf()

    var screen by mutableStateOf(
        if (prefs.getBoolean(KEY_ONBOARDING, false)) AppScreen.HOME else AppScreen.PRIVACY
    )
        private set

    var cookiesOpen by mutableStateOf(false)
        private set

    var cookiesImported by mutableStateOf(cookieFile().let { it.exists() && it.length() > 0L })
        private set

    var cookieFileSize by mutableStateOf(cookieFile().takeIf { it.exists() }?.length() ?: 0L)
        private set

    var storageUsedBytes by mutableStateOf(0L)
        private set

    var cookieCheckResult by mutableStateOf<String?>(null)
        private set

    var cookieCheckRunning by mutableStateOf(false)
        private set

    var isDarkTheme by mutableStateOf(prefs.getBoolean(KEY_DARK, false))
        private set

    init {
        DownloadDestination.stagingDir(context)
        migrateLegacyDownloads()
        refreshHistory()
    }

    /**
     * One-time migration: files downloaded by earlier versions lived in the
     * app-private files directory. Publish them into Downloads/Qivaryn and
     * remove the hidden copies so the public folder is the single source.
     */
    private fun migrateLegacyDownloads() {
        val legacy = context.filesDir.listFiles()
            ?.filter { DownloadDestination.isMediaFile(it) }
            ?: return
        if (legacy.isEmpty()) return
        Thread {
            var moved = false
            legacy.forEach { file ->
                val published = DownloadDestination.publishFile(
                    context = context,
                    source = file,
                    preferredName = file.name,
                    mimeType = DownloadDestination.mimeForName(file.name)
                )
                if (published != null && file.delete()) moved = true
            }
            if (moved) {
                mainHandler.post { refreshHistory() }
            }
        }.start()
    }

    // ---------------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------------

    fun navigate(target: AppScreen) {
        if (target == screen) return
        screen = target
        if (target == AppScreen.DOWNLOADS || target == AppScreen.HISTORY) {
            refreshHistory()
        }
    }

    fun continueFromPrivacy() {
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
        screen = AppScreen.HOME
    }

    fun showWelcomeAgain() {
        screen = AppScreen.PRIVACY
    }

    fun openCookies() {
        cookiesOpen = true
    }

    fun closeCookies() {
        cookiesOpen = false
    }

    fun updateDarkTheme(enabled: Boolean) {
        isDarkTheme = enabled
        prefs.edit().putBoolean(KEY_DARK, enabled).apply()
    }

    /** Returns true when the back press was consumed. */
    fun handleBack(): Boolean {
        if (cookiesOpen) {
            cookiesOpen = false
            return true
        }
        return when (screen) {
            AppScreen.INSTAGRAM -> {
                screen = AppScreen.HOME
                true
            }
            AppScreen.HISTORY -> {
                screen = AppScreen.DOWNLOADS
                true
            }
            AppScreen.UPDATE -> {
                screen = AppScreen.SETTINGS
                true
            }
            AppScreen.SETTINGS -> {
                screen = AppScreen.HOME
                true
            }
            AppScreen.DOWNLOADS -> {
                screen = AppScreen.HOME
                true
            }
            AppScreen.PRIVACY, AppScreen.HOME -> false
        }
    }

    // ---------------------------------------------------------------------
    // Downloads
    // ---------------------------------------------------------------------

    fun allEntries(): List<DownloadEntry> =
        _downloads.sortedWith(
            compareByDescending<DownloadEntry> { it.isActive }
                .thenByDescending { it.timestamp }
        )

    fun entries(filter: DownloadFilter): List<DownloadEntry> =
        allEntries().filter { it.matches(filter) }

    fun activeCount(): Int = _downloads.count { it.isActive }

    fun completedCount(): Int = _downloads.count { it.status == DownloadStatus.COMPLETED }

    fun failedCount(): Int = _downloads.count {
        it.status == DownloadStatus.FAILED ||
            it.status == DownloadStatus.EXPIRED ||
            it.status == DownloadStatus.FILE_REMOVED
    }

    fun removeEntry(id: String) {
        val entry = _downloads.firstOrNull { it.id == id } ?: return
        entry.localPath?.let { suppressedKeys.add(it) }
        entry.contentUri?.let { suppressedKeys.add(it) }
        persistSuppressed()
        _downloads.remove(entry)
    }

    fun clearHistory() {
        _downloads.forEach { entry ->
            entry.localPath?.let { suppressedKeys.add(it) }
            entry.contentUri?.let { suppressedKeys.add(it) }
        }
        persistSuppressed()
        _downloads.clear()
        refreshHistory()
    }

    private fun persistSuppressed() {
        prefs.edit().putStringSet(KEY_SUPPRESSED, suppressedKeys.toSet()).apply()
    }

    /**
     * Starts a real download through the Python engine and tracks the row in
     * the Downloads screen. Returns an error message when the request cannot
     * be started, otherwise null.
     *
     * [scope] selects a single URL, all reels, or all posts. [mediaType] is the
     * selected single-download media filter (Post, Reel, or Profile).
     */
    fun startDownload(
        rawUrl: String,
        scope: DownloadScope,
        mediaType: String,
        mediaCount: Int?,
        onFinished: ((String) -> Unit)? = null
    ): String? {
        val url = rawUrl.trim()
        if (url.isEmpty()) return "Paste an Instagram URL first."
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "That doesn't look like an Instagram link."
        }
        if (!url.contains("instagram.com")) {
            return "Only Instagram links are supported right now."
        }
        if (scope == DownloadScope.SINGLE && (mediaType == "Story" || mediaType == "Highlights")) {
            return "Story and Highlights downloads are not supported yet."
        }
        if (scope != DownloadScope.SINGLE &&
            (url.contains("/reel/") || url.contains("/p/") || url.contains("/tv/"))
        ) {
            return "Paste an Instagram profile URL for All Reels or All Posts."
        }
        if (_downloads.any { it.isActive }) {
            return "A download is already running. Wait for it to finish."
        }

        val title = when (scope) {
            DownloadScope.SINGLE -> singleDownloadTitle(url)
            DownloadScope.ALL_REELS -> "All reels · " + accountName(url)
            DownloadScope.ALL_POSTS -> "All posts · " + accountName(url)
        }
        val entry = DownloadEntry(
            id = "download-" + System.currentTimeMillis(),
            title = title,
            sourceUrl = url,
            status = DownloadStatus.DOWNLOADING,
            timestamp = System.currentTimeMillis(),
            progress = 0f
        )
        _downloads.add(0, entry)

        Thread {
            var success = false
            var downloadedCount = 0
            var stagedMediaCount = 0
            var failure: String? = null
            var stats = intArrayOf()
            var published: List<DownloadDestination.PublishedFile> = emptyList()
            val staging = DownloadDestination.stagingDir(context)
            val bridge = EngineProvider.getBridge(context)
            try {
                clearDirectory(staging)
                // The state database must outlive a single run, otherwise the
                // engine cannot skip items that were already downloaded.
                val dbFile = File(context.filesDir, "qivaryn_state.db")
                val cookie = cookieFile().takeIf { it.exists() && it.length() > 0L }
                bridge.initEngine(staging, dbFile, cookie, null)
                bridge.progressListener = { downloaded, total ->
                    mainHandler.post {
                        val index = _downloads.indexOfFirst { it.id == entry.id }
                        if (index >= 0) {
                            val current = _downloads[index]
                            _downloads[index] = current.copy(
                                downloadedBytes = downloaded,
                                totalBytes = if (total > 0L) total else current.totalBytes,
                                progress = if (total > 0L) {
                                    (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    current.progress
                                }
                            )
                        }
                    }
                }
                val engineType = when (scope) {
                    DownloadScope.ALL_REELS -> "reels"
                    DownloadScope.ALL_POSTS -> "posts"
                    DownloadScope.SINGLE -> when (mediaType) {
                        "Post" -> "posts"
                        "Reel" -> "reels"
                        else -> null
                    }
                }
                downloadedCount = bridge.downloadUrlCount(
                    url = url,
                    count = mediaCount,
                    mediaType = engineType
                )
                if (downloadedCount == 0 && failure == null) {
                    failure = bridge.lastError().takeIf { it.isNotBlank() }
                }
                stats = bridge.lastDownloadStats()
                // Single downloads return a bool (0/1); account feeds return
                // the number of new items and are considered successful even
                // when the account had nothing new to download.
                success = if (scope == DownloadScope.SINGLE) {
                    downloadedCount > 0
                } else {
                    true
                }
                // Only files produced during this run live in staging. An
                // empty staging folder after a successful single download
                // means the engine skipped an item that already exists.
                stagedMediaCount = staging.listFiles()
                    ?.count { DownloadDestination.isMediaFile(it) }
                    ?: 0
                if (stagedMediaCount > 0) {
                    published = publishStagingFiles(staging)
                }
            } catch (throwable: Throwable) {
                failure = throwable.message ?: "The download could not be completed."
            } finally {
                bridge.progressListener = null
                clearDirectory(staging)
            }

            mainHandler.post {
                val index = _downloads.indexOfFirst { it.id == entry.id }
                if (index >= 0) {
                    val current = _downloads[index]
                    if (success && published.isNotEmpty()) {
                        _downloads.removeAt(index)
                        published.forEach { file ->
                            _downloads.add(
                                0,
                                DownloadEntry(
                                    id = publicId(file),
                                    title = file.name,
                                    status = DownloadStatus.COMPLETED,
                                    sourceUrl = url,
                                    downloadedBytes = file.size,
                                    totalBytes = file.size,
                                    localPath = file.localPath,
                                    contentUri = file.contentUri,
                                    mimeType = file.mimeType,
                                    timestamp = file.timestamp
                                )
                            )
                        }
                    } else if (success && downloadedCount > 0 && stagedMediaCount == 0) {
                        // The engine skipped media that is already downloaded,
                        // so there is nothing to publish.
                        _downloads.removeAt(index)
                    } else if (success && downloadedCount > 0) {
                        _downloads[index] = current.copy(
                            status = DownloadStatus.FAILED,
                            progress = 0f,
                            errorMessage = "Downloaded media could not be saved to Downloads/Qivaryn."
                        )
                    } else if (success) {
                        // Bulk operation completed but the account had no new
                        // media. Do not leave a completed row without a file.
                        _downloads.removeAt(index)
                    } else {
                        _downloads[index] = current.copy(
                            status = DownloadStatus.FAILED,
                            progress = 0f,
                            errorMessage = friendlyFailure(failure)
                        )
                    }
                }
                refreshHistory()
                onFinished?.invoke(
                    when {
                        success && published.isNotEmpty() ->
                            "Saved " + published.size + " file(s) to Downloads/Qivaryn" +
                                statsSuffix(scope, stats)
                        success && downloadedCount > 0 && stagedMediaCount == 0 ->
                            "Already downloaded — nothing new to save."
                        success && downloadedCount == 0 ->
                            "No new media was found for this account." +
                                statsSuffix(scope, stats)
                        success ->
                            "Downloaded media could not be saved to Downloads/Qivaryn."
                        else -> friendlyFailure(failure)
                    }
                )
            }
        }.start()

        return null
    }

    fun refreshHistory() {
        val publicFiles = DownloadDestination.listPublicFiles(context)
        storageUsedBytes = publicFiles.sumOf { it.size }

        val presentKeys = buildSet {
            publicFiles.forEach { file ->
                file.contentUri?.let { add(it) }
                file.localPath?.let { add(it) }
            }
        }
        val knownKeys = buildSet {
            _downloads.forEach { entry ->
                entry.localPath?.let { add(it) }
                entry.contentUri?.let { add(it) }
            }
        }

        publicFiles.forEach { file ->
            val key = file.contentUri ?: file.localPath ?: return@forEach
            if (key in suppressedKeys || key in knownKeys) return@forEach
            _downloads.add(
                DownloadEntry(
                    id = publicId(file),
                    title = file.name,
                    status = DownloadStatus.COMPLETED,
                    downloadedBytes = file.size,
                    totalBytes = file.size,
                    localPath = file.localPath,
                    contentUri = file.contentUri,
                    mimeType = file.mimeType,
                    timestamp = file.timestamp
                )
            )
        }

        for (index in _downloads.indices) {
            val entry = _downloads[index]
            if (entry.status == DownloadStatus.COMPLETED && entry.hasFile) {
                val key = entry.contentUri ?: entry.localPath
                if (key != null && key !in presentKeys && key !in suppressedKeys) {
                    _downloads[index] = entry.copy(status = DownloadStatus.FILE_REMOVED)
                }
            }
        }
    }

    private fun publishStagingFiles(staging: File): List<DownloadDestination.PublishedFile> {
        val files = staging.listFiles()
            ?.filter { DownloadDestination.isMediaFile(it) }
            ?.sortedBy { it.lastModified() }
            ?: return emptyList()
        return files.mapNotNull { file ->
            DownloadDestination.publishFile(
                context = context,
                source = file,
                preferredName = file.name,
                mimeType = DownloadDestination.mimeForName(file.name)
            )
        }
    }

    private fun clearDirectory(directory: File) {
        directory.listFiles()?.forEach { child -> child.deleteRecursively() }
    }

    private fun statsSuffix(scope: DownloadScope, stats: IntArray): String {
        if (scope == DownloadScope.SINGLE || stats.size < 4) return ""
        return "  ·  processed " + stats[0] + ", skipped " + stats[2] + ", failed " + stats[3]
    }

    private fun publicId(file: DownloadDestination.PublishedFile): String =
        "pub:" + (file.contentUri ?: file.localPath ?: file.name)

    private fun accountName(url: String): String =
        url.trim()
            .trimEnd('/')
            .substringAfterLast('/')
            .substringBefore('?')
            .ifBlank { "account" }

    private fun singleDownloadTitle(url: String): String {
        val shortcode = url.trim()
            .trimEnd('/')
            .substringAfterLast('/')
            .substringBefore('?')
        return when {
            url.contains("/reel/", ignoreCase = true) ||
                url.contains("/reels/", ignoreCase = true) -> "Reel · " + shortcode
            url.contains("/tv/", ignoreCase = true) -> "IGTV · " + shortcode
            url.contains("/p/", ignoreCase = true) -> "Post · " + shortcode
            shortcode.isNotBlank() -> "Profile · " + shortcode
            else -> url
        }
    }

    private fun friendlyFailure(raw: String?): String {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return "Download failed. Check the link and cookies."
        val lower = text.lowercase()
        return when {
            "login_required" in lower || "logged out" in lower ||
                "authenticated session" in lower ->
                "Instagram rejected the saved session. Import fresh cookies from a " +
                    "logged-in browser, then try again."
            "please wait" in lower || "rate" in lower || "429" in lower ->
                "Instagram is temporarily rate-limiting this device. Wait a few " +
                    "minutes, then try again."
            "private" in lower ->
                "This post is private or unavailable to the saved account. Import " +
                    "cookies for an account that can view it."
            "timed out" in lower || "timeout" in lower || "connection" in lower ->
                "Network problem while contacting Instagram. Check your connection " +
                    "and try again."
            else -> text
        }
    }

    // ---------------------------------------------------------------------
    // Cookies
    // ---------------------------------------------------------------------

    fun cookieFile(): File = File(context.filesDir, "cookies.txt")

    /**
     * Imports a Netscape-format cookies.txt export.
     *
     * Only instagram.com cookies are kept: copying a full browser export into
     * the app would store unrelated sessions on the device. Returns null on
     * success or a user-facing error message.
     */
    fun importCookies(uri: Uri): String? {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return "Could not open that file."
            val cookies = LinkedHashSet<String>()
            stream.bufferedReader().useLines { lines ->
                lines.forEach { raw ->
                    val line = raw.trimEnd('\r')
                    if (line.isBlank()) return@forEach
                    val httpOnly = line.startsWith("#HttpOnly_")
                    if (!httpOnly && line.startsWith("#")) return@forEach
                    val domain = if (httpOnly) {
                        line.removePrefix("#HttpOnly_").substringBefore('\t')
                    } else {
                        line.substringBefore('\t')
                    }
                    if (domain.contains("instagram.com", ignoreCase = true)) {
                        cookies.add(line)
                    }
                }
            }
            if (cookies.isEmpty()) {
                return "No Instagram cookies found. Export cookies for instagram.com " +
                    "while logged in, then try again."
            }
            val session = cookies.firstOrNull { line ->
                line.substringAfterLast("#HttpOnly_")
                    .split('\t')
                    .getOrNull(5) == "sessionid"
            } ?: return "No Instagram sessionid found. Export cookies while logged " +
                "in to Instagram, then try again."
            val expiry = session.substringAfterLast("#HttpOnly_")
                .split('\t')
                .getOrNull(4)
                ?.toLongOrNull()
                ?: 0L
            if (expiry > 0L && expiry < System.currentTimeMillis() / 1000L) {
                return "The Instagram session in that file has expired. Export " +
                    "fresh cookies from a logged-in browser."
            }
            cookieFile().writeText(
                buildString {
                    append("# Netscape HTTP Cookie File\n")
                    cookies.forEach { append(it).append('\n') }
                }
            )
            cookiesImported = true
            cookieFileSize = cookieFile().length()
            cookieCheckResult = null
            null
        } catch (throwable: Throwable) {
            "Could not read that file: " +
                (throwable.message ?: throwable.javaClass.simpleName)
        }
    }

    fun clearCookies() {
        val file = cookieFile()
        if (file.exists()) file.delete()
        cookiesImported = false
        cookieFileSize = 0L
        cookieCheckResult = null
    }

    /**
     * Asks Instagram whether the imported session is still accepted. The
     * result is exposed through [cookieCheckResult].
     */
    fun checkCookieSession() {
        val file = cookieFile().takeIf { it.exists() && it.length() > 0L }
        if (file == null) {
            cookieCheckResult = "No cookies imported."
            return
        }
        if (cookieCheckRunning) return
        cookieCheckRunning = true
        cookieCheckResult = "Checking Instagram session…"
        Thread {
            val raw = runCatching {
                EngineProvider.getBridge(context).validateSession(file)
            }.getOrElse {
                "error:" + (it.message ?: it.javaClass.simpleName)
            }
            val message = when {
                raw.startsWith("valid:") ->
                    "Session valid — signed in as @" + raw.removePrefix("valid:")
                raw.startsWith("invalid:") -> {
                    val detail = raw.removePrefix("invalid:")
                    if (detail.contains("wait", ignoreCase = true) ||
                        detail.contains("rate", ignoreCase = true)
                    ) {
                        "Instagram is temporarily rate-limiting this device. " +
                            "Wait a few minutes, then try a download."
                    } else {
                        "Session invalid or expired (" + detail + "). Import fresh " +
                            "cookies from a browser where Instagram is logged in."
                    }
                }
                raw.startsWith("error:") ->
                    "Could not check the session: " + raw.removePrefix("error:")
                else -> raw
            }
            mainHandler.post {
                cookieCheckRunning = false
                cookieCheckResult = message
            }
        }.start()
    }

    fun cookieStatusLabel(): String =
        if (cookiesImported) "Imported  ·  " + formatBytes(cookieFileSize) else "None"

    private companion object {
        const val KEY_ONBOARDING = "onboarding_seen"
        const val KEY_DARK = "dark_mode"
        const val KEY_SUPPRESSED = "suppressed_download_keys"
    }
}