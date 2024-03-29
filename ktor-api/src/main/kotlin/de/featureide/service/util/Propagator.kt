package de.featureide.service.util

import de.featureide.service.data.propagationFileDataSource
import de.featureide.service.models.PropagationInput
import de.featureide.service.models.PropagationOutput
import de.ovgu.featureide.fm.core.analysis.cnf.CNF
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet
import de.ovgu.featureide.fm.core.analysis.cnf.analysis.CoreDeadAnalysis
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula
import de.ovgu.featureide.fm.core.configuration.Configuration
import de.ovgu.featureide.fm.core.configuration.ConfigurationPropagator
import de.ovgu.featureide.fm.core.configuration.SelectableFeature
import de.ovgu.featureide.fm.core.configuration.Selection
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.job.LongRunningWrapper
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

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file or directory.")
            .required()
        val selection by parser.option(
            ArgType.String,
            shortName = "s",
            description = "The names of the features that are selected separated by ','. For example: Antenna,AHEAD."
        )
        val deselection by parser.option(
            ArgType.String,
            shortName = "ds",
            description = "The names of the features that are deselected separated by ','. For example: Antenna,AHEAD."
        )

        parser.parse(args)

        val file = File(path)

        val output = "./files/output"
        Files.createDirectories(Paths.get(output))

        //Propagates the implied features
        if (!selection.isNullOrEmpty()) {
            if (file.isDirectory() || !file.exists()) exitProcess(0)
            try {
                val model = FeatureModelManager.load(Paths.get(file.path))

                val formula = FeatureModelFormula(model)

                val result = generateImpliedFeatures(selection!!.split(",").toTypedArray(), arrayOf(""), formula)

                val localFile = File("${output}/${file.nameWithoutExtension}_ImpliedFeatures.txt")
                localFile.writeText(result.selectedFeatureNames.joinToString())
                localFile.writeText(result.unselectedFeatureNames.joinToString())
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }

            exitProcess(0)
        }
    }

    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    suspend fun propagate(file: PropagationInput, id: Int): PropagationOutput {
        val filePath = "files"
        withContext(Dispatchers.IO) {
            Files.createDirectories(Paths.get(filePath))
        }
        val directory = File(filePath)

        try {

            directory.listFiles()?.forEach { it.delete() }

            val localFile = File("$filePath/${file.name}")
            localFile.writeText(String(file.content))

            val model = FeatureModelManager.load(Paths.get(localFile.path))

            val formula = FeatureModelFormula(model)

            val isValid = isValidMethod(file.selection, file.deselection, formula)

            val configuration = generateImpliedFeatures(file.selection, file.deselection, formula)

            val openClauses = generateOpenClauses(selection = file.selection, deselection = file.deselection, configuration, formula)

            val openParents = ArrayList<String>()

            val openChildren = ArrayList<String>()

            for (feature in openClauses){
                if (feature.recommended == Selection.SELECTED){
                    openChildren.add(feature.name)
                } else {
                    openParents.add(feature.name)
                }
            }

            propagationFileDataSource.update(
                id,
                name = file.name,
                content = String(file.content),
                selection = file.selection,
                impliedSelection = configuration.selectedFeatureNames.toTypedArray(),
                deselection = file.deselection,
                impliedDeselection = configuration.unselectedFeatureNames.toTypedArray(),
                openParents = openParents.toTypedArray(),
                openChildren = openChildren.toTypedArray(),
                valid = isValid
            )
            localFile.delete()
            return PropagationOutput(name = file.name, selection = file.selection, deselection = file.deselection,
                impliedSelection = configuration.selectedFeatureNames.toTypedArray(), impliedDeselection = configuration.unselectedFeatureNames.toTypedArray(),
                openParents = openParents.toTypedArray(), openChildren = openChildren.toTypedArray(), valid = isValid,
                content = file.content)
        } catch (e: Exception) {
            propagationFileDataSource.update(
                id,
                name = "No Propagation",
                content = e.stackTraceToString(),
                selection = file.selection,
                impliedSelection = arrayOf(""),
                deselection = file.deselection,
                impliedDeselection = arrayOf(""),
                openParents = arrayOf(""),
                openChildren = arrayOf(""),
                valid = false
            )
        }
        return PropagationOutput(file.name, file.selection, arrayOf(""), file.deselection, arrayOf(""), arrayOf(""), arrayOf(""),false, file.content)
    }

    /**
     * Generate implied features from a cnf with a selection of features
     *
     * @param cnf CNF of the feature model to generate the implied features
     * @param selection The selection of features to get the implied features
     * @return List<String> The samples as a list of literalset
     */
    private fun generateImpliedFeatures(selection: Array<String>, deselection: Array<String>, formula: FeatureModelFormula): Configuration {
        val manualLiterals = ArrayList<Int>()
        for (feature in selection) {
            val featureInt = formula.cnf.getVariables().getVariable(feature, true)
            if (featureInt != 0)
                manualLiterals.add(
                    featureInt
                )
        }
        for (feature in deselection) {
            val featureInt = formula.cnf.getVariables().getVariable(feature, false)
            if (featureInt != 0)
                manualLiterals.add(
                    featureInt
                )
        }

        val analysis = CoreDeadAnalysis(formula.cnf)
        val intLiterals = IntArray(manualLiterals.size)
        for (i in intLiterals.indices) {
            intLiterals[i] = manualLiterals.get(i)
        }
        analysis.assumptions = LiteralSet(*intLiterals)
        val impliedFeatures = LongRunningWrapper.runMethod(analysis)

        return Configuration.fromLiteralSet(formula, impliedFeatures)
    }

    private fun generateOpenClauses(selection: Array<String>, deselection: Array<String>, configuration: Configuration, formula: FeatureModelFormula): Collection<SelectableFeature> {
        val fullConfiguration = Configuration(configuration, formula)
        for (feature in selection) {
            fullConfiguration.setManual(feature, Selection.SELECTED)
        }
        for (feature in deselection) {
            fullConfiguration.setManual(feature, Selection.UNSELECTED)
        }

        val dp = ConfigurationPropagator(formula, fullConfiguration)

        return LongRunningWrapper.runMethod(dp.FindOpenClauses())
    }

    private fun isValidMethod(selection: Array<String>, deselection: Array<String>,  formula: FeatureModelFormula): Boolean{
        val configuration = Configuration(formula)
        for (feature in selection) {
            configuration.setManual(feature, Selection.SELECTED)
        }
        for (feature in deselection) {
            configuration.setManual(feature, Selection.UNSELECTED)
        }

        val dp = ConfigurationPropagator(formula, configuration)

        return LongRunningWrapper.runMethod(dp.isValid)
    }
}