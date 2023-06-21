package de.featureide.service.slice

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

class SliceTest {

    private val configPath = "application-test.conf"

    private val featureideFile = File("testmodels/FeatureIDE.xml")
    private val featureideSlicedFile = File("testmodels/FeatureIDEsliced.xml")

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
}