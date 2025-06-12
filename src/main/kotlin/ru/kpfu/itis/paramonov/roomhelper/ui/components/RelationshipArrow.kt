package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import javax.swing.JComponent
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class RelationshipArrow(
    private val fromBlock: EntityBlock,
    private val toBlock: EntityBlock,
    private val relationType: String
) : JComponent() {

    companion object {
        private const val RELATION_SYMBOL_SIZE = 22
        private const val MANY_TO_MANY_SYMBOL_SIZE = 12
    }

    init {
        isOpaque = false
        setBounds(0, 0, Int.MAX_VALUE, Int.MAX_VALUE)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.color = JBColor(Gray._60, Gray._200)
        g2d.stroke = BasicStroke(1f)

        val (fromSide, toSide) = determineConnectionSides()
        val fromPoint = fromBlock.getConnectionPoint(fromSide)
        val toPoint = toBlock.getConnectionPoint(toSide)

        when (relationType) {
            "m2m" -> {
                val fromLineEnd = when (fromSide) {
                    ConnectionSide.START -> Point(fromPoint.x - MANY_TO_MANY_SYMBOL_SIZE, fromPoint.y)
                    ConnectionSide.END -> Point(fromPoint.x + MANY_TO_MANY_SYMBOL_SIZE, fromPoint.y)
                }

                val toLineEnd = when (toSide) {
                    ConnectionSide.START -> Point(toPoint.x - MANY_TO_MANY_SYMBOL_SIZE, toPoint.y)
                    ConnectionSide.END -> Point(toPoint.x + MANY_TO_MANY_SYMBOL_SIZE, toPoint.y)
                }

                drawManySymbol(g2d, fromPoint, fromSide)
                drawManySymbol(g2d, fromPoint, toSide)

                g2d.drawLine(toPoint.x, toPoint.y, toLineEnd.x, toLineEnd.y)

                drawRelationLine(g2d, fromLineEnd, toLineEnd)
            }
            "m2o" -> {
                val fromLineEnd = when (fromSide) {
                    ConnectionSide.START -> Point(fromPoint.x - MANY_TO_MANY_SYMBOL_SIZE, fromPoint.y)
                    ConnectionSide.END -> Point(fromPoint.x + MANY_TO_MANY_SYMBOL_SIZE, fromPoint.y)
                }
                drawManySymbol(g2d, fromPoint, fromSide)

                val toLineEnd = when (toSide) {
                    ConnectionSide.START -> Point(toPoint.x - RELATION_SYMBOL_SIZE, toPoint.y)
                    ConnectionSide.END -> Point(toPoint.x + RELATION_SYMBOL_SIZE, toPoint.y)
                }
                g2d.drawLine(toPoint.x, toPoint.y, toLineEnd.x, toLineEnd.y)
                drawRelationLine(g2d, fromLineEnd, toLineEnd)
            }
            else -> {
                val fromLineEnd = when (fromSide) {
                    ConnectionSide.START -> Point(fromPoint.x - RELATION_SYMBOL_SIZE, fromPoint.y)
                    ConnectionSide.END -> Point(fromPoint.x + RELATION_SYMBOL_SIZE, fromPoint.y)
                }
                g2d.drawLine(fromPoint.x, fromPoint.y, fromLineEnd.x, fromLineEnd.y)
                val toLineEnd = when (toSide) {
                    ConnectionSide.START -> Point(toPoint.x - RELATION_SYMBOL_SIZE, toPoint.y)
                    ConnectionSide.END -> Point(toPoint.x + RELATION_SYMBOL_SIZE, toPoint.y)
                }
                g2d.drawLine(toPoint.x, toPoint.y, toLineEnd.x, toLineEnd.y)
                drawRelationLine(g2d, fromLineEnd, toLineEnd)
            }
        }
    }

    private fun drawRelationLine(g: Graphics2D, fromPoint: Point, toPoint: Point) {
        val middlePointY = (fromPoint.y + toPoint.y) / 2
        g.drawLine(fromPoint.x, fromPoint.y, fromPoint.x, middlePointY)
        g.drawLine(fromPoint.x, middlePointY, toPoint.x, middlePointY)
        g.drawLine(toPoint.x, middlePointY, toPoint.x, toPoint.y)
    }

    private fun drawManySymbol(g: Graphics2D, position: Point, side: ConnectionSide) {
        val angle = when (side) {
            ConnectionSide.START -> Math.PI
            ConnectionSide.END -> 0.0
        }
        val lineEnd = Point(position.x + (MANY_TO_MANY_SYMBOL_SIZE * cos(angle)).toInt(), position.y)
        g.drawLine(position.x, position.y, lineEnd.x, lineEnd.y)

        val branchAngle1 = angle + Math.PI/4
        val branchAngle2 = angle - Math.PI/4

        val branch1End = Point(
            (position.x + MANY_TO_MANY_SYMBOL_SIZE * cos(angle)).toInt(),
            position.y + (MANY_TO_MANY_SYMBOL_SIZE * sin(branchAngle1)).toInt()
        )

        val branch2End = Point(
            (position.x + MANY_TO_MANY_SYMBOL_SIZE * cos(angle)).toInt(),
            position.y + (MANY_TO_MANY_SYMBOL_SIZE * sin(branchAngle2)).toInt()
        )

        g.drawLine(position.x, branch1End.y, branch1End.x, position.y)
        g.drawLine(position.x, branch2End.y, branch2End.x, position.y)
    }

    private fun determineConnectionSides(): Pair<ConnectionSide, ConnectionSide> {
        val distance1 = abs(fromBlock.getConnectionPoint(ConnectionSide.START).x -
                toBlock.getConnectionPoint(ConnectionSide.START).x)
        val distance2 = abs(fromBlock.getConnectionPoint(ConnectionSide.START).x -
                toBlock.getConnectionPoint(ConnectionSide.END).x)
        val distance3 = abs(fromBlock.getConnectionPoint(ConnectionSide.END).x -
                toBlock.getConnectionPoint(ConnectionSide.START).x)
        val distance4 = abs(fromBlock.getConnectionPoint(ConnectionSide.END).x -
                toBlock.getConnectionPoint(ConnectionSide.END).x)
        return when (minOf(distance1, distance2, distance3, distance4)) {
            distance1 -> ConnectionSide.START to ConnectionSide.START
            distance2 -> ConnectionSide.START to ConnectionSide.END
            distance3 -> ConnectionSide.END to ConnectionSide.START
            else -> ConnectionSide.END to ConnectionSide.END
        }
    }
}
