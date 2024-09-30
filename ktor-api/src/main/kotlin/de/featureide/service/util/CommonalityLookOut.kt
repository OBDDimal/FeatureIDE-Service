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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs
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

        val threshold by parser.option(ArgType.Double, shortName = "th", description = "Commonality Bound.")

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
                commonalityFromFile(
                    fileFromList,
                    output,
                    outputCSV,
                    lowerBound,
                    upperBound,
                    parent,
                    maxChildren,
                    maxConstraints,
                    threshold
                )
            }
        } else if (file.exists()) {
            commonalityFromFile(file, output, outputCSV, lowerBound, upperBound, parent, maxChildren, maxConstraints, threshold)
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
        thresholdNull: Double?
    ) {

        val start = LocalDateTime.now()
        var start2 = LocalDateTime.now()

        val parent = parentNull ?: false
        val childrenMaxCount = childrenMaxCountNull ?: -1.0
        val constraintMaxCount = constraintMaxCountNull ?: -1.0
        val threshold = thresholdNull ?: 0.0


        if (!parent) {
            val lowerBound = lowerBoundNull ?: 0.9
            val upperBound = upperBoundNull ?: 1.0
            try {
                println(file.nameWithoutExtension)

                val model = FeatureModelManager.load(Paths.get(file.path))

                val allConstraints = model.constraints.size
                val allFeatures = model.features.size

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

                val comparatorAnd =
                    compareBy<Pair<ParentFeature, ChildFeature>> { it.second.featureStructure.isMandatory }
                        .thenBy { it.second.constraintSubtree }
                        .thenBy { it.second.childrenSubtree }
                        .thenByDescending { it.first.commonality }
                        .thenBy { abs(0.5 - it.second.commonality) }
                        .thenBy { !it.first.featureStructure.isMandatory }
                        .thenBy { it.first.featureStructure.relevantConstraints.size }
                        .thenBy { it.second.level }
                        .thenBy { it.first.featureStructure.childrenCount }
                        .thenBy { it.first.feature.name }

                val comparatorAlt =
                    compareBy<Pair<ParentFeature, ChildFeature>> { it.second.featureStructure.isMandatory }
                        .thenBy { it.second.constraintSubtree }
                        .thenBy { it.second.childrenSubtree }
                        .thenByDescending { it.first.commonality }
                        .thenBy { abs(0.5 - it.second.commonality) }
                        .thenBy { !it.first.featureStructure.isMandatory }
                        .thenBy { it.first.featureStructure.relevantConstraints.size }
                        .thenBy { it.first.featureStructure.childrenCount }
                        .thenBy { it.second.level }
                        .thenBy { it.first.feature.name }

                val comparatorOr =
                    compareBy<Pair<ParentFeature, ChildFeature>> { it.second.featureStructure.isMandatory }
                        .thenBy { it.second.constraintSubtree }
                        .thenBy { it.second.childrenSubtree }
                        .thenByDescending { it.first.commonality }
                        .thenBy { abs((2.0/3.0) - it.second.commonality) }
                        .thenBy { !it.first.featureStructure.isMandatory }
                        .thenBy { it.first.featureStructure.relevantConstraints.size }
                        .thenBy { it.first.featureStructure.childrenCount }
                        .thenBy { it.second.level }
                        .thenBy { it.first.feature.name }

                var parentChildAndSortedSet =
                    Collections.synchronizedList(mutableListOf<Pair<ParentFeature, ChildFeature>>())
                var parentChildAltSortedSet =
                    Collections.synchronizedList(mutableListOf<Pair<ParentFeature, ChildFeature>>())
                var parentChildOrSortedSet =
                    Collections.synchronizedList(mutableListOf<Pair<ParentFeature, ChildFeature>>())

                val sbAnd = StringBuilder()
                sbAnd.append("FeatureName,Commonality,isOptional,Level,Children,NumberOfConstraints,ParentName,ParentCommonality,ParentIsOptional,isParentAnd,isParentOr,isParentAlt,ChildrenParent,ParentConstraints\n")

                val sbOr = StringBuilder()
                sbOr.append("FeatureName,Commonality,isOptional,Level,Children,NumberOfConstraints,ParentName,ParentCommonality,ParentIsOptional,isParentAnd,isParentOr,isParentAlt,ChildrenParent,ParentConstraints\n")

                val sbAlt = StringBuilder()
                sbAlt.append("FeatureName,Commonality,isOptional,Level,Children,NumberOfConstraints,ParentName,ParentCommonality,ParentIsOptional,isParentAnd,isParentOr,isParentAlt,ChildrenParent,ParentConstraints\n")

                val start3 = LocalDateTime.now()
                mapFiltered.toList().parallelStream().forEach {
                    val parentFeatureName = cnf.variables.getName(it.first)
                    val parentCommonality = it.second
                    val parentFeatureFromModel = model.getFeature(parentFeatureName)
                    val parentFeatureStructure = parentFeatureFromModel.structure
                    val childLevel = getLevelForSubTree(parentFeatureStructure) + 1
                    val parentFeature = ParentFeature(parentFeatureFromModel, parentCommonality, parentFeatureStructure)
                    val isParentAnd = parentFeature.featureStructure.isAnd
                    val isParentOr = parentFeature.featureStructure.isOr
                    val isParentAlt = parentFeature.featureStructure.isAlternative
                    if (isParentAnd) {
                        for (featureStructure in parentFeatureStructure.children) {
                            val feature = featureStructure.feature
                            val commonality = map[cnf.variables.getVariable(feature.name)]
                            if (commonality == parentCommonality){
                                continue
                            }
                            val childFeature = ChildFeature(
                                feature,
                                commonality!!,
                                featureStructure,
                                getChildrenCountForSubTree(featureStructure),
                                getRelevantConstraintsForSubTree(featureStructure),
                                childLevel
                            )
                            if ((commonality <= 0.5 && commonality < threshold) || (commonality > 0.5 && (1.0-commonality) < threshold)) {
                                continue
                            } else if (childrenMaxCount != -1.0 && childrenMaxCount >= 1.0 && childFeature.childrenSubtree > childrenMaxCount) {
                                continue
                            } else if (childrenMaxCount != -1.0 && childrenMaxCount < 1.0 && childFeature.childrenSubtree > (childrenMaxCount*allFeatures)) {
                                continue
                            } else if (constraintMaxCount != -1.0 && constraintMaxCount >= 1.0 && childFeature.constraintSubtree > constraintMaxCount) {
                                continue
                            } else if (constraintMaxCount != -1.0 && constraintMaxCount < 1.0 && childFeature.constraintSubtree > (constraintMaxCount*allConstraints)) {
                                continue
                            }
                            parentChildAndSortedSet.add(Pair(parentFeature, childFeature))
                        }
                    } else if (isParentAlt) {
                        for (featureStructure in parentFeatureStructure.children) {
                            val feature = featureStructure.feature
                            val commonality = map[cnf.variables.getVariable(feature.name)]
                            if (commonality == parentCommonality){
                                continue
                            }
                            val childFeature = ChildFeature(
                                feature,
                                commonality!!,
                                featureStructure,
                                getChildrenCountForSubTree(featureStructure),
                                getRelevantConstraintsForSubTree(featureStructure),
                                childLevel
                            )
                            if ((commonality <= 0.5 && commonality < threshold) || (commonality > 0.5 && (1.0-commonality) < threshold)) {
                                continue
                            } else if (childrenMaxCount != -1.0 && childrenMaxCount >= 1.0 && childFeature.childrenSubtree > childrenMaxCount) {
                                continue
                            } else if (childrenMaxCount != -1.0 && childrenMaxCount < 1.0 && childFeature.childrenSubtree > (childrenMaxCount*allFeatures)) {
                                continue
                            } else if (constraintMaxCount != -1.0 && constraintMaxCount >= 1.0 && childFeature.constraintSubtree > constraintMaxCount) {
                                continue
                            } else if (constraintMaxCount != -1.0 && constraintMaxCount < 1.0 && childFeature.constraintSubtree > (constraintMaxCount*allConstraints)) {
                                continue
                            }
                            parentChildAltSortedSet.add(Pair(parentFeature, childFeature))
                        }
                    } else if (isParentOr) {
                        for (featureStructure in parentFeatureStructure.children) {
                            val feature = featureStructure.feature
                            val commonality = map[cnf.variables.getVariable(feature.name)]
                            if (commonality == parentCommonality){
                                continue
                            }
                            val childFeature = ChildFeature(
                                feature,
                                commonality!!,
                                featureStructure,
                                getChildrenCountForSubTree(featureStructure),
                                getRelevantConstraintsForSubTree(featureStructure),
                                childLevel
                            )
                            if ((commonality <= 0.5 && commonality < threshold) || (commonality > 0.5 && (1.0-commonality) < threshold)) {
                                continue
                            } else if (childrenMaxCount != -1.0 && childrenMaxCount >= 1.0 && childFeature.childrenSubtree > childrenMaxCount) {
                                continue
                            } else if (childrenMaxCount != -1.0 && childrenMaxCount < 1.0 && childFeature.childrenSubtree > (childrenMaxCount*allFeatures)) {
                                continue
                            } else if (constraintMaxCount != -1.0 && constraintMaxCount >= 1.0 && childFeature.constraintSubtree > constraintMaxCount) {
                                continue
                            } else if (constraintMaxCount != -1.0 && constraintMaxCount < 1.0 && childFeature.constraintSubtree > (constraintMaxCount*allConstraints)) {
                                continue
                            }
                            parentChildOrSortedSet.add(Pair(parentFeature, childFeature))
                        }
                    }
                }

                println(
                    file.nameWithoutExtension + ": inital features " + Duration.between(
                        start3,
                        LocalDateTime.now()
                    ).toString()
                )

                val start4 = LocalDateTime.now()

                parentChildAndSortedSet = parentChildAndSortedSet.parallelStream().sorted(comparatorAnd).toList()
                parentChildAltSortedSet = parentChildAltSortedSet.parallelStream().sorted(comparatorAlt).toList()
                parentChildOrSortedSet = parentChildOrSortedSet.parallelStream().sorted(comparatorOr).toList()

                println(
                    file.nameWithoutExtension + ": inital features " + Duration.between(
                        start4,
                        LocalDateTime.now()
                    ).toString()
                )


                for (entry in parentChildAndSortedSet) {
                    sbAnd.append("${entry.second.feature.name},${entry.second.commonality},${!entry.second.featureStructure.isMandatory},${entry.second.level},${entry.second.childrenSubtree},${entry.second.constraintSubtree},${entry.first.feature.name},${entry.first.commonality},${!entry.first.featureStructure.isMandatory},${entry.first.featureStructure.isAnd},${entry.first.featureStructure.isOr},${entry.first.featureStructure.isAlternative},${entry.first.featureStructure.children.size},${entry.first.featureStructure.relevantConstraints.size}\n")
                }
                for (entry in parentChildAltSortedSet) {
                    sbAlt.append("${entry.second.feature.name},${entry.second.commonality},${!entry.second.featureStructure.isMandatory},${entry.second.level},${entry.second.childrenSubtree},${entry.second.constraintSubtree},${entry.first.feature.name},${entry.first.commonality},${!entry.first.featureStructure.isMandatory},${entry.first.featureStructure.isAnd},${entry.first.featureStructure.isOr},${entry.first.featureStructure.isAlternative},${entry.first.featureStructure.children.size},${entry.first.featureStructure.relevantConstraints.size}\n")
                }
                for (entry in parentChildOrSortedSet) {
                    sbOr.append("${entry.second.feature.name},${entry.second.commonality},${!entry.second.featureStructure.isMandatory},${entry.second.level},${entry.second.childrenSubtree},${entry.second.constraintSubtree},${entry.first.feature.name},${entry.first.commonality},${!entry.first.featureStructure.isMandatory},${entry.first.featureStructure.isAnd},${entry.first.featureStructure.isOr},${entry.first.featureStructure.isAlternative},${entry.first.featureStructure.children.size},${entry.first.featureStructure.relevantConstraints.size}\n")
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
        if (featureStructure.children.size > 0) {
            return featureStructure.children.size + featureStructure.children.map { child ->
                getChildrenCountForSubTree(
                    child
                )
            }.sum()
        } else {
            return 0
        }
    }

    private fun getRelevantConstraintsForSubTree(featureStructure: IFeatureStructure): Int {
        if (featureStructure.children.size > 0) {
            return featureStructure.relevantConstraints.size + featureStructure.children.map { child ->
                getRelevantConstraintsForSubTree(
                    child
                )
            }.sum()
        } else {
            return featureStructure.relevantConstraints.size
        }
    }

    private fun getLevelForSubTree(featureStructure: IFeatureStructure): Int {
        if (featureStructure.isRoot) {
            return 0
        } else {
            return 1 + getLevelForSubTree(featureStructure.parent)
        }
    }


}