package de.featureide.service.plugins

import de.featureide.service.Util.Configurator
import de.featureide.service.Util.Converter
import de.featureide.service.Util.Slicer
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
                val id = Converter.addFileForConvert()
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

        get("/convert/{id?}") {
            val id = call.parameters["id"]?.toInt() ?: return@get call.respondText(
                "Bad Request",
                status = HttpStatusCode.BadRequest
            )
            val file = convertedFileDataSource.getFile(id)
            val results = convertedFileDataSource.isReady(id)
            if(file == null)
            {
                call.respond(HttpStatusCode.BadRequest, "File does not exist!")
            }


            else if(results) {
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
                val id = Slicer.addFileForSlice()
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

        get("/slice/{id?}") {
            val id = call.parameters["id"]?.toInt() ?: return@get call.respondText(
                "Bad Request",
                status = HttpStatusCode.BadRequest
            )
            val file = slicedFileDataSource.getFile(id)
            val results = slicedFileDataSource.isReady(id)

            if(file == null) {
                call.respond(HttpStatusCode.BadRequest, "File does not exist!")
            }


            else if(results) {
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

        post("/configuration") {
            val file = call.receive<ConfigurationInput>()
            try {
                val id = Configurator.addFileForConfiguration()
                if(id == null){
                    throw Exception()
                }
                launch(Dispatchers.IO) {
                    Configurator.generate(file, id)
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

        get("/configuration/{id?}") {
            val id = call.parameters["id"]?.toInt() ?: return@get call.respondText(
                "Bad Request",
                status = HttpStatusCode.BadRequest
            )
            val file = configurationFileDataSource.getFile(id)
            val results = configurationFileDataSource.isReady(id)

            if(file == null) {
                call.respond(HttpStatusCode.BadRequest, "File does not exist!")
            } else if(results) {
                call.respond(HttpStatusCode.Accepted, "File is not ready yet!")
            } else {
                val outputFile = configurationFileDataSource.getFile(id)
                val configurationOutput = ConfigurationOutput(outputFile!!)
                launch(Dispatchers.IO) {
                    configurationFileDataSource.delete(id)
                }
                call.response.status(HttpStatusCode.OK)
                call.respond(configurationOutput)
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
