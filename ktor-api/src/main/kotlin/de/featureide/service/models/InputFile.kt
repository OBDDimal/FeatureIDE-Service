package de.featureide.service.models

import kotlinx.serialization.Serializable

@Serializable
data class InputFile(val name: String, val typeOutput: Array<String>, val fileContent: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InputFile

        if (name != other.name) return false
        if (typeOutput != other.typeOutput) return false
        if (!fileContent.contentEquals(other.fileContent)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + typeOutput.hashCode()
        result = 31 * result + fileContent.contentHashCode()
        return result
    }

}