package cga.exercise.components.utility

import org.joml.Vector3f
import java.io.File

object Vector3Writer {

    val positions = mutableListOf<Vector3f>()
    fun save(position: Vector3f, list: MutableList<Vector3f>): Boolean {
        return list.add(position)
    }

    fun save(position: Vector3f): Boolean {
        return positions.add(position)
    }
    fun write(filePath: String) {
        val file = File(filePath)
        file.printWriter().use {
            for (position in positions) {
                it.println("v ${position.x()} ${position.y()} ${position.z()}")
            }

        }

    }

}