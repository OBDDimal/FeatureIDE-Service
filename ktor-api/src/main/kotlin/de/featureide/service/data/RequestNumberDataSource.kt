package de.featureide.service.data

import de.featureide.service.data.DatabaseFactory.dbQuery
import de.featureide.service.data.dao.RequestNumberDAO
import de.featureide.service.models.RequestNumber
import de.featureide.service.models.RequestNumbers
import org.jetbrains.exposed.sql.*

class RequestNumberDataSource : RequestNumberDAO {

    private fun resultRowToNumber(row: ResultRow) = RequestNumber(
        value = row[RequestNumbers.value],
    )

    override suspend fun getAll(): List<RequestNumber> = dbQuery {
        RequestNumbers.selectAll().map(::resultRowToNumber)
    }

    override suspend fun add(): RequestNumber? = dbQuery {
        RequestNumbers.insert {  }
            .resultedValues?.singleOrNull()?.let(::resultRowToNumber)
    }

    override suspend fun delete(value: Int): Boolean = dbQuery {
        RequestNumbers.deleteWhere { RequestNumbers.value eq value } > 0
    }

    override suspend fun deleteAll(): Boolean = dbQuery {
        RequestNumbers.deleteAll() > 0
    }
}

val requestNumberDataSource: RequestNumberDAO = RequestNumberDataSource()