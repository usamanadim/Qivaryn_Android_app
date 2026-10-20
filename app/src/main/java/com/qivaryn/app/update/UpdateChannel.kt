package com.qivaryn.app.update

/**
 * Which GitHub releases this build is allowed to install.
 *
 * The channel is compiled into the APK (`UPDATE_CHANNEL` in `app/build.gradle`),
 * exactly like the repository itself, so neither the user nor another app can
 * retarget an installation at a different feed.
 *
 * [BETA] deliberately accepts stable releases too: a tester on `0.1.1-beta` must
 * still be able to move to `0.2.0` when the beta line is promoted, and SemVer
 * ordering already prevents a beta from being offered over a newer stable build.
 */
enum class UpdateChannel(val wireName: String, val displayName: String) {
    STABLE("stable", "stable"),
    BETA("beta", "beta");

    /** True when a release with this prerelease flag is visible on the channel. */
    fun accepts(prerelease: Boolean): Boolean = this == BETA || !prerelease

    /** Human-readable phrase used by the Update screen. */
    val description: String
        get() = when (this) {
            STABLE -> "stable release"
            BETA -> "beta release"
        }

    companion object {
        /** Unknown values fall back to the safer stable-only behaviour. */
        fun from(raw: String?): UpdateChannel =
            if (raw?.trim()?.equals(BETA.wireName, ignoreCase = true) == true) BETA else STABLE
    }
}
