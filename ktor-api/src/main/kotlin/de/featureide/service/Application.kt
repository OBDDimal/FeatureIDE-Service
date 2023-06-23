package de.featureide.service

import de.featureide.service.data.DatabaseFactory
import de.featureide.service.plugins.configureSerialization
import de.featureide.service.plugins.configureRouting
import io.ktor.server.application.*

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    DatabaseFactory.init(environment.config)
    configureSerialization()
    configureRouting(environment.config)
}


