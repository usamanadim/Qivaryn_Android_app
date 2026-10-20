package com.qivaryn.app.update

/** One downloadable file attached to a GitHub release. */
data class ReleaseAsset(
    val id: Long,
    val name: String,
    val sizeBytes: Long,
    /** `https://api.github.com/repos/.../releases/assets/<id>` endpoint. */
    val apiUrl: String?,
    val browserUrl: String,
    /** Published SHA-256 of the file, when the release provides one. */
    val digest: String?
)

/**
 * A GitHub release that carries an APK this device can install on its channel.
 *
 * The engine lives inside the APK, so an engine fix is shipped as a new app
 * release; see `docs/UPDATES.md`.
 */
data class ReleaseInfo(
    val tag: String,
    val version: SemVer,
    val title: String,
    val notes: String,
    val publishedAt: String?,
    val htmlUrl: String,
    val asset: ReleaseAsset
)

/** What this installation currently runs. */
data class VersionSnapshot(
    val versionName: String,
    val versionCode: Long,
    val engineVersion: String
)

/** The release behind whatever the update screen is showing, if any. */
fun UpdateUiState.releaseOrNull(): ReleaseInfo? = when (this) {
    is UpdateUiState.Available -> release
    is UpdateUiState.Downloading -> release
    is UpdateUiState.Verifying -> release
    is UpdateUiState.NeedsInstallPermission -> release
    is UpdateUiState.AwaitingInstall -> release
    else -> null
}

/**
 * Outcome of asking GitHub for the newest installable release on this channel.
 *
 * The "no APK" case is kept separate from "no releases" on purpose: a release
 * could carry only Python packages (`.whl`, `.tar.gz`), which a running Android
 * app cannot install. Reporting that distinction turns a confusing "no updates"
 * into an actionable message.
 */
sealed interface ReleaseQuery {
    data class Found(val release: ReleaseInfo) : ReleaseQuery
    data object NoReleases : ReleaseQuery
    data class NoInstallableAsset(val tag: String?) : ReleaseQuery
}
