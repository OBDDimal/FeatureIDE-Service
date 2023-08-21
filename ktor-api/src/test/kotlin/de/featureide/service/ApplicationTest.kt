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
    fun wrongGetRequest() = testApplication {
        environment {
            config = ApplicationConfig(configPath)
        }
        client.get("/hello").apply {
            assertEquals(HttpStatusCode.NotFound, status)
        }
    }

    @Test
    fun wrongPostRequest() = testApplication {
        environment {
            config = ApplicationConfig(configPath)
        }
        client.post("/hello").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun fileNotFoundRequest() = testApplication {
        environment {
            config = ApplicationConfig(configPath)
        }
        client.get("/convert/-1").apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

}

