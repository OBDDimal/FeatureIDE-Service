package de.featureide.service.data

import de.featureide.service.data.DatabaseFactory.dbQuery
import de.featureide.service.data.dao.ConfigurationFileDAO
import de.featureide.service.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class ConfigurationFileDataSource : ConfigurationFileDAO {

    private fun resultRowToFile(row: ResultRow) = ConfigurationFile(
        id = row[ConfigurationFiles.id],
        name = row[ConfigurationFiles.name],
        content = row[ConfigurationFiles.content],
        algorithm = row[ConfigurationFiles.algorithm],
        t = row[ConfigurationFiles.t],
        limit = row[ConfigurationFiles.limit]
    )

    override suspend fun getFile(id: Int): ConfigurationFile? = dbQuery {
        ConfigurationFiles.select(ConfigurationFiles.id eq id).map(::resultRowToFile).singleOrNull()
    }

    override suspend fun addFile(): ConfigurationFile? = dbQuery {
        val insert = ConfigurationFiles.insert {
            it[ConfigurationFiles.name] = ""
            it[ConfigurationFiles.content] = ""
            it[ConfigurationFiles.algorithm] = ""
            it[ConfigurationFiles.t] = -1
            it[ConfigurationFiles.limit] = -1
        }

        insert.resultedValues?.singleOrNull()?.let(::resultRowToFile)
    }

    override suspend fun isReady(id: Int): Boolean = dbQuery {
        ConfigurationFiles.select(ConfigurationFiles.id eq id).map(::resultRowToFile).singleOrNull()?.content.isNullOrBlank()
    }

    override suspend fun delete(id: Int): Boolean = dbQuery {
        ConfigurationFiles.deleteWhere { ConfigurationFiles.id eq id } > 0
    }

    override suspend fun update(
        id: Int,
        name: String,
        content: String,
        algorithm: String,
        t: Int,
        limit: Int
    ): Boolean = dbQuery {
        ConfigurationFiles.update({ ConfigurationFiles.id eq id }) {
            it[ConfigurationFiles.name] = name
            it[ConfigurationFiles.content] = content
            it[ConfigurationFiles.algorithm] = algorithm
            it[ConfigurationFiles.t] = t
            it[ConfigurationFiles.limit] = limit
        } > 0
    }

}

val configurationFileDataSource: ConfigurationFileDAO = ConfigurationFileDataSource()