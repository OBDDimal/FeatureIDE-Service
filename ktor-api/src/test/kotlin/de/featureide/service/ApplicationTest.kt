package de.featureide.service


import de.featureide.service.data.DatabaseFactory
import de.featureide.service.data.requestDataSource
import de.featureide.service.data.requestNumberDataSource
import de.featureide.service.data.uploadedFileDataSource
import de.featureide.service.models.InputFile
import de.featureide.service.models.OutputFile
import de.featureide.service.models.Status
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals


class ApplicationTest {

    private val configPath = "test.conf"
    private val dimacsFile = File("files/input/berkeleydb.dimacs")
    private val sxfmFile = File("files/input/Dell-sxfm.xml")
    @Test
    fun testRoot() = testApplication {
        environment {
            config = ApplicationConfig(configPath)
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World", bodyAsText())
        }
    }

    @Test
    fun fullCall() {
        testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            environment {
                config = ApplicationConfig(configPath)
            }
            val response = client.post("/convert") {
                contentType(ContentType.Application.Json)
                setBody(
                    listOf(
                        InputFile(
                            dimacsFile.name,
                            arrayOf("uvl", "featureIde", "sxfm", "dimacs", "notValidFormat"),
                            dimacsFile.readBytes(),
                        ),
                        InputFile(
                            sxfmFile.name,
                            arrayOf("uvl", "dimacs"),
                            sxfmFile.readBytes(),
                        ),
                    ))
            }

            assertEquals(HttpStatusCode.OK, response.status)
            var status = response.body<Status>()
            assertEquals(false, status.finished)

            status = client.get("/check/${status.requestNumber}").body()
            while (!status.finished) {
                delay(5000L)
                status = client.get("/check/${status.requestNumber}").body()
            }

            val result: List<OutputFile> = client.get("/result/${status.requestNumber}").body()

            val success = result.filter {
                it.success
            }
            val failed = result.filter {
                !it.success
            }

            assertEquals(6, success.count())
            assertEquals(1, failed.count())
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun addFiles() = runTest {
        val config = ApplicationConfig(configPath)
        DatabaseFactory.init(config)

        val number = InputController.addFiles(
            listOf(
                InputFile(
                    dimacsFile.name,
                    arrayOf("featureIde", "sxfm", "uvl"),
                    dimacsFile.readBytes(),
                )
            )
        )

        val requests = requestDataSource.requests(number)
        assertEquals(3, requests.count())
    }

    @ExperimentalCoroutinesApi
    @Test
    fun convertRemainingFiles() = runTest {
        addFiles()
        val requestNumbers = requestNumberDataSource.getAll()

        Converter.convertRemainingFiles()
        for (number in requestNumbers) {
            assertEquals(0, requestDataSource.requests(number.value).count())
            assertEquals(0, uploadedFileDataSource.filesByRequestNumber(number.value).count())
        }
    }

    @Test
    fun noRequest() = testApplication {
        environment {
            config = ApplicationConfig(configPath)
        }
        client.get("/check/-1").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }
}