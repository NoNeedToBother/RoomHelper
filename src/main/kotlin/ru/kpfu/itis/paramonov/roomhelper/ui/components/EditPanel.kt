package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import ru.kpfu.itis.paramonov.roomhelper.model.Field
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.model.Relation
import ru.kpfu.itis.paramonov.roomhelper.util.deepCopy
import ru.kpfu.itis.paramonov.roomhelper.util.equalsIgnoring
import ru.kpfu.itis.paramonov.roomhelper.util.recursiveDisable
import ru.kpfu.itis.paramonov.roomhelper.util.relations
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import java.util.ArrayList
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
    private var relationPanel: JPanel? = null
    private var addRelationButton: JButton? = null

    private var newFieldIndex = 1
    private var newRelationIndex = 1

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
                    newRelationIndex = 1
                }
            })
            add(JButton("Cancel").apply {
                addActionListener { endCurrentEditing() }
            })
            add(JButton("Save").apply {
                addActionListener {
                    _editBuffer?.let {
                        onSave(
                            it.deepCopy(),
                            relationUpdates.map { it.first to it.second.map { it.copy() } }
                        )
                    }
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
        newRelationIndex = 1
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
        paintEditMenu(editBuffer)
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
            if (entity is Parsed.Entity || entity is Parsed.ManyToMany) {
                add(JLabel("Edit relations").apply {
                    font = font.deriveFont(Font.BOLD, 16f)
                })
                add(JPanel().apply {
                    layout = BoxLayout(this, BoxLayout.Y_AXIS)
                    relationPanel = this
                    paintRelations(panel = this, entity = entity)
                })
                add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
                    add(JButton("Add new relation").apply {
                        addRelationButton = this
                        addActionListener {
                            val newRelation = Relation("untitled$newRelationIndex",
                                "", "", "", "")
                            newRelationIndex++
                            val updated = editBuffer.relations().toMutableList().apply { add(newRelation) }
                            if (editBuffer is Parsed.Entity) (editBuffer as Parsed.Entity).relations = updated
                            if (editBuffer is Parsed.ManyToMany) (editBuffer as Parsed.ManyToMany).relations = updated
                            val bufferNameStore = BufferNameStore(newRelation.name)
                            val relationPanel = relationPanel(entity, newRelation, bufferNameStore)
                            this@EditPanel.relationPanel?.apply {
                                add(relationPanel)
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

    private fun paintRelations(panel: JPanel, entity: Parsed) {
        entity.relations().forEach { relation ->
            val bufferNameStore = BufferNameStore(relation.name)
            val relationPanel = relationPanel(entity, relation, bufferNameStore)

            panel.add(relationPanel)
            panel.add(Box.createVerticalStrut(5))
        }
    }

    private fun relationPanel(
        entity: Parsed, relation: Relation,
        bufferNameStore: BufferNameStore,
    ): RelationPanel {
        fun updateRelations(update: (Relation) -> Relation) {
            var changed = false
            val updated = editBuffer.relations()
                .map { if (it.name == bufferNameStore.bufferName && !changed) {
                    changed = true
                    update(it)
                } else it.copy() }
            if (editBuffer is Parsed.Entity) (editBuffer as Parsed.Entity).relations = updated
            if (editBuffer is Parsed.ManyToMany) (editBuffer as Parsed.ManyToMany).relations = updated
        }
        return RelationPanel(
            fieldWidth = EDIT_BLOCK_WIDTH,
            fieldHeight = EDIT_BLOCK_HEIGHT * 3 / 2,
            maximumWidth = EDIT_BLOCK_MAXIMUM_WIDTH * 2 / 3,
            entity = entity, relation = relation,
            onRemoveClicked = {
                var removed = false
                val updated = entity.relations().filter {
                    if (it.name == bufferNameStore.bufferName && !removed &&
                        it.equalsIgnoring(relation, ignoreName = true)) {
                        removed = true
                        false
                    }
                    else true
                }
                if (editBuffer is Parsed.Entity) (editBuffer as Parsed.Entity).relations = updated
                if (editBuffer is Parsed.ManyToMany) (editBuffer as Parsed.ManyToMany).relations = updated
                relationPanel?.repaintProperties { paintRelations(it, editBuffer) }
            },
            onNameChanged = { name ->
                var changed = false
                val updated = editBuffer.relations()
                    .map { if (it.name == bufferNameStore.bufferName && !changed &&
                        it.equalsIgnoring(relation, ignoreName = true)) {
                        changed = true
                        it.copy(name = name)
                    } else it.copy() }
                if (editBuffer is Parsed.Entity) (editBuffer as Parsed.Entity).relations = updated
                if (editBuffer is Parsed.ManyToMany) (editBuffer as Parsed.ManyToMany).relations = updated
                bufferNameStore.bufferName = name
            },
            onTypeChanged = { type ->
                updateRelations { relation -> relation.copy(type = type) }
            },
            onRefTableChanged = { refTable ->
                updateRelations { relation -> relation.copy(refTable = refTable) }
            },
            onRefColumnChanged = { refColumn ->
                updateRelations { relation -> relation.copy(refColumn = refColumn) }
            },
        )
    }

    private fun indexPanel(
        entity: Parsed, index: List<String>,
    ): IndexPanel {
        fun updateIndex(newIndex: List<String>) {
            var indexChanged = false
            if (editBuffer is Parsed.Entity) {
                (editBuffer as Parsed.Entity).indexes = (editBuffer as Parsed.Entity).indexes
                    .map { if (it == index && !indexChanged) {
                        indexChanged = true
                        newIndex
                    } else ArrayList(it) }
                indexPanel?.repaintProperties { paintIndexes(it, editBuffer as Parsed.Entity) }
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
                    indexPanel?.repaintProperties { paintIndexes(it, editBuffer as Parsed.Entity) }
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
        fun updateField(update: (Field) -> Field) {
            var changed = false
            editBuffer.fields = editBuffer.fields
                .map { if (it.name == bufferNameStore.bufferName && !changed) {
                    changed = true
                    update(it)
                } else it.copy() }
        }
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
                    indexPanel?.repaintProperties { paintIndexes(it, editBuffer as Parsed.Entity) }
                }
                panel.recursiveDisable()
            },
            onNameChanged = { name ->
                var fieldChanged = false
                updateOtherEntityRelations(name, bufferNameStore)
                editBuffer.fields = editBuffer.fields.map {
                    if (bufferNameStore.bufferName == it.name && !fieldChanged &&
                        it.equalsIgnoring(field, ignoreName = true)) {

                        fieldChanged = true
                        it.copy(name = name)
                    }
                    else it.copy()
                }
                if (editBuffer is Parsed.Entity) {
                    (editBuffer as Parsed.Entity).indexes = (editBuffer as Parsed.Entity).indexes
                        .map { index ->
                            var indexChanged = false
                            if (index.contains(bufferNameStore.bufferName)) index.map { indexPart ->
                                if (indexPart == bufferNameStore.bufferName && !indexChanged) {
                                    indexChanged = true
                                    name
                                } else indexPart
                            } else index
                        }
                    indexPanel?.repaintProperties { paintIndexes(it, editBuffer as Parsed.Entity) }
                }
                bufferNameStore.bufferName = name
            },
            onTypeChanged = { type ->
                updateField { it.copy(type = type) }
            },
            onPrimaryKeyCheckBoxClicked = { checked ->
                updatePrimaryKeys(bufferNameStore.bufferName, checked)
            },
            onUniqueCheckBoxClicked = { checked ->
                updateField { it.copy(isUnique = checked) }
            },
            onNotNullCheckBoxClicked = { checked ->
                updateField { it.copy(isNotNull = checked) }
            },
        )
    }

    private fun updateOtherEntityRelations(newName: String, bufferNameStore: BufferNameStore) {
        // check that field name is not same as others to prevent editing relations with other matched field
        if (editBuffer.fields.filter { it.name == bufferNameStore.bufferName }.size <= 1) {
            entities.forEach { entity ->
                // check whether relation update was already added to not specifically edit entities
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

    // needed to retrieve previous field or relation name for updating indexes, relations and editBuffer,
    // or settings new properties in editBuffer
    private data class BufferNameStore(var bufferName: String)

    companion object {
        private const val EDIT_BLOCK_HEIGHT = 50
        private const val EDIT_BLOCK_WIDTH = 300
        private const val EDIT_BLOCK_MAXIMUM_WIDTH = 600
    }
}

private fun JPanel.repaintProperties(paint: (JPanel) -> Unit) {
    removeAll()
    paint(this)
    revalidate()
}
