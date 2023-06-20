package de.featureide.service

import de.featureide.service.data.configurationFileDataSource
import de.featureide.service.data.slicedFileDataSource
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.ConfigurationInput
import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.*
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationGenerator
import de.ovgu.featureide.fm.core.io.csv.ConfigurationListFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.io.manager.FileHandler
import de.ovgu.featureide.fm.core.job.LongRunningWrapper
import de.ovgu.featureide.fm.core.job.monitor.ConsoleMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object Configurator {
    @Throws(
        CouldNotCreateFileException::class,
        CouldNotCreateRequestException::class
    )
    suspend fun addFileForConfiguration(): Int? {

        val id = configurationFileDataSource.addFile()?.id
        return id
    }

    suspend fun generate(file: ConfigurationInput, id: Int) {

        val t = file.t
        val limit = file.limit

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

                var generator: IConfigurationGenerator? = null
                with(file.algorithm) {
                    when {
                        contains("icpl") -> {
                            generator = SPLCAToolConfigurationGenerator(cnf, "ICPL", t, limit)
                        }
                        contains("chvatal")-> {
                            generator = SPLCAToolConfigurationGenerator(cnf, "Chvatal", t, limit)
                        }
                        contains("incling") -> {
                            generator = PairWiseConfigurationGenerator(cnf, limit)
                        }
                        contains("yasa") -> {
                            generator = TWiseConfigurationGenerator(cnf, t, limit)
                            val yasa = generator as TWiseConfigurationGenerator?
                            yasa!!.iterations = Integer.parseInt(file.algorithm.split(" ")[1])
                        }
                        contains("random") -> {
                            generator = RandomConfigurationGenerator(cnf, limit)
                        }
                        contains("all") -> {
                            generator = AllConfigurationGenerator(cnf, limit)
                        }
                        else -> throw IllegalArgumentException("No algorithm specified!")
                    }

                }
                val result = generator!!.execute(null)

                val newName = "${localFile.nameWithoutExtension}_sample.${localFile.extension}"
                val pathOutputFile = "$filePath/$newName"


                FileHandler.save(
                    Paths.get(pathOutputFile),
                    SolutionList(cnf.getVariables(), result),
                    ConfigurationListFormat()
                )

                val resultFile = File(pathOutputFile).absoluteFile
                configurationFileDataSource.update(
                    id,
                    name = newName,
                    content = resultFile.readText(),
                    algorithm = file.algorithm,
                    t = file.t,
                    limit = file.limit
                )
                localFile.delete()
                resultFile.delete()

            } catch (e: Exception){

            }
        }

    }
}