package de.featureide.service.models

import kotlinx.serialization.Serializable

@Serializable
data class ConvertOutput(
    val name: Array<String>,
    val originalName: String,
    val typeOutput: Array<String>,
    val content: Array<ByteArray>,
    ) {
    constructor(file: ConvertedFile) : this(name = file.name, originalName = file.originalName, typeOutput = file.typeOutput, content = file.content.map { f -> f.toByteArray() }.toTypedArray())
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConvertOutput

        if (!name.contentEquals(other.name)) return false
        if (originalName != other.originalName) return false
        if (!typeOutput.contentEquals(other.typeOutput)) return false
        if (!content.contentDeepEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.contentHashCode()
        result = 31 * result + originalName.hashCode()
        result = 31 * result + typeOutput.contentHashCode()
        result = 31 * result + content.contentDeepHashCode()
        return result
    }


}