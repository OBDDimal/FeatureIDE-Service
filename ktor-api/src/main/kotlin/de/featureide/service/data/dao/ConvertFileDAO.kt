package de.featureide.service.data.dao

import de.featureide.service.models.ConvertedFile

interface ConvertFileDAO {

    suspend fun getFile(id: Int): ConvertedFile?

    suspend fun addFile(): ConvertedFile?

    suspend fun isReady(id: Int): Boolean

    suspend fun delete(id: Int): Boolean

    suspend fun update(id: Int, content: Array<String>, name: Array<String>, typeOutput: Array<String>): Boolean

}