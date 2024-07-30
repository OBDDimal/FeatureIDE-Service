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
        File(output).listFiles()?.forEach { it.delete() }
        File(outputCSV).listFiles()?.forEach { it.delete() }

        if (file.isDirectory()) {

            val inputFiles = file.listFiles()
            for (fileFromList in inputFiles!!) {
                if (fileFromList.isDirectory || fileFromList.extension != "xml") {
                    continue
                }
                commonalityFromFile(fileFromList, output, outputCSV)
            }
        } else if (file.exists()) {
            commonalityFromFile(file, output, outputCSV)
        }

    }

    fun commonalityFromFile(file: File, output: String, outputCSV: String){

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

            val rt = Runtime.getRuntime()

            var csvPath = "${outputCSV}/${file.nameWithoutExtension}"

            val p = rt.exec(arrayOf(".\\ddnnife\\bin\\ddnnife.exe", dimacsPath, "-c", csvPath))
            p.waitFor(30, TimeUnit.SECONDS)
            p.destroy()
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


    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }
}