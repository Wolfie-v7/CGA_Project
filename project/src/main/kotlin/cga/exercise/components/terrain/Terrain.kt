package cga.exercise.components.terrain

import cga.exercise.components.geometry.Mesh
import cga.exercise.components.geometry.Renderable
import cga.exercise.components.geometry.VertexAttribute
import cga.exercise.components.shader.ShaderProgram
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOError
import javax.imageio.ImageIO
import kotlin.math.floor


class Terrain(var gridX : Int, var gridZ : Int, private var material : TerrainMaterial? = null, private val heightMap: String) {

    private var VERTEX_COUNT: Int = 0
    private val SIZE = 1024f


    private val MAX_HEIGHT = 300f
    //private val MAX_PIXEL_COLOR = 256 * 256 * 256
    private val MAX_PIXEL_COLOR = 65535 // Max UShort value
    private var gridCellSize = 0f

    var heights: Array<FloatArray> = Array(VERTEX_COUNT) { FloatArray(VERTEX_COUNT) }
    private var x : Float = 0f // World X Coordinate
    private var z : Float = 0f // World Z Coordinate
    private var model : Renderable? = null

    init {
        this.x = gridX * SIZE
        this.z = gridZ * SIZE
        this.model = generateTerrain(heightMap)

        this.gridCellSize = this.SIZE / (heights.size - 1)
        model?.translateLocal(Vector3f(-SIZE / 2, 0f, -SIZE / 2))
        this.x -= (SIZE / 2)
        this.z -= (SIZE / 2)

    }

    fun generateTerrain(heightMap : String) : Renderable? {
        var image : BufferedImage? = null
        try {
            image = ImageIO.read(File(heightMap))
        } catch (error : IOError) {
            error.printStackTrace()
        }

        VERTEX_COUNT = image!!.getHeight()
        this.heights = Array(VERTEX_COUNT) { FloatArray(VERTEX_COUNT) }

        val count = VERTEX_COUNT * VERTEX_COUNT
        val vertices = FloatArray(count * 3)
        val normals = FloatArray(count * 3)
        val textureCoords = FloatArray(count * 2)
        val indices = IntArray(6 * (VERTEX_COUNT - 1) * (VERTEX_COUNT - 1))
        val vertexData = FloatArray(count * 8)
        var vertexPointer = 0
        //println(image.type)
        for (i in 0 until VERTEX_COUNT) {
            for (j in 0 until VERTEX_COUNT) {

                //Flat terrain
                /*vertices[vertexPointer * 3] = j.toFloat() / (VERTEX_COUNT.toFloat() - 1) * SIZE
                vertices[vertexPointer * 3 + 1] = 0F
                vertices[vertexPointer * 3 + 2] = i.toFloat() / (VERTEX_COUNT.toFloat() - 1) * SIZE

                textureCoords[vertexPointer * 2] = j.toFloat() / (VERTEX_COUNT.toFloat() - 1)
                textureCoords[vertexPointer * 2 + 1] = i.toFloat() / (VERTEX_COUNT.toFloat() - 1)

                normals[vertexPointer * 3] = 0F
                normals[vertexPointer * 3 + 1] = 1F
                normals[vertexPointer * 3 + 2] = 0F*/

                val height = calculateHeight(j, i, image)
                heights[j][i] = height
                // Vector
                vertexData[vertexPointer * 8] = j.toFloat() / (VERTEX_COUNT - 1).toFloat() * SIZE
                vertexData[vertexPointer * 8 + 1] = height
                vertexData[vertexPointer * 8 + 2] = i.toFloat() / (VERTEX_COUNT - 1).toFloat() * SIZE

                // Texture Coordinates
                vertexData[vertexPointer * 8 + 3] = j.toFloat() / (VERTEX_COUNT - 1).toFloat()
                vertexData[vertexPointer * 8 + 4] = i.toFloat() / (VERTEX_COUNT - 1).toFloat()

                // Normal
                val normal = calculateNormal(j, i, image)
                vertexData[vertexPointer * 8 + 5] = normal.x
                vertexData[vertexPointer * 8 + 6] = normal.y
                vertexData[vertexPointer * 8 + 7] = normal.z

                vertexPointer++
            }
        }
        var pointer = 0
        for (gz in 0 until VERTEX_COUNT - 1) {
            for (gx in 0 until VERTEX_COUNT - 1) {
                val topLeft = gz * VERTEX_COUNT + gx
                val topRight = topLeft + 1
                val bottomLeft = (gz + 1) * VERTEX_COUNT + gx
                val bottomRight = bottomLeft + 1
                indices[pointer++] = topLeft
                indices[pointer++] = bottomLeft
                indices[pointer++] = topRight
                indices[pointer++] = topRight
                indices[pointer++] = bottomLeft
                indices[pointer++] = bottomRight
            }
        }

        val mesh = Mesh(vertexData, indices, arrayOf(VertexAttribute(3, GL_FLOAT, 32, 0),
                VertexAttribute(2, GL_FLOAT, 32, 12),
                VertexAttribute(3, GL_FLOAT, 32, 20)))

        return Renderable(mutableListOf(mesh))
    }

    private fun calculateHeight(x : Int, y : Int, image: BufferedImage) : Float {
        if (x < 0 || x >= image.height || y < 0 || y >= image.height) return 0f
        var height = image.getRGB(x, y).toFloat()
        val intArray = IntArray(1)
        var newHeight = image.raster.getPixel(x, y, intArray)[0].toFloat()
        //println(newHeight)
        /*height += (MAX_PIXEL_COLOR / 2f)
        height /= (MAX_PIXEL_COLOR / 2f)
        height *= MAX_HEIGHT //height += MAX_HEIGHT - 10
        return height*/
        newHeight -= (MAX_PIXEL_COLOR / 2f)
        newHeight /= (MAX_PIXEL_COLOR / 2f)
        newHeight *= MAX_HEIGHT

        return newHeight
    }

    private fun calculateNormal(x : Int, y : Int, image: BufferedImage) : Vector3f {
        val heightL = calculateHeight(x - 1, y, image)
        val heightR = calculateHeight(x + 1, y, image)
        val heightD = calculateHeight(x, y - 1, image)
        val heightU = calculateHeight(x, y + 1, image)
        val normal = Vector3f(heightL - heightR,2f,heightD - heightU)
        return normal.normalize()
    }

    fun getX() : Float = x
    fun getZ() : Float = z
    fun getMaterial(): TerrainMaterial? = material
    fun getModel(): Renderable? {
        return model
    }

    fun render(shaderProgram: ShaderProgram) {
        material?.bind(shaderProgram)
        model?.render(shaderProgram)
    }

    fun cleanup() {
        model?.MeshList?.forEach { it.cleanup() }
        material?.cleanup()
    }

    /**
     * Code from ThinMatrix
     *
     * Get terrain height at Position
     * https://www.dropbox.com/s/0md240yyc359ik3/code.txt?dl=0
     */
    fun getHeightAtPosition(x: Float, z: Float): Float {
        val output: Float
        val relativeX = x - this.x
        val relativeZ = z - this.z
        val gridX = floor(relativeX / this.gridCellSize).toInt()
        val gridZ = floor(relativeZ / this.gridCellSize).toInt()


        if (gridX >= (heights.size - 1) || gridX < 0 || gridZ >= (heights.size - 1) || gridZ < 0) return 0f

        val coordX = (relativeX % this.gridCellSize) / this.gridCellSize
        val coordZ = (relativeZ % this.gridCellSize) / this.gridCellSize

        output = if (coordX <= 1 - coordZ)
            baryCentric(
                Vector3f(0f, heights[gridX][gridZ], 0f),
                Vector3f(1f, heights[gridX + 1][gridZ], 0f),
                Vector3f(0f, heights[gridX][gridZ + 1], 1f),
                Vector2f(coordX, coordZ)
            )
        else
            baryCentric(
                Vector3f(1f, heights[gridX + 1][gridZ], 0f),
                Vector3f(0f, heights[gridX][gridZ + 1], 1f),
                Vector3f(1f, heights[gridX + 1][gridZ + 1], 1f),
                Vector2f(coordX, coordZ)
            )

        return output
    }


    private fun baryCentric(p1: Vector3f, p2: Vector3f, p3: Vector3f, pos: Vector2f): Float {
        val det = (p2.z - p3.z) * (p1.x - p3.x) + (p3.x - p2.x) * (p1.z - p3.z)
        val l1 = ((p2.z - p3.z) * (pos.x - p3.x) + (p3.x - p2.x) * (pos.y - p3.z)) / det
        val l2 = ((p3.z - p1.z) * (pos.x - p3.x) + (p1.x - p3.x) * (pos.y - p3.z)) / det
        val l3 = 1.0f - l1 - l2
        return l1 * p1.y + l2 * p2.y + l3 * p3.y
    }


}