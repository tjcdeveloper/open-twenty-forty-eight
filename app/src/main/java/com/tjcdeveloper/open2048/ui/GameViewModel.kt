package com.tjcdeveloper.open2048.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.tjcdeveloper.open2048.data.GameRepository
import com.tjcdeveloper.open2048.data.GameStateCodec
import com.tjcdeveloper.open2048.data.SavedData
import com.tjcdeveloper.open2048.data.SavedGameDecoder
import com.tjcdeveloper.open2048.data.ThemePreference
import com.tjcdeveloper.open2048.game.Direction
import com.tjcdeveloper.open2048.game.GameEngine
import com.tjcdeveloper.open2048.game.GameState
import com.tjcdeveloper.open2048.game.MoveHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

const val MAX_UNDO_HISTORY = 6

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameRepository(application)
    private val history = MoveHistory<GameState>(MAX_UNDO_HISTORY)

    // Saves must outlive the ViewModel: DataStore runs a queued write's transform on the
    // caller's Job, so a save launched in viewModelScope just before the activity finishes
    // would be cancelled and silently dropped, losing the last move or a new best score.
    // Parallelism 1 keeps saves FIFO in user-action order — on the unbounded IO pool two
    // saves could race and persist an older snapshot over a newer one.
    @OptIn(ExperimentalCoroutinesApi::class)
    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

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

    // False after a degraded (failed-read) load: the in-memory defaults are not the
    // user's real data, and persisting them would permanently overwrite the good save.
    private var persistAllowed = true

    val gridSize: Int get() = game.size
    val bestScore: Int get() = bestScores[game.size] ?: 0
    val isGameOver: Boolean get() = !GameEngine.canMove(game)
    val hasProgressToLose: Boolean get() = game.score > 0 && !isGameOver
    val showWinOverlay: Boolean get() = GameEngine.hasWon(game) && !winAcknowledged

    init {
        viewModelScope.launch {
            val saved = repository.load()
            theme = saved.theme
            bestScores = saved.bestScores
            winAcknowledged = saved.winAcknowledged
            persistAllowed = !saved.degraded

            val decoded = SavedGameDecoder.decode(saved)
            if (decoded.game != null) {
                game = decoded.game
                history.restore(decoded.undo, decoded.redo)
            } else {
                game = GameEngine.newGame(saved.gridSize)
            }
            syncHistoryFlags()
            isLoaded = true
        }
    }

    /** Applies a move; returns false when it changed nothing (blocked or win overlay up). */
    fun move(direction: Direction): Boolean {
        if (showWinOverlay) return false
        val next = GameEngine.move(game, direction) ?: return false
        history.record(game)
        game = next
        updateBestScore()
        syncHistoryFlags()
        persist()
        return true
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
        game = GameEngine.newGame(gridSize, idStart = game.nextId)
        syncHistoryFlags()
        persist()
    }

    fun setGridSize(size: Int) {
        if (size == gridSize) return
        history.clear()
        winAcknowledged = false
        game = GameEngine.newGame(size, idStart = game.nextId)
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
        if (!persistAllowed) {
            Log.w("GameViewModel", "Skipping save: loaded data was degraded; not overwriting the stored game")
            return
        }
        val snapshot = SavedData(
            theme = theme,
            gridSize = gridSize,
            bestScores = bestScores,
            gameEncoded = GameStateCodec.encode(game),
            undoEncoded = history.undoSnapshot().map(GameStateCodec::encode),
            redoEncoded = history.redoSnapshot().map(GameStateCodec::encode),
            winAcknowledged = winAcknowledged,
        )
        saveScope.launch { repository.save(snapshot) }
    }
}
