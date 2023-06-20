package de.featureide.service.data


import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import de.featureide.service.models.*
import io.ktor.server.config.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val driverClassName = config.property("ktor.database.driverClassName").getString()
        val jdbcURL = config.property("ktor.database.jdbcURL").getString()
        val connectionPool = createHikariDataSource(
            url = jdbcURL,
            driver = driverClassName
        )
        val database = Database.connect(connectionPool)
        transaction(database) {
            SchemaUtils.create(SlicedFiles)
            SchemaUtils.create(ConvertedFiles)
        }
    }

    private fun createHikariDataSource(
        url: String,
        driver: String
    ) = HikariDataSource(HikariConfig().apply {
        driverClassName = driver
        jdbcUrl = url
        validate()
    })

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}