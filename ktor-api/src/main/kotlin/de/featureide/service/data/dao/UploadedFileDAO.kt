package de.featureide.service.data.dao

import de.featureide.service.models.UploadedFile

interface UploadedFileDAO {
    suspend fun file(id: Int): UploadedFile?
    suspend fun filesByRequestNumber(requestNumber: Int): List<UploadedFile>
    suspend fun addFile(requestNumber: Int, content: String): UploadedFile?
    suspend fun delete(id: Int): Boolean
    suspend fun deleteRequest(requestNumber: Int): Boolean
}