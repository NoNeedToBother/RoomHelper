package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.IconUtil
import ru.kpfu.itis.paramonov.roomhelper.model.Field
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.model.Relation
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

class EditPanel(
    onSave: (
        updatedEntity: Parsed,
        otherEntityRelationUpdates: List<Pair<String, List<Relation>>>
            ) -> Unit,
) : JPanel() {

    private var entity: Parsed? = null
    private var entities: List<Parsed> = emptyList()

    private var relationUpdates: MutableList<Pair<String, List<Relation>>> = mutableListOf()

    private var _editBuffer: Parsed? = null
    private val editBuffer: Parsed get() = _editBuffer
        ?: throw IllegalStateException("No entity provided for editing")

    private var contentPane: JBScrollPane? = null

    private var fieldPanel: JPanel? = null
    private var indexPanel: JPanel? = null

    private var newFieldIndex = 1

    init {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        background = JBColor.background()

        add(JLabel("Edit entity").apply {
            font = font.deriveFont(Font.BOLD, 20f)
            border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
        }, BorderLayout.NORTH)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            add(JButton("Discard changes").apply {
                addActionListener {
                    contentPane?.let { this@EditPanel.remove(it) }
                    entity?.let { changeEntity(entities, it) }
                    this@EditPanel.revalidate()
                    this@EditPanel.repaint()
                    newFieldIndex = 1
                }
            })
            add(JButton("Cancel").apply {
                addActionListener { endCurrentEditing() }
            })
            add(JButton("Save").apply {
                addActionListener {
                    _editBuffer?.let { onSave(it.deepCopy(), relationUpdates) }
                    endCurrentEditing()
                }
            })
        }

        add(buttonPanel, BorderLayout.SOUTH)
    }

    fun endCurrentEditing() {
        contentPane?.let { this@EditPanel.remove(it) }
        this@EditPanel.revalidate()
        this@EditPanel.repaint()
        newFieldIndex = 1
        _editBuffer = null
        entity = null
        relationUpdates = mutableListOf()
    }

    fun changeEntity(entities: List<Parsed>, entity: Parsed) {
        this.entity = entity
        this.entities = entities
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

            add(JLabel("Edit fields").apply {
                font = font.deriveFont(Font.BOLD, 16f)
            })
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                fieldPanel = this
                paintFields(panel = this, entity = entity)
            })
            add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                add(JButton("Add new field").apply {
                    addActionListener {
                        val newField = Field(name = "untitled$newFieldIndex", type = "")
                        editBuffer.fields = editBuffer.fields.toMutableList().apply { add(newField) }
                        newFieldIndex++

                        val bufferNameStore = BufferNameStore(newField.name)
                        val fieldPanel = fieldPanel(
                            entity = entity, field = newField,
                            bufferNameStore = bufferNameStore,
                        )
                        this@EditPanel.fieldPanel?.apply {
                            add(fieldPanel)
                            add(Box.createVerticalStrut(5))
                            revalidate()
                        }
                    }
                })
            })
            if (entity is Parsed.Entity) {
                add(JLabel("Edit indexes").apply {
                    font = font.deriveFont(Font.BOLD, 16f)
                })
                add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    indexPanel = this
                    paintIndexes(panel = this, entity = entity)
                })
                add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    add(JButton("Add new index").apply {
                        addActionListener {
                            val newIndex = emptyList<String>()
                            (editBuffer as Parsed.Entity).indexes =
                                (editBuffer as Parsed.Entity).indexes.toMutableList().apply { add(newIndex) }
                            val indexPanel = indexPanel(entity, newIndex)
                            this@EditPanel.indexPanel?.apply {
                                add(indexPanel)
                                add(Box.createVerticalStrut(5))
                                revalidate()
                            }
                        }
                    })
                })
            }
        }

        JBScrollPane(contentPanel).let {
            add(it, BorderLayout.CENTER)
            contentPane = it
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
                val bufferNameStore = BufferNameStore(field.name)
                val fieldPanel = fieldPanel(
                    entity = entity, field = field,
                    bufferNameStore = bufferNameStore,
                )

                panel.add(fieldPanel)
                panel.add(Box.createVerticalStrut(5))
            }
    }

    private fun paintIndexes(panel: JPanel, entity: Parsed.Entity) {
        entity.indexes.forEach { index ->
            val indexPanel = indexPanel(entity, index)

            panel.add(indexPanel)
            panel.add(Box.createVerticalStrut(5))
        }
    }

    private fun indexPanel(
        entity: Parsed, index: List<String>,
    ): IndexPanel {
        fun updateIndex(newIndex: List<String>) {
            if (editBuffer is Parsed.Entity) {
                (editBuffer as Parsed.Entity).indexes = (editBuffer as Parsed.Entity).indexes
                    .map { if (it == index) newIndex else it }
                indexPanel?.let {
                    it.removeAll()
                    paintIndexes(it, editBuffer as Parsed.Entity)
                    it.revalidate()
                }
            }
        }
        return IndexPanel(
            fieldWidth = EDIT_BLOCK_WIDTH,
            fieldHeight = EDIT_BLOCK_HEIGHT * 3 / 2,
            maximumWidth = EDIT_BLOCK_MAXIMUM_WIDTH,
            entity = entity, index = index,
            onRemoveClicked = {
                if (editBuffer is Parsed.Entity) {
                    (editBuffer as Parsed.Entity).indexes = (editBuffer as Parsed.Entity).indexes
                        .filter { it.all { indexPart -> index.contains(indexPart) } }
                    indexPanel?.let {
                        it.removeAll()
                        paintIndexes(it, editBuffer as Parsed.Entity)
                        it.revalidate()
                    }
                }
            },
            onNewFieldAdded = { newIndex -> updateIndex(newIndex) },
            onFieldRemoved = { newIndex -> updateIndex(newIndex) },
        )
    }

    private fun fieldPanel(
        entity: Parsed, field: Field,
        bufferNameStore: BufferNameStore,
    ): FieldPanel {
        return FieldPanel(
            fieldWidth = EDIT_BLOCK_WIDTH,
            fieldHeight = EDIT_BLOCK_HEIGHT,
            maximumWidth = EDIT_BLOCK_MAXIMUM_WIDTH,
            entity = entity, field = field,
            onRemoveClicked = { panel ->
                editBuffer.fields = editBuffer.fields.filter { it.name != bufferNameStore.bufferName }
                if (editBuffer is Parsed.Entity) {
                    (editBuffer as Parsed.Entity).indexes =
                        (editBuffer as Parsed.Entity).indexes
                            .filter { index -> !index.contains(bufferNameStore.bufferName) }
                    indexPanel?.let {
                        it.removeAll()
                        paintIndexes(it, editBuffer as Parsed.Entity)
                        it.revalidate()
                    }
                }
                panel.recursiveDisable()
            },
            onNameChanged = { name ->
                updateOtherEntityRelations(name, bufferNameStore)
                editBuffer.fields = editBuffer.fields.map {
                    if (bufferNameStore.bufferName == it.name) it.copy(name = name)
                    else it.copy()
                }
                if (editBuffer is Parsed.Entity) {
                    (editBuffer as Parsed.Entity).indexes = (editBuffer as Parsed.Entity).indexes
                        .map { index ->
                            if (index.contains(bufferNameStore.bufferName)) index.map { indexPart ->
                                if (indexPart == bufferNameStore.bufferName) name else indexPart
                            } else index
                        }
                    indexPanel?.let {
                        it.removeAll()
                        paintIndexes(it, editBuffer as Parsed.Entity)
                        it.revalidate()
                    }
                }
                bufferNameStore.bufferName = name
            },
            onTypeChanged = { type ->
                editBuffer.fields = editBuffer.fields.map {
                    if (bufferNameStore.bufferName == it.name) it.copy(type = type)
                    else it.copy()
                }
            },
            onPrimaryKeyCheckBoxClicked = { checked ->
                updatePrimaryKeys(bufferNameStore.bufferName, checked)
            },
            onUniqueCheckBoxClicked = { checked ->
                editBuffer.fields = editBuffer.fields.map {
                    if (it.name == bufferNameStore.bufferName) it.copy(isUnique = checked)
                    else it
                }
            },
            onNotNullCheckBoxClicked = { checked ->
                editBuffer.fields = editBuffer.fields.map {
                    if (it.name == bufferNameStore.bufferName) it.copy(isNotNull = checked)
                    else it
                }
            },
        )
    }

    private fun updateOtherEntityRelations(newName: String, bufferNameStore: BufferNameStore) {
        // check that field name is not same as others to prevent editing relations with other matched field
        if (editBuffer.fields.filter { it.name == bufferNameStore.bufferName }.size <= 1) {
            entities.forEach { entity ->
                // check whether relation update was already added to not edit entities
                if (relationUpdates.none { it.first == entity.name }) {
                    if (entity is Parsed.Entity) {
                        if (entity.relations.find { it.refTable == editBuffer.name }?.refColumn ==
                            bufferNameStore.bufferName) {

                            addRelationUpdate(newName, entity.name, bufferNameStore, entity.relations)
                        }
                    }
                    if (entity is Parsed.ManyToMany) {
                        if (entity.relations.find { it.refTable == editBuffer.name }?.refColumn ==
                            bufferNameStore.bufferName) {

                            addRelationUpdate(newName, entity.name, bufferNameStore, entity.relations)
                        }
                    }
                } else {
                    relationUpdates = relationUpdates.map { update ->
                        if (update.first == entity.name) {
                            entity.name to update.second.map { relation ->
                                if (relation.refTable == editBuffer.name) relation.copy(refColumn = newName)
                                else relation
                            }
                        } else update
                    }.toMutableList()
                }
            }
        }
    }

    private fun addRelationUpdate(newName: String, entityName: String, bufferNameStore: BufferNameStore,
                                  relations: List<Relation>) {
        relationUpdates.add(entityName to
                relations.map { relation ->
                    if (relation.refTable == editBuffer.name &&
                        relation.refColumn == bufferNameStore.bufferName)

                        relation.copy(refColumn = newName)
                    else relation
                }
        )
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

    private data class BufferNameStore(var bufferName: String)

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
