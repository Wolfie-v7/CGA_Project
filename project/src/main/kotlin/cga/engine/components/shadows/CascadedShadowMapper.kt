package cga.engine.components.shadows

import cga.engine.components.camera.TronCamera
import cga.engine.components.light.DirectionalLight
import cga.engine.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.Float.Companion.MAX_VALUE
import kotlin.math.max
import kotlin.math.min


class CascadedShadowMapper(camera: TronCamera, width: Int, height: Int, val lightSource: DirectionalLight, val NUM_CASCADES: Int = 4) {

    private val z_near = camera.ZNear
    private val z_far = camera.ZFar
    val CASCADE_Z_SPLITS = floatArrayOf(z_near, z_far / 100f, z_far / 50f, z_far/ 20f, z_far / 4f)
    //val shadowMappers : Array<ShadowMapper> = TODO()
    var lightViewMatrices = Array(NUM_CASCADES) { Matrix4f() }
    var lightProjectionMatrices = Array(NUM_CASCADES) { Matrix4f() }
    var cascadesMatrices = Array(NUM_CASCADES) { Matrix4f() }

    val shadowMappersList = mutableListOf<DirLightShadowMapper>()

    

    val NEAR = 0
    val MIDDLE = 1
    val FAR = 2
    val FURTHEST = 3

    var xMax = 1f
    var yMax = 1f

    init {
        for (i in 0 until NUM_CASCADES) {
            val mapper = DirLightShadowMapper(2048, width, height, lightSource, camera, CASCADE_Z_SPLITS[i], CASCADE_Z_SPLITS[i + 1])
            shadowMappersList.add(mapper)
        }
    }

    fun render(shader: ShaderProgram, f: () -> Unit) {
        for (i in 0 until NUM_CASCADES) {
            shadowMappersList[i].bindCascade(shader, "cascades", i)
            f()
            shadowMappersList[i].unbind()
        }
    }

    fun uploadZSplits(shader: ShaderProgram, index: Int) {
        shader.setUniform("cascadeFarPlanes[$index]", CASCADE_Z_SPLITS[index + 1])
    }

    fun configureMatrices(shader: ShaderProgram) {
        for (s in shadowMappersList.indices) {
            uploadZSplits(shader, s)
            shadowMappersList[s].configureMatrices(shader, "cascades", s)
        }
    }

    fun bindDepthTextures(shader: ShaderProgram) {
        for (s in shadowMappersList.indices) {
            shadowMappersList[s].bindCascadeDepthTexture(shader, s)
        }
    }


    fun calculateBoundingBox(projViewMatrix: Matrix4f, index: Int) {
        val corners = Array(8) { Vector3f() }
        var maxZ_o: Float = -MAX_VALUE
        var minZ_o: Float = MAX_VALUE

        var minX = MAX_VALUE
        var maxX = -MAX_VALUE
        var minY = MAX_VALUE
        var maxY = -MAX_VALUE
        var minZ = MAX_VALUE
        var maxZ = -MAX_VALUE

        val center = Vector3f()
        for (i in 0 until 8) {
            val corner = projViewMatrix.frustumCorner(i, corners[i])
            center.add(corner)
            minZ_o = min(minZ_o, corner.z())
            maxZ_o = max(maxZ_o, corner.z())
        }
        center.div(8f)

        val distance = maxZ_o - minZ_o
        val lightWorldPos = Vector3f(lightSource.direction).mul(distance)
        val lightPos = Vector3f(center).add(lightWorldPos)

        val LightViewMatrix = Matrix4f().lookAt(lightPos, Vector3f(lightSource.direction), Vector3f(0f, 1f, 0f))

        for (i in 0 until 8) {
            val corner = projViewMatrix.frustumCorner(i, corners[i])
            val tmpVec = Vector4f().set(corner, 1f).mul(LightViewMatrix)
            minX = min(tmpVec.x(), minX);
            maxX = max(tmpVec.x(), maxX);
            minY = min(tmpVec.y(), minY);
            maxY = max(tmpVec.y(), maxY);
            minZ = min(tmpVec.z(), minZ);
            maxZ = max(tmpVec.z(), maxZ);
        }
        val distZ = maxZ - minZ

        val LightProjectionMatrix = Matrix4f().ortho(minX, maxX, minY, maxY, 0f, distZ)

        if (index < NUM_CASCADES) {
            lightViewMatrices[index] = LightViewMatrix
            lightProjectionMatrices[index] = LightProjectionMatrix
        }

        //for (c in corners) println(c)
    }


}