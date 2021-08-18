package cga.utility

import cga.engine.components.terrain.Terrain
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.random.Random

/**
 * An object that generates random 3D coordinates
 */
object PositionGenerator {
    fun generatePositions(number: Int = 0,
                          minRange: Vector3f,
                          maxRange: Vector3f,
                          offsetX: Int = 0,
                          offsetY: Int = 0,
                          offsetZ: Int = 0,
                          terrain: Terrain? = null) : Array<Vector3f>? {
        if(number == 0) return null;
        val list : MutableList<Vector3f> = mutableListOf()
        var oldX = 0f; var oldY = 0f; var oldZ = 0f
        for(i in 0 until number) {
            var x = if (minRange.x() == maxRange.x()) maxRange.x() else Random.nextInt(minRange.x().toInt(), maxRange.x().toInt()).toFloat();
            if(abs(x - oldX) < offsetX) x = offsetX + oldX
            //var y = if (minRange.y() == maxRange.y()) maxRange.y() else Random.nextInt(minRange.y().toInt(), maxRange.y().toInt()).toFloat();
            //if(abs(y - oldY) < offsetY) y = offsetY + oldY
            var z = if (minRange.z() == maxRange.z()) maxRange.z() else Random.nextInt(minRange.z().toInt(), maxRange.z().toInt()).toFloat();
            if(abs(z - oldZ) < offsetZ) z = offsetZ + oldZ

            val y = terrain?.getHeightAtPosition(x, z) ?: 0f

            list.add(Vector3f(x, y, z)); oldX = x; oldY = y; oldZ = z;
        }

        return list.toTypedArray()
    }
}