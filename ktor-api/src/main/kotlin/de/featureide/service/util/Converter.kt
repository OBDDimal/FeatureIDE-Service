package de.featureide.service.util


import de.featureide.service.data.*
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.exceptions.FileFormatNotFoundException
import de.featureide.service.exceptions.FormatNotFoundException
import de.featureide.service.models.ConvertInput
import de.ovgu.featureide.fm.core.base.IFeatureModel
import de.ovgu.featureide.fm.core.init.FMCoreLibrary
import de.ovgu.featureide.fm.core.init.LibraryManager
import de.ovgu.featureide.fm.core.io.IPersistentFormat
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager
import de.ovgu.featureide.fm.core.io.sxfm.SXFMFormat
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.lang.NullPointerException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.system.exitProcess


object Converter {

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val parser = ArgParser("featureide-cli")

        val path by parser.option(ArgType.String, shortName = "p", description = "Input path for file or directory.")
            .required()
        val check by parser.option(
            ArgType.String,
            shortName = "c",
            description = "Input path for the second file that should be checked with the first one."
        )
        val all by parser.option(
            ArgType.Boolean,
            shortName = "a",
            description = "Parsers all files from path into all formats."
        ).default(false)
        val dimacs by parser.option(
            ArgType.Boolean,
            shortName = "d",
            description = "Parses all files from path into dimacs files."
        ).default(false)
        val uvl by parser.option(
            ArgType.Boolean,
            shortName = "u",
            description = "Parses all files from path into uvl files."
        ).default(false)
        val sxfm by parser.option(
            ArgType.Boolean,
            shortName = "sf",
            description = "Parses all files from path into sxfm(xml) files."
        ).default(false)
        val featureIde by parser.option(
            ArgType.Boolean,
            shortName = "fi",
            description = "Parses all files from path into featureIde(xml) files."
        ).default(false)

        parser.parse(args)
        val file = File(path)
        val output = "./files/output"

        Files.createDirectories(Paths.get(output))

        //checks if two featureModels are the same
        if (!check.isNullOrEmpty()) {
            val file2 = File(check)
            if (file.isDirectory() || !file.exists() || file2.isDirectory() || !file2.exists()) exitProcess(0)
            val model = FeatureModelManager.load(Paths.get(file.path))
            val model2 = FeatureModelManager.load(Paths.get(file2.path))

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

        // If the file is a directory all files in the directory
        if (file.isDirectory) {
            val inputFiles = file.listFiles()
            for (fileFromList in inputFiles!!) {
                if (fileFromList.isDirectory) {
                    continue
                }
                try {
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
                } catch (e: Exception) {
                    println(e.stackTraceToString())
                }

            }
        } else if (file.exists()) {
            try {
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
            } catch (e: Exception) {
                println(e.stackTraceToString())
            }
        }
    }

    init {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance())
    }

    private val FormatMap = mapOf(
        Pair(FormatType.DIMACS, "DIMACS"),
        Pair(FormatType.UVL, "UVL"),
        Pair(FormatType.FEATURE_IDE, "FeatureIDE"),
        Pair(FormatType.SXFM, "SXFM"),
    )


    suspend fun convert(file: ConvertInput, id: Int) {

        withContext(Dispatchers.IO) {

            val filePath = "files"
            Files.createDirectories(Paths.get(filePath))
            val directory = File(filePath)

            try {

                directory.listFiles().forEach { it.delete() }

                val localFile = File("$filePath/${file.name}")
                localFile.writeText(String(file.content))

                val model = FeatureModelManager.load(Paths.get(localFile.path))

                val contents = arrayListOf<String>()

                val names = arrayListOf<String>()

                for (type in file.typeOutput) {

                    val format: IPersistentFormat<IFeatureModel> = when (type) {
                        "dimacs" -> DIMACSFormat()
                        "uvl" -> UVLFeatureModelFormat()
                        "featureIde" -> XmlFeatureModelFormat()
                        "sxfm" -> SXFMFormat()
                        else -> throw FormatNotFoundException()
                    }

                    val newName =
                        "${localFile.nameWithoutExtension}_${format.name}.${format.suffix}"
                    val pathOutputFile = "$filePath/$newName"

                    saveFeatureModel(
                        model,
                        pathOutputFile,
                        format,
                    )

                    val result = File(pathOutputFile).absoluteFile

                    names.add(newName)
                    contents.add(result.readText())

                    result.delete()
                }
                convertedFileDataSource.update(
                    id = id,
                    name = names.toTypedArray(),
                    content = contents.toTypedArray(),
                    typeOutput = file.typeOutput
                )
                localFile.delete()

            } catch (e: FormatNotFoundException) {
                convertedFileDataSource.update(
                    id = id,
                    name = arrayOf("Not converted"),
                    content = arrayOf("Could not find the requested format."),
                    typeOutput = file.typeOutput
                )
            } catch (e: FileFormatNotFoundException) {
                convertedFileDataSource.update(
                    id = id,
                    name = arrayOf("Not converted"),
                    content = arrayOf("Could not determine the format of the file."),
                    typeOutput = file.typeOutput
                )
            } catch (e: Exception) {
                convertedFileDataSource.update(
                    id = id,
                    name = arrayOf("Not converted"),
                    content = arrayOf("An unknown error occurred, could not convert file."),
                    typeOutput = file.typeOutput
                )
            }
        }
    }

    private fun saveFeatureModel(model: IFeatureModel?, savePath: String, format: IPersistentFormat<IFeatureModel>?) {
        FeatureModelManager.save(model, Paths.get(savePath), format)
    }

    private fun getFormatType(file: File): Int? {
        return when (file.extension) {
            "dimacs" -> FormatType.DIMACS
            "uvl" -> FormatType.UVL
            "xml" -> {
                var result: Int? = null
                file.bufferedReader().use {
                    for (line in it.lines()) {
                        with(line) {
                            when {
                                contains("<featureModel>") -> result = FormatType.FEATURE_IDE
                                contains("<feature_model") -> result = FormatType.SXFM
                            }
                        }
                        if (result != null) {
                            break
                        }
                    }
                }
                result
            }

            else -> null
        }
    }

    object FormatType {
        const val DIMACS = 0
        const val UVL = 1
        const val FEATURE_IDE = 2
        const val SXFM = 3
    }
}



