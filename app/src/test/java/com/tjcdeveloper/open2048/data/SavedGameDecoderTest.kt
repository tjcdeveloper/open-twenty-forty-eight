package com.tjcdeveloper.open2048.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedGameDecoderTest {

    private fun encoded(size: Int, vararg cells: Int) = "$size;0;${cells.joinToString(",")}"

    private fun saved(
        game: String?,
        undo: List<String> = emptyList(),
        redo: List<String> = emptyList(),
        gridSize: Int = 4,
    ) = SavedData(
        theme = ThemePreference.SYSTEM,
        gridSize = gridSize,
        bestScores = emptyMap(),
        gameEncoded = game,
        undoEncoded = undo,
        redoEncoded = redo,
        winAcknowledged = false,
    )

    private val board = intArrayOf(
        2, 0, 0, 4,
        0, 0, 0, 0,
        0, 0, 0, 0,
        0, 0, 0, 2,
    )

    @Test
    fun `tile ids are unique across game and history states`() {
        val entry = encoded(4, *board)
        val result = SavedGameDecoder.decode(saved(game = entry, undo = listOf(entry, entry), redo = listOf(entry)))
        assertNotNull(result.game)
        val allIds = (result.undo + result.redo + result.game!!).flatMap { state -> state.tiles.map { it.id } }
        assertEquals("ids must be unique across all decoded states", allIds.size, allIds.toSet().size)
        // New spawns draw from the live game's nextId, which must sit above every id.
        assertTrue(allIds.all { it < result.game!!.nextId })
    }

    @Test
    fun `game with mismatched grid size is discarded`() {
        val result = SavedGameDecoder.decode(saved(game = encoded(4, *board), gridSize = 5))
        assertNull(result.game)
        assertTrue(result.undo.isEmpty() && result.redo.isEmpty())
    }

    @Test
    fun `missing game yields empty result`() {
        val result = SavedGameDecoder.decode(saved(game = null, undo = listOf(encoded(4, *board))))
        assertNull(result.game)
        assertTrue(result.undo.isEmpty())
    }

    @Test
    fun `history entries of a different size are filtered out`() {
        val fiveByFive = encoded(5, *IntArray(25) { if (it == 0) 2 else 0 })
        val result = SavedGameDecoder.decode(
            saved(game = encoded(4, *board), undo = listOf(fiveByFive, encoded(4, *board))),
        )
        assertNotNull(result.game)
        assertEquals(1, result.undo.size)
        assertEquals(4, result.undo.single().size)
    }

    @Test
    fun `corrupt history entries are skipped but the game still loads`() {
        val result = SavedGameDecoder.decode(
            saved(game = encoded(4, *board), undo = listOf("garbage", encoded(4, *board))),
        )
        assertNotNull(result.game)
        assertEquals(1, result.undo.size)
    }
}
