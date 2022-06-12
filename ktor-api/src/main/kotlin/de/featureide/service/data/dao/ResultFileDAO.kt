package de.featureide.service.data.dao

import de.featureide.service.models.ResultFile

interface ResultFileDAO {
    suspend fun fileById(id: Int): ResultFile?
    suspend fun filesByRequestNumber(requestNumber: Int): List<ResultFile>
    suspend fun fileCount(requestNumber: Int): Int
    suspend fun add(
        requestNumber: Int,
        success: Boolean,
        name: String,
        originalName: String,
        type: String,
        content: String,
        timeCreated: Long,
    ): ResultFile?
    suspend fun deleteById(id: Int): Boolean
    suspend fun deleteByRequestNumber(requestNumber: Int): Boolean
}