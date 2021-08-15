package cga.exercise.components.water

import cga.exercise.components.geometry.*
import cga.exercise.components.light.DirectionalLight
import cga.exercise.components.light.ILight
import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.Texture2D
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30

class WaterSurface(val _position: Vector3f, val _rotation: Vector3f, val _size: Float, val _material: Material =
    Material(
        Texture2D("project/assets/textures/default_water.png", true),
        Texture2D("project/assets/textures/grass_emit.png", true),
        Texture2D("project/assets/textures/grass_spec.png", true),
        60f, Vector2f(128f, 128f))
){
    private val VERTEX_COUNT = 1024
    private var mesh : Renderable

    private val reflectionFrameBuffer = WaterFrameBuffer(1280, 720)
    private val refractionFrameBuffer = WaterFrameBuffer(1280, 720)
    private val specularTexture = Texture2D("project/assets/textures/WaterTextures/Water_001_SPEC.jpg", true);
    private val normalMap = Texture2D("project/assets/textures/WaterTextures/Water_001_NORM.jpg", true)
    private val duDvMap = Texture2D("project/assets/textures/WaterTextures/Water_001_DUDV.jpg", true)
    private var shininess = 60f
    private var tCMul = Vector2f(1f, 1f)
    private var distortionStrength = 1f
    private var distortionSpeed = 0f

    private var distortionFactor = 0f

    init {
        mesh = initialize()
        if(_position != Vector3f(0f)) mesh.translateLocal(_position)
        if(_rotation != Vector3f(0f)) mesh.rotateLocal(_rotation.x(), _rotation.y(), _rotation.z())

    }

    private fun initialize() : Renderable {

        val count = VERTEX_COUNT * VERTEX_COUNT
        val vertices = FloatArray(count * 3)
        val normals = FloatArray(count * 3)
        val textureCoords = FloatArray(count * 2)
        val indices = IntArray(6 * (VERTEX_COUNT - 1) * (VERTEX_COUNT - 1))
        val vertexData = FloatArray(count * 8)
        var vertexPointer = 0
        for (i in 0 until VERTEX_COUNT) {
            for (j in 0 until VERTEX_COUNT) {

                // Vector
                vertexData[vertexPointer * 8] = j.toFloat() / (VERTEX_COUNT.toFloat() - 1) * _size
                vertexData[vertexPointer * 8 + 1] = 0f
                vertexData[vertexPointer * 8 + 2] = i.toFloat() / (VERTEX_COUNT.toFloat() - 1) * _size

                // Texture Coordinates
                vertexData[vertexPointer * 8 + 3] = j.toFloat() / (VERTEX_COUNT.toFloat() - 1)
                vertexData[vertexPointer * 8 + 4] = i.toFloat() / (VERTEX_COUNT.toFloat() - 1)

                // Normal
                vertexData[vertexPointer * 8 + 5] = 0f
                vertexData[vertexPointer * 8 + 6] = 1f
                vertexData[vertexPointer * 8 + 7] = 0f

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
        val output = Mesh(vertexData, indices, arrayOf(VertexAttribute(3, GL11.GL_FLOAT, 32, 0),
            VertexAttribute(2, GL11.GL_FLOAT, 32, 12),
            VertexAttribute(3, GL11.GL_FLOAT, 32, 20)), _material)

        return Renderable(mutableListOf(output))
    }

    fun getMesh() = mesh
    fun getHeight() = _position.y()
    fun render(shader: ShaderProgram) {
        //_material.bind(shader)
        //bindTextures(shader)
        mesh.render(shader)
    }
    fun bindTextures(shader: ShaderProgram) {
        GL30.glActiveTexture(GL30.GL_TEXTURE0)
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, reflectionFrameBuffer.getTexture())
        GL30.glActiveTexture(GL30.GL_TEXTURE1)
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, refractionFrameBuffer.getTexture())
        GL30.glActiveTexture(GL30.GL_TEXTURE2)
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, getNormalMap().getTexture());
        GL30.glActiveTexture(GL30.GL_TEXTURE3)
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, getDuDvMap().getTexture())
        GL30.glActiveTexture(GL30.GL_TEXTURE4)
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, specularTexture.getTexture())

        (shader.setUniform("reflectionTexture", 0))
        (shader.setUniform("refractionTexture", 1))
        (shader.setUniform("normalMap", 2))
        (shader.setUniform("DuDvMap", 3))
        (shader.setUniform("waterSpec", 4))
        (shader.setUniform("shininess", shininess))
        (shader.setUniform("distortionStrength", distortionStrength))
        (shader.setUniform("tcMul", tCMul))

    }

    fun bindDistortionFactor(shader: ShaderProgram, time: Float) {
        distortionFactor += distortionSpeed * time;
        distortionFactor %= 1;

        (shader.setUniform("distFactor", distortionFactor))
    }

    fun bindCameraPosition(shader: ShaderProgram, position: Vector3f) {
        shader.setUniform("cameraPosition", position);
    }

    fun bindLight(shader: ShaderProgram, light: DirectionalLight) {
        (shader.setUniform("lightSpec", light.specular))
        (shader.setUniform("lightPosition", light.direction))
        //shader.setUniform("reflectivity")
    }

    fun bindReflecBuffer() { reflectionFrameBuffer.bind(1280, 720) }
    fun bindRefracBuffer() { refractionFrameBuffer.bind(1280, 720) }
    fun unbindReflecBuffer(width: Int, height: Int) = reflectionFrameBuffer.unbind(width, height)
    fun unbindRefracBuffer(width: Int, height: Int) = refractionFrameBuffer.unbind(width, height)

    fun getReflectionBuffer() = reflectionFrameBuffer
    fun getRefractionBuffer() = refractionFrameBuffer
    fun getNormalMap() = normalMap
    fun getDuDvMap() = duDvMap
    fun getTCMul() = tCMul;
    fun setTCMul(mul: Vector2f) { tCMul = mul }
    fun getShininess() = shininess
    fun setShininess(s: Float) { shininess = s }
    fun getDistortion() = distortionStrength;
    fun setDistortion(d: Float) { distortionStrength = d }
    fun getSpeed() = distortionSpeed;
    fun setSpeed(s: Float) { distortionSpeed = s }

    fun renderNoMat(shader: ShaderProgram) {
        GL30.glActiveTexture(GL30.GL_TEXTURE0)
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, reflectionFrameBuffer.getTexture())
        GL30.glActiveTexture(GL30.GL_TEXTURE1)
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, refractionFrameBuffer.getTexture())
        GL30.glActiveTexture(GL30.GL_TEXTURE2)
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, getNormalMap().getTexture());
        GL30.glActiveTexture(GL30.GL_TEXTURE3)
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, getDuDvMap().getTexture())
        GL30.glActiveTexture(GL30.GL_TEXTURE4)
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, specularTexture.getTexture())
        mesh.renderNoMat(shader)
    }


}