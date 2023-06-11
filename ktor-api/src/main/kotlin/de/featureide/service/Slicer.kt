package de.featureide.service

import de.featureide.service.data.requestDataSource
import de.featureide.service.data.requestNumberDataSource
import de.featureide.service.data.resultFileDataSource
import de.featureide.service.data.uploadedFileDataSource
import de.featureide.service.exceptions.FileFormatNotFoundException
import de.featureide.service.exceptions.FormatNotFoundException
import de.featureide.service.models.Request
import de.featureide.service.models.UploadedFile
import de.ovgu.featureide.fm.core.base.IFeature
import de.ovgu.featureide.fm.core.base.IFeatureModel
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.IPersistentFormat
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.io.sxfm.SXFMFormat
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat
import de.ovgu.featureide.fm.core.job.SliceFeatureModel
import de.ovgu.featureide.fm.core.job.monitor.NullMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

object Slicer {

    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }


    suspend fun sliceFiles(requestNumber: Int) {
        slice(requestDataSource.requests(requestNumber))
        uploadedFileDataSource.deleteRequest(requestNumber)
    }

    suspend fun sliceRemainingFiles() {
        val requestNumbers = requestNumberDataSource.getAll()
        for (number in requestNumbers) {
            Converter.convertFiles(number.value)
        }
    }

    private suspend fun slice(requests: List<Request>) {
        var fileId = -1
        var uploadedFile: UploadedFile? = null
        withContext(Dispatchers.IO) {

            val filePath = "files"
            Files.createDirectories(Paths.get(filePath))
            val directory = File(filePath)

            for (request in requests) {
                if (request.file != fileId) {
                    fileId = request.file
                    uploadedFile = uploadedFileDataSource.file(fileId)
                }

                val time = System.currentTimeMillis()

                try {
                    uploadedFile?.let { file ->

                        directory.listFiles().forEach { it.delete() }

                        val localFile = File("$filePath/${request.name}")
                        localFile.writeText(file.content)

                        val format: IPersistentFormat<IFeatureModel> = XmlFeatureModelFormat()

                        var model = FeatureModelManager.load(Paths.get(localFile.path))
                        val featuresToSlice = ArrayList<IFeature>()

                        for (name in request.typeOutput.split(", ")){
                            featuresToSlice.add(model.getFeature(name))
                        }
                        model = slice(model, featuresToSlice)

                        val newName = "${localFile.nameWithoutExtension}_sliced.${format.suffix}"
                        val pathOutputFile = "$filePath/$newName"

                        saveFeatureModel(
                            model,
                            pathOutputFile,
                            format,
                        )

                        val result = File(pathOutputFile).absoluteFile
                        resultFileDataSource.add(
                            requestNumber = request.requestNumber,
                            success = true,
                            name = newName,
                            originalName = request.name,
                            type = request.typeOutput,
                            content = result.readText(),
                            timeCreated = time,
                        )
                        localFile.delete()
                        result.delete()
                    }
                } catch (e: FormatNotFoundException) {
                    resultFileDataSource.add(
                        requestNumber = request.requestNumber,
                        success = false,
                        name = "",
                        originalName = request.name,
                        type = request.typeOutput,
                        content = "Could not find the requested format.",
                        timeCreated = time,
                    )
                } catch (e: FileFormatNotFoundException) {
                    resultFileDataSource.add(
                        requestNumber = request.requestNumber,
                        success = false,
                        name = "",
                        originalName = request.name,
                        type = request.typeOutput,
                        content = "Could not determine the format of the file.",
                        timeCreated = time,
                    )
                } catch (e: Exception){
                    resultFileDataSource.add(
                        requestNumber = request.requestNumber,
                        success = false,
                        name = "",
                        originalName = request.name,
                        type = request.typeOutput,
                        content = "An unknown error occurred, could not slice file.",
                        timeCreated = time,
                    )
                }

                if (uploadedFile == null) {
                    resultFileDataSource.add(
                        requestNumber = request.requestNumber,
                        success = false,
                        name = "",
                        originalName = request.name,
                        type = request.typeOutput,
                        content = "",
                        timeCreated = time,
                    )
                }

                requestDataSource.delete(request.id)
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

    private fun slice(featureModel: IFeatureModel, featuresToRemove: Collection<IFeature>?): IFeatureModel? {
        val featuresToKeep: MutableSet<IFeature> = HashSet(featureModel.features)
        featuresToKeep.removeAll(featuresToRemove!!.toSet())
        val featureNamesToKeep: Set<String> = featuresToKeep.stream().map { obj: IFeature -> obj.name }.collect(
            Collectors.toSet())
        val sliceJob = SliceFeatureModel(featureModel, featureNamesToKeep, false)
        return try {
            sliceJob.execute(NullMonitor())
        } catch (e: Exception) {
            throw e
        }
    }
}