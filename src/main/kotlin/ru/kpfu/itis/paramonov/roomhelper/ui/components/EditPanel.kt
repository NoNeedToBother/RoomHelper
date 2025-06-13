package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.IconUtil
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
                    prevPanel?.let { remove(it) }
                    prevPanel = null
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

            paintFields(
                panel = this,
                entity = entity
            )
        }

        JBScrollPane(contentPanel).let {
            add(it, BorderLayout.CENTER)
            prevPanel = it
        }
    }

    private fun paintFields(panel: JPanel, entity: Parsed) {
        entity.fields.forEach { field ->
            val fieldPanel = JPanel(BorderLayout()).apply panel@ {
                preferredSize = Dimension(200, FIELD_HEIGHT)
                maximumSize = Dimension(400, FIELD_HEIGHT)
                background = JBColor.background()

                add(JPanel(BorderLayout()).apply westPanel@ {
                    preferredSize = Dimension(100, FIELD_HEIGHT)
                    maximumSize = Dimension(400 - FIELD_HEIGHT, FIELD_HEIGHT)

                    add(JButton(
                        IconUtil.colorize(AllIcons.Actions.DeleteTag, JBColor.RED)
                    ).apply {
                        preferredSize = Dimension(FIELD_HEIGHT, FIELD_HEIGHT)
                        minimumSize = Dimension(FIELD_HEIGHT, FIELD_HEIGHT)
                        toolTipText = "Delete"
                        border = BorderFactory.createEmptyBorder()
                        addActionListener {
                            editBuffer?.fields = editBuffer?.fields?.filter { it.name != field.name } ?: listOf()
                            this@panel.components.forEach { it.isEnabled = false }
                            this@westPanel.components.forEach { it.isEnabled = false }
                        }
                    }, BorderLayout.WEST)

                    add(JTextField(field.name).apply {
                        toolTipText = "Edit field name"
                        foreground = JBColor.foreground()
                    }, BorderLayout.CENTER)
                }, BorderLayout.WEST)

                add(JTextField(field.type).apply {
                    toolTipText = "Edit field type"
                }, BorderLayout.EAST)
            }

            panel.add(fieldPanel)
            panel.add(Box.createVerticalStrut(5))
        }
    }

    companion object {
        private const val FIELD_HEIGHT = 50
    }
}
