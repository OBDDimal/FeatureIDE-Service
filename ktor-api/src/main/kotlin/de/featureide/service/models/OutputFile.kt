package de.featureide.service.models

import kotlinx.serialization.Serializable

@Serializable
data class OutputFile(
    val name: String,
    val originalName: String,
    val type: String,
    val success: Boolean,
    val content: ByteArray,
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OutputFile

        if (name != other.name) return false
        if (originalName != other.originalName) return false
        if (type != other.type) return false
        if (success != other.success) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + originalName.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + success.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}