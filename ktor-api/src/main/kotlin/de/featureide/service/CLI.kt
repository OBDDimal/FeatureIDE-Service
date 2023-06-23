package de.featureide.service

import de.ovgu.featureide.fm.core.analysis.cnf.SolutionList
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.*
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.twise.TWiseConfigurationGenerator
import de.ovgu.featureide.fm.core.base.IFeature
import de.ovgu.featureide.fm.core.base.IFeatureModel
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.IPersistentFormat
import de.ovgu.featureide.fm.core.io.csv.ConfigurationListFormat
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.io.manager.FileHandler
import de.ovgu.featureide.fm.core.io.sxfm.SXFMFormat
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat
import de.ovgu.featureide.fm.core.job.SliceFeatureModel
import de.ovgu.featureide.fm.core.job.monitor.ConsoleMonitor
import de.ovgu.featureide.fm.core.job.monitor.NullMonitor
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.util.stream.Collectors
import kotlin.system.exitProcess

object CLI {

    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val parser = ArgParser("featureide-cli")

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file or directory.").required()
        val slice by parser.option(ArgType.String, shortName = "s", description = "The names of the features that should be sliced separated by ','. For example: Antenna,AHEAD.")
        val check by parser.option(ArgType.String, shortName = "c", description = "Input path for the second file that should be checked with the first one.")
        val algorithm by parser.option(ArgType.String, shortName = "alg", description = "The algorithm to generate a configuration sample as csv file")
        val all by parser.option(ArgType.Boolean, shortName = "a", description = "Parsers all files from path into all formats.").default(false)
        val dimacs by parser.option(ArgType.Boolean, shortName = "d", description = "Parses all files from path into dimacs files.").default(false)
        val uvl by parser.option(ArgType.Boolean, shortName = "u", description = "Parses all files from path into uvl files.").default(false)
        val sxfm by parser.option(ArgType.Boolean, shortName = "sf", description = "Parses all files from path into sxfm(xml) files.").default(false)
        val featureIde by parser.option(ArgType.Boolean, shortName = "fi", description = "Parses all files from path into featureIde(xml) files.").default(false)
        val t by parser.option(ArgType.Int, shortName = "t", description = "The t wise pairing that should be covered by the configuration sample").default(0)
        val limit by parser.option(ArgType.Int, shortName = "l", description = "The maximum amount of configurations for the configuration sample").default(Int.MAX_VALUE)


        parser.parse(args)


        val file = File(path)

        val output = "./files/output"

        File(output).deleteRecursively()

        Files.createDirectories(Paths.get(output))

        //slices the featureModel
        if (!slice.isNullOrEmpty()){
            if(file.isDirectory() || !file.exists()) exitProcess(0)
            var model = FeatureModelManager.load(Paths.get(file.path))
            val featuresToSlice = ArrayList<IFeature>()

            for (name in slice!!.split(",")){
                featuresToSlice.add(model.getFeature(name))
            }
            model = slice(model, featuresToSlice)
            saveFeatureModel(model, "${output}/${file.nameWithoutExtension}.${XmlFeatureModelFormat().suffix}", XmlFeatureModelFormat())
            exitProcess(0)
        }

        if (!algorithm.isNullOrEmpty()){
            if(file.isDirectory() || !file.exists()) exitProcess(0)
            val model = FeatureModelManager.load(Paths.get(file.path))

            val cnf = FeatureModelFormula(model).cnf

            var generator: IConfigurationGenerator? = null
            with(algorithm!!) {
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
                        var iterations: Int = 10
                        if(algorithm!!.split("_").size > 1){
                            try {
                                iterations = Integer.parseInt(algorithm!!.split("_")[1])
                            } catch (_: Exception){
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

            FileHandler.save(
                Paths.get("${output}/${file.nameWithoutExtension}_${algorithm}_t${t}_${limit}.${ConfigurationListFormat().suffix}"),
                SolutionList(cnf.getVariables(), result),
                ConfigurationListFormat()
            )
            exitProcess(0)
        }

        //checks if two featureModel are the same
        if (!check.isNullOrEmpty()){
            val file2 = File(check)
            if(file.isDirectory() || !file.exists() || file2.isDirectory() || !file2.exists()) exitProcess(0)
            var model = FeatureModelManager.load(Paths.get(file.path))
            var model2 = FeatureModelManager.load(Paths.get(file2.path))

            val first = File.createTempFile("temp1", ".xml")
            val second = File.createTempFile("temp2", ".xml")
            saveFeatureModel(model, first.path, XmlFeatureModelFormat())
            saveFeatureModel(model2, second.path, XmlFeatureModelFormat())
            first.deleteOnExit()
            second.deleteOnExit()
            val md = MessageDigest.getInstance("MD5")
            val hash1 = md.digest(first.readBytes())
            val checksum1 = BigInteger(1, hash1).toString(16)
            val hash2 = md.digest(second.readBytes())
            val checksum2 = BigInteger(1, hash2).toString(16)
            System.out.print(checksum1.equals(checksum2))

            exitProcess(0)
        }

        if(file.isDirectory){
            val inputFiles = file.listFiles()
            inputFiles?.let { files ->
                for (fileFromList in files) {
                    if (fileFromList.isDirectory) {
                        continue
                    }

                    val model = FeatureModelManager.load(Paths.get(fileFromList.path))
                    val formats: MutableList<IPersistentFormat<IFeatureModel>> = mutableListOf()

                    if (dimacs || all) {
                        formats.add(DIMACSFormat())
                    }

                    if (uvl || all) {
                        formats.add(UVLFeatureModelFormat())
                    }

                    if (featureIde || all) {
                        formats.add(XmlFeatureModelFormat())
                    }

                    if (sxfm || all) {
                        formats.add(SXFMFormat())
                    }

                    for (format in formats) {
                        println("Converting ${file.name} to ${format.suffix}")
                        saveFeatureModel(
                            model,
                            "${output}/${fileFromList.nameWithoutExtension}.${format.suffix}",
                            format,
                        )
                    }
                }
            }
        } else if (file.exists()){
            val model = FeatureModelManager.load(Paths.get(file.path))
            val formats: MutableList<IPersistentFormat<IFeatureModel>> = mutableListOf()

            if (dimacs || all) {
                formats.add(DIMACSFormat())
            }

            if (uvl || all) {
                formats.add(UVLFeatureModelFormat())
            }

            if (featureIde || all) {
                formats.add(XmlFeatureModelFormat())
            }

            if (sxfm || all) {
                formats.add(SXFMFormat())
            }

            for (format in formats) {
                println("Converting ${file.name} to ${format.suffix}")
                saveFeatureModel(
                    model,
                    "${output}/${file.nameWithoutExtension}_${format.name}.${format.suffix}",
                    format,
                )
            }
        }
    }

    private fun saveFeatureModel(model: IFeatureModel?, savePath: String, format: IPersistentFormat<IFeatureModel>?) : String{
        FeatureModelManager.save(model, Paths.get(savePath), format)
        return savePath
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
            e.printStackTrace()
            null
        }
    }

}