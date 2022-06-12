package de.featureide.service.models

import org.jetbrains.exposed.sql.Table

data class ResultFile(
    val id: Int,
    val requestNumber: Int,
    val success: Boolean,
    val name: String,
    val originalName: String,
    val type: String,
    val content: String,
    val timeCreated: Long,
)

object ResultFiles : Table() {
    val id = integer("id").autoIncrement()
    val requestNumber = integer("requestNumber")
    val success = bool("success")
    val name = varchar("name", 1024)
    val originalName = varchar("originalName", 1024)
    val type = varchar("type", 1024)
    val content = text("content")
    val timeCreated = long("timeCreated")

    override val primaryKey = PrimaryKey(id)
}