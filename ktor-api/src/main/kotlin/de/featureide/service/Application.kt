package de.featureide.service

import de.featureide.service.data.DatabaseFactory
import de.featureide.service.plugins.configureRouting
import de.featureide.service.plugins.configureSerialization
import io.ktor.server.application.*
import kotlinx.coroutines.launch

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    DatabaseFactory.init(environment.config)
    configureRouting()
    configureSerialization()
    launch {
        Converter.convertRemainingFiles()
    }
}