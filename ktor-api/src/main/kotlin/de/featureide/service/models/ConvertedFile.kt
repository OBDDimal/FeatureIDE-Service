package de.featureide.service.models

import org.jetbrains.exposed.sql.Table

data class ConvertedFile(
    val id: Int,
    val name: Array<String>,
    val typeOutput: Array<String>,
    val content: Array<String>,
)

object ConvertedFiles : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 4096)
    val typeOutput = text("typeOutput")
    val content = text("content")

    override val primaryKey = PrimaryKey(id)
}