package ru.kpfu.itis.paramonov.roomhelper.generator.util.database

import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.util.lineSeparator
import ru.kpfu.itis.paramonov.roomhelper.util.tab

fun generateRoomEntity(entity: Parsed.Entity): String {
    val imports = mutableListOf(
        "import androidx.room.Entity",
        "import androidx.room.PrimaryKey",
        "import androidx.room.ColumnInfo",
    )

    val uniqueFields = entity.fields.filter { it.isUnique }
    val uniqueIndices = uniqueFields.map { field ->
        """
        Index(value = ["${field.name}"], unique = true)
        """.trimIndent()
    }
    val indices = entity.indexes.map {
        """
        Index(value = ["${it.joinToString(", ")}"])
        """.trimIndent()
    }

    val compositeKeyFields = entity.fields.filter { it.isPartOfCompositeKey }

    val foreignKeys = entity.relations
        .filter { it.type == "m2o" }
        .map { relation ->
            """ForeignKey(
            |${tab}entity = ${relation.refTable}::class,
            |${tab}parentColumns = ["${relation.refColumn}"],
            |${tab}childColumns = ["${relation.name}"],
            |${tab}onDelete = ForeignKey.CASCADE
            |)
            """.trimMargin()
        }

    val entityAnnotation = StringBuilder("""@Entity(
        |${"""tableName = "${entity.name.lowercase()}"""".prependIndent(tab)},
        |${if (foreignKeys.isNotEmpty())
        """foreignKeys = [
                |${foreignKeys.joinToString().prependIndent(tab)}
                |],""".trimMargin().prependIndent(tab) else ""
    }
        |${if (compositeKeyFields.isNotEmpty())
        """primaryKeys = [
                |${compositeKeyFields.joinToString(", ") { "\"${it.name}\"" }.prependIndent(tab)}
                |],""".trimMargin().prependIndent(tab) else ""
    }
        |${if (uniqueIndices.isNotEmpty() || indices.isNotEmpty())
        """indices = [
                |${uniqueIndices.joinToString(separator = ",$lineSeparator").prependIndent(tab)}
                |${indices.joinToString(separator = ",$lineSeparator").prependIndent(tab)}
                |],
            """.trimMargin().lines().filter { it.isNotBlank() }.joinToString("$lineSeparator").prependIndent(tab) else ""
    }
        |)
        """.trimMargin())

    if (foreignKeys.isNotEmpty()) {
        imports.add("import androidx.room.ForeignKey")
    }
    if (uniqueIndices.isNotEmpty() || entity.indexes.isNotEmpty()) {
        imports.add("import androidx.room.Index")
    }
    if (entity.fields.any { it.isEmbedded }) {
        imports.add("import androidx.room.Embedded")
    }
    if (entity.fields.any { it.type == "Date" }) {
        imports.add("import java.util.Date")
    }

    val fieldsCode = entity.fields.joinToString("$lineSeparator") { field ->
        val annotations = mutableListOf<String>()
        if (field.isPrimaryKey) annotations.add("@PrimaryKey")
        if (field.isEmbedded) annotations.add("@Embedded")
        val nullable = if (!field.isNotNull && !field.isPrimaryKey) "?" else ""
        """${annotations.joinToString("$lineSeparator")}
           |val ${field.name}: ${field.type}$nullable,
        """.trimMargin()
    }

    return """
        |${imports.joinToString(separator = "$lineSeparator")}
        |
        |${entityAnnotation.lines().filter { it.isNotBlank() }.joinToString("$lineSeparator") }
        |data class ${entity.name}(
        |${fieldsCode.prependIndent(tab)}
        |)
        """.trimMargin()
}

fun generateRoomEmbedded(entity: Parsed.Embedded): String {
    val fieldsCode = entity.fields.joinToString("$lineSeparator") { field ->
        val nullable = if (!field.isNotNull) "?" else ""
        "val ${field.name}: ${field.type}$nullable"
    }

    return """
        |data class ${entity.name}(
        |${fieldsCode.prependIndent(tab)}
        |)
    """.trimMargin()
}

fun generateManyToManyEntity(entity: Parsed.ManyToMany): String {
    val fieldsCode = entity.fields.joinToString(",$lineSeparator") { field ->
        val refParts = field.type.split(" ref ")
        "val ${field.name}: ${refParts[0]}"
    }

    val primaryKeys = entity.fields.joinToString(", ") { "\"${it.name}\"" }

    return """
        |@Entity(primaryKeys = [$primaryKeys])
        |data class ${entity.name}(
        |${fieldsCode.prependIndent(tab)}
        |)
    """.trimMargin()
}
