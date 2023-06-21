package de.featureide.service


import de.featureide.service.data.*
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.exceptions.FileFormatNotFoundException
import de.featureide.service.exceptions.FormatNotFoundException
import de.featureide.service.models.ConvertInput
import de.ovgu.featureide.fm.core.base.IFeatureModel
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.IPersistentFormat
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.io.sxfm.SXFMFormat
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths


object Converter {

    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    private val FormatMap = mapOf(
        Pair(FormatType.DIMACS, "DIMACS"),
        Pair(FormatType.UVL, "UVL"),
        Pair(FormatType.FEATURE_IDE, "FeatureIDE"),
        Pair(FormatType.SXFM, "SXFM"),
    )


    suspend fun convert(file: ConvertInput, id: Int) {

        withContext(Dispatchers.IO) {

            val filePath = "files"
            Files.createDirectories(Paths.get(filePath))
            val directory = File(filePath)

            try {

                directory.listFiles().forEach { it.delete() }

                val localFile = File("$filePath/${file.name}")
                localFile.writeText(String(file.content))

                val model = FeatureModelManager.load(Paths.get(localFile.path))

                val contents = arrayListOf<String>()

                val names = arrayListOf<String>()

                for (type in file.typeOutput) {

                    val format: IPersistentFormat<IFeatureModel> = when (type) {
                        "dimacs" -> DIMACSFormat()
                        "uvl" -> UVLFeatureModelFormat()
                        "featureIde" -> XmlFeatureModelFormat()
                        "sxfm" -> SXFMFormat()
                        else -> throw FormatNotFoundException()
                    }

                    val newName =
                        "${localFile.nameWithoutExtension}_${format.name}.${format.suffix}"
                    val pathOutputFile = "$filePath/$newName"

                    saveFeatureModel(
                        model,
                        pathOutputFile,
                        format,
                    )

                    val result = File(pathOutputFile).absoluteFile

                    names.add(newName)
                    contents.add(result.readText())

                    result.delete()
                }
                convertedFileDataSource.update(
                    id = id,
                    name = names.toTypedArray(),
                    content = contents.toTypedArray(),
                    typeOutput = file.typeOutput
                )
                localFile.delete()

            } catch (e: FormatNotFoundException) {
                convertedFileDataSource.update(
                    id = id,
                    name = arrayOf("Not converted"),
                    content = arrayOf("Could not find the requested format."),
                    typeOutput = file.typeOutput
                )
            } catch (e: FileFormatNotFoundException) {
                convertedFileDataSource.update(
                    id = id,
                    name = arrayOf("Not converted"),
                    content = arrayOf("Could not determine the format of the file."),
                    typeOutput = file.typeOutput
                )
            } catch (e: Exception) {
                convertedFileDataSource.update(
                    id = id,
                    name = arrayOf("Not converted"),
                    content = arrayOf("An unknown error occurred, could not convert file."),
                    typeOutput = file.typeOutput
                )
            }
        }
    }
    private fun saveFeatureModel(model: IFeatureModel?, savePath: String, format: IPersistentFormat<IFeatureModel>?) {
        FeatureModelManager.save(model, Paths.get(savePath), format)
    }

    private fun getFormatType(file: File): Int? {
        return when (file.extension) {
            "dimacs" -> FormatType.DIMACS
            "uvl" -> FormatType.UVL
            "xml" -> {
                var result: Int? = null
                file.bufferedReader().use {
                    for (line in it.lines()) {
                        with(line) {
                            when {
                                contains("<featureModel>") -> result = FormatType.FEATURE_IDE
                                contains("<feature_model") -> result = FormatType.SXFM
                            }
                        }
                        if (result != null) {
                            break
                        }
                    }
                }
                result
            }

            else -> null
        }
    }

    object FormatType {
        const val DIMACS = 0
        const val UVL = 1
        const val FEATURE_IDE = 2
        const val SXFM = 3
    }

    @Throws(
        CouldNotCreateFileException::class,
        CouldNotCreateRequestException::class
    )
    suspend fun addFileForConvert(): Int? {

        val id = convertedFileDataSource.addFile()?.id
        return id
    }
}



