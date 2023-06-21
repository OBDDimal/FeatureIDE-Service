package de.featureide.service.models

import kotlinx.serialization.Serializable

@Serializable
data class ConvertInput(val name: String, val typeOutput: Array<String>, val content: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConvertInput

        if (name != other.name) return false
        if (typeOutput != other.typeOutput) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + typeOutput.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }

}