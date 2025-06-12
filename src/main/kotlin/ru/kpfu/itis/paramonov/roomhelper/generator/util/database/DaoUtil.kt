package ru.kpfu.itis.paramonov.roomhelper.generator.util.database

fun generateDao(entityName: String): String {
    return """
        |import androidx.room.Dao
        |
        |@Dao
        |interface ${entityName}Dao {
        |}
    """.trimMargin()
}
