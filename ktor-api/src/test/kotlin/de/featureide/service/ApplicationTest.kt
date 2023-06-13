package de.featureide.service

import de.featureide.service.data.DatabaseFactory
import de.featureide.service.data.requestDataSource
import de.featureide.service.data.requestNumberDataSource
import de.featureide.service.data.uploadedFileDataSource
import de.featureide.service.models.InputFile
import de.featureide.service.models.OutputFile
import de.featureide.service.models.SliceOutput
import de.featureide.service.models.Status
import de.featureide.service.plugins.configureRouting
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import java.io.File

class ApplicationTest {

    private val configPath = "application-test.conf"

    private val featureideFile = File("testmodels/FeatureIDE.xml")
    private val featureideSlicedFile = File("testmodels/FeatureIDEsliced.xml")

    @Test
    fun testRoot() = testApplication {
        environment {
            config = ApplicationConfig(configPath)
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
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
                            featureideFile.name,
                            arrayOf("uvl", "featureIde", "sxfm", "dimacs", "notValidFormat"),
                            featureideFile.readBytes(),
                        )
                    )
                )
            }

            assertEquals(HttpStatusCode.OK, response.status)
            var status = response.body<Status>()
            assertEquals(false, status.finished)

            status = client.get("/check/${status.requestNumber}").body()
            while (!status.finished) {
                delay(5000L)
                status = client.get("/check/${status.requestNumber}").body()
            }

            val results: List<OutputFile> = client.get("/result/${status.requestNumber}").body()

            println(results.count())

            for (result in results) {
                println(String(result.content))
            }

            val success = results.filter {
                it.success
            }
            val failed = results.filter {
                !it.success
            }

            assertEquals(4, success.count())
            assertEquals(1, failed.count())
        }
    }

    @Test
    fun sliceCall() {
        testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            environment {
                config = ApplicationConfig(configPath)
            }
            val response = client.post("/slice") {
                contentType(ContentType.Application.Json)
                setBody(
                    InputFile(
                        featureideFile.name,
                        arrayOf("FeatureHouse", "FeatureCpp"),
                        featureideFile.readBytes(),
                    )
                )
            }

            assertEquals(HttpStatusCode.Created, response.status)
            var newLocation = response.headers[HttpHeaders.Location]

            assertTrue(newLocation != null)

            var result = client.get(newLocation)

            while (result.status != HttpStatusCode.OK) {
                delay(5000L)
                result = client.get(newLocation)
            }

            val resultBody: SliceOutput = client.get(newLocation).body()

            assertEquals(
                String(resultBody.content).replace("\\s".toRegex(), ""),
                String(featureideSlicedFile.readBytes()).replace("\\s".toRegex(), "")
            )
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

    /*@ExperimentalCoroutinesApi
    @Test
    fun addFiles() = runTest {
        val config = ApplicationConfig(configPath)
        DatabaseFactory.init(config)

        val number = InputController.addFiles(
            listOf(
                InputFile(
                    featureideFile.name,
                    arrayOf("featureIde", "sxfm", "uvl"),
                    featureideFile.readBytes(),
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
    }*/
}

