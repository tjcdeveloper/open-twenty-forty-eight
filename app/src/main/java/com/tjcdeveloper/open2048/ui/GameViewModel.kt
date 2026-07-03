package com.tjcdeveloper.open2048.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tjcdeveloper.open2048.data.GameRepository
import com.tjcdeveloper.open2048.data.GameStateCodec
import com.tjcdeveloper.open2048.data.SavedData
import com.tjcdeveloper.open2048.data.ThemePreference
import com.tjcdeveloper.open2048.game.Direction
import com.tjcdeveloper.open2048.game.GameEngine
import com.tjcdeveloper.open2048.game.GameState
import com.tjcdeveloper.open2048.game.MoveHistory
import kotlinx.coroutines.launch

const val MAX_UNDO_HISTORY = 6

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)
    private val history = MoveHistory<GameState>(MAX_UNDO_HISTORY)

    var isLoaded by mutableStateOf(false)
        private set
    var game by mutableStateOf(GameEngine.newGame(4))
        private set
    var theme by mutableStateOf(ThemePreference.SYSTEM)
        private set
    var bestScores by mutableStateOf(mapOf<Int, Int>())
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    var winAcknowledged by mutableStateOf(false)
        private set

    val gridSize: Int get() = game.size
    val bestScore: Int get() = bestScores[game.size] ?: 0
    val isGameOver: Boolean get() = !GameEngine.canMove(game)
    val showWinOverlay: Boolean get() = GameEngine.hasWon(game) && !winAcknowledged

    init {
        viewModelScope.launch {
            val saved = repository.load()
            theme = saved.theme
            bestScores = saved.bestScores
            winAcknowledged = saved.winAcknowledged

            // Decode with non-overlapping id ranges so tile keys stay unique across states.
            var idBase = 1L
            fun decode(encoded: String): GameState? {
                val state = GameStateCodec.decode(encoded, idBase) ?: return null
                idBase = state.nextId
                return state
            }

            val undo = saved.undoEncoded.mapNotNull(::decode)
            val redo = saved.redoEncoded.mapNotNull(::decode)
            val savedGame = saved.gameEncoded?.let(::decode)
            if (savedGame != null && savedGame.size == saved.gridSize) {
                game = savedGame
                history.restore(undo.filter { it.size == game.size }, redo.filter { it.size == game.size })
            } else {
                game = GameEngine.newGame(saved.gridSize)
            }
            syncHistoryFlags()
            isLoaded = true
        }
    }

    fun move(direction: Direction) {
        if (showWinOverlay) return
        val next = GameEngine.move(game, direction) ?: return
        history.record(game)
        game = next
        updateBestScore()
        syncHistoryFlags()
        persist()
    }

    fun undo() {
        game = history.undo(game) ?: return
        syncHistoryFlags()
        persist()
    }

    fun redo() {
        game = history.redo(game) ?: return
        updateBestScore()
        syncHistoryFlags()
        persist()
    }

    fun newGame() {
        history.clear()
        winAcknowledged = false
        game = GameEngine.newGame(gridSize)
        syncHistoryFlags()
        persist()
    }

    fun setGridSize(size: Int) {
        if (size == gridSize) return
        history.clear()
        winAcknowledged = false
        game = GameEngine.newGame(size)
        syncHistoryFlags()
        persist()
    }

    fun updateTheme(preference: ThemePreference) {
        theme = preference
        persist()
    }

    fun acknowledgeWin() {
        winAcknowledged = true
        persist()
    }

    private fun updateBestScore() {
        if (game.score <= bestScore) return
        bestScores = bestScores + (game.size to game.score)
    }

    private fun syncHistoryFlags() {
        canUndo = history.canUndo
        canRedo = history.canRedo
    }

    private fun persist() {
        val snapshot = SavedData(
            theme = theme,
            gridSize = gridSize,
            bestScores = bestScores,
            gameEncoded = GameStateCodec.encode(game),
            undoEncoded = history.undoSnapshot().map(GameStateCodec::encode),
            redoEncoded = history.redoSnapshot().map(GameStateCodec::encode),
            winAcknowledged = winAcknowledged,
        )
        viewModelScope.launch { repository.save(snapshot) }
    }
}
