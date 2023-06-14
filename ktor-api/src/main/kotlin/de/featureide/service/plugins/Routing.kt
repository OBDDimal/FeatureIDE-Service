package de.featureide.service.plugins

import de.featureide.service.Converter
import de.featureide.service.InputController
import de.featureide.service.Slicer
import de.featureide.service.data.*
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.*
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

        post("/convert") {
            val file = call.receive<ConvertInput>()
            try {
                val id = InputController.addFileForConvert(file)
                if(id == null){
                    throw Exception()
                }
                launch(Dispatchers.IO) {
                    Converter.convert(file, id)
                }
                call.response.created(id)
                call.respondText("Request accepted!")
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
            } catch (e: Exception){
                call.respond(
                    HttpStatusCode.BadRequest
                )
            }
        }

        get("convert/{id?}") {
            val id = call.parameters["id"]?.toInt() ?: return@get call.respondText(
                "Bad Request",
                status = HttpStatusCode.BadRequest
            )
            if(convertedFileDataSource.getFile(id) == null)
            {
                call.respond(HttpStatusCode.BadRequest, "File does not exist!")
            }

            val results = convertedFileDataSource.isReady(id)
            if(results) {
                call.respond(HttpStatusCode.Accepted, "File is not ready yet!")
            } else {
                val outputFile = convertedFileDataSource.getFile(id)
                val convertOutput = ConvertOutput(outputFile!!)
                launch(Dispatchers.IO) {
                    convertedFileDataSource.delete(id)
                }
                call.response.status(HttpStatusCode.OK)
                call.respond(convertOutput)
            }
        }

        post("/slice") {
            val file = call.receive<SliceInput>()
            try {
                val id = InputController.addFileForSlice(file)
                if(id == null){
                    throw Exception()
                }
                launch(Dispatchers.IO) {
                    Slicer.slice(file, id)
                }
                call.response.created(id)
                call.respondText("Request accepted!")
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
            } catch (e: Exception){
                call.respond(
                    HttpStatusCode.BadRequest
                )
            }
        }

        get("slice/{id?}") {
            val id = call.parameters["id"]?.toInt() ?: return@get call.respondText(
                "Bad Request",
                status = HttpStatusCode.BadRequest
            )
            if(slicedFileDataSource.getFile(id) == null)
            {
                call.respond(HttpStatusCode.BadRequest, "File does not exist!")
            }

            val results = slicedFileDataSource.isReady(id)
            if(results) {
                call.respond(HttpStatusCode.Accepted, "File is not ready yet!")
            } else {
                val outputFile = slicedFileDataSource.getFile(id)
                val sliceOutput = SliceOutput(outputFile!!)
                launch(Dispatchers.IO) {
                    slicedFileDataSource.delete(id)
                }
                call.response.status(HttpStatusCode.OK)
                call.respond(sliceOutput)
            }
        }
    }
}

private fun ApplicationResponse.created(id: Int) {
    call.response.status(HttpStatusCode.Created)
    call.response.header("Location", "${call.request.uri}/$id")
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
