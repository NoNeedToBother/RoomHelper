package ru.kpfu.itis.paramonov.roomhelper.model

sealed class Parsed {
    abstract val name: String
    abstract var fields: List<Field>

    data class Entity(
        override val name: String,
        override var fields: List<Field> = emptyList(),
        var relations: List<Relation> = emptyList(),
        var indexes: List<List<String>> = emptyList(),
    ) : Parsed()

    data class Embedded(
        override val name: String,
        override var fields: List<Field> = emptyList(),
    ) : Parsed()

    data class ManyToMany(
        override val name: String,
        override var fields: List<Field> = emptyList(),
        var relations: List<Relation> = emptyList(),
    ) : Parsed()
}

data class Field(
    val name: String,
    var type: String,
    var isPrimaryKey: Boolean = false,
    var isPartOfCompositeKey: Boolean = false,
    var isUnique: Boolean = false,
    var isNotNull: Boolean = false,
    var isEmbedded: Boolean = false,
)

data class Relation(
    val name: String,
    val type: String,
    val fieldType: String,
    val refTable: String,
    val refColumn: String,
)
