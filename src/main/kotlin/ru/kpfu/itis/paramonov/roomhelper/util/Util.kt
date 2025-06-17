package ru.kpfu.itis.paramonov.roomhelper.util

fun getFieldType(type: String): String {
    return when (type) {
        "int" -> "Int"
        "str" -> "String"
        "bool" -> "Boolean"
        "long" -> "Long"
        "float" -> "Float"
        "double" -> "Double"
        "date" -> "Date"
        else -> type
    }
}

fun getDatabaseType(fieldType: String): String {
    return when (fieldType) {
        "Int" -> "int"
        "String" -> "str"
        "Boolean" -> "bool"
        "Long" -> "long"
        "Float" -> "float"
        "Double" -> "double"
        "Date" -> "date"
        else -> fieldType
    }
}