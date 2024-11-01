package de.featureide.service.util

import de.featureide.service.util.Converter.saveFeatureModel
import de.featureide.service.util.Slicer.slice
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer
import de.ovgu.featureide.fm.core.base.IFeature
import de.ovgu.featureide.fm.core.base.IFeatureModel
import de.ovgu.featureide.fm.core.base.IFeatureStructure
import de.ovgu.featureide.fm.core.base.impl.FeatureStructure
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.IPersistentFormat
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.job.monitor.NullMonitor
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

object EvalFeatureChanger {

    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val parser = ArgParser("featureide-cli")

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file")
            .required()

        val relationShip by parser.option(ArgType.String, shortName = "r", description = "Relationship to the parent")

        val slicing by parser.option(ArgType.Boolean, shortName = "s", description = "Slice the features")

        val mandatory by parser.option(ArgType.Boolean, shortName = "m", description = "Make features mandatory")

        val constraint by parser.option(ArgType.Boolean, shortName = "c", description = "Add constraint")

        val features by parser.option(ArgType.String, shortName = "f", description = "Name of the features splitted by a , .")


        parser.parse(args)
        val file = File(path)
        val output = "./files/outputFeatureModel"
        val outputCSV = "./files/outputCSVEval"
        Files.createDirectories(Path.of(output))
        Files.createDirectories(Path.of(outputCSV))
        File(output).listFiles()?.forEach { it.delete() }
        File(outputCSV).listFiles()?.forEach { it.delete() }

        if (file.isDirectory()) {

        } else if (file.exists()) {

            if (slicing == true) {

                var model = FeatureModelManager.load(Paths.get(file.path))

                val statTriple = getDeadCoreFeatures(model)

                println("Before Slicing")

                println("Features: " + model.features.size)
                println("DeadFeatures: " + statTriple.first.size)
                println("FalseOptional: " + statTriple.second.size)
                println("CoreFeatures: " + statTriple.third.size)

                val format: IPersistentFormat<IFeatureModel> = DIMACSFormat()

                var newPath = "$output/${file.nameWithoutExtension}.${format.suffix}"

                saveFeatureModel(
                    model,
                    newPath,
                    format,
                )

                var file = File(newPath)

                var configurations = getNumberOfConfigurationsWithDDnife(outputCSV, file)

                println("Configurations: " + configurations )
                var digits = configurations.length
                var firstFour = configurations.substring(0,1) + "." + configurations.substring(1,4)
                println(firstFour + "E" + (digits-1))

                val featuresToLook = features?.split(",")
                var featuresToSlice = ArrayList<String>()

                if (featuresToLook != null) {
                    featuresToSlice = getChildrenForSubTree(featuresToLook, model)
                }

                val featuresToSliceArray = featuresToSlice.toTypedArray()

                model = slice(model, featuresToSliceArray)

                println("After Slicing")

                println("Features: " + model.features.size)
                println("DeadFeatures: " + statTriple.first.size)
                println("FalseOptional: " + statTriple.second.size)
                println("CoreFeatures: " + statTriple.third.size)

                newPath = "$output/${file.nameWithoutExtension}_sliced.${format.suffix}"

                saveFeatureModel(
                    model,
                    newPath,
                    format,
                )

                file = File(newPath)

                configurations = getNumberOfConfigurationsWithDDnife(outputCSV, file)

                println("Configurations: " + configurations)

                digits = configurations.length
                firstFour = configurations.substring(0,1) + "." + configurations.substring(1,4)
                println(firstFour + "E" + (digits-1))

            } else if (mandatory == true){

                if (relationShip == "and"){

                } else if (relationShip == "or"){

                } else if (relationShip == "alt"){

                }

            } else if (constraint == true){

            }

        }


    }

    fun getDeadCoreFeatures (model: IFeatureModel) : Triple<List<IFeature>, List<IFeature>, List<IFeature>>{
        val featureModelAnalyzer = FeatureModelAnalyzer(model)
        return Triple(featureModelAnalyzer.getDeadFeatures(NullMonitor()), featureModelAnalyzer.getFalseOptionalFeatures(NullMonitor()),featureModelAnalyzer.getCoreFeatures(NullMonitor()))
    }

    fun getNumberOfConfigurationsWithDDnife(outputCSV: String, dimacsPath: File): String{
        val rt = Runtime.getRuntime()

        var csvPath = "${outputCSV}/${dimacsPath.nameWithoutExtension}"

        val p = rt.exec(arrayOf(".\\ddnnife\\bin\\ddnnife.exe", dimacsPath.path, "-c", csvPath))
        p.waitFor(30, TimeUnit.SECONDS)
        p.destroy()
        csvPath += "-features.csv"

        val br = BufferedReader(FileReader(csvPath))
        var line: String? = null
        var configurations = ""

        while ((br.readLine().also { line = it }) != null) {
            val str = line!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (str[2].toDouble().equals(1.0)){
                configurations = str[1]
                break
            }
        }
        br.close()
        return configurations
    }


    fun getChildrenForSubTree(featuresToLook: List<String>, model: IFeatureModel): ArrayList<String> {

        val featuresToSlice = ArrayList<String>()
        for (feature in featuresToLook) {
            val featureStructure = model.getFeature(feature).structure
            featuresToSlice.addAll(getAllChildrenForStructure(featureStructure))
        }

        return featuresToSlice
    }

    fun getAllChildrenForStructure(featureStructure: IFeatureStructure): List<String> {

        val featureList = mutableListOf(featureStructure.feature.name)
        if (featureStructure.children.size > 0) {
            featureStructure.children.forEach { child ->
                featureList.addAll(getAllChildrenForStructure(
                    child
                ))
            }
            return featureList
        } else {
            return featureList
        }
    }
}