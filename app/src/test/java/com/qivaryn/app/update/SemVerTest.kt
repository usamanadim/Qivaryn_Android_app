package com.qivaryn.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Version comparison decides whether an update is offered at all, so the rules
 * are pinned down here rather than discovered on a device.
 */
class SemVerTest {

    @Test
    fun `parses the tag shapes GitHub releases use`() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("v1.2.3"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("1.2.3"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("V1.2.3"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("release-1.2.3"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("app-v1.2.3"))
        assertEquals(SemVer(2, 0, 0), SemVer.parse("refs/tags/v2.0.0"))
        assertEquals(SemVer(1, 2, 3), SemVer.parse("  v1.2.3  "))
    }

    @Test
    fun `fills missing components with zero`() {
        assertEquals(SemVer(1, 2, 0), SemVer.parse("1.2"))
        assertEquals(SemVer(1, 0, 0), SemVer.parse("1"))
        // The app's current versionName is "1.0".
        assertEquals(SemVer(1, 0, 0), SemVer.parse("1.0"))
    }

    @Test
    fun `ignores build metadata`() {
        assertEquals(SemVer(1, 2, 3), SemVer.parse("1.2.3+sha.abc123"))
        assertEquals(0L, SemVer.parse("1.2.3+a")!!.compareTo(SemVer.parse("1.2.3+b")!!).toLong())
    }

    @Test
    fun `rejects tags that are not versions`() {
        assertNull(SemVer.parse(null))
        assertNull(SemVer.parse(""))
        assertNull(SemVer.parse("nightly"))
        assertNull(SemVer.parse("latest"))
        // Four numeric components are not a version we ship, and guessing would
        // risk treating a malformed tag as newer than it is.
        assertNull(SemVer.parse("v1.2.3.4"))
        assertNull(SemVer.parse("v1..3"))
        assertNull(SemVer.parse("v1.2.x"))
    }

    @Test
    fun `orders releases numerically not lexicographically`() {
        assertTrue(SemVer.parse("1.10.0")!! > SemVer.parse("1.9.0")!!)
        assertTrue(SemVer.parse("2.0.0")!! > SemVer.parse("1.99.99")!!)
        assertTrue(SemVer.parse("1.0.1")!! > SemVer.parse("1.0.0")!!)
    }

    @Test
    fun `a prerelease ranks below its stable release`() {
        assertTrue(SemVer.parse("1.2.0-rc1")!! < SemVer.parse("1.2.0")!!)
        assertTrue(SemVer.parse("1.2.0")!! > SemVer.parse("1.2.0-beta")!!)
        // Prereleases still beat earlier stable versions.
        assertTrue(SemVer.parse("1.2.0-rc1")!! > SemVer.parse("1.1.9")!!)
    }

    @Test
    fun `parses the beta tags this project ships`() {
        assertEquals(SemVer(0, 1, 1, "beta"), SemVer.parse("v0.1.1-beta"))
        assertEquals(SemVer(0, 1, 1, "beta.1"), SemVer.parse("0.1.1-beta.1"))
        assertTrue(SemVer.parse("0.1.1-beta")!! < SemVer.parse("0.1.1")!!)
        // A beta of the next version still beats the previous stable release.
        assertTrue(SemVer.parse("0.2.0-beta")!! > SemVer.parse("0.1.1")!!)
    }

    @Test
    fun `orders prerelease identifiers by the semver rules`() {
        assertTrue(SemVer.parse("1.0.0-alpha")!! < SemVer.parse("1.0.0-alpha.1")!!)
        // A numeric identifier ranks below an alphanumeric one.
        assertTrue(SemVer.parse("1.0.0-alpha.1")!! < SemVer.parse("1.0.0-alpha.beta")!!)
        assertTrue(SemVer.parse("1.0.0-beta")!! < SemVer.parse("1.0.0-beta.2")!!)
        assertTrue(SemVer.parse("1.0.0-rc.1")!! < SemVer.parse("1.0.0-rc.2")!!)
    }

    @Test
    fun `stable flag reflects the prerelease suffix`() {
        assertTrue(SemVer.parse("v1.0.0")!!.isStable)
        assertTrue(!SemVer.parse("v1.0.0-rc1")!!.isStable)
    }

    @Test
    fun `installed 1_0 is not older than the published engine 0_1_0`() {
        val installed = SemVer.parse("1.0")!!
        val published = SemVer.parse("v0.1.0")!!
        assertTrue(published <= installed)
    }

    @Test
    fun `toString round-trips the canonical form`() {
        assertEquals("1.2.3", SemVer(1, 2, 3).toString())
        assertEquals("1.2.3-rc1", SemVer(1, 2, 3, "rc1").toString())
    }
}
