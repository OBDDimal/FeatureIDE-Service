package de.featureide.service.models

import org.jetbrains.exposed.sql.Table

data class UploadedFile(
    val id: Int,
    val requestNumber: Int,
    val content: String,
    )

object UploadedFiles : Table() {
    val id = integer("id").autoIncrement()
    val requestNumber = integer("requestNumber")
    val content = text("content")

    override val primaryKey = PrimaryKey(id)
}