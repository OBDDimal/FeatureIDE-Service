package de.featureide.service.data.dao

import de.featureide.service.models.PropagationFile

interface PropagationFileDAO {

    suspend fun getFile(id: Int): PropagationFile?

    suspend fun addFile(): PropagationFile?

    suspend fun isReady(id: Int): Boolean

    suspend fun delete(id: Int): Boolean

    suspend fun update(id: Int,
                       name: String,
                       content: String,
                       selection: Array<String>,
                       impliedSelection: Array<String>,
                       deselection: Array<String>,
                       impliedDeselection: Array<String>,
                       openParents: Array<String>,
                       openChildren: Array<String>,
                       valid: Boolean): Boolean
}