package com.tjcdeveloper.open2048.data

import com.tjcdeveloper.open2048.game.GameState

/**
 * Decodes a [SavedData] snapshot into game + history states, threading a single id
 * counter through every decode so tile ids never collide across the live game and
 * the undo/redo stacks (Compose keys tiles by id).
 */
object SavedGameDecoder {

    data class Result(
        val game: GameState?,
        val undo: List<GameState>,
        val redo: List<GameState>,
    )

    fun decode(saved: SavedData): Result {
        var idBase = 1L
        fun decodeOne(encoded: String): GameState? {
            val state = GameStateCodec.decode(encoded, idBase) ?: return null
            idBase = state.nextId
            return state
        }

        val undo = saved.undoEncoded.mapNotNull(::decodeOne)
        val redo = saved.redoEncoded.mapNotNull(::decodeOne)
        // The game decodes last so its ids — and the nextId new spawns draw from —
        // sit above everything still reachable through undo/redo.
        val game = saved.gameEncoded?.let(::decodeOne)
        if (game == null || game.size != saved.gridSize) {
            return Result(game = null, undo = emptyList(), redo = emptyList())
        }
        return Result(
            game = game,
            undo = undo.filter { it.size == game.size },
            redo = redo.filter { it.size == game.size },
        )
    }
}
