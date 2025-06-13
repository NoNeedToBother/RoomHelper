package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.IconUtil
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.util.deepCopy
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
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class EditPanel(
    onSave: (Parsed) -> Unit,
) : JPanel() {

    private var entity: Parsed? = null

    private var _editBuffer: Parsed? = null
    private val editBuffer: Parsed get() = _editBuffer
        ?: throw IllegalStateException("No entity provided for editing")

    private var prevPanel: JBScrollPane? = null
    private var contentPanel: JPanel? = null

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
                    //prevPanel?.let { remove(it) }
                    //prevPanel = null

                    contentPanel?.removeAll()
                    _editBuffer?.let { onSave(it.deepCopy()) }
                }
            })
        }

        add(buttonPanel, BorderLayout.SOUTH)
    }

    fun changeEntity(entity: Parsed) {
        this.entity = entity
        this._editBuffer = when(entity) {
            is Parsed.Entity -> entity.copy()
            is Parsed.Embedded -> entity.copy()
            is Parsed.ManyToMany -> entity.copy()
        }

        paintEditMenu(entity)
    }

    private fun paintEditMenu(entity: Parsed) {
        //prevPanel?.let { remove(it) }
        val contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = JBColor.background()

            paintFields(
                panel = this,
                entity = entity
            )
            contentPanel = this
        }

        JBScrollPane(contentPanel).let {
            add(it, BorderLayout.CENTER)
            prevPanel = it
        }
    }

    private fun paintFields(panel: JPanel, entity: Parsed) {
        entity.fields
            .filter { field ->
                if (entity is Parsed.Entity) {
                    entity.relations.none { relation -> relation.name == field.name }
                } else true
            }
            .forEach { field ->
                var bufferName = field.name
                val fieldPanel = JPanel(BorderLayout()).apply panel@ {
                    preferredSize = Dimension(300, FIELD_HEIGHT)
                    maximumSize = Dimension(600, FIELD_HEIGHT)
                    background = JBColor.background()

                    add(JPanel(BorderLayout()).apply westPanel@ {
                        preferredSize = Dimension(150, FIELD_HEIGHT)
                        maximumSize = Dimension(300 - FIELD_HEIGHT, FIELD_HEIGHT)

                        add(JButton(
                            IconUtil.colorize(AllIcons.Actions.DeleteTag, JBColor.RED)
                        ).apply {
                            preferredSize = Dimension(FIELD_HEIGHT, FIELD_HEIGHT)
                            minimumSize = Dimension(FIELD_HEIGHT, FIELD_HEIGHT)
                            toolTipText = "Delete"
                            border = BorderFactory.createEmptyBorder()
                            addActionListener {
                                editBuffer.fields = editBuffer.fields.filter { it.name != bufferName }
                                this@panel.components.forEach { it.isEnabled = false }
                                this@westPanel.components.forEach { it.isEnabled = false }
                            }
                        }, BorderLayout.WEST)

                        add(JTextField(field.name).apply {
                            toolTipText = "Edit field name"
                            addTextChangedListener { name ->
                                editBuffer.fields = editBuffer.fields.map {
                                    if (bufferName == it.name) it.copy(name = name)
                                    else it.copy()
                                }
                                bufferName = name
                            }
                        }, BorderLayout.CENTER)
                    }, BorderLayout.WEST)

                    add(JPanel(BorderLayout()).apply {
                        preferredSize = Dimension(150, FIELD_HEIGHT)
                        maximumSize = Dimension(300, FIELD_HEIGHT)
                        add(JTextField(field.type).apply {
                            toolTipText = "Edit field type"
                            addTextChangedListener { type ->
                                editBuffer.fields = editBuffer.fields.map {
                                    if (bufferName == it.name) it.copy(type = type)
                                    else it.copy()
                                }
                            }
                        }, BorderLayout.CENTER)
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

private fun JTextField.addTextChangedListener(onTextChange: (String) -> Unit) {
    document.addDocumentListener(object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) {
            onChange(e)
        }

        override fun removeUpdate(e: DocumentEvent?) {
            onChange(e)
        }

        override fun changedUpdate(e: DocumentEvent?) {
            onChange(e)
        }

        private fun onChange() {
            onTextChange(text)
        }
    })
}
