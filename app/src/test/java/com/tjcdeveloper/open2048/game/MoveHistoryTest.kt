package com.tjcdeveloper.open2048.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveHistoryTest {

    @Test
    fun `undo returns states in reverse order`() {
        val history = MoveHistory<Int>(6)
        history.record(1)
        history.record(2)
        assertEquals(2, history.undo(3))
        assertEquals(1, history.undo(2))
        assertNull(history.undo(1))
    }

    @Test
    fun `history is capped at six moves`() {
        val history = MoveHistory<Int>(6)
        (1..10).forEach(history::record)

        val undone = mutableListOf<Int>()
        var current = 11
        while (history.canUndo) {
            val previous = history.undo(current)!!
            undone += previous
            current = previous
        }
        assertEquals(listOf(10, 9, 8, 7, 6, 5), undone)
    }

    @Test
    fun `redo replays undone moves`() {
        val history = MoveHistory<Int>(6)
        history.record(1)
        history.record(2)
        var current = 3
        current = history.undo(current)!!
        current = history.undo(current)!!
        assertEquals(1, current)
        assertEquals(2, history.redo(current))
        assertEquals(3, history.redo(2))
        assertFalse(history.canRedo)
    }

    @Test
    fun `new move clears the redo stack`() {
        val history = MoveHistory<Int>(6)
        history.record(1)
        history.undo(2)
        assertTrue(history.canRedo)
        history.record(1)
        assertFalse(history.canRedo)
    }

    @Test
    fun `restore respects the cap`() {
        val history = MoveHistory<Int>(6)
        history.restore(undo = (1..10).toList(), redo = emptyList())
        val undone = mutableListOf<Int>()
        var current = 11
        while (history.canUndo) {
            val previous = history.undo(current)!!
            undone += previous
            current = previous
        }
        assertEquals(6, undone.size)
        assertEquals(listOf(10, 9, 8, 7, 6, 5), undone)
    }

    @Test
    fun `redo enforces the undo cap after an over-sized restore`() {
        val history = MoveHistory<Int>(6)
        history.restore(undo = (1..6).toList(), redo = (7..12).toList())
        var current = 13
        while (history.canRedo) current = history.redo(current)!!

        val undone = mutableListOf<Int>()
        while (history.canUndo) {
            val previous = history.undo(current)!!
            undone += previous
            current = previous
        }
        assertEquals(6, undone.size)
    }

    @Test
    fun `full undo redo cycle keeps stacks within cap`() {
        val history = MoveHistory<Int>(6)
        (1..8).forEach(history::record)
        var current = 9
        repeat(6) { current = history.undo(current)!! }
        assertFalse(history.canUndo)
        repeat(6) { current = history.redo(current)!! }
        assertFalse(history.canRedo)
        assertEquals(9, current)
    }
}
