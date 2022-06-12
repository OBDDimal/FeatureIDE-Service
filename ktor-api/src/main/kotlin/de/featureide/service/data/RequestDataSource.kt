package de.featureide.service.data

import de.featureide.service.data.DatabaseFactory.dbQuery
import de.featureide.service.data.dao.RequestDAO
import de.featureide.service.models.Request
import de.featureide.service.models.Requests
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select

class RequestDataSource : RequestDAO {

    private fun resultRowToRequest(row: ResultRow) = Request(
        id = row[Requests.id],
        requestNumber = row[Requests.requestNumber],
        name = row[Requests.name],
        typeOutput = row[Requests.typeOutput],
        file = row[Requests.file],
        uploadTime = row[Requests.uploadTime]
    )

    override suspend fun request(id: Int): Request? = dbQuery {
        Requests
            .select { Requests.id eq id }
            .map(::resultRowToRequest)
            .singleOrNull()
    }

    override suspend fun requests(requestNumber: Int): List<Request> = dbQuery {
        Requests
            .select { Requests.requestNumber eq requestNumber }
            .map(::resultRowToRequest)
    }

    override suspend fun requestCount(requestNumber: Int): Int = dbQuery {
        Requests
            .select { Requests.requestNumber eq requestNumber }
            .map(::resultRowToRequest)
            .count()
    }

    override suspend fun addRequest(
        requestNumber: Int,
        name: String,
        typeOutput: String,
        file: Int,
        uploadTime: Long,
    ): Request? = dbQuery {
        val insert = Requests.insert {
            it[Requests.requestNumber] = requestNumber
            it[Requests.name] = name
            it[Requests.typeOutput] = typeOutput
            it[Requests.file] = file
            it[Requests.uploadTime] = uploadTime
        }
        insert.resultedValues?.singleOrNull()?.let(::resultRowToRequest)
    }

    override suspend fun delete(id: Int): Boolean = dbQuery {
        Requests.deleteWhere { Requests.id eq id } > 0
    }

    override suspend fun deleteAllFromOneRequest(requestNumber: Int): Boolean = dbQuery {
        Requests.deleteWhere { Requests.requestNumber eq requestNumber } > 0
    }
}

val requestDataSource: RequestDAO = RequestDataSource()