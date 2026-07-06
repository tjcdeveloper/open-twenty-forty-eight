package com.tjcdeveloper.open2048.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.IOException

enum class ThemePreference { LIGHT, DARK, SYSTEM }

data class SavedData(
    val theme: ThemePreference,
    val gridSize: Int,
    val bestScores: Map<Int, Int>,
    val gameEncoded: String?,
    val undoEncoded: List<String>,
    val redoEncoded: List<String>,
    val winAcknowledged: Boolean,
    /** True when these are fallback defaults after a failed read, not real stored data. */
    val degraded: Boolean = false,
)

private const val TAG = "GameRepository"

// A corrupted preferences file must degrade to first-launch defaults, never a crash loop.
private val Context.dataStore by preferencesDataStore(
    name = "open2048",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

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
        val prefs = readPrefsOrNull() ?: return SavedData(
            theme = ThemePreference.SYSTEM,
            gridSize = 4,
            bestScores = listOf(4, 5, 6).associateWith { 0 },
            gameEncoded = null,
            undoEncoded = emptyList(),
            redoEncoded = emptyList(),
            winAcknowledged = false,
            degraded = true,
        )
        return SavedData(
            theme = themeFrom(prefs),
            gridSize = (prefs[Keys.gridSize] ?: 4).coerceIn(4, 6),
            bestScores = listOf(4, 5, 6).associateWith { prefs[Keys.best(it)] ?: 0 },
            gameEncoded = prefs[Keys.game],
            undoEncoded = prefs[Keys.undoStack]?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
            redoEncoded = prefs[Keys.redoStack]?.split("|")?.filter { it.isNotBlank() } ?: emptyList(),
            winAcknowledged = prefs[Keys.winAcknowledged] ?: false,
        )
    }

    /**
     * Synchronous theme read used once at startup so the first frame and window
     * background match the saved preference (avoids a light flash on the dark theme).
     * The file is tiny and DataStore caches it for the async [load] that follows.
     * Single attempt, no retries: this blocks the main thread, and a failed read
     * only costs the default theme for one frame, not data.
     */
    fun loadThemeBlocking(): ThemePreference = runBlocking {
        val prefs = try {
            context.dataStore.data.first()
        } catch (e: IOException) {
            emptyPreferences()
        }
        themeFrom(prefs)
    }

    suspend fun save(data: SavedData) {
        try {
            context.dataStore.edit { prefs ->
                prefs[Keys.theme] = data.theme.name
                prefs[Keys.gridSize] = data.gridSize
                // Never lower a stored best score: if a failed read degraded the in-memory
                // state to defaults, a later save must not wipe the real records.
                data.bestScores.forEach { (size, best) ->
                    prefs[Keys.best(size)] = maxOf(prefs[Keys.best(size)] ?: 0, best)
                }
                data.gameEncoded?.let { prefs[Keys.game] = it }
                prefs[Keys.undoStack] = data.undoEncoded.joinToString("|")
                prefs[Keys.redoStack] = data.redoEncoded.joinToString("|")
                prefs[Keys.winAcknowledged] = data.winAcknowledged
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to persist game data; keeping in-memory state", e)
        }
    }

    // Retry before giving up: a transient read failure must not present the app as
    // freshly installed. Callers get null (not defaults) so they can tell the
    // difference and avoid overwriting good on-disk data.
    private suspend fun readPrefsOrNull(): Preferences? {
        var lastError: IOException? = null
        repeat(3) { attempt ->
            try {
                return context.dataStore.data.first()
            } catch (e: IOException) {
                lastError = e
                delay(250L * (attempt + 1))
            }
        }
        Log.w(TAG, "Failed to read saved data after retries; using defaults", lastError)
        return null
    }

    private fun themeFrom(prefs: Preferences): ThemePreference =
        prefs[Keys.theme]?.let { name ->
            ThemePreference.entries.firstOrNull { it.name == name }
        } ?: ThemePreference.SYSTEM
}
