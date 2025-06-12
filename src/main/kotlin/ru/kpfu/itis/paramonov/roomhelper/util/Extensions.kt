package ru.kpfu.itis.paramonov.roomhelper.util

import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
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
        indices = ArrayList(indices.map { ArrayList(it) })
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
