package de.featureide.service.models

@kotlinx.serialization.Serializable
data class FeatureModelStatOutput(
    val name: String,
    val content: ByteArray,
    val deadFeatures: Array<String>,
    val falseOptionalFeatures: Array<String>,
    val coreFeatures: Array<String>
) {
    constructor(file: FeatureModelStatFile) : this(
        name = file.name,
        content = file.content.toByteArray(),
        deadFeatures = file.deadFeatures,
        falseOptionalFeatures = file.falseOptionalFeatures,
        coreFeatures = file.coreFeatures
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FeatureModelStatOutput

        if (name != other.name) return false
        if (!content.contentEquals(other.content)) return false
        if (!deadFeatures.contentEquals(other.deadFeatures)) return false
        if (!falseOptionalFeatures.contentEquals(other.falseOptionalFeatures)) return false
        if (!coreFeatures.contentEquals(other.coreFeatures)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + deadFeatures.contentHashCode()
        result = 31 * result + falseOptionalFeatures.contentHashCode()
        result = 31 * result + coreFeatures.contentHashCode()
        return result
    }

}

