package ru.kpfu.itis.paramonov.roomhelper.util

import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.model.ValidationResult

fun validateEntityName(
    name: String, isNameUnique: (String) -> Boolean
): ValidationResult {
    return if (name.isEmpty()) {
        ValidationResult.Failure("Entity name is empty")
    }
    else if (name.capitalize() != name) {
        ValidationResult.Failure("Entity name should start with capital letter")
    }
    else if (!isNameUnique(name)) {
        ValidationResult.Failure("Entity with this name already exists")
    }
    else ValidationResult.Success
}

fun validateEditedEntity(edited: Parsed, other: List<Parsed>): ValidationResult {
    val validations = listOf(
        validateFields(edited, other),
        if (edited is Parsed.Entity) validateIndexes(edited) else ValidationResult.Success,
        validateRelations(edited, other),
    )
    return validations.firstIsInstanceOrNull<ValidationResult.Failure>()?.let {
        ValidationResult.Failure(it.message)
    } ?: ValidationResult.Success
}

private fun validateFields(edited: Parsed, other: List<Parsed>): ValidationResult {
    val fields = edited.fields
    val fieldNames = fields.map { it.name }
    val uniqueFieldNames = fields.map { it.name }.toSet()

    if (fieldNames.any { it.isEmpty() }) return ValidationResult.Failure("Some field names are empty")
    if (fields.any { it.type.isEmpty() }) return ValidationResult.Failure("Some field types are empty")

    if (fieldNames.size != uniqueFieldNames.size)
        return ValidationResult.Failure("Field names are not unique")

    fieldNames.forEach {
        if (it == it.capitalize())
            return ValidationResult.Failure("Field name $it starts with capital letter")
    }

    if (edited is Parsed.Entity && fields.none { it.isPrimaryKey } && fields.none { it.isPartOfCompositeKey })
        return ValidationResult.Failure("No primary keys are set")

    fields.forEach { field ->
        if (!typeList.contains(field.type) &&
            other.none { it.name == field.type && it.name != edited.name }) {
            return ValidationResult.Failure(
                "Unsupported field type for field ${field.name}:\n" +
                        "Should be Int, Long, Double, Float, Boolean, String, Date or another Embedded table"
            )
        }
    }
    if (edited is Parsed.Entity) {
        fields.forEach {
            if (it.isPrimaryKey && !it.isNotNull || it.isPartOfCompositeKey && !it.isNotNull)
                return ValidationResult.Failure("Primary key ${it.name} is nullable")
        }
    }

    return ValidationResult.Success
}

private fun validateIndexes(edited: Parsed.Entity): ValidationResult {
    val indexes = edited.indexes
    val fieldNames = edited.fields.map { it.name }

    indexes.forEach { index ->
        if (index.any { indexPart -> !fieldNames.contains(indexPart) })
            return ValidationResult.Failure(
                "Index ${index.printIndex()} is corrupted, remove invalid fields"
            )
    }

    indexes.forEach { index ->
        if (index.size != index.toSet().size)
            return ValidationResult.Failure(
                "Index ${index.printIndex()} is corrupted, remove repeated fields"
            )
    }

    return ValidationResult.Success
}

private fun validateRelations(edited: Parsed, other: List<Parsed>): ValidationResult {
    if (edited is Parsed.Embedded) return ValidationResult.Success

    val relations = edited.relations()
    val regularEntityNames = other.filterIsInstance<Parsed.Entity>().map { it.name }

    val relationNames = relations.map { it.name }

    if (relationNames.size != relationNames.toSet().size) {
        return ValidationResult.Failure("Relation names are not unique")
    }

    if (edited is Parsed.ManyToMany && relations.size != 2)
        return ValidationResult.Failure("ManyToMany entities should have two m2m relations")

    if (relations.any { it.refTable == edited.name })
        return ValidationResult.Failure("Entity refers to itself")

    val refParts = relations.map { it.refTable to it.refColumn }
    if (refParts.size != refParts.toSet().size)
        return ValidationResult.Failure("Some relations refer to same table and column")

    relations.forEach {
        if (!regularEntityNames.contains(it.refTable))
            return ValidationResult.Failure("Reference table ${it.refTable} does not exist")
    }
    relations.forEach { relation ->
        if (regularEntityNames.contains(relation.refTable) &&
            !other.first { it.name == relation.refTable }.fields.map { it.name }.contains(relation.refColumn)
            )
            return ValidationResult.Failure(
                "Reference column ${relation.refColumn} for reference table ${relation.refTable} does not exist"
            )
    }
    relations.forEach { relation ->
        val expectedType = other.first { it.name == relation.refTable }.fields.first { it.name == relation.refColumn }.type
        if (regularEntityNames.contains(relation.refTable) && expectedType != relation.fieldType)
            return ValidationResult.Failure(
                "Field type for ${relation.refTable}:${relation.refColumn} is corrupted, " +
                        "expected $expectedType and got ${relation.fieldType}, fix configuration file"
            )
    }

    return ValidationResult.Success
}

private val typeList = listOf("Int", "Long", "Double", "Float", "Boolean", "String", "Date")

private fun List<String>.printIndex(): String {
    return joinToString { ", " }
}