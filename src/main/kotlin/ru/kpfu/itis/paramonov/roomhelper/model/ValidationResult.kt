package ru.kpfu.itis.paramonov.roomhelper.model

sealed interface ValidationResult {
    object Success : ValidationResult

    data class Failure(val message: String) : ValidationResult
}