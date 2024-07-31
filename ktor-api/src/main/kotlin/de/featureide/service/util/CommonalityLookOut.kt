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
import org.checkerframework.checker.index.qual.PolyLowerBound
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import kotlin.math.pow


object CommonalityLookOut {

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val parser = ArgParser("featureide-cli")

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file or directory.")
            .required()

        val lowerBound by parser.option(ArgType.Double, shortName = "l", description = "Lower Commonality Bound.")

        val upperBound by parser.option(ArgType.Double, shortName = "u", description = "Upper Commonality Bound.")

        val parent by parser.option(ArgType.Boolean, shortName = "pa", description = "Use the parent method.")

        parser.parse(args)
        val file = File(path)
        val output = "./files/output"
        val outputCSV = "./files/outputCSV"
        Files.createDirectories(Path.of(output))
        Files.createDirectories(Path.of(outputCSV))
        File(output).listFiles()?.forEach { it.delete() }
        File(outputCSV).listFiles()?.forEach { it.delete() }

        if (file.isDirectory()) {

            val inputFiles = file.listFiles()
            for (fileFromList in inputFiles!!) {
                if (fileFromList.isDirectory || fileFromList.extension != "xml") {
                    continue
                }
                commonalityFromFile(fileFromList, output, outputCSV, lowerBound, upperBound, parent)
            }
        } else if (file.exists()) {
            commonalityFromFile(file, output, outputCSV, lowerBound, upperBound, parent)
        }

    }

    fun commonalityFromFile(
        file: File,
        output: String,
        outputCSV: String,
        lowerBoundNull: Double?,
        upperBoundNull: Double?,
        parentNull: Boolean?
    ) {


        val parent = parentNull ?: false

        if (parent) {
            val lowerBound = lowerBoundNull ?: 0.9
            val upperBound = upperBoundNull ?: 1.0

            val sb = StringBuilder()
            sb.append("FeatureName;Commonality;isOptional;Children;NumberOfConstraints;ParentName;ParentCommonality;isParentAnd;isParentOr;isParentAlt;ChildrenParent;ParentConstraints\n")
            try {
                println(file.nameWithoutExtension)

                val model = FeatureModelManager.load(Paths.get(file.path))

                val cnf = FeatureModelFormula(model).cnf

                val dimacsPath = "${outputCSV}/${file.nameWithoutExtension}.${DIMACSFormat().suffix}"

                Path.of(dimacsPath)

                saveFeatureModel(
                    model,
                    dimacsPath,
                    DIMACSFormat(),
                )

                val map = getCommonalitiesWithDDnnife(file, outputCSV, dimacsPath)

                val mapFiltered =
                    map.filter { stringFloatEntry -> stringFloatEntry.value >= lowerBound && stringFloatEntry.value <= upperBound }

                for (entry in mapFiltered) {
                    val parentFeatureName = cnf.variables.getName(entry.key)
                    val parentCommonality = entry.value
                    val parentFeature = model.getFeature(parentFeatureName)
                    val parentFeatureStructure = parentFeature.structure
                    val isParentOptional = !parentFeatureStructure.isMandatory
                    val isParentAnd = parentFeatureStructure.isAnd
                    val isParentOr = parentFeatureStructure.isOr
                    val isParentAlt = parentFeatureStructure.isAlternative
                    val parentConstraints = parentFeatureStructure.relevantConstraints.size
                    if (isParentAnd) {
                        for (featureStructure in parentFeatureStructure.children) {
                            val featureName = featureStructure.feature.name
                            val commonality = map[cnf.variables.getVariable(featureName)]
                            val isOptional = !featureStructure.isMandatory
                            val childrenCount = featureStructure.children.size
                            val childrenCountParent = parentFeatureStructure.children.size
                            val constraints = featureStructure.relevantConstraints.size
                            if (commonality != parentCommonality / 2) {
                                continue
                            } else if (childrenCount > 0) {
                                continue
                            } else if (constraints > 0) {
                                continue
                            } else if (!isOptional) {
                                continue
                            }
                            sb.append("${featureName};${commonality};${isOptional};${childrenCount};${constraints};${parentFeatureName};${parentCommonality};${isParentAnd};${isParentOr};${isParentAlt};${childrenCountParent};${parentConstraints}\n")
                        }
                    } else if (isParentAlt) {
                        for (featureStructure in parentFeatureStructure.children) {
                            val featureName = featureStructure.feature.name
                            val commonality = map[cnf.variables.getVariable(featureName)]
                            val isOptional = !featureStructure.isMandatory
                            val childrenCount = featureStructure.children.size
                            val childrenCountParent = parentFeatureStructure.children.size
                            val constraints = featureStructure.relevantConstraints.size
                            if (commonality != parentCommonality / childrenCountParent) {
                                continue
                            } else if (childrenCount > 0) {
                                continue
                            } else if (constraints > 0) {
                                continue
                            } else if (!isOptional) {
                                continue
                            }
                            sb.append("${featureName};${commonality};${isOptional};${childrenCount};${constraints};${parentFeatureName};${parentCommonality};${isParentAnd};${isParentOr};${isParentAlt};${childrenCountParent};${parentConstraints}\n")
                        }

                    } else if (isParentOr) {
                        for (featureStructure in parentFeatureStructure.children) {
                            val featureName = featureStructure.feature.name
                            val commonality = map[cnf.variables.getVariable(featureName)]
                            val isOptional = !featureStructure.isMandatory
                            val childrenCount = featureStructure.children.size
                            val childrenCountParent = parentFeatureStructure.children.size
                            val constraints = featureStructure.relevantConstraints.size
                            val possibilities = 2.0.pow(childrenCountParent)
                            if (commonality != (possibilities/2)/(possibilities-1)*parentCommonality) {
                                continue
                            } else if (childrenCount > 0) {
                                continue
                            } else if (constraints > 0) {
                                continue
                            } else if (!isOptional) {
                                continue
                            }
                            sb.append("${featureName};${commonality};${isOptional};${childrenCount};${constraints};${parentFeatureName};${parentCommonality};${isParentAnd};${isParentOr};${isParentAlt};${childrenCountParent};${parentConstraints}\n")
                        }
                    }
                }
                val info = File("${output}/${file.nameWithoutExtension}_parentVarianceDriver.csv")
                info.writeText(sb.toString())
            } catch (e: NullPointerException) {
                println(e.stackTraceToString())
                println("FeatureModel: ${file.nameWithoutExtension}")
            } catch (e: Exception) {
                println(e.stackTraceToString())
                println("FeatureModel: ${file.nameWithoutExtension}")
            }
        } else {
            val lowerBound = lowerBoundNull ?: 0.4
            val upperBound = upperBoundNull ?: 0.6

            val sb = StringBuilder()
            sb.append("FeatureName;Commonality;isOptional;Children;NumberOfConstraints;ParentName;ParentCommonality;isParentAnd;isParentOr;isParentAlt;ChildrenParent\n")
            try {
                println(file.nameWithoutExtension)

                val model = FeatureModelManager.load(Paths.get(file.path))

                val cnf = FeatureModelFormula(model).cnf

                val dimacsPath = "${outputCSV}/${file.nameWithoutExtension}.${DIMACSFormat().suffix}"

                Path.of(dimacsPath)

                saveFeatureModel(
                    model,
                    dimacsPath,
                    DIMACSFormat(),
                )

                val map = getCommonalitiesWithDDnnife(file, outputCSV, dimacsPath)

                val mapFiltered =
                    map.filter { stringFloatEntry -> stringFloatEntry.value >= lowerBound && stringFloatEntry.value <= upperBound }

                for (entry in mapFiltered) {
                    val featureName = cnf.variables.getName(entry.key)
                    val commonality = entry.value
                    val feature = model.getFeature(featureName)
                    val featureStructure = feature.structure
                    val isOptional = !featureStructure.isMandatory
                    val childrenCount = featureStructure.children.size
                    val parent = featureStructure.parent
                    val parentName = parent.feature.name
                    val childrenCountParent = parent.children.size
                    val parentCommonality = map[cnf.variables.getVariable(parentName)]
                    val isParentAnd = parent.isAnd
                    val isParentOr = parent.isOr
                    val isParentAlt = parent.isAlternative
                    val constraints = featureStructure.relevantConstraints.size
                    sb.append("${featureName};${commonality};${isOptional};${childrenCount};${constraints};${parentName};${parentCommonality};${isParentAnd};${isParentOr};${isParentAlt};${childrenCountParent}\n")
                }
                val info = File("${output}/${file.nameWithoutExtension}_varianceDriver.csv")
                info.writeText(sb.toString())
            } catch (e: NullPointerException) {
                println(e.stackTraceToString())
                println("FeatureModel: ${file.nameWithoutExtension}")
            } catch (e: Exception) {
                println(e.stackTraceToString())
                println("FeatureModel: ${file.nameWithoutExtension}")
            }
        }
    }

    fun getCommonalitiesWithDDnnife(file: File, outputCSV: String, dimacsPath: String): Map<Int, Double> {
        val rt = Runtime.getRuntime()

        var csvPath = "${outputCSV}/${file.nameWithoutExtension}"

        val p = rt.exec(arrayOf(".\\ddnnife\\bin\\ddnnife.exe", dimacsPath, "-c", csvPath))
        p.waitFor(30, TimeUnit.SECONDS)
        p.destroy()
        csvPath += "-features.csv"

        val br = BufferedReader(FileReader(csvPath))
        var line: String? = null
        val map = HashMap<Int, Double>()

        while ((br.readLine().also { line = it }) != null) {
            val str = line!!.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            map.put(str[0].toInt(), str[2].toDouble())
        }

        return map
    }


    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }
}