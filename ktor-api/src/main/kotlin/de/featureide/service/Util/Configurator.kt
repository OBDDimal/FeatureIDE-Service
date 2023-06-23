package de.featureide.service.Util

import de.featureide.service.data.configurationFileDataSource
import de.featureide.service.data.propagationFileDataSource
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.ConfigurationInput
import de.featureide.service.models.PropagationInput
import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.*
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationGenerator
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.csv.ConfigurationListFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.io.manager.FileHandler
import de.ovgu.featureide.fm.core.job.monitor.ConsoleMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object Configurator {

    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    @Throws(
        CouldNotCreateFileException::class,
        CouldNotCreateRequestException::class
    )
    suspend fun addFileForConfiguration(): Int? {

        val id = propagationFileDataSource.addFile()?.id
        return id
    }

    suspend fun propagate(file: PropagationInput, id: Int) {
        withContext(Dispatchers.IO) {

            val filePath = "files"
            Files.createDirectories(Paths.get(filePath))
            val directory = File(filePath)

            try {

                directory.listFiles().forEach { it.delete() }

                val localFile = File("$filePath/${file.name}")
                localFile.writeText(String(file.content))

                val model = FeatureModelManager.load(Paths.get(localFile.path))

                val cnf = FeatureModelFormula(model).cnf

                val result = arrayOf("")

                propagationFileDataSource.update(
                    id,
                    name = file.name,
                    content = String(file.content),
                    selection = file.selection,
                    impliedSelection = result
                )
                localFile.delete()

            } catch (e: Exception){
                propagationFileDataSource.update(
                    id,
                    name = "No Propagation",
                    content = e.stackTraceToString(),
                    selection = file.selection,
                    impliedSelection = arrayOf("")
                )
            }
        }

    }
}