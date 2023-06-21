package de.featureide.service.configuration

import de.featureide.service.models.ConfigurationInput
import de.featureide.service.models.ConfigurationOutput
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

class ConfigurationTest {

    private val configPath = "application-test.conf"

    private val featureideFile = File("testmodels/FeatureIDE.xml")
    private val featureideConfigurationFile = File("testmodels/FeatureIDE_yasa_50_t2_10.csv")

    @Test
    fun configurationCall() {
        testApplication {
            val client = createClient {
                install(ContentNegotiation) {
                    json()
                }
            }
            environment {
                config = ApplicationConfig(configPath)
            }
            val response = client.post("/configuration") {
                contentType(ContentType.Application.Json)
                setBody(
                    ConfigurationInput(
                        name = featureideFile.name,
                        algorithm = "yasa_50",
                        content = featureideFile.readBytes(),
                        t = 2,
                        limit = 10
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

            val resultBody: ConfigurationOutput = result.body()

            println(String(resultBody.content))

            assertEquals(String(resultBody.content).replace("\\s".toRegex(), ""),
                String(featureideConfigurationFile.readBytes()).replace("\\s".toRegex(), "")
            )
        }
    }
}