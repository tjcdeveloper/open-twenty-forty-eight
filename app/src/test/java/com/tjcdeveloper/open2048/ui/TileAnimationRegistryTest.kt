package com.tjcdeveloper.open2048.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileAnimationRegistryTest {

    @Test
    fun `spawn animation can only be claimed once per tile`() {
        val registry = TileAnimationRegistry()
        assertFalse(registry.spawnPlayed(1L))
        assertTrue(registry.claimSpawn(1L))
        assertTrue(registry.spawnPlayed(1L))
        assertFalse(registry.claimSpawn(1L))
    }

    @Test
    fun `merge animation is claimed per tile and value`() {
        val registry = TileAnimationRegistry()
        assertTrue(registry.claimMerge(1L, 4))
        assertFalse(registry.claimMerge(1L, 4))
        // The same surviving tile can merge again to a higher value.
        assertTrue(registry.claimMerge(1L, 8))
    }

    @Test
    fun `clear forgets played animations`() {
        val registry = TileAnimationRegistry()
        registry.claimSpawn(1L)
        registry.clear()
        assertTrue(registry.claimSpawn(1L))
    }
}
