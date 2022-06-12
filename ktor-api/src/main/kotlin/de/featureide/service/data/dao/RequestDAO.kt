package de.featureide.service.data.dao

import de.featureide.service.models.Request

interface RequestDAO {
    suspend fun request(id: Int): Request?
    suspend fun requests(requestNumber: Int): List<Request>
    suspend fun requestCount(requestNumber: Int): Int
    suspend fun addRequest(
        requestNumber: Int,
        name: String,
        typeOutput: String,
        file: Int,
        uploadTime: Long,
    ): Request?
    suspend fun delete(id: Int): Boolean
    suspend fun deleteAllFromOneRequest(requestNumber: Int): Boolean
}