package ru.kpfu.itis.paramonov.roomhelper.generator.util.database

import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.util.capitalize
import ru.kpfu.itis.paramonov.roomhelper.util.lineSeparator

fun generateDatabase(entities: List<Parsed.Entity>, databaseName: String): String {
    val entityClasses = """entities = [
        |${entities.joinToString(",$lineSeparator") { it.name.capitalize() + "::class" }.prependIndent("\t")}
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
        |    ${entityClasses.prependIndent("\t")},
        |    version = 1
        |)${if (hasDateFields) "$lineSeparator@TypeConverters(DateConverter::class)" else ""}
        |abstract class ${databaseName.capitalize()} : RoomDatabase() {
        |${daoProperties.prependIndent("\t")}
        |}
    """.trimMargin()
}
