package ru.kpfu.itis.paramonov.roomhelper.generator.util.database

import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.util.capitalize
import ru.kpfu.itis.paramonov.roomhelper.util.tab

fun generateOneToOneRelationshipClasses(entities: List<Parsed.Entity>): List<RelationshipClass> {
    val result = mutableListOf<RelationshipClass>()

    entities.forEach { entity ->
        entity.relations.filter { it.type == "o2o" }.forEach { relation ->
            val parentEntity = entities.find { it.name.equals(relation.refTable, true) }

            parentEntity?.let {
                val className = "${parentEntity.name.capitalize()}And${entity.name.capitalize()}"

                val code = """
                    |data class $className(
                    |${tab}@Embedded val ${parentEntity.name.lowercase()}: ${parentEntity.name},
                    |${tab}@Relation(
                    |${tab}${tab}parentColumn = "${relation.refColumn}",
                    |${tab}${tab}entityColumn = "${relation.name}"
                    |${tab})
                    |${tab}val ${entity.name.lowercase()}: ${entity.name.capitalize()}
                    |)
                """.trimMargin()

                result.add(RelationshipClass(code = code, fileName = className))
            }
        }
    }
    return result
}

fun generateManyToOneRelationshipClasses(entities: List<Parsed.Entity>): List<RelationshipClass> {
    val result = mutableListOf<RelationshipClass>()

    entities.forEach { entity ->
        entity.relations.filter { it.type == "m2o" }.forEach { relation ->
            val parentEntity = entities.find { it.name.equals(relation.refTable, true) }

            parentEntity?.let {
                val className = "${parentEntity.name.capitalize()}}With${entity.name.capitalize()}s"

                val code = """
                    |data class $className(
                    |${tab}@Embedded val ${parentEntity.name.lowercase()}: ${parentEntity.name.capitalize()}
                    |${tab}@Relation(
                    |${tab}${tab}parentColumn = "${relation.refColumn}",
                    |${tab}${tab}entityColumn = "${relation.name}"
                    |${tab})
                    |${tab}val ${entity.name.lowercase()}List: List<${entity.name.capitalize()}>
                    |)
                """.trimMargin()

                result.add(RelationshipClass(code = code, fileName = className))
            }
        }
    }
    return result
}

fun generateManyToManyRelationshipClasses(entities: List<Parsed.ManyToMany>): List<RelationshipClass> {
    return entities.flatMap { m2mEntity ->
        listOf(
            generateManyToManyRelationshipJunction(
                entityName = m2mEntity.name,
                parentClass = m2mEntity.relations[0].refTable,
                childClass = m2mEntity.relations[1].refTable,
                parentColumn = m2mEntity.relations[0].refColumn,
                childColumn = m2mEntity.relations[1].refColumn,
                entityParentColumn = m2mEntity.relations[0].name,
                entityChildColumn = m2mEntity.relations[1].name,
            ),
            generateManyToManyRelationshipJunction(
                entityName = m2mEntity.name,
                parentClass = m2mEntity.relations[1].refTable,
                childClass = m2mEntity.relations[0].refTable,
                parentColumn = m2mEntity.relations[1].refColumn,
                childColumn = m2mEntity.relations[0].refColumn,
                entityParentColumn = m2mEntity.relations[1].name,
                entityChildColumn = m2mEntity.relations[0].name,
            )
        )
    }
}

private fun generateManyToManyRelationshipJunction(
    parentColumn: String,
    childColumn: String,
    parentClass: String,
    childClass: String,
    entityName: String,
    entityParentColumn: String,
    entityChildColumn: String,
): RelationshipClass {
    val code = """
        |data class ${parentClass}With${childClass}sJunction(
        |${tab}@Embedded
        |${tab}val ${parentClass.lowercase()}: $parentClass
        |
        |${tab}@Relation(
        |${tab}${tab}parentColumn = "$parentColumn",
        |${tab}${tab}entityColumn = "$childColumn",
        |${tab}${tab}entity = ${childClass}::class,
        |${tab}${tab}associateBy = Junction(${entityName}::class, parentColumn = $entityParentColumn, childColumn = $entityChildColumn)
        |${tab})
        |${tab}val ${childClass.lowercase()}List: List<${childClass}>
        |)
    """.trimMargin()
    return RelationshipClass(code = code, fileName = "${parentClass}With${childClass}sJunction")
}

data class RelationshipClass(
    val code: String,
    val fileName: String,
)
