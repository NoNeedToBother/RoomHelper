package ru.kpfu.itis.paramonov.roomhelper.ui.components

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionComboBoxModel
import ru.kpfu.itis.paramonov.roomhelper.model.ValidationResult
import ru.kpfu.itis.paramonov.roomhelper.util.addTextChangedListener
import ru.kpfu.itis.paramonov.roomhelper.util.validateEntityName
import javax.swing.Action
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

enum class EntityType {
    Regular, Embedded, ManyToMany
}

class AddEntityDialog(
    private val onChosen: (String, EntityType) -> Unit,
    private val isNameUnique: (String) -> Boolean,
) : DialogWrapper(false) {

    private var entityName: String? = null
    private var entityType: EntityType? = null

    init {
        title = "Add Entity"
        init()
    }

    override fun createCenterPanel(): JComponent? {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)

            add(JTextField().apply {
                toolTipText = "Enter entity name"
                addTextChangedListener { text ->
                    entityName = text
                }
            })

            add(ComboBox<EntityType>().apply {
                model = CollectionComboBoxModel(EntityType.entries.toTypedArray().toList())
                addActionListener {
                    selectedItem?.let { entityType = it as EntityType }
                }
            })
        }
    }

    override fun doOKAction() {
        entityName?.let { name ->
            val validationResult = validateEntityName(name, isNameUnique)
            when (validationResult) {
                is ValidationResult.Success ->
                    entityType?.let { type ->
                        onChosen(name, type)
                        super.doOKAction()
                    } ?: Messages.showErrorDialog(
                        "Entity type is not chosen",  "Add Fail"
                        )
                is ValidationResult.Failure ->
                    Messages.showErrorDialog(validationResult.message,  "Add Fail")
            }
        } ?: Messages.showErrorDialog(
            "Entity name is empty",  "Add Fail"
        )
    }
}

class UnsavedCloseRationale(
    private val onCloseAgree: () -> Unit,
) : DialogWrapper(false) {

    init {
        init()
    }

    override fun createCenterPanel(): JComponent? {
        return JPanel().apply {
            add(JLabel("Are you sure you want to close without saving?"))
        }
    }

    override fun getOKAction(): Action {
        return super.getOKAction().apply {
            putValue(Action.NAME, "Yes")
        }
    }

    override fun getCancelAction(): Action {
        return super.getCancelAction().apply {
            putValue(Action.NAME, "No")
        }
    }

    override fun doOKAction() {
        onCloseAgree()
        super.doOKAction()
    }
}

class NewFileSaveDialog(
    private val onFileNameSubmitted: (String) -> Unit
) : DialogWrapper(false) {

    private var fileName = ""

    init {
        init()
    }

    override fun createCenterPanel(): JComponent? {
        return JPanel().apply {
            add(JTextField().apply {
                addTextChangedListener { fileName ->
                    this@NewFileSaveDialog.fileName = fileName
                }
            })
        }
    }

    override fun doOKAction() {
        onFileNameSubmitted(fileName)
        super.doOKAction()
    }
}
