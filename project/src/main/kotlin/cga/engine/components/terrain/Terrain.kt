package cga.engine.components.terrain

import cga.engine.components.geometry.Mesh
import cga.engine.components.geometry.Renderable
import cga.engine.components.geometry.VertexAttribute
import cga.engine.components.shader.ShaderProgram
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
    var vectors: Array<Vector3f> = Array(VERTEX_COUNT * VERTEX_COUNT) { Vector3f() }
    var texCoords: Array<Vector2f> = Array(VERTEX_COUNT * VERTEX_COUNT) { Vector2f() }
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
        this.vectors = Array(VERTEX_COUNT * VERTEX_COUNT) {Vector3f()}
        this.texCoords = Array(VERTEX_COUNT * VERTEX_COUNT) { Vector2f() }

        val count = VERTEX_COUNT * VERTEX_COUNT
        val vertices = FloatArray(count * 3)
        val normals = FloatArray(count * 3)
        val textureCoords = FloatArray(count * 2)
        val indices = IntArray(6 * (VERTEX_COUNT - 1) * (VERTEX_COUNT - 1))
        val vertexData = FloatArray(count * 14)
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
                val vec = Vector3f(j.toFloat() / (VERTEX_COUNT - 1).toFloat() * SIZE, height, i.toFloat() / (VERTEX_COUNT - 1).toFloat() * SIZE)
                this.vectors[vertexPointer] = vec
                vertexData[vertexPointer * 14] = vec.x()
                vertexData[vertexPointer * 14 + 1] = vec.y()
                vertexData[vertexPointer * 14 + 2] = vec.z()

                // Texture Coordinates
                val uv = Vector2f(j.toFloat() / (VERTEX_COUNT - 1).toFloat(), i.toFloat() / (VERTEX_COUNT - 1).toFloat())
                this.texCoords[vertexPointer] = uv
                vertexData[vertexPointer * 14 + 3] = uv.x()
                vertexData[vertexPointer * 14 + 4] = uv.y()

                // Normal
                val normal = calculateNormal(j, i, image)
                vertexData[vertexPointer * 14 + 5] = normal.x
                vertexData[vertexPointer * 14 + 6] = normal.y
                vertexData[vertexPointer * 14 + 7] = normal.z

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

        vertexPointer = 0
        //println("${vectors.size} == ${texCoords.size}")

        //println(indices.size)
        for (i in 0 .. (vectors.size - 3) step 3) {
            //println(i)
            val v0 = vectors[indices[i]]
            val v1 = vectors[indices[i + 1]]
            val v2 = vectors[indices[i + 2]]

            //println("$v0 : $v1 : $v2")

            val uv0 = texCoords[indices[i]]
            val uv1 = texCoords[indices[i + 1]]
            val uv2 = texCoords[indices[i + 2]]

            //println("$uv0 : $uv1 : $uv2")

            val deltaPos1 = v1.sub(v0, Vector3f())
            val deltaPos2 = v2.sub(v0, Vector3f())

            val deltaUV1 = uv1.sub(uv0, Vector2f())
            val deltaUV2 = uv2.sub(uv0, Vector2f())

            //println("$deltaUV1 : $deltaUV2")

            val r = 1.0f / (deltaUV1.x * deltaUV2.y - deltaUV1.y * deltaUV2.x)

            val tangent = ((deltaPos1.mul(deltaUV2.y, Vector3f())).sub(deltaPos2.mul(deltaUV1.y, Vector3f()), Vector3f())).mul(r);
            val bitangent = ((deltaPos2.mul(deltaUV1.x, Vector3f())).sub(deltaPos1.mul(deltaUV2.x, Vector3f()), Vector3f())).mul(r);

            //println("$i : $r : $deltaPos1 $deltaPos2")
            vertexData[vertexPointer * 14 + 8] = tangent.x()
            vertexData[vertexPointer * 14 + 9] = tangent.y()
            vertexData[vertexPointer * 14 + 10] = tangent.z()

            vertexData[vertexPointer * 14 + 11] = bitangent.x()
            vertexData[vertexPointer * 14 + 12] = bitangent.y()
            vertexData[vertexPointer * 14 + 13] = bitangent.z()

            vertexPointer++;

            vertexData[vertexPointer * 14 + 8] = tangent.x()
            vertexData[vertexPointer * 14 + 9] = tangent.y()
            vertexData[vertexPointer * 14 + 10] = tangent.z()

            vertexData[vertexPointer * 14 + 11] = bitangent.x()
            vertexData[vertexPointer * 14 + 12] = bitangent.y()
            vertexData[vertexPointer * 14 + 13] = bitangent.z()

            vertexPointer++;

            vertexData[vertexPointer * 14 + 8] = tangent.x()
            vertexData[vertexPointer * 14 + 9] = tangent.y()
            vertexData[vertexPointer * 14 + 10] = tangent.z()

            vertexData[vertexPointer * 14 + 11] = bitangent.x()
            vertexData[vertexPointer * 14 + 12] = bitangent.y()
            vertexData[vertexPointer * 14 + 13] = bitangent.z()

            vertexPointer++;
        }

        val mesh = Mesh(vertexData, indices, arrayOf(VertexAttribute(3, GL_FLOAT, 56, 0),
            VertexAttribute(2, GL_FLOAT, 56, 12),
            VertexAttribute(3, GL_FLOAT, 56, 20),
            VertexAttribute(3,GL_FLOAT, 56, 32),
            VertexAttribute(3,GL_FLOAT, 56, 44)
        ))

        return Renderable(mutableListOf(mesh))
    }

    private fun calculateHeight(x : Int, y : Int, image: BufferedImage) : Float {
        if (x < 0 || x >= image.height || y < 0 || y >= image.height) return 0f


        val intArray = IntArray(1)
        var newHeight = image.raster.getPixel(x, y, intArray)[0].toFloat()
        newHeight -= (MAX_PIXEL_COLOR / 2f)
        newHeight /= (MAX_PIXEL_COLOR / 2f)
        newHeight *= MAX_HEIGHT

        //val variation = -0.1f + Random.nextFloat() * 0.2f
        //newHeight += variation

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


        if (gridX >= (heights.size - 1) || gridX < 0 || gridZ >= (heights.size - 1) || gridZ < 0) return -999f

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