package ru.kpfu.itis.paramonov.roomhelper.generator.util.database

import ru.kpfu.itis.paramonov.roomhelper.model.Field
import ru.kpfu.itis.paramonov.roomhelper.model.ManyToManyRelation
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.model.Relation
import kotlin.math.min

fun parseEntities(text: String): List<Parsed> {
    val entities = mutableListOf<Parsed>()
    val entityBlocks = text.split("entity ").drop(1)

    for (block in entityBlocks) {
        val lines = block.lines().map { it.trim() }.filter { !it.startsWith("//") }
        val isEmbeddable = lines.first().startsWith("embed ")
        val isManyToMany = lines.first().startsWith("m2m ")

        val name = when {
            isEmbeddable -> lines.first().removePrefix("embed ").removeSuffix(":")
            isManyToMany -> lines.first().removePrefix("m2m ").removeSuffix(":")
            else -> lines.first().removeSuffix(":")
        }

        val fieldsStart = lines.indexOfFirst { it == "fields:" } + 1
        val relationsStart = lines.indexOfFirst { it == "relations:" }.let {
            if (it == -1) lines.size + 1 else it + 1
        }
        val indicesStart = lines.indexOfFirst { it == "indices:" }.let {
            if (it == -1) lines.size + 1 else it + 1
        }

        val manyToManyRelations = mutableListOf<ManyToManyRelation>()
        val fields = lines
            .subList(fieldsStart, min(indicesStart - 1, relationsStart - 1))
            .filter { it.isNotBlank() }
            .map {
                if (isManyToMany) {
                    manyToManyRelations.add(parseManyToManyEntityField(it))
                }
                parseField(it)
            }
            .toMutableList()

        val relations = if (!isEmbeddable && relationsStart < lines.size)
            lines
                .subList(relationsStart, indicesStart - 1)
                .filter { it.isNotBlank() }
                .map {
                    val relation = parseRelation(it)
                    fields.add(Field(name = relation.name, type = relation.fieldType))
                    relation
                }
        else emptyList()

        val indices = if (indicesStart < lines.size) {
            lines.subList(indicesStart, lines.size)
                .filter { it.isNotBlank() }
                .map { it.split(",").map { col -> col.trim() } }
        } else emptyList()

        if (isEmbeddable) {
            entities.add(Parsed.Embedded(name, fields))
        }
        else if (isManyToMany) {
            entities.add(Parsed.ManyToMany(name, fields, manyToManyRelations))
        }
        else {
            entities.add(Parsed.Entity(name, fields, relations, indices))
        }
    }

    return entities
}

fun parseField(line: String): Field {
    val parts = line.split("\\s+".toRegex())
    val type = getFieldType(parts[1])
    return Field(
        name = parts[0],
        type = type,
        isPrimaryKey = parts.contains("pk"),
        isPartOfCompositeKey = parts.contains("cpk"),
        isNotNull = parts.contains("nnull"),
        isUnique = parts.contains("unique"),
        isEmbedded = parts[1] == type,
    )
}

fun parseManyToManyEntityField(line: String): ManyToManyRelation {
    val parts = line.split("\\s+".toRegex())

    val refIndex = parts.indexOfFirst { it == "ref" } + 1
    val relation = parts[refIndex].split(":")

    return ManyToManyRelation(
        refColumn = relation[1],
        refTable = relation[0]
    )
}

fun parseRelation(line: String): Relation {
    val parts = line.split("\\s+".toRegex())
    val type = parts[2].removeSuffix(":")

    val refParts = parts[4].split(":")
    return Relation(
        name = parts[0],
        type = type,
        fieldType = getFieldType(parts[1]),
        refTable = refParts[0],
        refColumn = refParts[1],
    )
}

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
