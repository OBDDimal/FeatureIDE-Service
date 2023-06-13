package de.featureide.service.models

@kotlinx.serialization.Serializable
data class SliceInput(val name: String, val featuresToSlice: Array<String>, val content: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SliceInput

        if (name != other.name) return false
        if (featuresToSlice != other.featuresToSlice) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + featuresToSlice.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}