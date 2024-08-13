package de.featureide.service.util

import de.featureide.service.helpclasses.ChildFeature
import de.featureide.service.helpclasses.ParentFeature
import de.featureide.service.util.Converter.saveFeatureModel
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula
import de.ovgu.featureide.fm.core.base.IFeatureStructure
import de.ovgu.featureide.fm.core.base.impl.FeatureStructure
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import io.ktor.util.*
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
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
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

        val maxChildren by parser.option(ArgType.Double, shortName = "ch", description = "Children Bound.")

        val maxConstraints by parser.option(ArgType.Double, shortName = "co", description = "Constraints Bound.")

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
                commonalityFromFile(fileFromList, output, outputCSV, lowerBound, upperBound, parent, maxChildren, maxConstraints)
            }
        } else if (file.exists()) {
            commonalityFromFile(file, output, outputCSV, lowerBound, upperBound, parent, maxChildren, maxConstraints)
        }

    }

    fun commonalityFromFile(
        file: File,
        output: String,
        outputCSV: String,
        lowerBoundNull: Double?,
        upperBoundNull: Double?,
        parentNull: Boolean?,
        childrenMaxCountNull: Double?,
        constraintMaxCountNull: Double?,
    ) {

        val start = LocalDateTime.now()
        var start2 = LocalDateTime.now()

        val parent = parentNull ?: false
        val childrenMaxCount = childrenMaxCountNull ?: -1.0
        val constraintMaxCount = constraintMaxCountNull ?: -1.0


        if (parent) {
            val lowerBound = lowerBoundNull ?: 0.9
            val upperBound = upperBoundNull ?: 1.0
            try {
                println(file.nameWithoutExtension)

                val model = FeatureModelManager.load(Paths.get(file.path))

                model.constraints.size

                val cnf = FeatureModelFormula(model).cnf

                val dimacsPath = "${outputCSV}/${file.nameWithoutExtension}.${DIMACSFormat().suffix}"

                Path.of(dimacsPath)

                saveFeatureModel(
                    model,
                    dimacsPath,
                    DIMACSFormat(),
                )

                val map = getCommonalitiesWithDDnnife(file, outputCSV, dimacsPath)

                println(map.size)

                start2 = LocalDateTime.now()

                val mapFiltered =
                    map.filter { stringFloatEntry -> stringFloatEntry.value >= lowerBound && stringFloatEntry.value <= upperBound }

                var parentChildAndMap = HashMap<ParentFeature, List<ChildFeature>>()
                val parentChildAltMap = HashMap<ParentFeature, List<ChildFeature>>()
                val parentChildOrMap = HashMap<ParentFeature, List<ChildFeature>>()

                val sbAnd = StringBuilder()
                sbAnd.append("FeatureName,Commonality,isOptional,Children,NumberOfConstraints,ParentName,ParentCommonality,ParentIsOptional,isParentAnd,isParentOr,isParentAlt,ChildrenParent,ParentConstraints\n")

                val sbOr = StringBuilder()
                sbOr.append("FeatureName,Commonality,isOptional,Children,NumberOfConstraints,ParentName,ParentCommonality,ParentIsOptional,isParentAnd,isParentOr,isParentAlt,ChildrenParent,ParentConstraints\n")

                val sbAlt = StringBuilder()
                sbAlt.append("FeatureName,Commonality,isOptional,Children,NumberOfConstraints,ParentName,ParentCommonality,ParentIsOptional,isParentAnd,isParentOr,isParentAlt,ChildrenParent,ParentConstraints\n")

                for (entry in mapFiltered) {
                    val parentFeatureName = cnf.variables.getName(entry.key)
                    val parentCommonality = entry.value
                    val parentFeatureFromModel = model.getFeature(parentFeatureName)
                    val parentFeatureStructure = parentFeatureFromModel.structure
                    val parentFeature = ParentFeature(parentFeatureFromModel, parentCommonality, parentFeatureStructure)
                    val isParentAnd = parentFeature.featureStructure.isAnd
                    val isParentOr = parentFeature.featureStructure.isOr
                    val isParentAlt = parentFeature.featureStructure.isAlternative
                    if (isParentAnd) {
                        val childFeatureList = ArrayList<ChildFeature>()
                        for (featureStructure in parentFeatureStructure.children) {
                            val feature = featureStructure.feature
                            val commonality = map[cnf.variables.getVariable(feature.name)]
                            val childFeature = ChildFeature(
                                feature,
                                commonality!!,
                                featureStructure,
                                getChildrenCountForSubTree(featureStructure),
                                getRelevantConstraintsForSubTree(featureStructure)
                            )
                            if (childrenMaxCount > -1.0 && childFeature.childrenSubtree > childrenMaxCount) {
                                continue
                            } else if (constraintMaxCount > -1.0 && childFeature.constraintSubtree > constraintMaxCount) {
                                continue
                            }
                            childFeatureList.add(childFeature)
                        }
                        val sortedChildFeatureList = childFeatureList.sortedWith(compareBy<ChildFeature> { it.childrenSubtree }.thenBy { it.constraintSubtree })
                        parentChildOrMap[parentFeature] = sortedChildFeatureList
                    } else if (isParentAlt) {
                        val childFeatureList = ArrayList<ChildFeature>()
                        for (featureStructure in parentFeatureStructure.children) {
                            val feature = featureStructure.feature
                            val commonality = map[cnf.variables.getVariable(feature.name)]
                            val childFeature = ChildFeature(
                                feature,
                                commonality!!,
                                featureStructure,
                                getChildrenCountForSubTree(featureStructure),
                                getRelevantConstraintsForSubTree(featureStructure)
                            )
                            if (childrenMaxCount > -1.0 && childFeature.childrenSubtree > childrenMaxCount) {
                                continue
                            } else if (constraintMaxCount > -1.0 && childFeature.constraintSubtree > constraintMaxCount) {
                                continue
                            }
                            childFeatureList.add(childFeature)
                        }
                        val sortedChildFeatureList = childFeatureList.sortedWith(compareBy<ChildFeature> { it.childrenSubtree }.thenBy { it.constraintSubtree })
                        parentChildOrMap[parentFeature] = sortedChildFeatureList

                    } else if (isParentOr) {
                        val childFeatureList = ArrayList<ChildFeature>()
                        for (featureStructure in parentFeatureStructure.children) {
                            val feature = featureStructure.feature
                            val commonality = map[cnf.variables.getVariable(feature.name)]
                            val childFeature = ChildFeature(
                                feature,
                                commonality!!,
                                featureStructure,
                                getChildrenCountForSubTree(featureStructure),
                                getRelevantConstraintsForSubTree(featureStructure)
                            )
                            if (childrenMaxCount > -1.0 && childFeature.childrenSubtree > childrenMaxCount) {
                                continue
                            } else if (constraintMaxCount > -1.0 && childFeature.constraintSubtree > constraintMaxCount) {
                                continue
                            }
                            childFeatureList.add(childFeature)
                        }
                        val sortedChildFeatureList = childFeatureList.sortedWith(compareBy<ChildFeature> { it.childrenSubtree }.thenBy { it.constraintSubtree })
                        parentChildOrMap[parentFeature] = sortedChildFeatureList
                    }
                }
                parentChildAndMap = parentChildAndMap.toSortedMap(compareBy { it.featureStructure.childrenCount }).toMap(HashMap())

                for (entry in parentChildOrMap) {
                    for (entryChild in entry.value) {
                        sbOr.append("${entryChild.feature.name},${entryChild.commonality},${!entryChild.featureStructure.isMandatory},${entryChild.childrenSubtree},${entryChild.constraintSubtree},${entry.key.feature.name},${entry.key.commonality},${!entry.key.featureStructure.isMandatory},${entry.key.featureStructure.isAnd},${entry.key.featureStructure.isOr},${entry.key.featureStructure.isAlternative},${entry.key.featureStructure.children.size},${entry.key.featureStructure.relevantConstraints.size}\n")
                    }
                }
                for (entry in parentChildAltMap) {
                    for (entryChild in entry.value) {
                        sbAlt.append("${entryChild.feature.name},${entryChild.commonality},${!entryChild.featureStructure.isMandatory},${entryChild.childrenSubtree},${entryChild.constraintSubtree},${entry.key.feature.name},${entry.key.commonality},${!entry.key.featureStructure.isMandatory},${entry.key.featureStructure.isAnd},${entry.key.featureStructure.isOr},${entry.key.featureStructure.isAlternative},${entry.key.featureStructure.children.size},${entry.key.featureStructure.relevantConstraints.size}\n")
                    }
                }
                for (entry in parentChildAndMap) {
                    for (entryChild in entry.value) {
                        sbAnd.append("${entryChild.feature.name},${entryChild.commonality},${!entryChild.featureStructure.isMandatory},${entryChild.childrenSubtree},${entryChild.constraintSubtree},${entry.key.feature.name},${entry.key.commonality},${!entry.key.featureStructure.isMandatory},${entry.key.featureStructure.isAnd},${entry.key.featureStructure.isOr},${entry.key.featureStructure.isAlternative},${entry.key.featureStructure.children.size},${entry.key.featureStructure.relevantConstraints.size}\n")
                    }
                }


                val infoAnd = File("${output}/${file.nameWithoutExtension}_parentVarianceDriverAnd.csv")
                infoAnd.writeText(sbAnd.toString())
                val infoAlt = File("${output}/${file.nameWithoutExtension}_parentVarianceDriverAlt.csv")
                infoAlt.writeText(sbAlt.toString())
                val infoOr = File("${output}/${file.nameWithoutExtension}_parentVarianceDriverOr.csv")
                infoOr.writeText(sbOr.toString())
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

                start2 = LocalDateTime.now()

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

        println(
            file.nameWithoutExtension + ": " + Duration.between(
                start,
                LocalDateTime.now()
            ).toString()
        )

        println(
            file.nameWithoutExtension + ": without ddnnife" + Duration.between(
                start2,
                LocalDateTime.now()
            ).toString()
        )
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

    private fun getChildrenCountForSubTree(featureStructure: IFeatureStructure): Int {
        if (featureStructure.children.size > 1) {
            return featureStructure.children.size + featureStructure.children.map { child ->
                getChildrenCountForSubTree(
                    child
                )
            }.sum()
        } else {
            return 0;
        }
    }

    private fun getRelevantConstraintsForSubTree(featureStructure: IFeatureStructure): Int {
        if (featureStructure.children.size > 1) {
            return featureStructure.relevantConstraints.size + featureStructure.children.map { child ->
                getRelevantConstraintsForSubTree(
                    child
                )
            }.sum()
        } else {
            return featureStructure.relevantConstraints.size;
        }
    }


}