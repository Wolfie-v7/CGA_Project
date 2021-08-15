package cga.exercise.components.animation

data class Animation_old(val duration: Float, val ticksPerSecond: Double, val channels: Array<AnimNode>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Animation_old

        if (duration != other.duration) return false
        if (ticksPerSecond != other.ticksPerSecond) return false
        if (!channels.contentEquals(other.channels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = duration.hashCode()
        result = 31 * result + ticksPerSecond.hashCode()
        result = 31 * result + channels.contentHashCode()
        return result
    }
}
