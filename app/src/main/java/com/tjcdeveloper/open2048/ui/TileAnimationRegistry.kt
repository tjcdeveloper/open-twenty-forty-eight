package com.tjcdeveloper.open2048.ui

/**
 * Remembers which tile spawn/merge animations have already played. Tile flags
 * (justSpawned/justMerged) stay set on the game state until the next slide, so a
 * recomposition from scratch — fold layout swap, Settings round trip — would replay
 * them without this registry. Lives on the ViewModel to survive navigation.
 */
class TileAnimationRegistry {

    private val spawns = HashSet<Long>()
    private val merges = HashSet<Pair<Long, Int>>()

    fun spawnPlayed(id: Long): Boolean = id in spawns

    /** Marks the spawn animation as played; returns true only the first time. */
    fun claimSpawn(id: Long): Boolean = spawns.add(id)

    /** Marks the merge-to-[value] animation as played; returns true only the first time. */
    fun claimMerge(id: Long, value: Int): Boolean = merges.add(id to value)

    fun clear() {
        spawns.clear()
        merges.clear()
    }
}
