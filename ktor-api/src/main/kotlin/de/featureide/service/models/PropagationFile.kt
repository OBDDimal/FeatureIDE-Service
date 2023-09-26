package de.featureide.service.models

import de.featureide.service.models.ConfigurationFiles.autoIncrement
import org.jetbrains.exposed.sql.Table

data class PropagationFile(
    val id: Int,
    val name: String,
    val satCount: Int,
    val selection: Array<String>,
    val impliedSelection: Array<String>,
    val deselection: Array<String>,
    val impliedDeselection: Array<String>,
    val content: String,
)

object PropagationFiles : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 4096)
    val satCount= integer("satCount")
    val selection = text("selection")
    val impliedSelection = text("impliedSelection")
    val deselection = text("deselection")
    val impliedDeselection = text("impliedDeselection")
    val content = text("content")

    override val primaryKey = PrimaryKey(id)
}