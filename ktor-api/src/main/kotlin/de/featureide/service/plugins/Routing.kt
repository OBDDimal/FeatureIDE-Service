package de.featureide.service.plugins

import de.featureide.service.Converter
import de.featureide.service.InputController
import de.featureide.service.Slicer
import de.featureide.service.data.requestDataSource
import de.featureide.service.data.requestNumberDataSource
import de.featureide.service.data.resultFileDataSource
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.InputFile
import de.featureide.service.models.OutputFile
import de.featureide.service.models.Status
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Application.configureRouting(config: ApplicationConfig) {
    install(StatusPages) {
        exception<AuthenticationException> { call, _ ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { call, _ ->
            call.respond(HttpStatusCode.Forbidden)
        }
    }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/check/{id?}") {
            val id = call.parameters["id"]?.toInt() ?: return@get call.respondText(
                "Bad Request",
                status = HttpStatusCode.BadRequest
            )
            val requests = requestDataSource.requestCount(id)
            val results = resultFileDataSource.fileCount(id)

            if (requests == 0 && results == 0) {
                call.respond(HttpStatusCode.BadRequest, "No such request exists.")
            }

            if (requests > 0) {
                call.respond(
                    Status(
                        requestNumber = id,
                        finished = false,
                        amountToProcess = requests,
                        resourceLocation = "",
                    )
                )
            }

            if (results > 0 && requests == 0) {
                call.respond(
                    Status(
                        requestNumber = id,
                        finished = true,
                        amountToProcess = requests,
                        resourceLocation = "result/$id",
                    )
                )
            }
        }

        get("/result/{id?}") {
            val id = call.parameters["id"]?.toInt() ?: return@get call.respondText(
                "Bad Request",
                status = HttpStatusCode.BadRequest
            )
            val results = resultFileDataSource.filesByRequestNumber(id)
            val outputFiles = mutableListOf<OutputFile>()
            for (result in results) {
                outputFiles.add(
                    OutputFile(
                        name = result.name,
                        originalName = result.originalName,
                        type = result.type,
                        success = result.success,
                        content = result.content.toByteArray(),
                    )
                )
            }
            launch(Dispatchers.IO) {
                resultFileDataSource.deleteByRequestNumber(id)
                requestNumberDataSource.delete(id)
            }
            call.respond(outputFiles)
        }

        post("/convert") {
            val files = call.receive<List<InputFile>>()
            try {
                val requestNumber = InputController.addFiles(files)
                launch(Dispatchers.IO) {
                    Converter.convertFiles(requestNumber)
                }
                call.respond(
                    Status(
                        requestNumber = requestNumber,
                        finished = false,
                        amountToProcess = requestDataSource.requestCount(requestNumber),
                        resourceLocation = "check/$requestNumber",
                    )
                )
            } catch (e: CouldNotCreateRequestException) {
                if (e.requestNumber < 0) {
                    call.respond(HttpStatusCode.InternalServerError, "Could not queue the request.")
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "A file from request ${e.requestNumber} could not be added to the database."
                    )
                }
            } catch (e: CouldNotCreateFileException) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "A request from request ${e.requestNumber} could not be added to the database."
                )
            }
        }

        post("/slice") {
            val files = call.receive<List<InputFile>>()
            try {
                val requestNumber = InputController.addFilesForSlice(files)
                launch(Dispatchers.IO) {
                    Slicer.sliceFiles(requestNumber)
                }
                call.respond(
                    Status(
                        requestNumber = requestNumber,
                        finished = false,
                        amountToProcess = requestDataSource.requestCount(requestNumber),
                        resourceLocation = "check/$requestNumber",
                    )
                )
            } catch (e: CouldNotCreateRequestException) {
                if (e.requestNumber < 0) {
                    call.respond(HttpStatusCode.InternalServerError, "Could not queue the request.")
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "A file from request ${e.requestNumber} could not be added to the database."
                    )
                }
            } catch (e: CouldNotCreateFileException) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    "A request from request ${e.requestNumber} could not be added to the database."
                )
            }
        }
    }
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
