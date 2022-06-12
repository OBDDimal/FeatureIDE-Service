package de.featureide.service.data.dao

import de.featureide.service.models.RequestNumber

interface RequestNumberDAO {
    suspend fun getAll(): List<RequestNumber>
    suspend fun add(): RequestNumber?
    suspend fun delete(value: Int): Boolean
    suspend fun deleteAll():Boolean
}