package cga.exercise.components.utility

import cga.framework.OBJLoader
import org.joml.Vector3f
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*

object Vector3Reader {
    private val positions : MutableList<Vector3f> = mutableListOf()
    fun read(path: String) : Array<Vector3f>? {


        val file = File(path)
        val stream = BufferedInputStream(FileInputStream(file))
        val scanner = Scanner(stream)

        var c = 0
        while(scanner.hasNextLine()) {
            if (scanner.hasNext("v")) {
                val s = scanner.next(); println(s)
                positions.add(parsePosition(scanner))
            }
            else scanner.nextLine()
        }
        if (positions.isNotEmpty()) return positions.toTypedArray()
        return null;
    }

    private fun parsePosition(scanner: Scanner): Vector3f {
        return try {
            parseVector3(scanner)
        } catch (ex: Exception) {
            throw OBJLoader.OBJException("Error parsing v command:\n" + ex.message)
        }
    }
    private fun parseVector3(scanner: Scanner): Vector3f {
        val x: Double = scanner.next().toDouble()
        val y: Double = scanner.next().toDouble()
        val z: Double = scanner.next().toDouble()
        return Vector3f(x.toFloat(), y.toFloat(), z.toFloat())
    }
}