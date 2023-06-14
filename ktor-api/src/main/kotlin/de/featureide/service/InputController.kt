package de.featureide.service

import de.featureide.service.data.*
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.ConvertInput
import de.featureide.service.models.SliceInput

object InputController {

    @Throws(
        CouldNotCreateFileException::class,
        CouldNotCreateRequestException::class
    )
    suspend fun addFileForSlice(file: SliceInput): Int? {

        val id = slicedFileDataSource.addFile(file.name)?.id
        return id
    }

    @Throws(
        CouldNotCreateFileException::class,
        CouldNotCreateRequestException::class
    )
    suspend fun addFileForConvert(file: ConvertInput): Int? {

        val id = convertedFileDataSource.addFile(file.name)?.id
        return id
    }
}