package com.tjcdeveloper.open2048.game

import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

/**
 * A tile on the board. [consumed] marks a tile that was merged into another this
 * move — it is kept for one state so the UI can animate it sliding into the merge
 * target, and is pruned on the next move.
 */
data class Tile(
    val id: Long,
    val value: Int,
    val row: Int,
    val col: Int,
    val justMerged: Boolean = false,
    val justSpawned: Boolean = false,
    val consumed: Boolean = false,
)

data class GameState(
    val size: Int,
    val tiles: List<Tile>,
    val score: Int,
    val nextId: Long,
) {
    val activeTiles: List<Tile> get() = tiles.filter { !it.consumed }
}

object GameEngine {

    const val WINNING_VALUE = 2048

    /**
     * [idStart] lets callers keep ids monotonic across games: Compose keys tiles by id,
     * so a new game reusing an id still on the outgoing board would recycle that tile's
     * composition and glide it across the board instead of playing the spawn pop.
     */
    fun newGame(size: Int, rng: Random = Random.Default, idStart: Long = 1L): GameState {
        var state = GameState(size = size, tiles = emptyList(), score = 0, nextId = idStart)
        repeat(2) { state = spawnTile(state, rng) }
        return state
    }

    /** Applies a full move: slide + merge, then spawn. Returns null if nothing moved. */
    fun move(state: GameState, direction: Direction, rng: Random = Random.Default): GameState? {
        val slid = slide(state, direction) ?: return null
        return spawnTile(slid, rng)
    }

    /** Slide + merge only (no spawn). Returns null if the move changes nothing. */
    fun slide(state: GameState, direction: Direction): GameState? {
        val n = state.size
        val grid = Array(n) { arrayOfNulls<Tile>(n) }
        state.activeTiles
            .map { it.copy(justMerged = false, justSpawned = false) }
            .forEach { grid[it.row][it.col] = it }

        var movedAny = false
        var gained = 0
        val result = mutableListOf<Tile>()

        for (line in 0 until n) {
            val tiles = lineTiles(grid, direction, line, n)
            var slot = 0
            var i = 0
            while (i < tiles.size) {
                val current = tiles[i]
                val mergeWith = tiles.getOrNull(i + 1)?.takeIf { it.value == current.value }
                val (row, col) = cellAt(direction, line, slot, n)
                if (mergeWith != null) {
                    gained += current.value * 2
                    result += current.copy(value = current.value * 2, row = row, col = col, justMerged = true)
                    result += mergeWith.copy(row = row, col = col, consumed = true)
                    movedAny = true
                    i += 2
                } else {
                    movedAny = movedAny || current.row != row || current.col != col
                    result += current.copy(row = row, col = col)
                    i += 1
                }
                slot += 1
            }
        }

        if (!movedAny) return null
        return state.copy(tiles = result, score = state.score + gained)
    }

    fun spawnTile(state: GameState, rng: Random = Random.Default): GameState {
        val occupied = state.activeTiles.map { it.row * state.size + it.col }.toSet()
        val empty = (0 until state.size * state.size).filter { it !in occupied }
        if (empty.isEmpty()) return state
        val cell = empty[rng.nextInt(empty.size)]
        val tile = Tile(
            id = state.nextId,
            value = if (rng.nextInt(10) == 0) 4 else 2,
            row = cell / state.size,
            col = cell % state.size,
            justSpawned = true,
        )
        return state.copy(tiles = state.tiles + tile, nextId = state.nextId + 1)
    }

    fun canMove(state: GameState): Boolean {
        val n = state.size
        val values = Array(n) { IntArray(n) }
        state.activeTiles.forEach { values[it.row][it.col] = it.value }
        for (r in 0 until n) {
            for (c in 0 until n) {
                val v = values[r][c]
                if (v == 0) return true
                if (c + 1 < n && values[r][c + 1] == v) return true
                if (r + 1 < n && values[r + 1][c] == v) return true
            }
        }
        return false
    }

    fun hasWon(state: GameState): Boolean = state.activeTiles.any { it.value >= WINNING_VALUE }

    /** Tiles of one row/column, ordered starting from the edge tiles slide toward. */
    private fun lineTiles(grid: Array<Array<Tile?>>, direction: Direction, line: Int, n: Int): List<Tile> {
        val indices = when (direction) {
            Direction.LEFT, Direction.UP -> 0 until n
            Direction.RIGHT, Direction.DOWN -> (n - 1) downTo 0
        }
        return indices.mapNotNull { i ->
            when (direction) {
                Direction.LEFT, Direction.RIGHT -> grid[line][i]
                Direction.UP, Direction.DOWN -> grid[i][line]
            }
        }
    }

    /** Board cell for the [slot]-th resting position in a line, counted from the target edge. */
    private fun cellAt(direction: Direction, line: Int, slot: Int, n: Int): Pair<Int, Int> = when (direction) {
        Direction.LEFT -> line to slot
        Direction.RIGHT -> line to (n - 1 - slot)
        Direction.UP -> slot to line
        Direction.DOWN -> (n - 1 - slot) to line
    }
}
