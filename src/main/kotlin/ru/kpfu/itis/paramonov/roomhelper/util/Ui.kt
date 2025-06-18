package ru.kpfu.itis.paramonov.roomhelper.util

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.filechooser.FileNameExtensionFilter

fun showErrorMessage(e: Throwable) {
    Messages.showErrorDialog(
        e.message ?: "Error: ${e.javaClass.simpleName}",
        "Save Fail"
    )
}

fun openFileChooser(
    onFileChosen: (String) -> Unit,
) {
    val descriptor = FileChooserDescriptor(
        true, false, false, false, false, false
    )
        .withTitle("Choose Configuration File")

    FileChooser.chooseFile(descriptor, null, null)?.let { file ->
        if (file.extension == "rh") {
            onFileChosen(file.path)
        } else {
            Messages.showErrorDialog(
                "Please provide .rh file",
                "Invalid File Format"
            )
            openFileChooser(onFileChosen)
        }
    }
}

fun openFileSaver(
    onFileChosen: (String) -> Unit,
) {
    val chooser = project?.projectFile?.path?.let { JFileChooser(it) } ?: JFileChooser()
    chooser.apply {
        dialogTitle = "Enter New Configuration File"
        fileFilter = FileNameExtensionFilter("Configuration file", "rh")
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        onFileChosen(chooser.selectedFile.path)
    }
}

fun openGenerationDestination(
    onDirChosen: (String) -> Unit
) {
    val descriptor = FileChooserDescriptor(
        false, true, false, false, false, false
    )
        .withTitle("Choose Code Generation Destination")

    FileChooser.chooseFile(descriptor, null, null)?.let { dir ->
        onDirChosen(dir.path)
    }
}

fun JTextField.addTextChangedListener(onTextChange: (String) -> Unit) {
    document.addDocumentListener(object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) {
            onChange()
        }

        override fun removeUpdate(e: DocumentEvent?) {
            onChange()
        }

        override fun changedUpdate(e: DocumentEvent?) {
            onChange()
        }

        private fun onChange() {
            onTextChange(text)
        }
    })
}

fun JComponent.recursiveDisable() {
    components.forEach { component ->
        component.isEnabled = false
        if (component is JComponent) {
            component.recursiveDisable()
        }
    }
}
