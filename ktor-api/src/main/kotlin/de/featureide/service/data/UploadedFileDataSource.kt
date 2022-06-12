package de.featureide.service.data

import de.featureide.service.data.DatabaseFactory.dbQuery
import de.featureide.service.data.dao.UploadedFileDAO
import de.featureide.service.models.UploadedFile
import de.featureide.service.models.UploadedFiles
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

class UploadedFileDataSource : UploadedFileDAO {

    private fun resultRowToFile(row: ResultRow) = UploadedFile(
        id = row[UploadedFiles.id],
        requestNumber = row[UploadedFiles.requestNumber],
        content = row[UploadedFiles.content],
    )

    override suspend fun file(id: Int): UploadedFile? = dbQuery {
        UploadedFiles
            .select { UploadedFiles.id eq id }
            .map(::resultRowToFile)
            .singleOrNull()
    }

    override suspend fun filesByRequestNumber(requestNumber: Int): List<UploadedFile> = dbQuery {
        UploadedFiles
            .select { UploadedFiles.requestNumber eq requestNumber }
            .map(::resultRowToFile)
    }

    override suspend fun addFile(requestNumber: Int, content: String): UploadedFile? = dbQuery {
        val insert = UploadedFiles.insert {
            it[UploadedFiles.requestNumber] = requestNumber
            it[UploadedFiles.content] = content
        }
        insert.resultedValues?.singleOrNull()?.let(::resultRowToFile)
    }

    override suspend fun delete(id: Int): Boolean = dbQuery {
        UploadedFiles.deleteWhere { UploadedFiles.id eq id } > 0
    }

    override suspend fun deleteRequest(requestNumber: Int): Boolean = dbQuery {
        UploadedFiles.deleteWhere { UploadedFiles.requestNumber eq requestNumber } > 0
    }
}

val uploadedFileDataSource: UploadedFileDAO = UploadedFileDataSource()