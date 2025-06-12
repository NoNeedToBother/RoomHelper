package ru.kpfu.itis.paramonov.roomhelper.ui

import com.intellij.openapi.ui.DialogWrapper
import ru.kpfu.itis.paramonov.roomhelper.util.openFileChooser
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JButton

class DatabaseFileChooserDialog : DialogWrapper(true) {

    private var file: String? = null

    init {
        title = "Choose .rh File"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()
        val selectButton = JButton("Choose file").apply {
            addActionListener {
                openFileChooser { file ->
                    this@DatabaseFileChooserDialog.file = file
                }
            }
        }

        panel.add(selectButton)
        return panel
    }

    fun showAndGetResult(): String? {
        return if (showAndGet()) file else null
    }
}
