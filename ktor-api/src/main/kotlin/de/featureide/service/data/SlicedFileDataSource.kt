package de.featureide.service.data

import de.featureide.service.data.DatabaseFactory.dbQuery
import de.featureide.service.data.dao.SlicedFileDAO
import de.featureide.service.models.Request
import de.featureide.service.models.Requests
import de.featureide.service.models.SlicedFile
import de.featureide.service.models.SlicedFiles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class SlicedFileDataSource : SlicedFileDAO {

    private fun resultRowToFile(row: ResultRow) = SlicedFile(
        id = row[SlicedFiles.id],
        name = row[SlicedFiles.name],
        originalName = row[SlicedFiles.originalName],
        content = row[SlicedFiles.content],
        featuresSliced = row[SlicedFiles.featuresSliced]
    )

    override suspend fun getFile(id: Int): SlicedFile? = dbQuery {
        SlicedFiles.select(SlicedFiles.id eq id).map(::resultRowToFile).singleOrNull()
    }

    override suspend fun addFile(originalName: String): SlicedFile? = dbQuery {
        val insert = SlicedFiles.insert {
            it[SlicedFiles.originalName] = originalName
        }

        insert.resultedValues?.singleOrNull()?.let(::resultRowToFile)
    }

    override suspend fun isReady(id: Int): Boolean = dbQuery {
        SlicedFiles.select(SlicedFiles.id eq id).map(::resultRowToFile).singleOrNull()?.content.isNullOrEmpty()
    }


    override suspend fun delete(id: Int): Boolean = dbQuery {
        SlicedFiles.deleteWhere { SlicedFiles.id eq id } > 0
    }

    override suspend fun update(id: Int, content: String, name: String, featuresSliced: String): Boolean = dbQuery {
        SlicedFiles.update({ SlicedFiles.id eq id }) {
            it[SlicedFiles.name] = name
            it[SlicedFiles.content] = content
            it[SlicedFiles.featuresSliced] = featuresSliced
        } > 0
    }


}

val slicedFileDataSource: SlicedFileDAO = SlicedFileDataSource()