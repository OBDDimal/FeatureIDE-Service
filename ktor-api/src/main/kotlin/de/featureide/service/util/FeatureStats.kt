package de.featureide.service.util

import de.featureide.service.data.slicedFileDataSource
import de.featureide.service.models.FeatureModelStatInput
import de.featureide.service.models.FeatureModelStatOutput
import de.featureide.service.models.SliceInput
import de.featureide.service.models.SliceOutput
import de.ovgu.featureide.fm.core.base.IFeature
import de.ovgu.featureide.fm.core.base.IFeatureModel
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.IPersistentFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat
import de.ovgu.featureide.fm.core.job.LongRunningWrapper
import de.ovgu.featureide.fm.core.job.SliceFeatureModel
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

object FeatureStats {

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val parser = ArgParser("featureide-cli")

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file or directory.")
            .required()

        parser.parse(args)


        val file = File(path)

        val output = "./files/output"
        Files.createDirectories(Paths.get(output))

        if (file.isDirectory() || !file.exists()) exitProcess(0)
        var model = FeatureModelManager.load(Paths.get(file.path))
        exitProcess(0)
    }


    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    suspend fun stats(file: FeatureModelStatInput, id: Int): FeatureModelStatOutput {

        val filePath = "files"
        withContext(Dispatchers.IO) {
            Files.createDirectories(Paths.get(filePath))
        }
        val directory = File(filePath)

        try {
            directory.listFiles()?.forEach { it.delete() }

            val localFile = File("$filePath/${file.name}")
            localFile.writeText(String(file.content))

            val format: IPersistentFormat<IFeatureModel> = XmlFeatureModelFormat()

            var model = FeatureModelManager.load(Paths.get(localFile.path))


            localFile.delete()

        } catch (e: Exception) {

        }
        return FeatureModelStatOutput(file.name, file.content, arrayOf(), arrayOf(), arrayOf())
    }

    private fun getDeadFeatures(featureModel: IFeatureModel): Array<String> {
        return arrayOf()
    }

    private fun getFalseOptionalFeatures(featureModel: IFeatureModel): Array<String>  {
        return arrayOf()
    }

    private fun getCoreFeatures(featureModel: IFeatureModel): Array<String>  {
        return arrayOf()
    }
}