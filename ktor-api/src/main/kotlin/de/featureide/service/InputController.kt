package de.featureide.service

import de.featureide.service.data.*
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.InputFile
import de.featureide.service.models.SliceInput

object InputController {

    @Throws(
        CouldNotCreateFileException::class,
        CouldNotCreateRequestException::class
    )
    suspend fun addFiles(files: List<InputFile>): Int {
        val time = System.currentTimeMillis()
        val requestNumber = requestNumberDataSource.add() ?: throw CouldNotCreateRequestException(-1)
        for (file in files) {

            val uploadedFile = uploadedFileDataSource.addFile(requestNumber.value, file.fileContent.decodeToString()) ?: throw CouldNotCreateFileException(requestNumber.value)

            for (type in file.typeOutput) {
                requestDataSource.addRequest(
                    requestNumber = requestNumber.value,
                    name = file.name,
                    typeOutput = type,
                    file = uploadedFile.id,
                    uploadTime = time,
                ) ?: throw CouldNotCreateRequestException(requestNumber.value)
            }
        }
        return requestNumber.value
    }

    @Throws(
        CouldNotCreateFileException::class,
        CouldNotCreateRequestException::class
    )
    suspend fun addFileForSlice(file: SliceInput): Int? {

        val id = slicedFileDataSource.addFile(file.name)?.id
        return id
    }
}