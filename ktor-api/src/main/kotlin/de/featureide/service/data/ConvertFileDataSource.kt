package de.featureide.service.data

import de.featureide.service.data.DatabaseFactory.dbQuery
import de.featureide.service.data.dao.ConvertFileDAO
import de.featureide.service.models.ConvertedFile
import de.featureide.service.models.ConvertedFiles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class ConvertFileDataSource : ConvertFileDAO {

    private fun resultRowToFile(row: ResultRow) = ConvertedFile(
        id = row[ConvertedFiles.id],
        name = row[ConvertedFiles.name].split(":::").toTypedArray(),
        originalName = row[ConvertedFiles.originalName],
        content = row[ConvertedFiles.content].split(":::").toTypedArray(),
        typeOutput = row[ConvertedFiles.typeOutput].split(":::").toTypedArray()
    )

    override suspend fun getFile(id: Int): ConvertedFile? = dbQuery {
        ConvertedFiles.select(ConvertedFiles.id eq id).map(::resultRowToFile).singleOrNull()
    }

    override suspend fun addFile(originalName: String): ConvertedFile? = dbQuery {
        val insert = ConvertedFiles.insert {
            it[ConvertedFiles.name] = ""
            it[ConvertedFiles.originalName] = originalName
            it[ConvertedFiles.content] = ""
            it[ConvertedFiles.typeOutput] = ""
        }

        insert.resultedValues?.singleOrNull()?.let(::resultRowToFile)
    }

    override suspend fun isReady(id: Int): Boolean = dbQuery {
        ConvertedFiles.select(ConvertedFiles.id eq id).map(::resultRowToFile).singleOrNull()?.content.isNullOrEmpty()
    }


    override suspend fun delete(id: Int): Boolean = dbQuery {
        ConvertedFiles.deleteWhere { ConvertedFiles.id eq id } > 0
    }

    override suspend fun update(id: Int, content: Array<String>, name: Array<String>, typeOutput: Array<String>): Boolean = dbQuery {
        ConvertedFiles.update({ ConvertedFiles.id eq id }) {
            it[ConvertedFiles.name] = name.joinToString(":::")
            it[ConvertedFiles.content] = content.joinToString(":::")
            it[ConvertedFiles.typeOutput] = typeOutput.joinToString(":::")
        } > 0
    }
}

val convertedFileDataSource: ConvertFileDAO = ConvertFileDataSource()