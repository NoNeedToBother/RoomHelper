package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.IconUtil
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.util.addTextChangedListener
import ru.kpfu.itis.paramonov.roomhelper.util.deepCopy
import ru.kpfu.itis.paramonov.roomhelper.util.recursiveDisable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class EditPanel(
    onSave: (Parsed) -> Unit,
) : JPanel() {

    private var _editBuffer: Parsed? = null
    private val editBuffer: Parsed get() = _editBuffer
        ?: throw IllegalStateException("No entity provided for editing")

    private var contentPane: JBScrollPane? = null

    private var indicesComponents = listOf<JComponent>()

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
                    contentPane?.let { this@EditPanel.remove(it) }
                    _editBuffer?.let { onSave(it.deepCopy()) }
                }
            })
        }

        add(buttonPanel, BorderLayout.SOUTH)
    }

    fun changeEntity(entity: Parsed) {
        this._editBuffer = when(entity) {
            is Parsed.Entity -> entity.copy()
            is Parsed.Embedded -> entity.copy()
            is Parsed.ManyToMany -> entity.copy()
        }
        contentPane?.let { remove(it) }
        paintEditMenu(entity)
    }

    private fun paintEditMenu(entity: Parsed) {
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
            contentPane = it
        }
    }

    private fun paintFields(panel: JPanel, entity: Parsed) {
        panel.add(JLabel("Edit fields").apply {
            font = font.deriveFont(Font.BOLD, 12f)
        }, BorderLayout.NORTH)
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
                                if (editBuffer is Parsed.Entity) {
                                    (editBuffer as Parsed.Entity).indices =
                                        (editBuffer as Parsed.Entity).indices
                                            .filter { index -> !index.contains(bufferName) }
                                }
                                this@panel.recursiveDisable()
                            }
                        }, BorderLayout.WEST)

                        add(JTextField(field.name).apply {
                            toolTipText = "Edit field name"
                            addTextChangedListener { name ->
                                editBuffer.fields = editBuffer.fields.map {
                                    if (bufferName == it.name) it.copy(name = name)
                                    else it.copy()
                                }
                                if (editBuffer is Parsed.Entity) {
                                    (editBuffer as Parsed.Entity).indices = (editBuffer as Parsed.Entity).indices
                                        .map { index ->
                                            if (index.contains(bufferName)) index.map { indexPart ->
                                                if (indexPart == bufferName) name else indexPart
                                            } else index
                                        }
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

                    add(JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
                        if (entity is Parsed.Entity) {
                            add(JBCheckBox("PK").apply {
                                isSelected = field.isPrimaryKey || field.isPartOfCompositeKey
                                toolTipText = "Is primary key or part of one"
                                addActionListener { e ->
                                    updatePrimaryKeys(bufferName, isSelected)
                                }
                            })

                            add(JBCheckBox("Unique").apply {
                                isSelected = field.isUnique
                                toolTipText = "Is unique"
                                addActionListener {
                                    editBuffer.fields = editBuffer.fields.map {
                                        if (it.name == bufferName) it.copy(isUnique = isSelected)
                                        else it
                                    }
                                }
                            })
                        }

                        add(JBCheckBox("Not null").apply {
                            isSelected = field.isNotNull
                            toolTipText = "Is not null"
                            addActionListener {
                                editBuffer.fields = editBuffer.fields.map {
                                    if (it.name == bufferName) it.copy(isNotNull = isSelected)
                                    else it
                                }
                            }
                        })
                    }, BorderLayout.SOUTH)
                }

                panel.add(fieldPanel)
                panel.add(Box.createVerticalStrut(5))
            }
    }

    private fun updatePrimaryKeys(bufferName: String, isChecked: Boolean) {
        val otherPrimaryKeyAmount = editBuffer.fields.filter {
            (it.isPrimaryKey || it.isPartOfCompositeKey) && it.name != bufferName
        }.size

        if (isChecked) {
            editBuffer.fields = editBuffer.fields.map {
                if (it.name == bufferName) {
                    it.copy(
                        isPrimaryKey = otherPrimaryKeyAmount == 0,
                        isPartOfCompositeKey = otherPrimaryKeyAmount > 0
                    )
                } else if (it.isPrimaryKey) {
                    it.copy(
                        isPrimaryKey = false,
                        isPartOfCompositeKey = true
                    )
                } else it
            }
        } else {
            editBuffer.fields = editBuffer.fields.map {
                if (it.name == bufferName) {
                    it.copy(
                        isPrimaryKey = false,
                        isPartOfCompositeKey = false
                    )
                } else if (it.isPrimaryKey || it.isPartOfCompositeKey) {
                    it.copy(
                        isPrimaryKey = otherPrimaryKeyAmount == 1,
                        isPartOfCompositeKey = otherPrimaryKeyAmount > 1
                    )
                } else it
            }
        }
    }

    private fun paintIndices(panel: JPanel, entity: Parsed.Entity) {
        panel.add(JLabel("Edit fields").apply {
            font = font.deriveFont(Font.BOLD, 12f)
        }, BorderLayout.NORTH)

        entity.indices.forEach { index ->

        }
    }

    companion object {
        private const val FIELD_HEIGHT = 50
    }
}
