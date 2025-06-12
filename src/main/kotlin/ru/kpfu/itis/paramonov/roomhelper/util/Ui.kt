package ru.kpfu.itis.paramonov.roomhelper.util

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages

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
