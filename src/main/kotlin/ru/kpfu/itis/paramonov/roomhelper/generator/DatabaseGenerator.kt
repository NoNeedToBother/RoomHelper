package ru.kpfu.itis.paramonov.roomhelper.generator

import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.generateDao
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.generateDatabase
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.generateManyToManyEntity
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.generateManyToManyRelationshipClasses
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.generateManyToOneRelationshipClasses
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.generateOneToOneRelationshipClasses
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.generateRoomEmbedded
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.generateRoomEntity
import ru.kpfu.itis.paramonov.roomhelper.generator.util.database.parseEntities
import ru.kpfu.itis.paramonov.roomhelper.model.Parsed
import java.io.File
import kotlin.collections.forEach
import kotlin.io.nameWithoutExtension
import kotlin.io.readText

class DatabaseGenerator(
    private val inputFile: File,
    private val outputDir: File,
) {
    fun generate() {
        val databaseName = inputFile.nameWithoutExtension
            .replaceFirstChar { it.uppercase() }
            .plus("Database")

        if (!outputDir.exists()) outputDir.mkdir()

        val entities = parseEntities(inputFile.readText())
        entities.forEach { entity ->
            val kotlinCode = when(entity) {
                is Parsed.Entity -> generateRoomEntity(entity)
                is Parsed.Embedded -> generateRoomEmbedded(entity)
                is Parsed.ManyToMany -> generateManyToManyEntity(entity)
            }
            val outputFile = File(outputDir, "${entity.name}.kt")
            outputFile.writeText(kotlinCode)
            println("Generated: ${outputFile.path}")
        }

        val regularEntities: MutableList<Parsed.Entity> = mutableListOf()
        entities.forEach { if (it is Parsed.Entity) regularEntities.add(it) }
        regularEntities.forEach { entity ->
            File(outputDir, "${entity.name.replaceFirstChar { it.uppercase() }}Dao.kt")
                .writeText(generateDao(entity.name))
        }

        File(outputDir, "${databaseName}.kt")
            .writeText(generateDatabase(regularEntities, databaseName))

        val hasDateFields = regularEntities.any { entity ->
            entity.fields.any { it.type == "Date" }
        }
        if (hasDateFields) {
            File(outputDir, "DateConverter.kt").writeText(generateDateConverter())
        }

        generateOneToOneRelationshipClasses(regularEntities).forEach { file ->
            File(outputDir, "${file.fileName}.kt")
                .writeText("""
                    |import androidx.room.Embedded
                    |import androidx.room.Relation
                    |
                    |${file.code}
                """.trimMargin()
                )
        }

        generateManyToOneRelationshipClasses(regularEntities).forEach { file ->
            File(outputDir, "${file.fileName}.kt")
                .writeText("""
                    |import androidx.room.Embedded
                    |import androidx.room.Relation
                    |import java.util.List
                    |
                    |${file.code}
                """.trimMargin()
                )
        }

        val manyToManyEntities: MutableList<Parsed.ManyToMany> = mutableListOf()
        entities.forEach { if (it is Parsed.ManyToMany) manyToManyEntities.add(it) }

        generateManyToManyRelationshipClasses(
            entities = manyToManyEntities,
        ).forEach { file ->
            File(outputDir, "${file.fileName}.kt")
                .writeText("""
                    |import androidx.room.Embedded
                    |import androidx.room.Relation
                    |import androidx.room.Junction
                    |import java.util.List
                    |
                    |${file.code}
                """.trimMargin()
                )
        }
    }

    private fun generateDateConverter(): String {
        return """
            |import androidx.room.TypeConverter
            |import java.util.Date
            |
            |class DateConverter {
            |    @TypeConverter
            |    static fun toDate(dateLong: Long?): Date? {
            |        return dateLong?.let { Date(it) }
            |    }
            |
            |    @TypeConverter
            |    static fun fromDate(date: Date?): Long? {
            |        return date?.time
            |    }
            |}
        """.trimMargin()
    }
}
