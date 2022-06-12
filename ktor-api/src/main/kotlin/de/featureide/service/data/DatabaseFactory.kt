package de.featureide.service.data


import de.featureide.service.models.RequestNumbers
import de.featureide.service.models.Requests
import de.featureide.service.models.ResultFiles
import de.featureide.service.models.UploadedFiles
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val driverClassName = config.property("storage.driverClassName").getString()
        val jdbcURL = config.property("storage.jdbcURL").getString()
        val database = Database.connect(jdbcURL, driverClassName)
        transaction(database) {
            SchemaUtils.create(Requests)
            SchemaUtils.create(RequestNumbers)
            SchemaUtils.create(ResultFiles)
            SchemaUtils.create(UploadedFiles)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}