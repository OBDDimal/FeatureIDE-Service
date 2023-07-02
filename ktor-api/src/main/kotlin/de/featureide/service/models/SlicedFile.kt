package de.featureide.service.models

import org.jetbrains.exposed.sql.Table

data class SlicedFile(
    val id: Int,
    val name: String,
    val selection: Array<String>,
    val content: String,
)

object SlicedFiles : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 1024)
    val selection = text("featuresSliced")
    val content = text("content")

    override val primaryKey = PrimaryKey(id)
}