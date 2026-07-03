package com.tjcdeveloper.open2048.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    private fun stateOf(size: Int, vararg cells: Int, score: Int = 0): GameState {
        val tiles = cells.toList().mapIndexedNotNull { index, value ->
            if (value == 0) null else Tile(id = index + 1L, value = value, row = index / size, col = index % size)
        }
        return GameState(size = size, tiles = tiles, score = score, nextId = cells.size + 1L)
    }

    private fun values(state: GameState): List<Int> {
        val cells = IntArray(state.size * state.size)
        state.activeTiles.forEach { cells[it.row * state.size + it.col] = it.value }
        return cells.toList()
    }

    @Test
    fun `new game spawns two tiles`() {
        val state = GameEngine.newGame(4, Random(1))
        assertEquals(2, state.activeTiles.size)
        assertTrue(state.activeTiles.all { it.value == 2 || it.value == 4 })
        assertEquals(0, state.score)
    }

    @Test
    fun `slide left merges equal pair and scores`() {
        val state = stateOf(
            4,
            2, 2, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val result = GameEngine.slide(state, Direction.LEFT)
        assertNotNull(result)
        assertEquals(
            listOf(
                4, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
            ),
            values(result!!),
        )
        assertEquals(4, result.score)
    }

    @Test
    fun `merge only happens once per tile per move`() {
        val state = stateOf(
            4,
            2, 2, 2, 2,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val result = GameEngine.slide(state, Direction.LEFT)!!
        assertEquals(
            listOf(
                4, 4, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
            ),
            values(result),
        )
        assertEquals(8, result.score)
    }

    @Test
    fun `nearest pair to the edge merges first`() {
        val state = stateOf(
            4,
            4, 2, 2, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val result = GameEngine.slide(state, Direction.LEFT)!!
        assertEquals(
            listOf(
                4, 4, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
            ),
            values(result),
        )
    }

    @Test
    fun `slide right and down move toward far edges`() {
        val state = stateOf(
            4,
            2, 0, 0, 2,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val right = GameEngine.slide(state, Direction.RIGHT)!!
        assertEquals(4, values(right)[3])

        val down = GameEngine.slide(state, Direction.DOWN)!!
        assertEquals(2, values(down)[12])
        assertEquals(2, values(down)[15])
    }

    @Test
    fun `slide up merges columns`() {
        val state = stateOf(
            4,
            2, 0, 0, 0,
            2, 0, 0, 0,
            4, 0, 0, 0,
            0, 0, 0, 0,
        )
        val result = GameEngine.slide(state, Direction.UP)!!
        assertEquals(4, values(result)[0])
        assertEquals(4, values(result)[4])
        assertEquals(4, result.score)
    }

    @Test
    fun `move returns null when nothing changes`() {
        val state = stateOf(
            4,
            2, 4, 8, 16,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        assertNull(GameEngine.slide(state, Direction.LEFT))
        assertNull(GameEngine.slide(state, Direction.UP))
    }

    @Test
    fun `move spawns exactly one new tile`() {
        val state = stateOf(
            4,
            2, 2, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val result = GameEngine.move(state, Direction.LEFT, Random(7))!!
        assertEquals(2, result.activeTiles.size)
        assertEquals(1, result.activeTiles.count { it.justSpawned })
    }

    @Test
    fun `merged tile keeps consumed partner for animation`() {
        val state = stateOf(
            4,
            2, 2, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val result = GameEngine.slide(state, Direction.LEFT)!!
        val consumed = result.tiles.filter { it.consumed }
        assertEquals(1, consumed.size)
        assertEquals(0, consumed[0].row)
        assertEquals(0, consumed[0].col)
        // Consumed tiles are pruned on the next slide.
        val next = GameEngine.slide(result, Direction.RIGHT)!!
        assertFalse(next.tiles.any { it.value == 2 && it.consumed })
    }

    @Test
    fun `game over when board is full with no adjacent equal tiles`() {
        val full = stateOf(
            4,
            2, 4, 2, 4,
            4, 2, 4, 2,
            2, 4, 2, 4,
            4, 2, 4, 2,
        )
        assertFalse(GameEngine.canMove(full))
        assertNull(GameEngine.slide(full, Direction.LEFT))
    }

    @Test
    fun `not game over when a merge is possible on a full board`() {
        val full = stateOf(
            4,
            2, 2, 4, 8,
            4, 8, 16, 32,
            8, 16, 32, 64,
            16, 32, 64, 128,
        )
        assertTrue(GameEngine.canMove(full))
    }

    @Test
    fun `win detected at 2048`() {
        val state = stateOf(
            4,
            1024, 1024, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        assertFalse(GameEngine.hasWon(state))
        val result = GameEngine.slide(state, Direction.LEFT)!!
        assertTrue(GameEngine.hasWon(result))
    }

    @Test
    fun `works for 5x5 and 6x6 grids`() {
        listOf(5, 6).forEach { n ->
            val state = GameEngine.newGame(n, Random(3))
            assertEquals(n, state.size)
            assertEquals(2, state.activeTiles.size)
            assertTrue(state.activeTiles.all { it.row < n && it.col < n })
        }
    }

    @Test
    fun `spawn does not use cells occupied by active tiles`() {
        var state = GameEngine.newGame(4, Random(11))
        val rng = Random(13)
        repeat(30) {
            for (direction in Direction.entries) {
                val next = GameEngine.move(state, direction, rng) ?: continue
                val positions = next.activeTiles.map { it.row to it.col }
                assertEquals("no two active tiles share a cell", positions.size, positions.toSet().size)
                state = next
                break
            }
        }
    }
}
