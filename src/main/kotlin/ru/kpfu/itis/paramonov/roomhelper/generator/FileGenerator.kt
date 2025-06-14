package ru.kpfu.itis.paramonov.roomhelper.generator

import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.getDatabaseType
import ru.kpfu.itis.paramonov.roomhelper.model.Field
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.model.Relation
import ru.kpfu.itis.paramonov.roomhelper.util.lineSeparator
import java.io.File

class FileGenerator {
    fun generate(file: File, entities: List<Parsed>) {
        val content = entities.joinToString("$lineSeparator") { entity ->
            when (entity) {
                is Parsed.Entity -> generateEntityBlock(entity)
                is Parsed.Embedded -> generateEmbeddedBlock(entity)
                is Parsed.ManyToMany -> generateManyToManyBlock(entity)
            }
        }
        file.writeText(content)
    }

    private fun generateEntityBlock(entity: Parsed.Entity): String {
        val relationFields = entity.relations.map { relation -> relation.name }
        val fields = entity.fields
            .filter { !relationFields.contains(it.name) }
            .joinToString("$lineSeparator\t\t") { generateFieldLine(it) }
        val relations = entity.relations.joinToString("$lineSeparator\t\t") { generateRelationLine(it) }
        val indices = entity.indexes.joinToString("$lineSeparator\t\t") { it.joinToString(", ") }

        return """
            |entity ${entity.name}:
            |    fields:
            |        $fields
            |${if (relations.isNotBlank()) 
                """|    relations:
                   |        $relations""".trimMargin() else ""
            }
            |${if (indices.isNotBlank())
                """|    indices:
                   |        $indices""".trimMargin() else ""
            }
        """.trimMargin()
            .lines()
            .filter { it.isNotEmpty() }
            .joinToString(separator = "$lineSeparator")
    }

    private fun generateEmbeddedBlock(embedded: Parsed.Embedded): String {
        val fields = embedded.fields.joinToString("$lineSeparator\t\t\t") { generateFieldLine(it) }
        return """
            |entity embed ${embedded.name}:
            |    fields:
            |        $fields
        """.trimMargin()
    }

    private fun generateManyToManyBlock(m2m: Parsed.ManyToMany): String {
        val relations = m2m.relations.joinToString("$lineSeparator\t\t") { relation ->
            generateRelationLine(relation)
        }

        return """
            |entity m2m ${m2m.name}:
            |    relations:
            |        $relations
        """.trimMargin()
    }

    private fun generateFieldLine(field: Field): String {
        val modifiers = listOfNotNull(
            if (field.isPrimaryKey) "pk" else null,
            if (field.isPartOfCompositeKey) "cpk" else null,
            if (field.isUnique) "unique" else null,
            if (field.isNotNull) "nnull" else null
        ).joinToString(" ")

        return "${field.name} ${getDatabaseType(field.type)} $modifiers".trim()
    }

    private fun generateRelationLine(relation: Relation): String {
        val ref = "ref ${relation.refTable}:${relation.refColumn}"
        return "${relation.name} ${getDatabaseType(relation.fieldType)} ${relation.type} $ref"
    }
}
