package ru.kpfu.itis.paramonov.roomhelper.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import ru.kpfu.itis.paramonov.roomhelper.generator.DatabaseGenerator
import ru.kpfu.itis.paramonov.roomhelper.generator.FileGenerator
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.parseEntities
import ru.kpfu.itis.paramonov.roomhelper.ui.model.DatabaseStateHistory
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.model.ValidationResult
import ru.kpfu.itis.paramonov.roomhelper.state.DatabaseFilePersistentState
import ru.kpfu.itis.paramonov.roomhelper.ui.components.EditPanel
import ru.kpfu.itis.paramonov.roomhelper.ui.components.EntityBlock
import ru.kpfu.itis.paramonov.roomhelper.ui.components.EntityType
import ru.kpfu.itis.paramonov.roomhelper.ui.components.MenuBar
import ru.kpfu.itis.paramonov.roomhelper.ui.components.RelationshipArrow
import ru.kpfu.itis.paramonov.roomhelper.ui.components.UnsavedCloseRationale
import ru.kpfu.itis.paramonov.roomhelper.util.deepCopy
import ru.kpfu.itis.paramonov.roomhelper.util.openFileSaver
import ru.kpfu.itis.paramonov.roomhelper.util.openGenerationDestination
import ru.kpfu.itis.paramonov.roomhelper.util.relations
import ru.kpfu.itis.paramonov.roomhelper.util.showErrorMessage
import ru.kpfu.itis.paramonov.roomhelper.util.validateEditedEntity
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import java.io.File
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JLayeredPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.KeyStroke
import javax.swing.border.Border
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI
import kotlin.random.Random

class RoomHelperWindow(
    private var isNewFile: Boolean = false
) : DialogWrapper(true) {

    private val entityBlocks = mutableListOf<EntityBlock>()
    private val history = DatabaseStateHistory()
    private val entityPositions = mutableMapOf<String, Point>()

    private var entitiesPane: JLayeredPane? = null
    private var menuBar: MenuBar? = null
    private var editPanel: EditPanel? = null

    private var isSaved = true

    init {
        init()

        window.preferredSize = Dimension(800, 600)
        window.minimumSize = Dimension(400, 300)

        isResizable = true
        isModal = false

        if (isNewFile) changeTitle(null)
    }

    override fun createNorthPanel(): JComponent? {
        return MenuBar(
            history = history,
            window = this,
            onUpdate = {
                editPanel?.apply {
                    endCurrentEditing()
                }
                entitiesPane?.let {
                    paintUI(it)
                }
            },
            onFileChosen = {
                DatabaseFilePersistentState.getInstance().state.value = it
                history.clear()
                try {
                    openDatabaseFile().let { file ->
                        changeTitle(file.name)
                        history.add(parseEntities(file.readText()))
                    }
                    updateUIOnEdit()
                } catch (e: Throwable) {
                    showErrorMessage(e)
                }
            },
            onNewFileClicked = {
                DatabaseFilePersistentState.getInstance().state.value = null
                isNewFile = true
                history.clear()
                changeTitle(null)
                updateUIOnEdit()
            },
            onSaveClicked = { save() },
            onAddEntityClicked = { entityName, entityType ->
                val newEntity = when (entityType) {
                    EntityType.Regular -> Parsed.Entity(entityName)
                    EntityType.Embedded -> Parsed.Embedded(entityName)
                    EntityType.ManyToMany -> Parsed.ManyToMany(entityName)
                }
                val newState = ArrayList(history.currentState.map { it.deepCopy() })
                    .apply { add(newEntity) }
                history.add(newState)

                entitiesPane?.let {
                    paintUI(it)
                }
            },
            isNameUnique = { name -> history.currentState.none { it.name == name } }
        ).apply {
            menuBar = this
        }
    }

    override fun createCenterPanel(): JComponent? {
        return JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = entitiesPane()
            rightComponent = editPanel().apply { editPanel = this }
            dividerSize = 5
            dividerLocation = 600
            setUI(object : BasicSplitPaneUI() {
                override fun createDefaultDivider(): BasicSplitPaneDivider? =
                    object : BasicSplitPaneDivider(this) {
                        override fun setBorder(border: Border?) {}

                        override fun paint(g: Graphics?) {
                            g?.color = JBColor(
                                JBColor.PanelBackground.darker(),
                                JBColor.PanelBackground.brighter()
                            )
                            g?.fillRect(0, 0, size.width, size.height)
                            super.paint(g)
                        }
                    }
            })
        }
    }

    private fun editPanel(): EditPanel {
        return EditPanel(
            onSave = { updatedEntity, otherEntityRelationUpdates ->
                val newState = history.currentState.map { it.deepCopy() }.toMutableList()
                if (updatedEntity is Parsed.Entity || updatedEntity is Parsed.ManyToMany) {
                    val updated = updatedEntity.relations().map { relation ->
                        newState.find { it.name == relation.refTable }
                            ?.fields?.find { it.name == relation.refColumn }
                            ?.type?.let { fieldType ->
                                relation.copy(fieldType = fieldType)
                            } ?: relation
                    }
                    if (updatedEntity is Parsed.Entity) updatedEntity.relations = updated
                    if (updatedEntity is Parsed.ManyToMany) updatedEntity.relations = updated
                }
                newState.removeIf { it.name == updatedEntity.name }
                newState.add(updatedEntity)
                otherEntityRelationUpdates.forEach { update ->
                    newState.forEach { entity ->
                        if (entity.name == update.first) {
                            if (entity is Parsed.Entity) {
                                entity.relations = update.second
                            }
                            if (entity is Parsed.ManyToMany) {
                                entity.relations = update.second
                            }
                        }
                    }
                }
                val validation = validateEditedEntity(
                    edited = updatedEntity,
                    other = newState.filter { it.name != updatedEntity.name }
                )
                if (validation is ValidationResult.Failure) {
                    Messages.showErrorDialog(validation.message, "Invalid Save Data")
                } else {
                    history.add(newState)
                    editPanel?.endCurrentEditing()
                    updateUIOnEdit()
                }
            },
        )
    }

    private fun entitiesPane(): JComponent {
        try {
            if (!isNewFile) {
                openDatabaseFile().let { file ->
                    changeTitle(file.name)
                    history.add(parseEntities(file.readText()))
                }
            }
        } catch (e: Throwable) {
            showErrorMessage(e)
        }

        val pane = JLayeredPane().apply pane@ {
            layout = BorderLayout()
            paintUI(this)
        }
        this.entitiesPane = pane

        val scrollPane = JBScrollPane(pane).apply {
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            addKeyShortcutActions(this)
        }
        return scrollPane
    }

    private fun changeTitle(fileName: String?) {
        title = "RoomHelper — ${fileName ?: "untitled"}"
    }

    private fun changeSavedStatus(saved: Boolean) {
        if (isSaved == saved) return
        isSaved = saved
        title = if (!saved) "$title*" else title.replace("*", "")
    }

    private fun addKeyShortcutActions(pane: JBScrollPane) {
        addKeyShortcutAction(
            pane = pane, name = "undoAction",
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK),
            action = { if (!history.isFirstState) undoAction() }
        )
        addKeyShortcutAction(
            pane = pane, name = "redoAction",
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK),
            action = { if (!history.isLastState) redoAction() }
        )
    }

    private fun addKeyShortcutAction(
        pane: JBScrollPane,
        keyStroke: KeyStroke,
        action: (e: ActionEvent?) -> Unit,
        name: String,
    ) {
        pane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            keyStroke, name
        )
        pane.actionMap.put(name, object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                action(e)
            }
        })
    }

    private fun redoAction() {
        history.redo()
        updateUIOnEdit()
    }

    private fun undoAction() {
        history.undo()
        updateUIOnEdit()
    }

    private fun updateUIOnEdit() {
        entitiesPane?.let {
            paintUI(it)
        }
        menuBar?.apply {
            changeRedoButton(!history.isLastState)
            changeUndoButton(!history.isFirstState)
        }
        changeSavedStatus(false)
    }

    private fun openDatabaseFile(): File {
        val filePath = DatabaseFilePersistentState.getInstance().state.value
        if (filePath == null) {
            throw RuntimeException("No configuration file provided")
        }
        val file = File(filePath)
        if (!file.exists()) {
            throw RuntimeException("Configuration file no longer exists")
        }
        return file
    }

    private fun save() {
        try {
            if (isNewFile) {
                openFileSaver(
                    onFileChosen = {
                        DatabaseFilePersistentState.getInstance().state.value = it
                        val file = File(it)
                        if (!file.exists()) file.createNewFile()
                        FileGenerator().generate(
                            file = openDatabaseFile(),
                            entities = history.currentState
                        )
                        changeTitle(file.name)
                        changeSavedStatus(true)
                    }
                )
            }
            else {
                FileGenerator().generate(
                    file = openDatabaseFile(),
                    entities = history.currentState
                )
                changeSavedStatus(true)
            }
        } catch (e: Throwable) {
            showErrorMessage(e)
        }
    }

    private fun paintUI(pane: JLayeredPane) {
        pane.removeAll()
        entityBlocks.clear()
        history.currentState.forEach { entity ->
            val block = EntityBlock(
                entity = entity,
                onDeleteRequest = {
                    removeEntity(it.entity)
                    updateUIOnEdit()
                    editPanel?.endCurrentEditing()
                },
                onEditRequest = {
                    editPanel?.changeEntity(history.currentState, it.entity)
                    editPanel?.revalidate()
                },
                onPositionChanged = {
                    pane.repaint()
                    entityPositions.put(it.entity.name, Point(it.x, it.y))
                }
            ).apply {
                val position = entityPositions[entity.name]
                    ?: Point(
                        Random.nextInt(500),
                        Random.nextInt(300))
                if (entityPositions[entity.name] == null) entityPositions.put(entity.name, position)
                bounds = Rectangle(
                    position,
                    Dimension(200, 30 + entity.fields.size * 30 + 20)
                )
            }
            pane.add(block, JLayeredPane.DEFAULT_LAYER)
            entityBlocks.add(block)
        }
        pane.add(JPanel())

        history.currentState.filterIsInstance<Parsed.Entity>().forEach { entity ->
            entity.relations.forEach relations@ { relation ->
                val fromBlock = entityBlocks.find { it.entity.name == entity.name } ?: return@relations
                val toBlock = entityBlocks.find { it.entity.name == relation.refTable } ?: return@relations

                val arrow = RelationshipArrow(
                    fromBlock = fromBlock,
                    toBlock = toBlock,
                    relationType = relation.type
                )
                pane.add(arrow, JLayeredPane.PALETTE_LAYER)
            }
        }

        history.currentState.filterIsInstance<Parsed.ManyToMany>().forEach { m2mEntity ->
            m2mEntity.relations.forEach relations@ { relation ->
                val fromBlock = entityBlocks.find { it.entity.name == m2mEntity.name } ?: return@relations
                val toBlock = entityBlocks.find { it.entity.name == relation.refTable } ?: return@relations

                val arrow = RelationshipArrow(
                    fromBlock = fromBlock,
                    toBlock = toBlock,
                    relationType = "m2m"
                )
                pane.add(arrow, JLayeredPane.PALETTE_LAYER)
            }
        }
    }

    private fun removeEntity(removed: Parsed) {
        val newState = ArrayList(history.currentState.map { it.deepCopy() })
        if (removed is Parsed.Embedded) {
            newState.forEach { entity ->
                entity.fields = entity.fields.filter { field -> field.type != removed.name }
            }
        }
        if (removed is Parsed.Entity) {
            newState.removeIf { entity ->
                entity is Parsed.ManyToMany &&
                        entity.relations.any { it.refTable == removed.name }
            }
            newState.forEach { entity ->
                if (entity is Parsed.Entity) {
                    entity.fields = entity.fields.toMutableList().filter { field ->
                        entity.relations.none { relation ->
                            relation.refTable == removed.name &&
                                    relation.name == field.name
                        }
                    }
                }
            }
        }
        newState.removeIf { it.name == removed.name }
        history.add(newState)
        entityBlocks.removeIf {
            it.entity == removed
        }
    }

    override fun getOKAction(): Action {
        return super.getOKAction().apply {
            putValue(Action.NAME, "Generate code")
        }
    }

    override fun doOKAction() {
        if (DatabaseFilePersistentState.getInstance().state.value == null) {
            Messages.showErrorDialog(
                "Save new database before generating",
                "Generation Fail"
            )
            return
        }
        openGenerationDestination { dir ->
            try {
                DatabaseGenerator(
                    inputFile = openDatabaseFile(),
                    outputDir = File(dir),
                ).generate()
                Messages.showMessageDialog(
                    "Database files were successfully generated", "Success",
                    AllIcons.General.SuccessDialog
                )
            } catch (e: Throwable) {
                showErrorMessage(e)
            }
        }
    }

    override fun getCancelAction(): Action {
        return super.getCancelAction().apply {
            putValue(Action.NAME, "Close")
        }
    }

    override fun doCancelAction() {
        if (!isSaved) {
            UnsavedCloseRationale(
                onCloseAgree = { super.doOKAction() },
            ).show()
        } else super.doCancelAction()
    }
}
