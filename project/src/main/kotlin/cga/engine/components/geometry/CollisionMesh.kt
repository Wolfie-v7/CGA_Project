package cga.engine.components.geometry

import cga.engine.components.camera.TronCamera
import cga.engine.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.opengl.GL30

class CollisionMesh(private val vertexdata: FloatArray, indexdata: IntArray, private var attributes: Array<VertexAttribute>, _parent: Transformable? = null, var bisBlocking : Boolean = false)
    : Transformable(_parent?.getWorldModelMatrix() ?: Matrix4f(), _parent)
{
    private var vao = 0
    private var vbo = 0
    private var ibo = 0
    private var indexcount = 0
    var attribCount = attributes.size;
    private var bInstanced = false;

    var center = Vector3f()
        get() = getWorldPosition()
    var Ax = getWorldXAxis()
        get() = getWorldXAxis()
    var Ay = getWorldYAxis()
        get() = getWorldYAxis()
    var Az = getWorldZAxis()
        get() = getWorldZAxis()
    var halfWidth = 1f
    var halfHeight = 1f
    var halfDepth = 1f
    var color = Vector3f(1f, 0f, 0f) // Red


    init {

        indexcount = indexdata.size


        vao = GL30.glGenVertexArrays()
        vbo = GL30.glGenBuffers()
        ibo = GL30.glGenBuffers()


        GL30.glBindVertexArray(vao)
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo)
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, ibo)


        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, vertexdata, GL30.GL_STATIC_DRAW)

        for (i in attributes.indices) {
            GL30.glEnableVertexAttribArray(i)
            GL30.glVertexAttribPointer(
                i,
                attributes[i].n,
                attributes[i].type,
                false,
                attributes[i].stride,
                attributes[i].offset.toLong()
            )
        }

        GL30.glBufferData(GL30.GL_ELEMENT_ARRAY_BUFFER, indexdata, GL30.GL_STATIC_DRAW)


        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun render(shader: ShaderProgram, camera: TronCamera) {
        val viewMat = camera.getCalculateViewMatrix()
        val projMat = camera.getCalculateProjectionMatrix()
        shader.setUniform("model_matrix", getWorldModelMatrix(), false);
        shader.setUniform("view_matrix", viewMat, false);
        shader.setUniform("projection_matrix", projMat, false);
        shader.setUniform("in_color", color)
        GL30.glBindVertexArray(vao)
        GL30.glDrawElements(GL30.GL_LINE_LOOP, indexcount, GL30.GL_UNSIGNED_INT, 0)
        GL30.glBindVertexArray(0)
    }


    fun OnCollision(
        otherCollider: CollisionMesh,
        overlapDistance: Float,
        overlapAxis: Vector3f,
        pushBackVector: Vector3f
    ) {
        color = Vector3f(0f, 1f, 0f)

        parent?.translateLocal(pushBackVector.mul(overlapDistance))
    }

}