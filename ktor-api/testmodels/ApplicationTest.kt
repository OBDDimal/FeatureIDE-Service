package de.featureide.service

import de.featureide.service.models.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import kotlinx.coroutines.delay
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
                        ConvertInput(
                            featureideFile.name,
                            arrayOf("uvl", "featureIde", "sxfm", "dimacs"),
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

            val resultBody: ConvertOutput = result.body()

            assertEquals(
                String(resultBody.content[1]).replace("\\s".toRegex(), ""),
                String(featureideFile.readBytes()).replace("\\s".toRegex(), "")
            )

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
                    SliceInput(
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

            val resultBody: SliceOutput = result.body()

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

