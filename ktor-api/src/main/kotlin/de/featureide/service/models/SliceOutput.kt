package de.featureide.service.models

@kotlinx.serialization.Serializable
data class SliceOutput(val name: String, val originalName: String, val featuresSliced: String, val content: ByteArray) {
    constructor(file: SlicedFile) : this(name = file.name, originalName = file.originalName, featuresSliced = "", content = file.content.toByteArray())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SliceOutput

        if (name != other.name) return false
        if (originalName != other.originalName) return false
        if (featuresSliced != other.featuresSliced) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + originalName.hashCode()
        result = 31 * result + featuresSliced.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}