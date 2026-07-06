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
    fun `nearest pair to the edge merges first when sliding right`() {
        val state = stateOf(
            4,
            2, 4, 2, 2,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val result = GameEngine.slide(state, Direction.RIGHT)!!
        assertEquals(
            listOf(
                0, 2, 4, 4,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, 0,
            ),
            values(result),
        )
        // The rightmost tile of the pair (id 4, nearest the target edge) keeps its id.
        assertEquals(4L, result.tiles.first { it.justMerged }.id)
        assertEquals(3L, result.tiles.first { it.consumed }.id)
    }

    @Test
    fun `nearest pair to the edge merges first when sliding down`() {
        val state = stateOf(
            4,
            2, 0, 0, 0,
            4, 0, 0, 0,
            2, 0, 0, 0,
            2, 0, 0, 0,
        )
        val result = GameEngine.slide(state, Direction.DOWN)!!
        assertEquals(
            listOf(
                0, 0, 0, 0,
                2, 0, 0, 0,
                4, 0, 0, 0,
                4, 0, 0, 0,
            ),
            values(result),
        )
        // The bottom tile of the pair (id 13, nearest the target edge) keeps its id.
        assertEquals(13L, result.tiles.first { it.justMerged }.id)
        assertEquals(9L, result.tiles.first { it.consumed }.id)
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
    fun `slide clears stale justMerged and justSpawned flags`() {
        val state = GameState(
            size = 4,
            tiles = listOf(
                Tile(id = 1, value = 4, row = 0, col = 0, justMerged = true),
                Tile(id = 2, value = 2, row = 1, col = 0, justSpawned = true),
            ),
            score = 4,
            nextId = 3L,
        )
        val result = GameEngine.slide(state, Direction.RIGHT)!!
        assertTrue(result.tiles.none { it.justMerged || it.justSpawned })
    }

    @Test
    fun `spawns are mostly 2s with occasional 4s`() {
        val rng = Random(42)
        val empty = GameState(size = 4, tiles = emptyList(), score = 0, nextId = 1L)
        val values = (1..200).map { GameEngine.spawnTile(empty, rng).tiles.single().value }
        assertTrue(values.all { it == 2 || it == 4 })
        val fours = values.count { it == 4 }
        assertTrue("expected some 4s, got $fours", fours > 0)
        assertTrue("expected mostly 2s, got $fours fours", fours < values.size / 2)
    }

    @Test
    fun `new game starts tile ids at the given floor`() {
        val state = GameEngine.newGame(4, Random(1), idStart = 100L)
        assertTrue(state.activeTiles.all { it.id >= 100L })
        assertEquals(102L, state.nextId)
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
    fun `score accumulates onto the existing score`() {
        val state = stateOf(
            4,
            2, 2, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            score = 100,
        )
        val result = GameEngine.slide(state, Direction.LEFT)!!
        assertEquals(104, result.score)
    }

    @Test
    fun `merge keeps the id of the tile nearer the target edge`() {
        val state = stateOf(
            4,
            2, 2, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        )
        val result = GameEngine.slide(state, Direction.LEFT)!!
        // stateOf assigns id 1 to (0,0) and id 2 to (0,1); sliding left the
        // edge-nearest tile (id 1) survives and id 2 is consumed.
        assertEquals(1L, result.tiles.first { it.justMerged }.id)
        assertEquals(2L, result.tiles.first { it.consumed }.id)
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
    fun `can move on a sparse board with no possible merges`() {
        val state = stateOf(
            4,
            2, 0, 0, 0,
            0, 4, 0, 0,
            0, 0, 8, 0,
            0, 0, 0, 0,
        )
        assertTrue(GameEngine.canMove(state))
    }

    @Test
    fun `not game over when the only merge on a full board is vertical`() {
        val full = stateOf(
            4,
            2, 4, 8, 16,
            2, 8, 16, 32,
            4, 16, 32, 64,
            8, 32, 64, 128,
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
