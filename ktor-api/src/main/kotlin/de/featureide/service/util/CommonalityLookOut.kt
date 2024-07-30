package de.featureide.service.util

import de.featureide.service.util.Converter.saveFeatureModel
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
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
import java.util.concurrent.TimeUnit


object CommonalityLookOut {

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val parser = ArgParser("featureide-cli")

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file or directory.")
            .required()

        parser.parse(args)
        val file = File(path)
        val output = "./files/output"
        val outputCSV = "./files/outputCSV"
        Files.createDirectories(Path.of(output))
        Files.createDirectories(Path.of(outputCSV))

        if (file.isDirectory()) {

            File(output).listFiles()?.forEach { it.delete() }
            File(outputCSV).listFiles()?.forEach { it.delete() }

            val inputFiles = file.listFiles()
            for (fileFromList in inputFiles!!) {
                if (fileFromList.isDirectory || fileFromList.extension != "xml") {
                    continue
                }
                val sb = StringBuilder()
                sb.append("FeatureName;Commonality;isOptional;ParentName;ParentCommonality;isParentAnd;isParentOr;isParentAlt;NumberOfConstraints\n")
                try {
                    println(fileFromList.nameWithoutExtension)

                    val model = FeatureModelManager.load(Paths.get(fileFromList.path))

                    val cnf = FeatureModelFormula(model).cnf

                    val dimacsPath = "${outputCSV}/${fileFromList.nameWithoutExtension}.${DIMACSFormat().suffix}"

                    Path.of(dimacsPath)

                    saveFeatureModel(
                        model,
                        dimacsPath,
                        DIMACSFormat(),
                    )

                    val rt = Runtime.getRuntime()

                    var csvPath = "${outputCSV}/${fileFromList.nameWithoutExtension}"

                    val p = rt.exec(arrayOf(".\\ddnnife\\bin\\ddnnife.exe", dimacsPath, "-c", csvPath))
                    p.waitFor(30, TimeUnit.SECONDS)
                    csvPath += "-features.csv"

                    val br = BufferedReader(FileReader(csvPath))
                    var line: String? = null
                    val map = HashMap<Int, Float>()

                    while ((br.readLine().also { line = it }) != null) {
                        val str = line!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        map.put(str[0].toInt(), str[2].toFloat())
                    }

                    val mapFiltered = map.filter { stringFloatEntry -> stringFloatEntry.value >= 0.4 && stringFloatEntry.value <= 0.6 }

                    for (entry in mapFiltered){
                        val featureName = cnf.variables.getName(entry.key)
                        val commonality = entry.value
                        val feature = model.getFeature(featureName)
                        val featureStructure = feature.structure
                        val isOptional = !featureStructure.isMandatory
                        val parent = featureStructure.parent
                        val parentName = parent.feature.name
                        val parentCommonality = map[cnf.variables.getVariable(parentName)]
                        val isParentAnd = parent.isAnd
                        val isParentOr = parent.isOr
                        val isParentAlt = parent.isAlternative
                        val constraints = featureStructure.relevantConstraints.size
                        sb.append("${featureName};${commonality};${isOptional};${parentName};${parentCommonality};${isParentAnd};${isParentOr};${isParentAlt};${constraints}\n")
                    }
                    val info = File("${output}/${fileFromList.nameWithoutExtension}_varianceDriver.csv")
                    info.writeText(sb.toString())
                } catch (e: NullPointerException) {
                    println(e.stackTraceToString())
                    println("FeatureModel: ${fileFromList.nameWithoutExtension}")
                } catch (e: Exception) {
                    println(e.stackTraceToString())
                    println("FeatureModel: ${fileFromList.nameWithoutExtension}")
                }
            }


        } else if (file.exists()) {

            val sb = StringBuilder()
            sb.append("FeatureName;Commonality;isOptional;Parent;ParentCommonality;isParentAnd;isParentOr;isParentAlt;NumberOfConstraints\n")

            try {

                val model = FeatureModelManager.load(Paths.get(file.path))

                val cnf = FeatureModelFormula(model).cnf

                val dimacsPath = "${outputCSV}/${file.nameWithoutExtension}.${DIMACSFormat().suffix}"

                Path.of(dimacsPath)

                saveFeatureModel(
                    model,
                    dimacsPath,
                    DIMACSFormat(),
                )

                val rt = Runtime.getRuntime()

                var csvPath = "${outputCSV}/${file.nameWithoutExtension}"

                val p = rt.exec(arrayOf(".\\ddnnife\\bin\\ddnnife.exe", dimacsPath, "-c", csvPath))
                p.waitFor(30, TimeUnit.SECONDS)
                csvPath += "-features.csv"

                val br = BufferedReader(FileReader(csvPath))
                var line: String? = null
                val map = HashMap<Int, Float>()

                while ((br.readLine().also { line = it }) != null) {
                    val str = line!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    map.put(str[0].toInt(), str[2].toFloat())
                }

                val mapFiltered = map.filter { stringFloatEntry -> stringFloatEntry.value >= 0.4 && stringFloatEntry.value <= 0.6 }

                for (entry in mapFiltered){
                    val featureName = cnf.variables.getName(entry.key)
                    val commonality = entry.value
                    val feature = model.getFeature(featureName)
                    val featureStructure = feature.structure
                    val isOptional = !featureStructure.isMandatory
                    val parent = featureStructure.parent
                    val parentName = parent.feature.name
                    val parentCommonality = map[cnf.variables.getVariable(parentName)]
                    val isParentAnd = parent.isAnd
                    val isParentOr = parent.isOr
                    val isParentAlt = parent.isAlternative
                    val constraints = featureStructure.relevantConstraints.size
                    sb.append("${featureName};${commonality};${isOptional};${parentName};${parentCommonality};${isParentAnd};${isParentOr};${isParentAlt};${constraints}\n")
                }
                val info = File("${output}/${file.nameWithoutExtension}_varianceDriver.csv")
                info.writeText(sb.toString())

            } catch (e: NullPointerException) {
                println("Could not convert file, because file could not be converted to a feature model.")
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }

    }

    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }
}