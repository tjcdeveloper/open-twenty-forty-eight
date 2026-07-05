package com.tjcdeveloper.open2048.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DoubleBackToExitTest {

    @Test
    fun `first press does not exit`() {
        val tracker = DoubleBackToExit(windowMillis = 5_000L)
        assertFalse(tracker.onBackPressed(nowMillis = 0L))
    }

    @Test
    fun `second press within window exits`() {
        val tracker = DoubleBackToExit(windowMillis = 5_000L)
        tracker.onBackPressed(nowMillis = 0L)
        assertTrue(tracker.onBackPressed(nowMillis = 4_999L))
    }

    @Test
    fun `second press exactly at window boundary exits`() {
        val tracker = DoubleBackToExit(windowMillis = 5_000L)
        tracker.onBackPressed(nowMillis = 0L)
        assertTrue(tracker.onBackPressed(nowMillis = 5_000L))
    }

    @Test
    fun `second press after window re-arms instead of exiting`() {
        val tracker = DoubleBackToExit(windowMillis = 5_000L)
        tracker.onBackPressed(nowMillis = 0L)
        assertFalse(tracker.onBackPressed(nowMillis = 5_001L))
    }

    @Test
    fun `press within window of a re-armed press exits`() {
        val tracker = DoubleBackToExit(windowMillis = 5_000L)
        tracker.onBackPressed(nowMillis = 0L)
        tracker.onBackPressed(nowMillis = 10_000L)
        assertTrue(tracker.onBackPressed(nowMillis = 13_000L))
    }

    @Test
    fun `expired presses never accumulate into an exit`() {
        val tracker = DoubleBackToExit(windowMillis = 5_000L)
        assertFalse(tracker.onBackPressed(nowMillis = 0L))
        assertFalse(tracker.onBackPressed(nowMillis = 6_000L))
        assertFalse(tracker.onBackPressed(nowMillis = 12_000L))
    }
}
