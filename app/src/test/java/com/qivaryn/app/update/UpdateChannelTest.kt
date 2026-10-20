package com.qivaryn.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The channel decides which releases a build may see. Stable ships to everyone,
 * so it must never accidentally accept a prerelease.
 */
class UpdateChannelTest {

    @Test
    fun `stable channel rejects prereleases`() {
        assertFalse(UpdateChannel.STABLE.accepts(prerelease = true))
        assertTrue(UpdateChannel.STABLE.accepts(prerelease = false))
    }

    @Test
    fun `beta channel accepts prereleases and stable releases`() {
        assertTrue(UpdateChannel.BETA.accepts(prerelease = true))
        assertTrue(UpdateChannel.BETA.accepts(prerelease = false))
    }

    @Test
    fun `channel is parsed from the compiled-in value`() {
        assertEquals(UpdateChannel.BETA, UpdateChannel.from("beta"))
        assertEquals(UpdateChannel.BETA, UpdateChannel.from(" BETA "))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.from("stable"))
        // Anything unexpected must fall back to the safer stable-only channel.
        assertEquals(UpdateChannel.STABLE, UpdateChannel.from(null))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.from(""))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.from("nightly"))
    }

    @Test
    fun `display names match the wire values`() {
        assertEquals("stable", UpdateChannel.STABLE.wireName)
        assertEquals("beta", UpdateChannel.BETA.wireName)
        assertEquals("stable release", UpdateChannel.STABLE.description)
        assertEquals("beta release", UpdateChannel.BETA.description)
    }
}
