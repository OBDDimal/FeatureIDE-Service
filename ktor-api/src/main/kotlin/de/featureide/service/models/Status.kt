package de.featureide.service.models

import kotlinx.serialization.Serializable

@Serializable
data class Status(
    val requestNumber: Int,
    val finished: Boolean,
    val amountToProcess: Int,
    val resourceLocation: String,
)