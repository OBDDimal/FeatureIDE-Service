package de.featureide.service.models

import org.jetbrains.exposed.sql.Table

data class Request(
    val id: Int,
    val requestNumber: Int,
    val name: String,
    val typeOutput: String,
    val file: Int,
    val uploadTime: Long,
)

object Requests : Table() {
    val id = integer("id").autoIncrement()
    val requestNumber = integer("requestNumber")
    val name = varchar("name", 1024)
    val typeOutput = varchar("typeOutput", 1024)
    val file = integer("file")
    val uploadTime = long("uploadTime")

    override val primaryKey = PrimaryKey(id)
}