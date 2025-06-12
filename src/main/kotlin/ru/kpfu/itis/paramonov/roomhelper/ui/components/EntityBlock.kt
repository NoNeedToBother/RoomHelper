package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.getDatabaseType
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.SwingConstants
import javax.swing.UIManager
import javax.swing.border.LineBorder

class EntityBlock(
    val entity: Parsed,
    private val onDeleteRequest: (EntityBlock) -> Unit,
    private val onEditRequest: (EntityBlock) -> Unit,
    private val onPositionChanged: (EntityBlock) -> Unit,
) : JPanel() {
    private var screenX = 0
    private var screenY = 0
    private var myX = 0
    private var myY = 0

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = LineBorder(JBColor.GRAY, 1)
        background = UIManager.getColor("Panel.background")
        isOpaque = true

        addComponentListener(object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent?) {
                onPositionChanged(this@EntityBlock)
            }
        })

        val header = JPanel(BorderLayout()).apply {
            background = JBColor(
                Color(150, 190, 240),
                Color(110, 150, 200)
            )
            isOpaque = true
            preferredSize = Dimension(200, 10)
            add(JBLabel(
                if (entity is Parsed.Embedded) "embedded " + entity.name else entity.name
            ).apply {
                foreground = Gray._1
                horizontalAlignment = SwingConstants.CENTER
                font = font.deriveFont(Font.BOLD)
            }, BorderLayout.CENTER)
        }

        val fieldsPanel = JPanel().apply {
            layout = GridLayout(entity.fields.size, 2, 5, 5)
            background = UIManager.getColor("Panel.background")
            isOpaque = true

            entity.fields.forEach { field ->
                add(JBLabel(field.name).apply {
                    foreground = JBColor(Gray._70, Gray._240)
                })
                add(JBLabel(getDatabaseType(field.type)).apply {
                    foreground = JBColor(Gray._50, Gray._200)
                    font = font.deriveFont(Font.ITALIC)
                })
            }
        }

        add(header)
        add(fieldsPanel)

        addMouseListeners()
    }

    val connectionPoints = mutableMapOf(
        ConnectionSide.START to Point(0, height / 2),
        ConnectionSide.END to Point(width, height / 2),
    )

    override fun setBounds(x: Int, y: Int, width: Int, height: Int) {
        super.setBounds(x, y, width, height)
        updateConnectionPoints()
    }

    fun getConnectionPoint(side: ConnectionSide): Point {
        return connectionPoints[side]?.let { point ->
            Point(location.x + point.x, location.y + point.y)
        } ?: throw RuntimeException("Unknown connection side")
    }

    private fun addMouseListeners() {
        val popupMenu = JPopupMenu().apply {
            add(JMenuItem("Edit").apply {
                addActionListener {
                    onEditRequest(this@EntityBlock)
                }
            })
            add(JMenuItem("Delete")).apply {
                addActionListener {
                    onDeleteRequest(this@EntityBlock)
                }
            }
        }

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e?.isPopupTrigger == true) {
                    popupMenu.show(this@EntityBlock, e.x, e.y)
                }
            }
            override fun mousePressed(e: MouseEvent) {
                screenX = e.xOnScreen
                screenY = e.yOnScreen
                myX = x
                myY = y
            }

            override fun mouseReleased(e: MouseEvent?) {
                if (e?.isPopupTrigger == true) {
                    popupMenu.show(this@EntityBlock, e.x, e.y)
                }
            }
        })

        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                val deltaX = e.xOnScreen - screenX
                val deltaY = e.yOnScreen - screenY

                setLocation(myX + deltaX, myY + deltaY)
            }
        })
    }

    private fun updateConnectionPoints() {
        connectionPoints[ConnectionSide.START] = Point(0, height / 2)
        connectionPoints[ConnectionSide.END] = Point(width, height / 2)
    }
}

enum class ConnectionSide {
    START, END
}
