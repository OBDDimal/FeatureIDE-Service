package de.featureide.service.Util

import de.featureide.service.data.slicedFileDataSource
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.SliceInput
import de.ovgu.featureide.fm.core.base.IFeature
import de.ovgu.featureide.fm.core.base.IFeatureModel
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.IPersistentFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
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

    suspend fun slice(sliceFile: SliceInput, id: Int) {
        withContext(Dispatchers.IO) {

            val filePath = "files"
            Files.createDirectories(Paths.get(filePath))
            val directory = File(filePath)

            try {
                directory.listFiles().forEach { it.delete() }

                val localFile = File("$filePath/${sliceFile.name}")
                localFile.writeText(String(sliceFile.content))

                val format: IPersistentFormat<IFeatureModel> = XmlFeatureModelFormat()

                var model = FeatureModelManager.load(Paths.get(localFile.path))
                val featuresToSlice = ArrayList<IFeature>()

                for (name in sliceFile.featuresToSlice) {
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
                slicedFileDataSource.update(
                    id,
                    name = newName,
                    content = result.readText(),
                    featuresSliced = sliceFile.featuresToSlice.joinToString()
                )
                localFile.delete()
                result.delete()

            } catch (e: Exception) {
                slicedFileDataSource.update(
                    id,
                    name = "Not sliced",
                    content = "Could not slice file",
                    featuresSliced = sliceFile.featuresToSlice.joinToString()
                )
            }
        }
    }

    private fun saveFeatureModel(model: IFeatureModel?, savePath: String, format: IPersistentFormat<IFeatureModel>?) {
        FeatureModelManager.save(model, Paths.get(savePath), format)
    }

    private fun slice(featureModel: IFeatureModel, featuresToRemove: Collection<IFeature>?): IFeatureModel? {
        val featuresToKeep: MutableSet<IFeature> = HashSet(featureModel.features)
        featuresToKeep.removeAll(featuresToRemove!!.toSet())
        val featureNamesToKeep: Set<String> = featuresToKeep.stream().map { obj: IFeature -> obj.name }.collect(
            Collectors.toSet()
        )
        val sliceJob = SliceFeatureModel(featureModel, featureNamesToKeep, false)
        return try {
            sliceJob.execute(NullMonitor())
        } catch (e: Exception) {
            throw e
        }
    }

    @Throws(
        CouldNotCreateFileException::class,
        CouldNotCreateRequestException::class
    )
    suspend fun addFileForSlice(): Int? {

        val id = slicedFileDataSource.addFile()?.id
        return id
    }
}




