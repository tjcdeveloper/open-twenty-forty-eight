package com.tjcdeveloper.open2048.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

enum class ThemePreference { LIGHT, DARK, SYSTEM }

data class SavedData(
    val theme: ThemePreference,
    val gridSize: Int,
    val bestScores: Map<Int, Int>,
    val gameEncoded: String?,
    val undoEncoded: List<String>,
    val redoEncoded: List<String>,
    val winAcknowledged: Boolean,
)

private val Context.dataStore by preferencesDataStore(name = "open2048")

class GameRepository(private val context: Context) {

    private object Keys {
        val theme = stringPreferencesKey("theme")
        val gridSize = intPreferencesKey("grid_size")
        val game = stringPreferencesKey("game")
        val undoStack = stringPreferencesKey("undo_stack")
        val redoStack = stringPreferencesKey("redo_stack")
        val winAcknowledged = booleanPreferencesKey("win_acknowledged")
        fun best(size: Int) = intPreferencesKey("best_$size")
    }

    suspend fun load(): SavedData {
        val prefs = context.dataStore.data.first()
        return SavedData(
            theme = prefs[Keys.theme]?.let { name ->
                ThemePreference.entries.firstOrNull { it.name == name }
            } ?: ThemePreference.SYSTEM,
            gridSize = prefs[Keys.gridSize] ?: 4,
            bestScores = listOf(4, 5, 6).associateWith { prefs[Keys.best(it)] ?: 0 },
            gameEncoded = prefs[Keys.game],
            undoEncoded = prefs[Keys.undoStack]?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
            redoEncoded = prefs[Keys.redoStack]?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
            winAcknowledged = prefs[Keys.winAcknowledged] ?: false,
        )
    }

    suspend fun save(data: SavedData) {
        context.dataStore.edit { prefs ->
            prefs[Keys.theme] = data.theme.name
            prefs[Keys.gridSize] = data.gridSize
            data.bestScores.forEach { (size, best) -> prefs[Keys.best(size)] = best }
            data.gameEncoded?.let { prefs[Keys.game] = it }
            prefs[Keys.undoStack] = data.undoEncoded.joinToString("|")
            prefs[Keys.redoStack] = data.redoEncoded.joinToString("|")
            prefs[Keys.winAcknowledged] = data.winAcknowledged
        }
    }
}
