package com.tjcdeveloper.open2048.data

import com.tjcdeveloper.open2048.game.GameState
import com.tjcdeveloper.open2048.game.Tile

/** Serializes a game state as "size;score;v0,v1,..." (row-major cell values, 0 = empty). */
object GameStateCodec {

    fun encode(state: GameState): String {
        val cells = IntArray(state.size * state.size)
        state.activeTiles.forEach { cells[it.row * state.size + it.col] = it.value }
        return "${state.size};${state.score};${cells.joinToString(",")}"
    }

    fun decode(encoded: String, idStart: Long): GameState? {
        val parts = encoded.split(";")
        if (parts.size != 3) return null
        val size = parts[0].toIntOrNull() ?: return null
        val score = parts[1].toIntOrNull() ?: return null
        val cells = parts[2].split(",").map { it.toIntOrNull() ?: return null }
        if (size < 2 || cells.size != size * size) return null

        var id = idStart
        val tiles = cells.mapIndexedNotNull { index, value ->
            if (value == 0) null else Tile(id = id++, value = value, row = index / size, col = index % size)
        }
        return GameState(size = size, tiles = tiles, score = score, nextId = id)
    }
}
