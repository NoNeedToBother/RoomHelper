package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class EditPanel() : JPanel() {

    private var entity: Parsed? = null

    private var editBuffer: Parsed? = null

    private var prevPanel: JBScrollPane? = null

    init {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        background = JBColor.background()

        add(JLabel("Edit entity").apply {
            font = font.deriveFont(Font.BOLD, 16f)
            border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
        }, BorderLayout.NORTH)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            add(JButton("Save").apply {
                addActionListener {

                }
            })
        }

        add(buttonPanel, BorderLayout.SOUTH)
    }

    fun changeEntity(entity: Parsed) {
        this.entity = entity
        this.editBuffer = when(entity) {
            is Parsed.Entity -> entity.copy()
            is Parsed.Embedded -> entity.copy()
            is Parsed.ManyToMany -> entity.copy()
        }

        paintEditMenu(entity)
    }

    private fun paintEditMenu(entity: Parsed) {
        prevPanel?.let { remove(it) }
        val contentPanel = JPanel(GridLayout(0, 2, 5, 5)).apply {
            background = JBColor.background()

            entity.fields.forEach { field ->
                add(JLabel(field.name).apply {
                    foreground = JBColor.foreground()
                })
                add(JTextField(field.type).apply {
                    toolTipText = "Edit field type"
                })
            }
        }

        JBScrollPane(contentPanel).let {
            add(it, BorderLayout.CENTER)
            prevPanel = it
        }
    }
}
