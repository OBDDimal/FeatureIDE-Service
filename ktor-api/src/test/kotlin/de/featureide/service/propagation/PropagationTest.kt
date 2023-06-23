package de.featureide.service.propagation

import de.featureide.service.models.PropagationInput
import de.featureide.service.models.PropagationOutput
import de.featureide.service.models.SliceInput
import de.featureide.service.models.SliceOutput
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import kotlinx.coroutines.delay
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PropagationTest {

    private val configPath = "application-test.conf"

    private val featureideFile = File("testmodels/FeatureIDE.xml")


    @Test
    fun propagationEmptyCall() {
        testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            environment {
                config = ApplicationConfig(configPath)
            }
            val response = client.post("/propagate") {
                contentType(ContentType.Application.Json)
                setBody(
                    PropagationInput(
                        featureideFile.name,
                        arrayOf(),
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

            val resultBody: PropagationOutput = result.body()

            assertEquals(
                arrayOf("Eclipse").joinToString(),
                resultBody.impliedSelection.joinToString()
            )
        }
    }

    @Test
    fun propagationTwoSelectionCall() {
        testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            environment {
                config = ApplicationConfig(configPath)
            }
            val response = client.post("/propagate") {
                contentType(ContentType.Application.Json)
                setBody(
                    PropagationInput(
                        featureideFile.name,
                        arrayOf("FeatureCpp"), // ,  "AspectJ"
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

            val resultBody: PropagationOutput = result.body()

            assertEquals(
                arrayOf("Eclipse", "CDT", "FeatureModeling", "FeatureIDE").joinToString(),
                resultBody.impliedSelection.joinToString()
            )
        }
    }

    @Test
    fun propagationSelectionCall() {
        testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            environment {
                config = ApplicationConfig(configPath)
            }
            val response = client.post("/propagate") {
                contentType(ContentType.Application.Json)
                setBody(
                    PropagationInput(
                        featureideFile.name,
                        arrayOf("FeatureCpp", "AspectJ"),
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

            val resultBody: PropagationOutput = result.body()

            assertEquals(
                arrayOf("Eclipse", "CDT", "AJDT", "FeatureModeling", "FeatureIDE").joinToString(),
                resultBody.impliedSelection.joinToString()
            )
        }
    }
}