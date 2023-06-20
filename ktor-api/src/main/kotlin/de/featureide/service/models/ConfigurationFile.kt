package de.featureide.service.models

import de.featureide.service.models.ConvertedFiles.autoIncrement
import org.jetbrains.exposed.sql.Table

data class ConfigurationFile(
    val id: Int,
    val name: String,
    val algorithm: String,
    val t: Int,
    val limit: Int,
    val content: String,
)

object ConfigurationFiles : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 4096)
    val algorithm = varchar("algorithm", 4096)
    val t = integer("t")
    val limit = integer("limit")
    val content = text("content")

    override val primaryKey = PrimaryKey(id)
}