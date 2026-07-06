package com.tjcdeveloper.open2048.game

/** Undo/redo stacks capped at [maxSize] recorded moves. */
class MoveHistory<T>(private val maxSize: Int = 6) {
    private val undoStack = ArrayDeque<T>()
    private val redoStack = ArrayDeque<T>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    /** Records the state that existed before a new move. Clears the redo stack. */
    fun record(state: T) {
        undoStack.addLast(state)
        while (undoStack.size > maxSize) undoStack.removeFirst()
        redoStack.clear()
    }

    /** Returns the state to restore, pushing [current] onto the redo stack; null if empty. */
    fun undo(current: T): T? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(current)
        return undoStack.removeLast()
    }

    /** Returns the state to restore, pushing [current] onto the undo stack; null if empty. */
    fun redo(current: T): T? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(current)
        while (undoStack.size > maxSize) undoStack.removeFirst()
        return redoStack.removeLast()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun undoSnapshot(): List<T> = undoStack.toList()

    fun redoSnapshot(): List<T> = redoStack.toList()

    fun restore(undo: List<T>, redo: List<T>) {
        clear()
        undoStack.addAll(undo.takeLast(maxSize))
        redoStack.addAll(redo.takeLast(maxSize))
    }
}
