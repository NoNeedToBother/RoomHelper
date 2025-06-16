package ru.kpfu.itis.paramonov.roomhelper.ui.model

import ru.kpfu.itis.paramonov.roomhelper.model.Parsed

class DatabaseStateHistory {
    private val history = mutableListOf<List<Parsed>>(emptyList())
    private var isInInitialState = true
    private var cursor = 0

    val currentState: List<Parsed> get() = history[cursor]

    val isFirstState: Boolean get() = cursor == 0

    val isLastState: Boolean get() = cursor == history.size - 1

    fun add(new: List<Parsed>) {
        if (isInInitialState) {
            isInInitialState = false
            history.clear()
        } else {
            val remained = ArrayList(history.subList(0, cursor + 1))
            history.clear()
            history.addAll(remained)
            cursor++
        }
        history.add(ArrayList(new))
    }

    fun undo() {
        if (cursor > 0) cursor--
    }

    fun redo() {
        if (cursor < history.size) cursor++
    }

    fun clear() {
        history.clear()
        history.add(emptyList())
        isInInitialState = true
        cursor = 0
    }
}
