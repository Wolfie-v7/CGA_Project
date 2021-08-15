package cga.engine.components.skybox

import cga.engine.components.camera.TronCamera
import cga.engine.components.geometry.VertexAttribute
import cga.engine.components.shader.ShaderProgram
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL30

class Skybox(var texture: CubeMap, var camera: TronCamera? = null) {

    private var vao = 0
    private var vbo = 0
    private var skyboxVertices: FloatArray = floatArrayOf( // positions
            -1.0f,  1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
             1.0f, -1.0f, -1.0f,
             1.0f, -1.0f, -1.0f,
             1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,

            -1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

             1.0f, -1.0f, -1.0f,
             1.0f, -1.0f,  1.0f,
             1.0f,  1.0f,  1.0f,
             1.0f,  1.0f,  1.0f,
             1.0f,  1.0f, -1.0f,
             1.0f, -1.0f, -1.0f,

            -1.0f, -1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
             1.0f,  1.0f,  1.0f,
             1.0f,  1.0f,  1.0f,
             1.0f, -1.0f,  1.0f,
            -1.0f, -1.0f,  1.0f,

            -1.0f,  1.0f, -1.0f,
             1.0f,  1.0f, -1.0f,
             1.0f,  1.0f,  1.0f,
             1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f, -1.0f,

            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f,  1.0f,
             1.0f, -1.0f, -1.0f,
             1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f,  1.0f,
             1.0f, -1.0f,  1.0f
    )

    init {


        vao = GL30.glGenVertexArrays();
        vbo = GL30.glGenBuffers()
        GL30.glBindVertexArray(vao);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vbo)
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, skyboxVertices, GL30.GL_STATIC_DRAW)
        val attribute = VertexAttribute(3, GL_FLOAT, 12, 0);
        GL30.glEnableVertexAttribArray(0)
        GL30.glVertexAttribPointer(0, attribute.n, attribute.type, false, attribute.stride, attribute.offset.toLong())
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)


    }

    fun bind(shaderProgram: ShaderProgram) {
        texture.bind(GL30.GL_TEXTURE0);
        shaderProgram.setUniform("skybox", 0);
    }

    fun render(shaderProgram: ShaderProgram)
    {
        bind(shaderProgram);
        camera?.getCalculateProjectionMatrix()?.let { shaderProgram.setUniform("projection_matrix", it, false) };
        val viewMat = Matrix4f(Matrix3f(camera?.getCalculateViewMatrix()));
        shaderProgram.setUniform("view_matrix", viewMat, false)
        GL30.glBindVertexArray(vao)
        GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, skyboxVertices.size)
        GL30.glBindVertexArray(0)

    }

    fun cleanup() {
        if (vbo != 0) GL15.glDeleteBuffers(vbo)
        if (vao != 0) GL30.glDeleteVertexArrays(vao)
        texture.cleanup();
    }
}