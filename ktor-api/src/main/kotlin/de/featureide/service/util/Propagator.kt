package de.featureide.service.util

import de.featureide.service.data.propagationFileDataSource
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.PropagationInput
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet
import de.ovgu.featureide.fm.core.analysis.cnf.analysis.CoreDeadAnalysis
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula
import de.ovgu.featureide.fm.core.base.IFeature
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat
import de.ovgu.featureide.fm.core.job.monitor.NullMonitor
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

object Propagator {

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val parser = ArgParser("featureide-cli")

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file or directory.").required()
        val selection by parser.option(ArgType.String, shortName = "s", description = "The names of the features that are selected separated by ','. For example: Antenna,AHEAD.")

        parser.parse(args)

        val file = File(path)

        val output = "./files/output"

        File(output).deleteRecursively()

        Files.createDirectories(Paths.get(output))

        //Propagates the implied features
        if (!selection.isNullOrEmpty()){
            if(file.isDirectory() || !file.exists()) exitProcess(0)
            val model = FeatureModelManager.load(Paths.get(file.path))

            val cnf = FeatureModelFormula(model).cnf

            val manualLiterals = ArrayList<Int>()
            for (feature in selection!!.split(",")) {
                val featureInt = cnf.getVariables().getVariable(feature, true)
                if(featureInt != 0)
                    manualLiterals.add(
                        featureInt
                    )
            }
            val result = ArrayList<String>()

            val analysis = CoreDeadAnalysis(cnf)
            val intLiterals = IntArray(selection!!.split(",").size)
            for (i in intLiterals.indices) {
                intLiterals[i] = manualLiterals.get(i)
            }
            analysis.assumptions = LiteralSet(*intLiterals)
            val impliedFeatures = analysis.execute(NullMonitor())
            for (feature in impliedFeatures.literals){
                result.add(cnf.variables.getName(feature))
            }

            val localFile = File("${output}/${file.nameWithoutExtension}_ImpliedFeatures.txt")
            localFile.writeText(result.joinToString())
            exitProcess(0)
        }
    }


    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    @Throws(
        CouldNotCreateFileException::class,
        CouldNotCreateRequestException::class
    )
    suspend fun addFileForPropagation(): Int? {

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

                val manualLiterals = ArrayList<Int>()
                for (feature in file.selection) {
                    val featureInt = cnf.getVariables().getVariable(feature, true)
                    if(featureInt != 0)
                        manualLiterals.add(
                            featureInt
                        )
                }
                val result = ArrayList<String>()

                val analysis = CoreDeadAnalysis(cnf)
                val intLiterals = IntArray(file.selection.size)
                for (i in intLiterals.indices) {
                    intLiterals[i] = manualLiterals.get(i)
                }
                analysis.assumptions = LiteralSet(*intLiterals)
                val impliedFeatures = analysis.execute(NullMonitor())
                for (feature in impliedFeatures.literals){
                    result.add(cnf.variables.getName(feature))
                }

                propagationFileDataSource.update(
                    id,
                    name = file.name,
                    content = String(file.content),
                    selection = file.selection,
                    impliedSelection = result.toTypedArray()
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