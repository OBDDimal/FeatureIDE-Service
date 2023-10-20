package de.featureide.service.models

@kotlinx.serialization.Serializable
data class PropagationOutput(val name: String, val selection: Array<String>, val deselection: Array<String>,
                             val impliedSelection: Array<String>, val impliedDeselection: Array<String>,
                             val openParents: Array<String>, val openChildren: Array<String>, val content: ByteArray) {
    constructor(file: PropagationFile) : this(name = file.name,
        selection = file.selection, impliedSelection = file.impliedSelection,
        deselection = file.deselection, impliedDeselection = file.impliedDeselection,
        openParents = file.openParents, openChildren = file.openChildren,
        content = file.content.toByteArray())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PropagationOutput

        if (name != other.name) return false
        if (!selection.contentEquals(other.selection)) return false
        if (!deselection.contentEquals(other.deselection)) return false
        if (!impliedSelection.contentEquals(other.impliedSelection)) return false
        if (!impliedDeselection.contentEquals(other.impliedDeselection)) return false
        if (!openParents.contentEquals(other.openParents)) return false
        if (!openChildren.contentEquals(other.openChildren)) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + selection.contentHashCode()
        result = 31 * result + deselection.contentHashCode()
        result = 31 * result + impliedSelection.contentHashCode()
        result = 31 * result + impliedDeselection.contentHashCode()
        result = 31 * result + openParents.contentHashCode()
        result = 31 * result + openChildren.contentHashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}