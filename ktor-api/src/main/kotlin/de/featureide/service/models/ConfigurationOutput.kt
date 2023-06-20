package de.featureide.service.models

@kotlinx.serialization.Serializable
data class ConfigurationOutput(val name: String, val algorithm: String, val t: Int, val limit: Int, val content: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConfigurationInput

        if (name != other.name) return false
        if (algorithm != other.algorithm) return false
        if (t != other.t) return false
        if (limit != other.limit) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + t
        result = 31 * result + limit
        result = 31 * result + content.contentHashCode()
        return result
    }
}