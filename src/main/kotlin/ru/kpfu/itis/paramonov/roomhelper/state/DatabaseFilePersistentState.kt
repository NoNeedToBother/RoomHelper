package ru.kpfu.itis.paramonov.roomhelper.state

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service

@Service
@State(name = "database-file", storages = [Storage(StoragePathMacros.CACHE_FILE)])
class DatabaseFilePersistentState :
    SimplePersistentStateComponent<DatabaseFilePersistentState.State>(State()) {

    class State : BaseState() {
        var value by string(null)
    }

    companion object {
        fun getInstance(): DatabaseFilePersistentState = service()
    }
}
