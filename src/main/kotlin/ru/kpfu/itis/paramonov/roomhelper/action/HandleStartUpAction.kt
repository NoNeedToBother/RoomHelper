package ru.kpfu.itis.paramonov.roomhelper.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import ru.kpfu.itis.paramonov.roomhelper.state.DatabaseFilePersistentState
import ru.kpfu.itis.paramonov.roomhelper.ui.DatabaseFileChooserDialog
import ru.kpfu.itis.paramonov.roomhelper.ui.FileOpenResult
import ru.kpfu.itis.paramonov.roomhelper.ui.RoomHelperWindow

class HandleStartUpAction: AnAction() {
    override fun actionPerformed(p0: AnActionEvent) {
        val databaseFileState = DatabaseFilePersistentState.getInstance()

        val file = databaseFileState.state.value
        if (!file.isNullOrEmpty()) {
            val roomHelperWindow = RoomHelperWindow()
            roomHelperWindow.show()
        } else {
            val dialog = DatabaseFileChooserDialog()
            val result = dialog.showAndGetResult()

            when(result) {
                is FileOpenResult.Existing -> {
                    databaseFileState.state.value = result.file
                    RoomHelperWindow().show()
                }
                is FileOpenResult.New -> {
                    databaseFileState.state.value = null
                    RoomHelperWindow(isNewFile = true).show()
                }
                is FileOpenResult.NoFile -> {}
            }
        }
    }
}
