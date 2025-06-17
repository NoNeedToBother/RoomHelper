package ru.kpfu.itis.paramonov.roomhelper.util

import ru.kpfu.itis.paramonov.roomhelper.model.Field
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import ru.kpfu.itis.paramonov.roomhelper.model.Relation
import java.util.Locale.getDefault

fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(
    getDefault()
) else it.toString() }

fun Parsed.deepCopy(): Parsed {
    return when (this) {
        is Parsed.Entity -> deepCopy()
        is Parsed.ManyToMany -> deepCopy()
        is Parsed.Embedded -> deepCopy()
    }
}

fun Parsed.Entity.deepCopy(): Parsed.Entity {
    return copy(
        fields = ArrayList(fields.map { it.copy() }),
        relations = ArrayList(relations.map { it.copy() }),
        indexes = ArrayList(indexes.map { ArrayList(it) })
    )
}

fun Parsed.ManyToMany.deepCopy(): Parsed.ManyToMany {
    return copy(
        fields = ArrayList(fields.map { it.copy() }),
        relations = ArrayList(relations.map { it.copy() }),
    )
}

fun Parsed.Embedded.deepCopy(): Parsed.Embedded {
    return copy(
        fields = ArrayList(fields.map { it.copy() }),
    )
}

fun Parsed.relations(): List<Relation> {
    return when (this) {
        is Parsed.Entity -> relations
        is Parsed.ManyToMany -> relations
        is Parsed.Embedded -> emptyList()
    }
}

// needed to check on field change whether buffer field and other entity overlap in everything other than
// edited field property to not edit the wrong field, if they do coincide than it does not matter
// which one of those two entities should be edited
fun Field.equalsIgnoring(
    other: Field,
    ignoreName: Boolean = false,
    ignoreType: Boolean = false,
    ignoreIsPrimaryKey: Boolean = false,
    ignoreIsCompositeKey: Boolean = false,
    ignoreIsUnique: Boolean = false,
    ignoreIsNotNull: Boolean = false,
    ignoreIsEmbedded: Boolean = false,
): Boolean {
    return (ignoreName || name == other.name) && (ignoreType || type == other.type) &&
            (ignoreIsPrimaryKey || isPrimaryKey == other.isPrimaryKey) &&
            (ignoreIsCompositeKey || isPartOfCompositeKey == other.isPartOfCompositeKey) &&
            (ignoreIsUnique || isUnique == other.isUnique) && (ignoreIsNotNull || isNotNull == other.isNotNull) &&
            (ignoreIsEmbedded || isEmbedded == other.isEmbedded)
}

// same as Field.equalsIgnoring but for relations
fun Relation.equalsIgnoring(
    other: Relation,
    ignoreName: Boolean = false,
    ignoreType: Boolean = false,
    ignoreFieldType: Boolean = false,
    ignoreRefTable: Boolean = false,
    ignoreRefColumn: Boolean = false,
): Boolean {
    return (ignoreName || name == other.name) && (ignoreType || type == other.type) &&
            (ignoreFieldType || fieldType == other.fieldType) &&
            (ignoreRefTable || refTable == other.refTable) && (ignoreRefColumn || refColumn == other.refColumn)
}
