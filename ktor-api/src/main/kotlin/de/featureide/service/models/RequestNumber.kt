package de.featureide.service.models

import org.jetbrains.exposed.sql.Table

data class RequestNumber(val value: Int)

object RequestNumbers : Table() {
    val value = integer("value").autoIncrement()

    override val primaryKey = PrimaryKey(value)
}