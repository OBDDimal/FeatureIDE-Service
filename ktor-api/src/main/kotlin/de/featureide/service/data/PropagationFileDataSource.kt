package de.featureide.service.data

import de.featureide.service.data.dao.ConfigurationFileDAO
import de.featureide.service.data.dao.PropagationFileDAO
import de.featureide.service.models.ConfigurationFile
import de.featureide.service.models.ConfigurationFiles
import de.featureide.service.models.PropagationFile
import de.featureide.service.models.PropagationFiles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class PropagationFileDataSource : PropagationFileDAO {

    private fun resultRowToFile(row: ResultRow) = PropagationFile(
        id = row[PropagationFiles.id],
        name = row[PropagationFiles.name],
        satCount = row[PropagationFiles.satCount],
        content = row[PropagationFiles.content],
        selection = row[PropagationFiles.selection].split(":::").toTypedArray(),
        impliedSelection = row[PropagationFiles.impliedSelection].split(":::").toTypedArray(),
        deselection = row[PropagationFiles.deselection].split(":::").toTypedArray(),
        impliedDeselection = row[PropagationFiles.impliedDeselection].split(":::").toTypedArray()
    )

    override suspend fun getFile(id: Int): PropagationFile? = DatabaseFactory.dbQuery {
        PropagationFiles.select(PropagationFiles.id eq id).map(::resultRowToFile).singleOrNull()
    }

    override suspend fun addFile(): PropagationFile? = DatabaseFactory.dbQuery {
        val insert = PropagationFiles.insert {
            it[name] = ""
            it[content] = ""
            it[satCount] = 0
            it[selection] = ""
            it[impliedSelection] = ""
            it[deselection] = ""
            it[impliedDeselection] = ""
        }

        insert.resultedValues?.singleOrNull()?.let(::resultRowToFile)
    }

    override suspend fun isReady(id: Int): Boolean = DatabaseFactory.dbQuery {
        PropagationFiles.select(PropagationFiles.id eq id).map(::resultRowToFile)
            .singleOrNull()?.content.isNullOrBlank()
    }

    override suspend fun delete(id: Int): Boolean = DatabaseFactory.dbQuery {
        PropagationFiles.deleteWhere { PropagationFiles.id eq id } > 0
    }

    override suspend fun update(
        id: Int,
        name: String,
        content: String,
        satCount: Int,
        selection: Array<String>,
        impliedSelection: Array<String>,
        deselection: Array<String>,
        impliedDeselection: Array<String>,
    ): Boolean = DatabaseFactory.dbQuery {
        PropagationFiles.update({ PropagationFiles.id eq id }) {
            it[PropagationFiles.name] = name
            it[PropagationFiles.content] = content
            it[PropagationFiles.selection] = selection.joinToString(":::")
            it[PropagationFiles.impliedSelection] = impliedSelection.joinToString(":::")
            it[PropagationFiles.satCount] = satCount
            it[PropagationFiles.deselection] = deselection.joinToString(":::")
            it[PropagationFiles.impliedDeselection] = impliedDeselection.joinToString(":::")
        } > 0
    }

}

val propagationFileDataSource: PropagationFileDAO = PropagationFileDataSource()