package de.featureide.service.convert

import de.featureide.service.models.ConvertInput
import de.featureide.service.models.ConvertOutput
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

class ConvertTest {

    private val configPath = "application-test.conf"

    private val featureideFile = File("testmodels/FeatureIDE.xml")
    private val featureideUvlFile = File("testmodels/FeatureIDE_UVL.uvl")
    private val featureideDimacsFile = File("testmodels/FeatureIDE_DIMACS.dimacs")
    private val featureideSXFMFile = File("testmodels/FeatureIDE_SXFM.xml")
    private val featureideFideFile = File("testmodels/FeatureIDE_FeatureIDE.xml")

    @Test
    fun convertInAll() {
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
                String(featureideUvlFile.readBytes()).replace("\\s".toRegex(), ""),
                String(resultBody.content[0]).replace("\\s".toRegex(), "")
            )

            assertEquals(
                String(featureideFideFile.readBytes()).replace("\\s".toRegex(), ""),
                String(resultBody.content[1]).replace("\\s".toRegex(), "")
            )

            assertEquals(
                String(featureideSXFMFile.readBytes()).replace("\\s".toRegex(), ""),
                String(resultBody.content[2]).replace("\\s".toRegex(), "")
            )

            assertEquals(
                String(featureideDimacsFile.readBytes()).replace("\\s".toRegex(), ""),
                String(resultBody.content[3]).replace("\\s".toRegex(), "")
            )

        }
    }
}