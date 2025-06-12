package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class EditPanel(
    onSave: (Parsed) -> Unit,
) : JPanel() {

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
                    editBuffer?.let { onSave(it) }
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
        val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = JBColor.background()

            val fieldHeight = 50
            entity.fields.forEach { field ->
                val fieldPanel = JPanel(BorderLayout()).apply {
                    preferredSize = Dimension(300, fieldHeight)
                    maximumSize = Dimension(300, fieldHeight)
                    background = JBColor.background()

                    add(JButton(AllIcons.Actions.DeleteTag).apply {
                        toolTipText = "Delete"
                        border = BorderFactory.createEmptyBorder()
                        background = JBColor.RED
                        addActionListener {
                            editBuffer?.fields?.toMutableList()?.remove(field)
                        }
                    })

                    add(JLabel(field.name).apply {
                        foreground = JBColor.foreground()
                        border = BorderFactory.createEmptyBorder(0, 0, 0, 10)
                    }, BorderLayout.WEST)

                    val textField = JTextField(field.type).apply {
                        toolTipText = "Edit field type"
                    }
                    add(textField, BorderLayout.CENTER)
                }

                add(fieldPanel)
                add(Box.createVerticalStrut(5))
            }
        }

        JBScrollPane(contentPanel).let {
            add(it, BorderLayout.CENTER)
            prevPanel = it
        }
    }
}
