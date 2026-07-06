package com.tjcdeveloper.open2048.ui

/**
 * Tracks back presses on the root screen so the app only exits on a double press.
 * The first press arms an exit window; a second press within [windowMillis] confirms
 * the exit. A press after the window has expired re-arms it instead of exiting.
 */
class DoubleBackToExit(private val windowMillis: Long = 5_000L) {

    private var armedAtMillis: Long? = null

    /** Registers a back press at [nowMillis]; returns true when the app should exit. */
    fun onBackPressed(nowMillis: Long): Boolean {
        val armedAt = armedAtMillis
        if (armedAt != null && nowMillis - armedAt <= windowMillis) return true
        armedAtMillis = nowMillis
        return false
    }

    /** Disarms the window, e.g. when the user navigates away from the root screen. */
    fun reset() {
        armedAtMillis = null
    }
}
