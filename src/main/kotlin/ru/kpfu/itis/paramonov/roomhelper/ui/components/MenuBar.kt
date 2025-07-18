package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.JBMenuItem
import ru.kpfu.itis.paramonov.roomhelper.ui.model.DatabaseStateHistory
import ru.kpfu.itis.paramonov.roomhelper.util.openFileChooser
import javax.swing.JMenu
import javax.swing.JMenuBar

class MenuBar(
    private val window: DialogWrapper,
    private val onUpdate: (MenuBar) -> Unit,
    private val history: DatabaseStateHistory,
    private val onFileChosen: (String) -> Unit,
    private val onNewFileClicked: () -> Unit,
    private val onSaveClicked: () -> Unit,
    private val onAddEntityClicked: (String, EntityType) -> Unit,
    private val isNameUnique: (String) -> Boolean,
): JMenuBar() {

    private var undoButton: JBMenuItem? = null
    private var redoButton: JBMenuItem? = null

    init {
        add(fileMenu())
        add(editMenu())
    }

    fun changeUndoButton(enabled: Boolean) {
        undoButton?.isEnabled = enabled
    }

    fun changeRedoButton(enabled: Boolean) {
        redoButton?.isEnabled = enabled
    }

    private fun fileMenu(): JMenu {
        return JMenu("File").apply {
            add(JBMenuItem("New").apply {
                addActionListener {
                    onNewFileClicked()
                }
            })
            add(JBMenuItem("Open").apply {
                addActionListener {
                    openFileChooser {
                        onFileChosen(it)
                    }
                }
            })
            addSeparator()
            add(JBMenuItem("Save").apply {
                addActionListener {
                    onSaveClicked()
                }
            })
            addSeparator()
            add(JBMenuItem("Exit").apply {
                addActionListener {
                    window.disposeIfNeeded()
                }
            })
        }
    }

    private fun editMenu(): JMenu {
        return JMenu("Edit").apply menu@ {
            add(JBMenuItem("Add entity").apply {
                addActionListener {
                    AddEntityDialog(
                        onChosen = onAddEntityClicked,
                        isNameUnique = isNameUnique
                    ).show()
                }
            })
            addSeparator()
            add(JBMenuItem("Undo").apply {
                undoButton = this
                isEnabled = !history.isFirstState
                addActionListener {
                    history.undo()
                    changeRedoButton(!history.isLastState)
                    changeUndoButton(!history.isFirstState)
                    onUpdate(this@MenuBar)
                }
            })
            add(JBMenuItem("Redo").apply {
                redoButton = this
                isEnabled = !history.isLastState
                addActionListener {
                    history.redo()
                    changeRedoButton(!history.isLastState)
                    changeUndoButton(!history.isFirstState)
                    onUpdate(this@MenuBar)
                }
            })
        }
    }
}
