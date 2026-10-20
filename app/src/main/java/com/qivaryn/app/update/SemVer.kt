package com.qivaryn.app.update

/**
 * Minimal semantic version used to decide whether a published GitHub release is
 * newer than the installed build.
 *
 * Qivaryn releases are tagged `vMAJOR.MINOR.PATCH`. [parse] rejects any tag it
 * cannot fully understand, so a stray tag such as `nightly` or `v1.2.3.4` can
 * never be mistaken for a newer build and silently offered to the user.
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val prerelease: String? = null
) : Comparable<SemVer> {

    /** A version without a prerelease suffix is a stable release. */
    val isStable: Boolean get() = prerelease == null

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        if (patch != other.patch) return patch - other.patch
        // Semver rule 11: a prerelease sorts *below* its own stable release, so
        // `1.2.0-rc1 < 1.2.0` and a stable release always wins a tie.
        return when {
            prerelease == null && other.prerelease == null -> 0
            prerelease == null -> 1
            other.prerelease == null -> -1
            else -> comparePrerelease(prerelease, other.prerelease)
        }
    }

    override fun toString(): String =
        "$major.$minor.$patch" + (prerelease?.let { "-$it" } ?: "")

    companion object {
        /** Tag prefixes seen on GitHub, from `v1.2.0` to `app-v1.2.0`. */
        private val TAG_PREFIXES = listOf("v", "V", "release-", "app-v", "app-")

        fun parse(raw: String?): SemVer? {
            if (raw.isNullOrBlank()) return null

            // Handles fully qualified refs such as `refs/tags/v1.2.3` and
            // `release/1.2.3` before any prefix stripping.
            var text = raw.trim().substringAfterLast('/')

            for (prefix in TAG_PREFIXES) {
                if (text.startsWith(prefix, ignoreCase = true) &&
                    text.length > prefix.length &&
                    text[prefix.length].isDigit()
                ) {
                    text = text.substring(prefix.length)
                    break
                }
            }

            // Build metadata (`1.2.0+sha`) never affects ordering.
            val withoutBuild = text.substringBefore('+')
            val core = withoutBuild.substringBefore('-')
            val prerelease = withoutBuild.substringAfter('-', "").ifBlank { null }

            val parts = core.split('.')
            if (parts.isEmpty() || parts.size > 3) return null
            if (parts.any { it.isBlank() || !it.all(Char::isDigit) }) return null

            val numbers = IntArray(3)
            for (index in parts.indices) {
                numbers[index] = parts[index].toIntOrNull() ?: return null
            }
            return SemVer(numbers[0], numbers[1], numbers[2], prerelease)
        }

        private fun comparePrerelease(left: String, right: String): Int {
            val a = left.split('.')
            val b = right.split('.')
            for (index in 0 until maxOf(a.size, b.size)) {
                // A shorter prerelease list is the smaller one (`1.0.0-alpha` <
                // `1.0.0-alpha.1`).
                val x = a.getOrNull(index) ?: return -1
                val y = b.getOrNull(index) ?: return 1
                if (x == y) continue
                val xNumber = x.toIntOrNull()
                val yNumber = y.toIntOrNull()
                if (xNumber != null && yNumber != null) return xNumber - yNumber
                // Numeric identifiers always rank below alphanumeric ones.
                if (xNumber != null) return -1
                if (yNumber != null) return 1
                return x.compareTo(y)
            }
            return 0
        }
    }
}
