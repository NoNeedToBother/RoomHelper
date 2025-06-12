package ru.kpfu.itis.paramonov.roomhelper.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import ru.kpfu.itis.paramonov.roomhelper.generator.FileGenerator
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.parseEntities
import ru.kpfu.itis.paramonov.roomhelper.model.DatabaseStateHistory
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.state.DatabaseFilePersistentState
import ru.kpfu.itis.paramonov.roomhelper.ui.components.EditPanel
import ru.kpfu.itis.paramonov.roomhelper.ui.components.EntityBlock
import ru.kpfu.itis.paramonov.roomhelper.ui.components.MenuBar
import ru.kpfu.itis.paramonov.roomhelper.ui.components.RelationshipArrow
import ru.kpfu.itis.paramonov.roomhelper.util.showErrorMessage
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
import javax.swing.JLayeredPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.KeyStroke
import javax.swing.border.Border
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI
import kotlin.random.Random

class RoomHelperWindow : DialogWrapper(true) {

    private val entityBlocks = mutableListOf<EntityBlock>()
    private val history = DatabaseStateHistory()
    private val entityPositions = mutableMapOf<String, Point>()

    private var entitiesPane: JLayeredPane? = null
    private var menuBar: MenuBar? = null
    private var editPanel: EditPanel? = null

    private var isNewFile = false
    private var isSaved = true

    init {
        init()

        window.preferredSize = Dimension(800, 600)
        window.minimumSize = Dimension(400, 300)

        isResizable = true
        isModal = false
    }

    override fun createNorthPanel(): JComponent? {
        return MenuBar(
            history = history,
            window = this,
            onUpdate = {
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
            onNewFile = {
                DatabaseFilePersistentState.getInstance().state.value = null
                isNewFile = true
                history.clear()
                changeTitle(null)
                updateUIOnEdit()
            },
            onSave = { save() }
        ).apply {
            menuBar = this
        }
    }

    override fun createCenterPanel(): JComponent? {
        return JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = entitiesPane()
            rightComponent = editPanel()
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

    private fun editPanel(): JComponent {
        return EditPanel().apply {
            editPanel = this
        }
    }

    private fun entitiesPane(): JComponent {
        try {
            openDatabaseFile().let { file ->
                changeTitle(file.name)
                history.add(parseEntities(file.readText()))
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
            FileGenerator().generate(
                file = openDatabaseFile(),
                entities = history.currentState
            )
            changeSavedStatus(true)
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
                },
                onEditRequest = {
                    editPanel?.changeEntity(it.entity)
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
            entity.relations.forEach { relation ->
                relation.refTable?.let { refTable ->
                    val fromBlock = entityBlocks.find { it.entity.name == refTable } ?: return@let
                    val toBlock = entityBlocks.find { it.entity.name == entity.name } ?: return@let

                    val arrow = RelationshipArrow(
                        fromBlock = fromBlock,
                        toBlock = toBlock,
                        relationType = relation.type
                    )
                    pane.add(arrow, JLayeredPane.PALETTE_LAYER)
                }
            }
        }

        history.currentState.filterIsInstance<Parsed.ManyToMany>().forEach { m2mEntity ->
            m2mEntity.relations.forEach second@ { relation ->
                val fromBlock = entityBlocks.find { it.entity.name == m2mEntity.name } ?: return@second
                val toBlock = entityBlocks.find { it.entity.name == relation.refTable } ?: return@second

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
        val newState = ArrayList(history.currentState)
        if (removed is Parsed.Embedded) {
            newState.forEach {
                it.fields.toMutableList().removeIf { field -> field.type == removed.name }
            }
        }
        if (removed is Parsed.Entity) {
            newState.removeIf { entity ->
                entity is Parsed.ManyToMany &&
                        entity.relations.any { it.refTable == removed.name }
            }
            newState.forEach { entity ->
                if (entity is Parsed.Entity) {
                    entity.fields.toMutableList().removeIf { field ->
                        entity.relations.any { relation ->
                            relation.refTable == removed.name &&
                                    relation.refColumn == field.name
                        }
                    }
                }
            }
        }
        newState.remove(removed)
        history.add(newState)
        entityBlocks.removeIf {
            it.entity == removed
        }
    }
}
