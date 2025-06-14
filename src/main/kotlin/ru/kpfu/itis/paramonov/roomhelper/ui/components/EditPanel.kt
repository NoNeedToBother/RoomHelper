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

    private var indexPanel: JPanel? = null

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
                    this@EditPanel.revalidate()
                    this@EditPanel.repaint()
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

            paintFields(panel = this, entity = entity)
            if (entity is Parsed.Entity) {
                add(JLabel("Edit indexes").apply {
                    font = font.deriveFont(Font.BOLD, 12f)
                })
                add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    indexPanel = this
                    paintIndexes(panel = this, entity = entity)
                })
            }
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
                    preferredSize = Dimension(EDIT_BLOCK_WIDTH, EDIT_BLOCK_HEIGHT)
                    maximumSize = Dimension(EDIT_BLOCK_MAXIMUM_WIDTH, EDIT_BLOCK_HEIGHT)
                    background = JBColor.background()

                    add(JPanel(BorderLayout()).apply westPanel@ {
                        preferredSize = Dimension(EDIT_BLOCK_WIDTH / 2, EDIT_BLOCK_HEIGHT)
                        maximumSize = Dimension(
                            EDIT_BLOCK_MAXIMUM_WIDTH / 2 - EDIT_BLOCK_HEIGHT,
                            EDIT_BLOCK_HEIGHT
                        )

                        add(RemoveButton(EDIT_BLOCK_HEIGHT) {
                            editBuffer.fields = editBuffer.fields.filter { it.name != bufferName }
                            if (editBuffer is Parsed.Entity) {
                                (editBuffer as Parsed.Entity).indexes =
                                    (editBuffer as Parsed.Entity).indexes
                                        .filter { index -> !index.contains(bufferName) }
                                indexPanel?.let {
                                    it.removeAll()
                                    paintIndexes(it, editBuffer as Parsed.Entity)
                                    it.revalidate()
                                }
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
                                    (editBuffer as Parsed.Entity).indexes = (editBuffer as Parsed.Entity).indexes
                                        .map { index ->
                                            if (index.contains(bufferName)) index.map { indexPart ->
                                                if (indexPart == bufferName) name else indexPart
                                            } else index
                                        }
                                    indexPanel?.let {
                                        it.removeAll()
                                        paintIndexes(it, editBuffer as Parsed.Entity)
                                        it.revalidate()
                                    }
                                }
                                bufferName = name
                            }
                        }, BorderLayout.CENTER)
                    }, BorderLayout.WEST)

                    add(JPanel(BorderLayout()).apply {
                        preferredSize = Dimension(EDIT_BLOCK_WIDTH / 2, EDIT_BLOCK_HEIGHT)
                        maximumSize = Dimension(EDIT_BLOCK_MAXIMUM_WIDTH / 2, EDIT_BLOCK_HEIGHT)
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
                                addActionListener {
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

    private fun paintIndexes(panel: JPanel, entity: Parsed.Entity) {
        entity.indexes.forEach { index ->
            val indexPanel = JPanel(BorderLayout()).apply indexPanel@ {
                preferredSize = Dimension(EDIT_BLOCK_WIDTH, EDIT_BLOCK_HEIGHT)
                maximumSize = Dimension(EDIT_BLOCK_MAXIMUM_WIDTH, EDIT_BLOCK_HEIGHT)
                background = JBColor.background()

                add(JPanel(BorderLayout()).apply {
                    add(RemoveButton(EDIT_BLOCK_HEIGHT) {
                        if (editBuffer is Parsed.Entity) {
                            (editBuffer as Parsed.Entity).indexes = (editBuffer as Parsed.Entity).indexes
                                .filter { it.all { indexPart -> index.contains(indexPart) } }
                        }
                        this@indexPanel.recursiveDisable()
                    }, BorderLayout.WEST)
                    add(JLabel(index.joinToString(", ")), BorderLayout.CENTER)
                }, BorderLayout.CENTER)
            }

            panel.add(indexPanel)
            panel.add(Box.createVerticalStrut(5))
        }
    }

    companion object {
        private const val EDIT_BLOCK_HEIGHT = 50
        private const val EDIT_BLOCK_WIDTH = 300
        private const val EDIT_BLOCK_MAXIMUM_WIDTH = 600
    }
}

class RemoveButton(size: Int, onClick: () -> Unit) : JButton(
    IconUtil.colorize(AllIcons.Actions.DeleteTag, JBColor.RED)
) {

    init {
        preferredSize = Dimension(size, size)
        minimumSize = Dimension(size, size)
        toolTipText = "Delete"
        border = BorderFactory.createEmptyBorder()

        addActionListener {
            onClick()
        }
    }
}
