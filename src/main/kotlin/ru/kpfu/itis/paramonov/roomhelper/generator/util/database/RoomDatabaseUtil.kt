package ru.kpfu.itis.paramonov.roomhelper.generator.util.database

import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.util.capitalize
import ru.kpfu.itis.paramonov.roomhelper.util.lineSeparator
import ru.kpfu.itis.paramonov.roomhelper.util.tab

fun generateDatabase(entities: List<Parsed.Entity>, databaseName: String): String {
    val entityClasses = """entities = [
        |${entities.joinToString(",$lineSeparator") { it.name.capitalize() + "::class" }.prependIndent(tab)}
        |]
    """.trimIndent()
    val daoProperties = entities.joinToString("$lineSeparator") {
        "abstract fun ${it.name.lowercase()}Dao(): ${it.name.capitalize()}Dao"
    }
    val hasDateFields = entities.any { entity ->
        entity.fields.any { it.type == "Date" }
    }
    val imports = """
        |import androidx.room.Database
        |import androidx.room.RoomDatabase
        |${if (hasDateFields) """
            |import androidx.room.TypeConverters
        """.trimMargin() else ""}
    """.trimMargin()

    return """
        |${imports.lines().filter { it.isNotBlank() }.joinToString("$lineSeparator") }
        |
        |@Database(
        |${tab}${entityClasses.prependIndent(tab)},
        |${tab}version = 1
        |)${if (hasDateFields) "$lineSeparator@TypeConverters(DateConverter::class)" else ""}
        |abstract class ${databaseName.capitalize()} : RoomDatabase() {
        |${daoProperties.prependIndent(tab)}
        |}
    """.trimMargin()
}
