package de.featureide.service.models

import org.jetbrains.exposed.sql.Table

data class FeatureModelStatFile(
    val id: Int,
    val name: String,
    val content: String,
    val deadFeatures: Array<String>,
    val falseOptionalFeatures: Array<String>,
    val coreFeatures: Array<String>
)

object FeatureModelStatFiles : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 4096)
    val content = text("content")
    val deadFeatures = text("deadFeatures")
    val falseOptionalFeatures = text("falseOptionalFeatures")
    val coreFeatures = text("coreFeatures")

    override val primaryKey = PrimaryKey(id)
}