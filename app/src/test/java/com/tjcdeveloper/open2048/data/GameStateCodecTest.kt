package com.tjcdeveloper.open2048.data

import com.tjcdeveloper.open2048.game.GameState
import com.tjcdeveloper.open2048.game.Tile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GameStateCodecTest {

    private fun stateOf(size: Int, vararg cells: Int, score: Int = 0): GameState {
        val tiles = cells.toList().mapIndexedNotNull { index, value ->
            if (value == 0) null else Tile(id = index + 1L, value = value, row = index / size, col = index % size)
        }
        return GameState(size = size, tiles = tiles, score = score, nextId = cells.size + 1L)
    }

    @Test
    fun `encode and decode round-trip preserves board and score`() {
        val state = stateOf(
            4,
            2, 0, 0, 4,
            0, 8, 0, 0,
            0, 0, 16, 0,
            0, 0, 0, 2048,
            score = 3116,
        )
        val decoded = GameStateCodec.decode(GameStateCodec.encode(state), idStart = 100L)
        assertNotNull(decoded)
        assertEquals(state.size, decoded!!.size)
        assertEquals(state.score, decoded.score)
        assertEquals(
            state.activeTiles.map { Triple(it.row, it.col, it.value) }.toSet(),
            decoded.activeTiles.map { Triple(it.row, it.col, it.value) }.toSet(),
        )
    }

    @Test
    fun `decode regenerates ids from idStart`() {
        val encoded = GameStateCodec.encode(stateOf(4, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
        val decoded = GameStateCodec.decode(encoded, idStart = 50L)!!
        assertEquals(listOf(50L, 51L), decoded.activeTiles.map { it.id })
        assertEquals(52L, decoded.nextId)
    }

    @Test
    fun `decode rejects malformed strings`() {
        listOf(
            "",
            "garbage",
            "4;100",
            "4;100;1,2;extra",
            "x;100;" + List(16) { "0" }.joinToString(","),
            "4;x;" + List(16) { "0" }.joinToString(","),
            "4;100;" + List(15) { "0" }.joinToString(","),
            "1;100;0",
        ).forEach { encoded ->
            assertNull("expected rejection of \"$encoded\"", GameStateCodec.decode(encoded, idStart = 1L))
        }
    }

    @Test
    fun `decode rejects a negative score`() {
        val encoded = "4;-50;" + List(16) { "0" }.joinToString(",")
        assertNull(GameStateCodec.decode(encoded, idStart = 1L))
    }

    @Test
    fun `decode rejects tile values the game cannot produce`() {
        listOf(3, 1, -2, 6, 100).forEach { bad ->
            val cells = MutableList(16) { 0 }.apply { this[0] = bad }
            val encoded = "4;0;" + cells.joinToString(",")
            assertNull("expected rejection of tile value $bad", GameStateCodec.decode(encoded, idStart = 1L))
        }
    }
}
