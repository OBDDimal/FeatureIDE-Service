package de.featureide.service.plugins

import de.featureide.service.data.*
import de.featureide.service.exceptions.CouldNotCreateFileException
import de.featureide.service.exceptions.CouldNotCreateRequestException
import de.featureide.service.models.*
import de.featureide.service.util.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun Application.configureRouting() {
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

        post("/{action}") {
            try {
                var id: Int = -1
                val action = Action.valueOf(call.parameters["action"]!!.uppercase())
                val result: Any
                when (action) {
                    Action.CONVERT -> {
                        val file = call.receive<ConvertInput>()
                        id = addFile(action)
                        result = Converter.convert(file, id)
                    }

                    Action.SLICE -> {
                        val file = call.receive<SliceInput>()
                        id = addFile(action)
                        result = Slicer.slice(file, id)
                    }

                    Action.CONFIGURATION -> {
                        val file = call.receive<ConfigurationInput>()
                        id = addFile(action)
                        result = Configurator.generate(file, id)
                    }

                    Action.PROPAGATION -> {
                        val file = call.receive<PropagationInput>()
                        id = addFile(action)
                        result = Propagator.propagate(file, id)
                    }
                }
                if (id != -1) {
                    call.response.created(id)
                    call.respond(result)
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest
                    )
                }

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
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest
                )
            }
        }

        get("/{action}/{id}") {
            val id = call.parameters["id"]?.toInt() ?: return@get call.respondText(
                "Bad Request",
                status = HttpStatusCode.BadRequest
            )
            val action = Action.valueOf(call.parameters["action"]!!.uppercase())
            when (action) {
                Action.CONVERT -> {
                    val file = convertedFileDataSource.getFile(id)
                    val results = convertedFileDataSource.isReady(id)
                    if (file == null) {
                        call.respond(HttpStatusCode.BadRequest, "File does not exist!")
                    } else if (results) {
                        call.respond(HttpStatusCode.Accepted, "File is not ready yet!")
                    } else {
                        val outputFile = convertedFileDataSource.getFile(id)
                        val convertOutput = ConvertOutput(outputFile!!)
                        call.response.status(HttpStatusCode.OK)
                        call.respond(convertOutput)
                    }
                }

                Action.SLICE -> {
                    val file = slicedFileDataSource.getFile(id)
                    val results = slicedFileDataSource.isReady(id)

                    if (file == null) {
                        call.respond(HttpStatusCode.BadRequest, "File does not exist!")
                    } else if (results) {
                        call.respond(HttpStatusCode.Accepted, "File is not ready yet!")
                    } else {
                        val outputFile = slicedFileDataSource.getFile(id)
                        val sliceOutput = SliceOutput(outputFile!!)
                        call.response.status(HttpStatusCode.OK)
                        call.respond(sliceOutput)
                    }
                }

                Action.CONFIGURATION -> {
                    val file = configurationFileDataSource.getFile(id)
                    val results = configurationFileDataSource.isReady(id)

                    if (file == null) {
                        call.respond(HttpStatusCode.BadRequest, "File does not exist!")
                    } else if (results) {
                        call.respond(HttpStatusCode.Accepted, "File is not ready yet!")
                    } else {
                        val outputFile = configurationFileDataSource.getFile(id)
                        val configurationOutput = ConfigurationOutput(outputFile!!)
                        call.response.status(HttpStatusCode.OK)
                        call.respond(configurationOutput)
                    }
                }

                Action.PROPAGATION -> {
                    val file = propagationFileDataSource.getFile(id)
                    val results = propagationFileDataSource.isReady(id)
                    if (file == null) {
                        call.respond(HttpStatusCode.BadRequest, "File does not exist!")
                    } else if (results) {
                        call.respond(HttpStatusCode.Accepted, "File is not ready yet!")
                    } else {
                        val outputFile = propagationFileDataSource.getFile(id)
                        val convertOutput = PropagationOutput(outputFile!!)
                        call.response.status(HttpStatusCode.OK)
                        call.respond(convertOutput)
                    }
                }
            }
        }

        delete("/{action}/{id}") {
            val id = call.parameters["id"]?.toInt() ?: return@delete call.respondText(
                "Bad Request",
                status = HttpStatusCode.BadRequest
            )
            val action = Action.valueOf(call.parameters["action"]!!.uppercase())
            when (action) {
                Action.CONVERT -> {
                    val file = convertedFileDataSource.getFile(id)
                    if (file == null) {
                        call.respond(HttpStatusCode.BadRequest, "File does not exist!")
                    } else {
                        launch(Dispatchers.IO) {
                            convertedFileDataSource.delete(id)
                        }
                        call.response.status(HttpStatusCode.OK)
                        call.respond("$action: File with ID: $id deleted")
                    }
                }

                Action.SLICE -> {
                    val file = slicedFileDataSource.getFile(id)
                    if (file == null) {
                        call.respond(HttpStatusCode.BadRequest, "File does not exist!")
                    } else {
                        launch(Dispatchers.IO) {
                            slicedFileDataSource.delete(id)
                        }
                        call.response.status(HttpStatusCode.OK)
                        call.respond("$action: File with ID: $id deleted")
                    }
                }

                Action.CONFIGURATION -> {
                    val file = configurationFileDataSource.getFile(id)
                    if (file == null) {
                        call.respond(HttpStatusCode.BadRequest, "File does not exist!")
                    } else {
                        launch(Dispatchers.IO) {
                            configurationFileDataSource.delete(id)
                        }
                        call.response.status(HttpStatusCode.OK)
                        call.respond("$action: File with ID: $id deleted")
                    }
                }

                Action.PROPAGATION -> {
                    val file = propagationFileDataSource.getFile(id)
                    if (file == null) {
                        call.respond(HttpStatusCode.BadRequest, "File does not exist!")
                    } else {
                        launch(Dispatchers.IO) {
                            propagationFileDataSource.delete(id)
                        }
                        call.response.status(HttpStatusCode.OK)
                        call.respond("$action: File with ID: $id deleted")
                    }
                }
            }
        }
    }
}

@Throws(
    CouldNotCreateFileException::class,
    CouldNotCreateRequestException::class
)
suspend fun addFile(action: Action): Int {
    when (action) {
        Action.CONVERT -> {
            val id = convertedFileDataSource.addFile()?.id
            return id ?: throw Exception()
        }

        Action.SLICE -> {
            val id = slicedFileDataSource.addFile()?.id
            return id ?: throw Exception()
        }

        Action.CONFIGURATION -> {
            val id = configurationFileDataSource.addFile()?.id
            return id ?: throw Exception()
        }

        Action.PROPAGATION -> {
            val id = propagationFileDataSource.addFile()?.id
            return id ?: throw Exception()
        }
    }
}

private fun ApplicationResponse.created(id: Int) {
    call.response.status(HttpStatusCode.Created)
    call.response.header("Location", "${call.request.uri}/$id")
}

class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()
