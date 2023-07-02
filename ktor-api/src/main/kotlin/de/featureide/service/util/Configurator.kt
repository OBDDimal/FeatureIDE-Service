package de.featureide.service.util

import de.featureide.service.data.configurationFileDataSource
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.ConfigurationInput
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet
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
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object Configurator {

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val parser = ArgParser("featureide-cli")

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file or directory.")
            .required()

        val algorithm by parser.option(
            ArgType.String,
            shortName = "alg",
            description = "The algorithm to generate a configuration sample as csv file"
        )
        val t by parser.option(
            ArgType.Int,
            shortName = "t",
            description = "The t wise pairing that should be covered by the configuration sample"
        ).default(0)
        val limit by parser.option(
            ArgType.Int,
            shortName = "l",
            description = "The maximum amount of configurations for the configuration sample"
        ).default(Int.MAX_VALUE)

        val time by parser.option(
            ArgType.Int,
            shortName = "ti",
            description = "Timelimit for the yasa algorithm in seconds"
        ).default(-1)

        parser.parse(args)
        val file = File(path)
        val output = "./files/output"
        //File(output).deleteRecursively()
        Files.createDirectories(Paths.get(output))

        if (!algorithm.isNullOrEmpty()) {
            if (file.isDirectory()) {

                val sb = StringBuilder()
                sb.append("Filename;Time;Configurations\n")

                val inputFiles = file.listFiles()
                for (fileFromList in inputFiles!!) {
                    if (fileFromList.isDirectory) {
                        continue
                    }
                    try {
                        val model = FeatureModelManager.load(Paths.get(fileFromList.path))

                        val cnf = FeatureModelFormula(model).cnf

                        var generator: IConfigurationGenerator? = null
                        with(algorithm!!) {
                            when {

                                contains("icpl") -> {
                                    generator = SPLCAToolConfigurationGenerator(cnf, "ICPL", t, limit)
                                }

                                contains("chvatal") -> {
                                    generator = SPLCAToolConfigurationGenerator(cnf, "Chvatal", t, limit)
                                }


                                contains("incling") -> {
                                    generator = PairWiseConfigurationGenerator(cnf, limit)
                                }

                                contains("yasa") -> {
                                    generator = TWiseConfigurationGenerator(cnf, t, limit)
                                    val yasa = generator as TWiseConfigurationGenerator?
                                    var iterations: Int = 1
                                    if (algorithm!!.split("_").size > 1) {
                                        try {
                                            iterations = Integer.parseInt(algorithm!!.split("_")[1])
                                        } catch (_: Exception) {
                                        }
                                    }
                                    yasa!!.iterations = iterations
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

                        val monitor = ConsoleMonitor<List<LiteralSet>>()
                        var result = ArrayList<LiteralSet>()
                        val start = LocalDateTime.now()
                        if (generator is TWiseConfigurationGenerator?) {
                            GlobalScope.launch(Dispatchers.IO) {
                                result = ArrayList(generator!!.execute(monitor))
                            }
                            val yasa = generator as TWiseConfigurationGenerator?
                            while (result.size == 0) {
                                if (Duration.between(start, LocalDateTime.now()).seconds >= time && time != -1) {
                                    monitor.cancel()
                                    break
                                }
                            }
                            if (result.size == 0) {
                                yasa!!.resultList.stream().forEach {
                                    it.clear()
                                    result.add(it.completeSolution)
                                }
                            }
                        } else {
                            result = ArrayList(generator!!.execute(monitor))
                        }
                        println(fileFromList.nameWithoutExtension + ": " + result.size + " in " + Duration.between(start, LocalDateTime.now()).toString())

                        sb.append(fileFromList.nameWithoutExtension+";"+Duration.between(start, LocalDateTime.now()).toString() + ";" + result.size + "\n")

                        FileHandler.save(
                            Paths.get("${output}/${fileFromList.nameWithoutExtension}_${algorithm}_t${t}_${limit}.${ConfigurationListFormat().suffix}"),
                            SolutionList(cnf.variables, result),
                            ConfigurationListFormat()
                        )



                    } catch (e: Exception) {
                        println(e.stackTraceToString())
                    }
                }
                val info = File("${output}/informations.csv")
                info.writeText(sb.toString())

            } else if (file.exists()) {
                try {
                    val model = FeatureModelManager.load(Paths.get(file.path))

                    val cnf = FeatureModelFormula(model).cnf

                    var generator: IConfigurationGenerator? = null
                    with(algorithm!!) {
                        when {

                            contains("icpl") -> {
                                generator = SPLCAToolConfigurationGenerator(cnf, "ICPL", t, limit)
                            }

                            contains("chvatal") -> {
                                generator = SPLCAToolConfigurationGenerator(cnf, "Chvatal", t, limit)
                            }

                            contains("incling") -> {
                                generator = PairWiseConfigurationGenerator(cnf, limit)
                            }

                            contains("yasa") -> {
                                generator = TWiseConfigurationGenerator(cnf, t, limit)
                                val yasa = generator as TWiseConfigurationGenerator?
                                var iterations: Int = 1
                                if (algorithm!!.split("_").size > 1) {
                                    try {
                                        iterations = Integer.parseInt(algorithm!!.split("_")[1])
                                    } catch (_: Exception) {
                                    }
                                }
                                yasa!!.iterations = iterations
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
                    val monitor = ConsoleMonitor<List<LiteralSet>>()
                    var result = ArrayList<LiteralSet>()
                    if (generator is TWiseConfigurationGenerator?) {
                        GlobalScope.launch(Dispatchers.IO) {
                            result = ArrayList(generator!!.execute(monitor))
                        }

                        val yasa = generator as TWiseConfigurationGenerator?
                        val start = LocalDateTime.now()
                        while (result.size == 0) {
                            if (Duration.between(start, LocalDateTime.now()).seconds >= time && time != -1) {
                                monitor.cancel()
                                break
                            }
                        }

                        if (result.size == 0) {
                            yasa!!.resultList.stream().forEach {
                                it.clear()
                                result.add(it.completeSolution)
                            }
                        }

                        println(file.nameWithoutExtension + ": " + result.size + " in " + Duration.between(start, LocalDateTime.now()).toString())

                    } else {
                        result = ArrayList(generator!!.execute(monitor))
                    }

                    FileHandler.save(
                        Paths.get("${output}/${file.nameWithoutExtension}_${algorithm}_t${t}_${limit}.${ConfigurationListFormat().suffix}"),
                        SolutionList(cnf.variables, result),
                        ConfigurationListFormat()
                    )
                    exitProcess(0)
                } catch (e: Exception) {
                    println(e.stackTraceToString())
                }
            }
        }

    }

    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    @Throws(
        CouldNotCreateFileException::class,
        CouldNotCreateRequestException::class
    )
    suspend fun addFileForConfiguration(): Int? {

        val id = configurationFileDataSource.addFile()?.id
        return id
    }

    suspend fun generate(file: ConfigurationInput, id: Int) {
        withContext(Dispatchers.IO) {
            val t = file.t
            val limit = file.limit

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

                        contains("chvatal") -> {
                            generator = SPLCAToolConfigurationGenerator(cnf, "Chvatal", t, limit)
                        }

                        contains("incling") -> {
                            generator = PairWiseConfigurationGenerator(cnf, limit)
                        }

                        contains("yasa") -> {
                            generator = TWiseConfigurationGenerator(cnf, t, limit)
                            val yasa = generator as TWiseConfigurationGenerator?
                            var iterations: Int = 1
                            if (file.algorithm.split("_").size > 1) {
                                try {
                                    iterations = java.lang.Integer.parseInt(file.algorithm.split("_")[1])
                                } catch (_: Exception) {
                                }
                            }
                            yasa!!.iterations = iterations
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
                val result = generator!!.execute(ConsoleMonitor())

                val newName =
                    "${localFile.nameWithoutExtension}_${file.algorithm}_t${file.t}_${limit}.${ConfigurationListFormat().suffix}"
                val pathOutputFile = "$filePath/$newName"


                FileHandler.save(
                    Paths.get(pathOutputFile),
                    SolutionList(cnf.getVariables(), result),
                    ConfigurationListFormat()
                )

                val resultFile = File(pathOutputFile).absoluteFile
                println(resultFile.readText())

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

            } catch (e: Exception) {
                configurationFileDataSource.update(
                    id,
                    name = "Not generated",
                    content = e.stackTraceToString(),
                    algorithm = file.algorithm,
                    t = file.t,
                    limit = file.limit
                )
            }
        }
    }
}