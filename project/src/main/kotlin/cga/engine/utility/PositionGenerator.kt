package cga.engine.utility

import cga.engine.components.terrain.Terrain
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * An object that generates random 3D coordinates
 */
object PositionGenerator {
    fun generatePositions(number: Int = 0,
                          minRange: Vector3f,
                          maxRange: Vector3f,
                          offsetX: Float = 0f,
                          offsetY: Float = 0f,
                          offsetZ: Float = 0f,
                          terrain: Terrain? = null) : Array<Vector3f>? {
        if(number == 0) return null;
        val list : MutableList<Vector3f> = mutableListOf()
        var oldX = 0f; var oldY = 0f; var oldZ = 0f
        for(i in 0 until number) {
            var x = if (minRange.x() == maxRange.x()) maxRange.x() else Random.nextInt(minRange.x().toInt(), maxRange.x().toInt()).toFloat();
            if(abs(x - oldX) < offsetX) x = offsetX + oldX
            var y = 0f
            var z = if (minRange.z() == maxRange.z()) maxRange.z() else Random.nextInt(minRange.z().toInt(), maxRange.z().toInt()).toFloat();
            if(abs(z - oldZ) < offsetZ) z = offsetZ + oldZ

            if (terrain != null) {
                y = terrain.getHeightAtPosition(x, z)
                y -= offsetY
            }
            else {
                val min = min(minRange.y(), maxRange.y()); val max = max(minRange.y(), maxRange.y())
                y = if (minRange.y().toInt() == maxRange.y().toInt()) maxRange.y() else Random.nextInt(min.toInt(), max.toInt()).toFloat();
                if(abs(y - oldY) < offsetY) y = offsetY + oldY
            }


            list.add(Vector3f(x, y, z)); oldX = x; oldY = y; oldZ = z;
        }

        return list.toTypedArray()
    }
}