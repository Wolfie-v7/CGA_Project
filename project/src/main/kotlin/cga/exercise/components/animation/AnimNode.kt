package cga.exercise.components.animation

import org.joml.Quaternionf
import org.joml.Vector3f

data class AnimNode(val name: String, val keyPositions: Array<Vector3f>, val keyRotations: Array<Quaternionf>, val keyScales: Array<Vector3f>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnimNode

        if (name != other.name) return false
        if (!keyPositions.contentEquals(other.keyPositions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + keyPositions.contentHashCode()
        return result
    }
}
