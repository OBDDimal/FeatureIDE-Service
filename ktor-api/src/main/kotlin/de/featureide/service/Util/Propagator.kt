package de.featureide.service.Util

import de.featureide.service.data.propagationFileDataSource
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.PropagationInput
import de.ovgu.featureide.fm.core.analysis.cnf.LiteralSet
import de.ovgu.featureide.fm.core.analysis.cnf.analysis.CoreDeadAnalysis
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula
import de.ovgu.featureide.fm.core.analysis.cnf.generator.configuration.*
import de.ovgu.featureide.fm.core.configuration.Selection
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.job.LongRunningWrapper
import de.ovgu.featureide.fm.core.job.monitor.NullMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object Propagator {

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
                        manualLiterals.add(
                            cnf.getVariables().getVariable(feature, true)
                        )
                }

                val analysis = CoreDeadAnalysis(cnf)
                val intLiterals = IntArray(file.selection.size)
                for (i in intLiterals.indices) {
                    intLiterals[i] = manualLiterals.get(i)
                }
                analysis.assumptions = LiteralSet(*intLiterals)
                val impliedFeatures = analysis.execute(NullMonitor())

                val result = ArrayList<String>(impliedFeatures.size())
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