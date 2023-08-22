package de.featureide.service.data.dao

import de.featureide.service.models.FeatureModelStatFile

interface FeatureModelStatFileDAO {

    suspend fun getFile(id: Int): FeatureModelStatFile?

    suspend fun addFile(): FeatureModelStatFile?

    suspend fun isReady(id: Int): Boolean

    suspend fun delete(id: Int): Boolean

    suspend fun update(id: Int, content: String, name: String, deadFeatures: Array<String>, falseOptionalFeatures: Array<String>, coreFeatures: Array<String>): Boolean

}