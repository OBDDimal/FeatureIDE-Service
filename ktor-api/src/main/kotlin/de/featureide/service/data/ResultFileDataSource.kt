package de.featureide.service.data

import de.featureide.service.data.DatabaseFactory.dbQuery
import de.featureide.service.data.dao.ResultFileDAO
import de.featureide.service.models.ResultFile
import de.featureide.service.models.ResultFiles
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
class ResultFileDataSource : ResultFileDAO {

    private fun resultRowToFile(row: ResultRow) = ResultFile(
        id = row[ResultFiles.id],
        requestNumber = row[ResultFiles.requestNumber],
        success = row[ResultFiles.success],
        name = row[ResultFiles.name],
        originalName = row[ResultFiles.originalName],
        type = row[ResultFiles.type],
        content = row[ResultFiles.content],
        timeCreated = row[ResultFiles.timeCreated],
    )

    override suspend fun fileById(id: Int): ResultFile? = dbQuery {
        ResultFiles
            .select { ResultFiles.id eq id }
            .map(::resultRowToFile)
            .singleOrNull()
    }

    override suspend fun filesByRequestNumber(requestNumber: Int): List<ResultFile> = dbQuery {
        ResultFiles
            .select { ResultFiles.requestNumber eq requestNumber }
            .map(::resultRowToFile)
    }

    override suspend fun fileCount(requestNumber: Int): Int = dbQuery {
        ResultFiles
            .select { ResultFiles.requestNumber eq requestNumber }
            .map(::resultRowToFile)
            .count()
    }

    override suspend fun add(
        requestNumber: Int,
        success: Boolean,
        name: String,
        originalName: String,
        type: String,
        content: String,
        timeCreated: Long
    ): ResultFile? = dbQuery {
        val insert = ResultFiles.insert {
            it[ResultFiles.requestNumber] = requestNumber
            it[ResultFiles.success] = success
            it[ResultFiles.name] = name
            it[ResultFiles.originalName] = originalName
            it[ResultFiles.type] = type
            it[ResultFiles.content] = content
            it[ResultFiles.timeCreated] = timeCreated
        }
        insert.resultedValues?.singleOrNull()?.let(::resultRowToFile)
    }

    override suspend fun deleteById(id: Int): Boolean = dbQuery {
        ResultFiles.deleteWhere { ResultFiles.id eq id } > 0
    }

    override suspend fun deleteByRequestNumber(requestNumber: Int): Boolean = dbQuery {
        ResultFiles.deleteWhere { ResultFiles.requestNumber eq requestNumber } > 0
    }
}

val resultFileDataSource: ResultFileDAO = ResultFileDataSource()

