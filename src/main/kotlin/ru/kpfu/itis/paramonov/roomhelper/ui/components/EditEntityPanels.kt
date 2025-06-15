package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import ru.kpfu.itis.paramonov.roomhelper.model.Field
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.util.addTextChangedListener
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class FieldPanel(
    fieldWidth: Int, maximumWidth: Int, fieldHeight: Int,
    val entity: Parsed,
    val field: Field,
    private val onRemoveClicked: (FieldPanel) -> Unit,
    private val onNameChanged: (String) -> Unit,
    private val onTypeChanged: (String) -> Unit,
    private val onPrimaryKeyCheckBoxClicked: (checked: Boolean) -> Unit,
    private val onUniqueCheckBoxClicked: (checked: Boolean) -> Unit,
    private val onNotNullCheckBoxClicked: (checked: Boolean) -> Unit,
) : JPanel(BorderLayout()) {
    
    init {
        preferredSize = Dimension(fieldWidth, fieldHeight)
        maximumSize = Dimension(maximumWidth, fieldHeight)
        background = JBColor.background()

        add(JPanel(BorderLayout()).apply westPanel@ {
            preferredSize = Dimension(fieldWidth / 2, fieldHeight)
            maximumSize = Dimension(
                maximumWidth / 2 - fieldHeight,
                fieldHeight
            )

            add(RemoveButton(fieldHeight) {
                onRemoveClicked(this@FieldPanel)
            }, BorderLayout.WEST)

            add(JTextField(field.name).apply {
                toolTipText = "Edit field name"
                addTextChangedListener { name ->
                    onNameChanged(name)
                }
            }, BorderLayout.CENTER)
        }, BorderLayout.WEST)

        add(JPanel(BorderLayout()).apply {
            preferredSize = Dimension(fieldWidth / 2, fieldHeight)
            maximumSize = Dimension(maximumWidth / 2, fieldHeight)
            add(JTextField(field.type).apply {
                toolTipText = "Edit field type"
                addTextChangedListener { type ->
                    onTypeChanged(type)
                }
            }, BorderLayout.CENTER)
        }, BorderLayout.EAST)

        add(JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            if (entity is Parsed.Entity) {
                add(JBCheckBox("PK").apply {
                    isSelected = field.isPrimaryKey || field.isPartOfCompositeKey
                    toolTipText = "Is primary key or part of one"
                    addActionListener { onPrimaryKeyCheckBoxClicked(isSelected) }
                })

                add(JBCheckBox("Unique").apply {
                    isSelected = field.isUnique
                    toolTipText = "Is unique"
                    addActionListener { onUniqueCheckBoxClicked(isSelected) }
                })
            }

            add(JBCheckBox("Not null").apply {
                isSelected = field.isNotNull
                toolTipText = "Is not null"
                addActionListener { onNotNullCheckBoxClicked(isSelected) }
            })
        }, BorderLayout.SOUTH)
    }
}

class IndexPanel(
    fieldWidth: Int, maximumWidth: Int, fieldHeight: Int,
    val entity: Parsed, val index: List<String>,
    private val onRemoveClicked: (IndexPanel) -> Unit,
    private val onNewFieldAdded: (List<String>) -> Unit,
    private val onFieldRemoved: (List<String>) -> Unit,
) : JPanel(BorderLayout()) {

    private var mainPanel: JPanel? = null

    private var addButton: JButton? = null
    private var addComboBox: JComboBox<String>? = null

    private var removeButton: JButton? = null
    private var removeComboBox: JComboBox<String>? = null

    init {
        preferredSize = Dimension(fieldWidth, fieldHeight)
        maximumSize = Dimension(maximumWidth, fieldHeight)
        background = JBColor.background()

        add(JPanel(BorderLayout()).apply {
            mainPanel = this
            add(RemoveButton(fieldHeight / 2) {
                onRemoveClicked(this@IndexPanel)
            }, BorderLayout.WEST)
            add(JLabel(index.joinToString(", ")), BorderLayout.CENTER)
        }, BorderLayout.CENTER)

        add(JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            preferredSize = Dimension(fieldWidth, fieldHeight / 2)
            val allFields = entity.fields.map { it.name }
            val availableFields = allFields.minus(index)

            add(JButton("Add fields").apply {
                addButton = this
                addActionListener {
                    addButton?.isVisible = false
                    addComboBox?.isVisible = true
                    addComboBox?.showPopup()
                }
            })
            add(ComboBox<String>().apply {
                addComboBox = this
                isVisible = false
                model = CollectionComboBoxModel<String>().apply { add(availableFields) }

                addActionListener {
                    if (selectedItem != null) {
                        val selected = selectedItem as String
                        onNewFieldAdded(
                            index.toMutableList().apply { add(selected) }
                        )
                    }
                }
            })
            add(JButton("Remove fields").apply {
                removeButton = this
                addActionListener {
                    removeButton?.isVisible = false
                    removeComboBox?.isVisible = true
                    removeComboBox?.showPopup()
                }
            })
            add(ComboBox<String>().apply {
                removeComboBox = this
                isVisible = false
                model = CollectionComboBoxModel<String>().apply { add(index) }

                addActionListener {
                    if (selectedItem != null) {
                        val selected = selectedItem as String
                        onFieldRemoved(
                            index.toMutableList().apply { remove(selected) }
                        )
                    }
                }
            })
        }, BorderLayout.SOUTH)
    }
}
