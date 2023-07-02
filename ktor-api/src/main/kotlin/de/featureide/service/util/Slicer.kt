package de.featureide.service.util

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
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.system.exitProcess

object Slicer {

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val parser = ArgParser("featureide-cli")

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file or directory.").required()
        val selection by parser.option(ArgType.String, shortName = "s", description = "The names of the features that should be sliced separated by ','. For example: Antenna,AHEAD.")

        parser.parse(args)


        val file = File(path)

        val output = "./files/output"
        Files.createDirectories(Paths.get(output))

        //slices the featureModel
        if (!selection.isNullOrEmpty()){
            if(file.isDirectory() || !file.exists()) exitProcess(0)
            var model = FeatureModelManager.load(Paths.get(file.path))
            model = slice(model, selection!!.split(",").toTypedArray())
            saveFeatureModel(
                model,
                "${output}/${file.nameWithoutExtension}_sliced.${XmlFeatureModelFormat().suffix}",
                XmlFeatureModelFormat()
            )
            exitProcess(0)
        }
    }


    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    suspend fun slice(file: SliceInput, id: Int) {
        withContext(Dispatchers.IO) {

            val filePath = "files"
            Files.createDirectories(Paths.get(filePath))
            val directory = File(filePath)

            try {
                directory.listFiles().forEach { it.delete() }

                val localFile = File("$filePath/${file.name}")
                localFile.writeText(String(file.content))

                val format: IPersistentFormat<IFeatureModel> = XmlFeatureModelFormat()

                var model = FeatureModelManager.load(Paths.get(localFile.path))

                model = slice(model, file.selection)

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
                    featuresSliced = file.selection
                )
                localFile.delete()
                result.delete()

            } catch (e: Exception) {
                slicedFileDataSource.update(
                    id,
                    name = "Not sliced",
                    content = "Could not slice file",
                    featuresSliced = file.selection
                )
            }
        }
    }

    private fun saveFeatureModel(model: IFeatureModel?, savePath: String, format: IPersistentFormat<IFeatureModel>?) {
        FeatureModelManager.save(model, Paths.get(savePath), format)
    }

    /**
     * Slice selected features from a feature model
     *
     * @param featureModel The feature model to slice the features
     * @param selection The selection of features to get the implied features
     * @return IFeatureModel Returns the new featuremodel without the selected features
     */
    private fun slice(featureModel: IFeatureModel, selection: Array<String>): IFeatureModel? {
        val featuresToSlice = ArrayList<IFeature>()

        for (name in selection) {
            featuresToSlice.add(featureModel.getFeature(name))
        }
        val featuresToKeep: MutableSet<IFeature> = HashSet(featureModel.features)
        featuresToKeep.removeAll(featuresToSlice.toSet())
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
}




