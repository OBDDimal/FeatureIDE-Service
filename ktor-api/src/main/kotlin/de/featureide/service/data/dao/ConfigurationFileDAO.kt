package de.featureide.service.data.dao

import de.featureide.service.models.ConfigurationFile

interface ConfigurationFileDAO {

    suspend fun getFile(id: Int): ConfigurationFile?

    suspend fun addFile(): ConfigurationFile?

    suspend fun isReady(id: Int): Boolean

    suspend fun delete(id: Int): Boolean

    suspend fun update(id: Int, name: String, content: String, algorithm: String, t: Int, limit: Int): Boolean
}