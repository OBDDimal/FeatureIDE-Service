package de.featureide.service.data.dao

import de.featureide.service.models.SlicedFile

interface SlicedFileDAO {

    suspend fun getFile(id: Int): SlicedFile?

    suspend fun addFile(
        originalName: String,
    ): SlicedFile?

    suspend fun isReady(id: Int): Boolean

    suspend fun delete(id: Int): Boolean

    suspend fun update(id: Int, content: String, name: String, featuresSliced: String): Boolean

}