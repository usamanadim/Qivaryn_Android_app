package com.qivaryn.app.update

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Reads releases from the app's own GitHub repository and downloads the APK
 * artifact that fits this device.
 *
 * The repository and channel are compiled into the app (see `UPDATE_REPO` and
 * `UPDATE_CHANNEL` in `app/build.gradle`) rather than stored in preferences: an
 * update source that the user — or a malicious app on the device — could
 * retarget would defeat the purpose of the check.
 *
 * A stable build ignores GitHub prereleases. A beta build accepts both beta and
 * stable releases so testers keep receiving updates after a beta is promoted.
 * SemVer ordering still decides which accepted release is newest.
 *
 * Only assets that were published by the repository owner are considered, and
 * every candidate is re-validated after download by [UpdateInstaller], so a
 * compromised release page still cannot install a foreign or downgraded APK.
 */
class GitHubReleaseSource(
    private val repository: String,
    private val channel: UpdateChannel = UpdateChannel.STABLE,
    private val token: String? = null,
    private val deviceAbis: List<String> = Build.SUPPORTED_ABIS.toList()
) {

    /**
     * Looks for the newest non-draft release this [channel] accepts that
     * actually carries an APK for this device.
     */
    suspend fun findLatest(): ReleaseQuery = withContext(Dispatchers.IO) {
        val payload = requestJson("$API_BASE/repos/$repository/releases?per_page=30")
        if (payload !is JSONArray) return@withContext ReleaseQuery.NoReleases

        var bestInfo: ReleaseInfo? = null
        var bestJson: JSONObject? = null
        var latestCandidate: SemVer? = null
        var latestCandidateTag: String? = null

        for (index in 0 until payload.length()) {
            val release = payload.optJSONObject(index) ?: continue
            // Drafts are private previews and are never offered. Prereleases
            // are only visible on the beta channel, and a prerelease tag is
            // treated as a prerelease even if the GitHub flag was forgotten.
            if (release.optBoolean("draft")) continue

            val tag = release.optString("tag_name")
            val version = SemVer.parse(tag) ?: continue
            val prerelease = release.optBoolean("prerelease") || !version.isStable
            if (!channel.accepts(prerelease)) continue

            // Remember the newest accepted tag even if it has no installable
            // asset, so a missing APK can be reported by version.
            if (latestCandidate == null || version > latestCandidate) {
                latestCandidate = version
                latestCandidateTag = tag
            }

            val asset = selectApkAsset(release) ?: continue
            val info = ReleaseInfo(
                tag = tag,
                version = version,
                title = release.optString("name").ifBlank { tag },
                notes = release.optString("body"),
                publishedAt = release.optString("published_at").takeIf { it.isNotBlank() },
                htmlUrl = release.optString("html_url"),
                asset = asset
            )
            if (bestInfo == null || info.version > bestInfo.version) {
                bestInfo = info
                bestJson = release
            }
        }

        val winner = bestInfo
        if (winner == null) {
            return@withContext if (latestCandidateTag == null) {
                ReleaseQuery.NoReleases
            } else {
                ReleaseQuery.NoInstallableAsset(latestCandidateTag)
            }
        }

        val json = bestJson ?: return@withContext ReleaseQuery.Found(winner)
        // The API only inlines a digest for recent uploads; older releases ship
        // a sidecar checksum file instead. Resolve it once, for the winner only.
        ReleaseQuery.Found(
            winner.copy(asset = winner.asset.copy(digest = resolveDigest(json, winner.asset)))
        )
    }

    /**
     * Streams [asset] into [destination], reporting cumulative byte counts.
     * The file is written to a `.part` sibling first so an interrupted download
     * can never be mistaken for a complete APK.
     */
    suspend fun download(
        asset: ReleaseAsset,
        destination: File,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit
    ) = withContext(Dispatchers.IO) {
        val url = if (!token.isNullOrBlank()) {
            asset.apiUrl ?: asset.browserUrl
        } else {
            asset.browserUrl
        }
        val connection = open(url, ACCEPT_BINARY)
        val temp = File(destination.parentFile, destination.name + ".part")
        if (temp.exists()) temp.delete()
        destination.parentFile?.mkdirs()

        val total = connection.contentLengthLong.takeIf { it > 0L } ?: asset.sizeBytes
        var read = 0L
        try {
            connection.inputStream.use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(COPY_BUFFER)
                    var count = input.read(buffer)
                    while (count >= 0) {
                        output.write(buffer, 0, count)
                        read += count
                        onProgress(read, total)
                        count = input.read(buffer)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        // A truncated body would otherwise validate as an unreadable APK later;
        // failing here keeps the error message specific.
        if (asset.sizeBytes > 0L && read != asset.sizeBytes) {
            temp.delete()
            throw IOException("Downloaded $read of ${asset.sizeBytes} bytes.")
        }
        if (destination.exists()) destination.delete()
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
    }

    // -------------------------------------------------------------------------
    // Release parsing
    // -------------------------------------------------------------------------

    /**
     * Picks the best APK among a release's assets.
     *
     * Ranking: an APK built for an ABI this device actually runs wins; next a
     * universal APK; then an APK with no ABI in its name. Debug and unsigned
     * builds are rejected outright because they can never replace the installed
     * release build.
     */
    private fun selectApkAsset(release: JSONObject): ReleaseAsset? {
        val assets = release.optJSONArray("assets") ?: return null
        var bestScore = Int.MIN_VALUE
        var best: ReleaseAsset? = null

        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            if (!name.endsWith(".apk", ignoreCase = true)) continue

            val size = asset.optLong("size")
            if (size <= 0L) continue

            val score = scoreAsset(name)
            if (score < 0 || score <= bestScore) continue

            bestScore = score
            best = ReleaseAsset(
                id = asset.optLong("id"),
                name = name,
                sizeBytes = size,
                apiUrl = asset.optString("url").takeIf { it.isNotBlank() },
                browserUrl = asset.optString("browser_download_url"),
                digest = parseDigest(asset.optString("digest"))
            )
        }
        return best
    }

    private fun scoreAsset(name: String): Int {
        val lower = name.lowercase(Locale.US)
        if ("debug" in lower || "unsigned" in lower) return -1

        // Ordered longest-first so `arm64-v8a` is recognised before `armeabi`.
        val mentioned = ABI_TOKENS.filter { lower.contains(it) }
        if (mentioned.isNotEmpty()) {
            for ((rank, abi) in deviceAbis.withIndex()) {
                if (abi.lowercase(Locale.US) in mentioned) return 100 - rank
            }
            // Built only for an ABI this device cannot run.
            return -1
        }
        if ("universal" in lower) return 60
        // No ABI in the filename: treat it as a universal build.
        return 40
    }

    /** `"sha256:abcd..."` as returned by the GitHub API, normalised to hex. */
    private fun parseDigest(raw: String?): String? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val hex = text.substringAfter(':', text).trim().lowercase(Locale.US)
        return hex.takeIf { it.length == 64 && it.all { char -> char in HEX_DIGITS } }
    }

    /**
     * Falls back to a sibling `*.sha256` file or a `checksums.txt`-style asset.
     * Returns null when the release publishes no checksum at all; the APK
     * signature check in [UpdateInstaller] still applies in that case.
     */
    private fun resolveDigest(release: JSONObject, asset: ReleaseAsset): String? {
        asset.digest?.let { return it }
        val assets = release.optJSONArray("assets") ?: return null

        assetUrlByName(assets, asset.name + ".sha256")?.let { url ->
            fetchText(url)?.let { text ->
                firstDigest(text)?.let { return it }
            }
        }
        for (name in CHECKSUM_ASSET_NAMES) {
            val url = assetUrlByName(assets, name) ?: continue
            fetchText(url)?.let { text ->
                digestForFile(text, asset.name)?.let { return it }
            }
        }
        return null
    }

    private fun assetUrlByName(assets: JSONArray, name: String): String? {
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (!asset.optString("name").equals(name, ignoreCase = true)) continue
            val browser = asset.optString("browser_download_url")
            if (browser.isNotBlank()) return browser
            return asset.optString("url").takeIf { it.isNotBlank() }
        }
        return null
    }

    private fun firstDigest(text: String): String? =
        text.split(Regex("[^0-9a-fA-F]"))
            .firstOrNull { it.length == 64 }
            ?.lowercase(Locale.US)

    /** Finds the 64-char hex token on the line that mentions [fileName]. */
    private fun digestForFile(text: String, fileName: String): String? {
        text.lineSequence()
            .firstOrNull { it.contains(fileName, ignoreCase = true) }
            ?.let { return firstDigest(it) }
        return null
    }

    // -------------------------------------------------------------------------
    // HTTP
    // -------------------------------------------------------------------------

    private fun fetchText(url: String): String? = try {
        val connection = open(url, ACCEPT_BINARY)
        try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    } catch (throwable: Throwable) {
        Log.w(TAG, "Could not read checksum asset: " + throwable.message)
        null
    }

    private fun requestJson(url: String): Any? {
        val connection = open(url, ACCEPT_JSON)
        return try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONTokener(body).nextValue()
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String, accept: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.setRequestProperty("Accept", accept)
        connection.setRequestProperty("User-Agent", USER_AGENT)
        if (!token.isNullOrBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $token")
        }
        connection.connect()

        val code = connection.responseCode
        if (code !in 200..299) {
            val detail = runCatching {
                connection.errorStream?.bufferedReader()?.use { it.readText() }?.take(300)
            }.getOrNull()
            connection.disconnect()
            throw IOException(
                "GitHub responded with HTTP $code" + (detail?.let { ": $it" } ?: "")
            )
        }
        return connection
    }

    private companion object {
        const val TAG = "QivarynUpdate"
        const val API_BASE = "https://api.github.com"
        const val ACCEPT_JSON = "application/vnd.github+json"
        const val ACCEPT_BINARY = "application/octet-stream"
        const val USER_AGENT = "Qivaryn-Android"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val COPY_BUFFER = 64 * 1024
        const val HEX_DIGITS = "0123456789abcdef"

        val ABI_TOKENS = listOf("arm64-v8a", "x86_64", "armeabi-v7a", "armeabi", "x86")

        val CHECKSUM_ASSET_NAMES =
            listOf("checksums.txt", "CHECKSUMS.txt", "SHA256SUMS", "sha256sums.txt", "SHA256SUMS.txt")
    }
}
