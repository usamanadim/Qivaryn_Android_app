package com.qivaryn.app.update

/** The step an update failed at, so the UI can offer the right next action. */
enum class UpdateStage { CHECK, DOWNLOAD, VERIFY, INSTALL }

/**
 * Every state the Update section can be in. Modelled as a sealed interface so
 * the UI must handle each case explicitly instead of guessing from flags.
 */
sealed interface UpdateUiState {

    /** Nothing has been checked yet. */
    data object Idle : UpdateUiState

    data object Checking : UpdateUiState

    /** The installed build is the newest release on this build's channel. */
    data class UpToDate(val installed: String, val latest: String) : UpdateUiState

    /** A newer release on this build's channel is waiting to be installed. */
    data class Available(val release: ReleaseInfo, val installed: String) : UpdateUiState

    data class Downloading(
        val release: ReleaseInfo,
        val bytesRead: Long,
        val totalBytes: Long,
        val progress: Float
    ) : UpdateUiState

    /** Bytes are on disk and are being checksum-verified and inspected. */
    data class Verifying(val release: ReleaseInfo) : UpdateUiState

    /**
     * Android will not let Qivaryn install packages yet. The user has to grant
     * "install unknown apps" for this app first.
     */
    data class NeedsInstallPermission(val release: ReleaseInfo) : UpdateUiState

    /** The system installer has been handed the APK. */
    data class AwaitingInstall(val release: ReleaseInfo) : UpdateUiState

    data class Installed(val version: String) : UpdateUiState

    data class Failed(
        val stage: UpdateStage,
        val message: String,
        val detail: String? = null,
        /** Overrides the default headline when the failure is not generic. */
        val title: String? = null,
        /** Overrides the default "what to do next" line. */
        val advice: String? = null
    ) : UpdateUiState
}
