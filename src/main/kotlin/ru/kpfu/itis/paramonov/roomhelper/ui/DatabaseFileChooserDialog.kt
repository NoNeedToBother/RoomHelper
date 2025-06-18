package ru.kpfu.itis.paramonov.roomhelper.ui

import com.intellij.openapi.ui.DialogWrapper
import ru.kpfu.itis.paramonov.roomhelper.util.openFileChooser
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JButton

class DatabaseFileChooserDialog : DialogWrapper(true) {

    private var result: FileOpenResult = FileOpenResult.NoFile

    init {
        title = "Choose .rh File"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        val selectButton = JButton("Choose file").apply {
            addActionListener {
                openFileChooser { file ->
                    this@DatabaseFileChooserDialog.result = FileOpenResult.Existing(file)
                }
            }
        }
        val newButton = JButton("New").apply {
            addActionListener {
                this@DatabaseFileChooserDialog.result = FileOpenResult.New
                doOKAction()
            }
        }

        panel.add(selectButton)
        panel.add(newButton)
        return panel
    }

    fun showAndGetResult(): FileOpenResult {
        return if (showAndGet()) result else FileOpenResult.NoFile
    }
}

sealed interface FileOpenResult {
    object New : FileOpenResult
    object NoFile : FileOpenResult
    data class Existing(val file: String) : FileOpenResult
}
