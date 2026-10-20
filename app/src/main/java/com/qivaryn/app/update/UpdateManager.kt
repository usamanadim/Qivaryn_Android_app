package com.qivaryn.app.update

import android.content.Context
import android.util.Log
import com.qivaryn.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Drives the whole update flow: check GitHub, download the APK, verify it, and
 * hand it to Android's installer.
 *
 * Everything runs off the main thread and publishes progress through [state],
 * which the Update screen collects. The manager is deliberately a singleton so
 * the flow survives navigation away from the settings screen and so
 * [InstallResultReceiver] can report into the same state.
 */
object UpdateManager {

    private const val TAG = "QivarynUpdate"
    private const val CACHE_DIR = "updates"
    private const val PROGRESS_INTERVAL_MS = 120L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)

    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    /** The compile-time release channel this build follows. */
    val channel: UpdateChannel = UpdateChannel.from(BuildConfig.UPDATE_CHANNEL)

    @Volatile
    private var snapshot: VersionSnapshot? = null

    /** The release the user is currently acting on, kept across state changes. */
    @Volatile
    private var pendingRelease: ReleaseInfo? = null

    /** The release needs permission before it can be installed. */
    fun snapshot(): VersionSnapshot? = snapshot

    /**
     * Reads what this installation actually runs. The version comes from the
     * platform (not from `BuildConfig`) so it always describes the installed
     * package, which is what the comparison must use.
     */
    suspend fun loadSnapshot(context: Context): VersionSnapshot =
        snapshot ?: withContext(Dispatchers.IO) {
            buildSnapshot(context).also { snapshot = it }
        }

    private fun buildSnapshot(context: Context): VersionSnapshot {
        val (versionCode, versionName) = UpdateInstaller.installedVersion(context)
        return VersionSnapshot(
            versionName = versionName,
            versionCode = versionCode,
            // Baked in from app/src/main/python/__init__.py at build time, so it
            // always matches the engine actually compiled into this APK.
            engineVersion = BuildConfig.ENGINE_VERSION
        )
    }

    // -------------------------------------------------------------------------
    // Checking
    // -------------------------------------------------------------------------

    /**
     * Queries GitHub for the newest stable release. Safe to call on every visit
     * to the Update screen: a check already in flight, or one that already found
     * an update, is not repeated unless [force] is set.
     */
    fun check(context: Context, force: Boolean = false) {
        val current = _state.value
        if (current is UpdateUiState.Checking ||
            current is UpdateUiState.Downloading ||
            current is UpdateUiState.Verifying ||
            current is UpdateUiState.AwaitingInstall
        ) {
            return
        }
        if (!force && current is UpdateUiState.Available) return

        _state.value = UpdateUiState.Checking
        scope.launch {
            val appContext = context.applicationContext
            try {
                val versions = loadSnapshot(appContext)
                val installed = SemVer.parse(versions.versionName)

                val release = when (
                    val query = withContext(Dispatchers.IO) { releaseSource().findLatest() }
                ) {
                    is ReleaseQuery.Found -> query.release

                    ReleaseQuery.NoReleases -> {
                        _state.value = UpdateUiState.Failed(
                            UpdateStage.CHECK,
                            "There is no " + channel.description + " published for " +
                                BuildConfig.UPDATE_REPO + " yet.",
                            "Releases must be public and not drafts, tagged " +
                                "vMAJOR.MINOR.PATCH" +
                                if (channel == UpdateChannel.BETA) " (a trailing -beta is allowed)." else ".",
                            title = "No release to install",
                            advice = if (channel == UpdateChannel.BETA) {
                                "Publish a release tagged vMAJOR.MINOR.PATCH-beta with the " +
                                    "signed APK attached."
                            } else {
                                "Publish a stable release tagged vMAJOR.MINOR.PATCH with the " +
                                    "signed APK attached."
                            }
                        )
                        return@launch
                    }

                    is ReleaseQuery.NoInstallableAsset -> {
                        // A release with no APK can still be older than, or the
                        // same as, the installed build. Then there is genuinely
                        // nothing to install and reporting a missing asset would
                        // be misleading — the honest answer is "up to date".
                        val published = SemVer.parse(query.tag)
                        if (installed != null && published != null && published <= installed) {
                            pendingRelease = null
                            _state.value = UpdateUiState.UpToDate(
                                versions.versionName,
                                published.toString()
                            )
                            return@launch
                        }
                        _state.value = UpdateUiState.Failed(
                            UpdateStage.CHECK,
                            "Release " + (query.tag ?: "latest") +
                                " does not include an Android APK.",
                            "That release only carries files an app cannot install " +
                                "(for example .whl or .tar.gz packages).",
                            title = "No Android APK in that release",
                            advice = "Attach the signed APK to the GitHub release, or point " +
                                "UPDATE_REPO at the repository that publishes Qivaryn app " +
                                "releases."
                        )
                        return@launch
                    }
                }

                if (installed != null && release.version <= installed) {
                    pendingRelease = null
                    _state.value = UpdateUiState.UpToDate(versions.versionName, release.version.toString())
                    return@launch
                }
                // An unparsable installed version cannot be compared here; the
                // version-code guard after download still protects the install.
                pendingRelease = release
                _state.value = UpdateUiState.Available(release, versions.versionName)
            } catch (io: IOException) {
                _state.value = UpdateUiState.Failed(
                    UpdateStage.CHECK,
                    "Could not reach GitHub.",
                    io.message,
                    title = "Couldn't reach GitHub",
                    advice = "Check the internet connection and try again. Nothing on your " +
                        "device was changed."
                )
            } catch (throwable: Throwable) {
                Log.e(TAG, "Update check failed", throwable)
                _state.value = UpdateUiState.Failed(
                    UpdateStage.CHECK,
                    "The update check failed.",
                    throwable.message ?: throwable.javaClass.simpleName
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // Download, verify, install
    // -------------------------------------------------------------------------

    fun downloadAndInstall(context: Context) {
        val release = pendingRelease ?: return
        val current = _state.value
        if (current is UpdateUiState.Downloading ||
            current is UpdateUiState.Verifying ||
            current is UpdateUiState.AwaitingInstall
        ) {
            return
        }
        if (!UpdateInstaller.canInstall(context)) {
            _state.value = UpdateUiState.NeedsInstallPermission(release)
            return
        }

        _state.value = UpdateUiState.Downloading(release, 0L, release.asset.sizeBytes, 0f)
        scope.launch {
            val appContext = context.applicationContext
            var downloaded: File? = null
            try {
                val directory = File(appContext.cacheDir, CACHE_DIR)
                directory.listFiles()?.forEach { it.delete() }
                directory.mkdirs()
                val target = File(directory, "qivaryn-" + release.version + ".apk")

                var lastEmit = 0L
                withContext(Dispatchers.IO) {
                    releaseSource().download(release.asset, target) { read, total ->
                        val now = System.currentTimeMillis()
                        if (now - lastEmit >= PROGRESS_INTERVAL_MS || read >= total) {
                            lastEmit = now
                            _state.value = UpdateUiState.Downloading(
                                release = release,
                                bytesRead = read,
                                totalBytes = total,
                                progress = if (total > 0L) {
                                    (read.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                            )
                        }
                    }
                }
                downloaded = target

                _state.value = UpdateUiState.Verifying(release)
                withContext(Dispatchers.IO) { verifyIntegrity(release, target) }

                val validation = withContext(Dispatchers.IO) {
                    UpdateInstaller.validate(appContext, target)
                }
                if (!validation.ok) {
                    target.delete()
                    _state.value = UpdateUiState.Failed(
                        UpdateStage.VERIFY,
                        validation.reason ?: "The downloaded update could not be validated."
                    )
                    return@launch
                }

                UpdateInstaller.commitInstall(appContext, target)
                    .onSuccess { _state.value = UpdateUiState.AwaitingInstall(release) }
                    .onFailure { failure ->
                        _state.value = UpdateUiState.Failed(
                            UpdateStage.INSTALL,
                            "Android's installer could not be started.",
                            failure.message
                        )
                    }
            } catch (failure: UpdateStageException) {
                downloaded?.delete()
                _state.value = UpdateUiState.Failed(
                    failure.stage,
                    failure.message ?: "The update failed."
                )
            } catch (io: IOException) {
                downloaded?.delete()
                _state.value = UpdateUiState.Failed(
                    UpdateStage.DOWNLOAD,
                    "The download was interrupted.",
                    io.message
                )
            } catch (throwable: Throwable) {
                Log.e(TAG, "Update failed", throwable)
                downloaded?.delete()
                _state.value = UpdateUiState.Failed(
                    UpdateStage.DOWNLOAD,
                    "The update could not be completed.",
                    throwable.message ?: throwable.javaClass.simpleName
                )
            }
        }
    }

    /**
     * Checks the published SHA-256 when the release provides one. A release
     * without a checksum is still installable: the APK signature comparison in
     * [UpdateInstaller] is the security boundary, this only catches a corrupted
     * or truncated transfer.
     */
    private fun verifyIntegrity(release: ReleaseInfo, file: File) {
        val expected = release.asset.digest ?: run {
            Log.w(TAG, "Release ${release.tag} publishes no SHA-256; skipping checksum")
            return
        }
        val actual = sha256Hex(file)
        if (!actual.equals(expected, ignoreCase = true)) {
            file.delete()
            throw UpdateStageException(
                UpdateStage.VERIFY,
                "The downloaded file does not match the SHA-256 published with the release."
            )
        }
    }

    // -------------------------------------------------------------------------
    // Results reported by the installer
    // -------------------------------------------------------------------------

    internal fun onInstallFinished(success: Boolean, message: String?) {
        if (success) {
            val version = pendingRelease?.version?.toString().orEmpty()
            pendingRelease = null
            _state.value = UpdateUiState.Installed(version)
        } else {
            _state.value = UpdateUiState.Failed(
                UpdateStage.INSTALL,
                message ?: "The update could not be installed.",
                "Qivaryn is unchanged and still usable."
            )
        }
    }

    /** Called when the user comes back from the "install unknown apps" screen. */
    fun onInstallPermissionDenied() {
        val release = pendingRelease ?: return
        _state.value = UpdateUiState.NeedsInstallPermission(release)
    }

    /**
     * Re-reads the installed version after the installer has been dismissed.
     *
     * The process may survive a cancelled install, so this reconciles the UI
     * with what is actually installed instead of leaving the screen stuck on
     * "waiting for installer".
     */
    fun refresh(context: Context) {
        val current = _state.value
        if (current !is UpdateUiState.AwaitingInstall && current !is UpdateUiState.Installed) return
        val target = if (current is UpdateUiState.AwaitingInstall) current.release else return

        scope.launch {
            val fresh = withContext(Dispatchers.IO) { buildSnapshot(context.applicationContext) }
            snapshot = fresh
            val installed = SemVer.parse(fresh.versionName)
            if (installed != null && installed >= target.version) {
                pendingRelease = null
                _state.value = UpdateUiState.Installed(fresh.versionName)
            } else {
                // The user cancelled the installer; offer the release again.
                _state.value = UpdateUiState.Available(target, fresh.versionName)
            }
        }
    }

    /** Clears a terminal message so the screen can return to its idle state. */
    fun dismissResult() {
        if (_state.value is UpdateUiState.Installed) _state.value = UpdateUiState.Idle
    }

    private fun releaseSource(): GitHubReleaseSource = GitHubReleaseSource(
        repository = BuildConfig.UPDATE_REPO,
        channel = channel,
        token = BuildConfig.GITHUB_TOKEN.takeIf { it.isNotBlank() }
    )

    /** An update failure that already knows which step it belongs to. */
    private class UpdateStageException(
        val stage: UpdateStage,
        message: String
    ) : IOException(message)
}
