package cga.engine.components.shadows

import cga.engine.components.camera.TronCamera
import cga.engine.components.light.DirectionalLight
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max
import kotlin.math.min

class DirLightShadowMapper(private var shadowResolution : Int,
                           private var width : Int,
                           private var height : Int,
                           private val lightSource : DirectionalLight,
                           private val camera: TronCamera) : ShadowMapper(shadowResolution, width, height, lightSource)
{
    constructor (resoluion: Int,
                 width: Int,
                 height: Int,
                 lightSource: DirectionalLight,
                 camera: TronCamera,
                 zNear: Float,
                 zFar: Float
    ): this(resoluion, width, height, lightSource, camera)
    {
        z_near = zNear; z_far = zFar
        //println("$z_near, $z_far")
    }
    private var z_near = camera.ZNear
    private var z_far = camera.ZFar

    override var zNear: Float = 1f
    override var zFar: Float = 10f
    private var up = 10f;
    private var down = -10f;
    private var right = 10f;
    private var left = -10f;



    override fun calculateLightViewMatrix(): Matrix4f {

        val projViewMatrix = camera.getCalculateProjectionMatrix(z_near, z_far).mul(camera.getCalculateViewMatrix())
        val corners = Array(8) { Vector3f() }
        var maxZ_o: Float = -Float.MAX_VALUE
        var minZ_o: Float = Float.MAX_VALUE
        val center = Vector3f()
        for (i in 0 until 8) {
            val corner = projViewMatrix.frustumCorner(i, corners[i])
            center.add(corner)
            minZ_o = min(minZ_o, corner.z())
            maxZ_o = max(maxZ_o, corner.z())
        }
        center.div(8f)

        var distance = maxZ_o - minZ_o; //distance /= 1f
        //println("Distance: $distance")
        //println("$maxZ_o - $minZ_o = $distance")
        val lightWorldPos = Vector3f(lightSource.direction).normalize().mul(distance * 10f)
        val lightPos = Vector3f(center).add(lightWorldPos)
        //println("Position: $lightPos; Direction: $center")

        val LightViewMatrix = Matrix4f().lookAt(lightPos, Vector3f(center), Vector3f(0f, 1f, 0f))

        return LightViewMatrix
        //return Matrix4f().lookAt(lightSource.direction, Vector3f(0f), Vector3f(0.0f, 1.0f, 0.0f));
        //return Matrix4f().lookAt(camera.getWorldPosition().add(lightSource.direction), camera.getWorldPosition(), Vector3f(0.0f, 1.0f, 0.0f));
        //return Matrix4f(lightSource.getWorldModelMatrix()).invert();
    }

    override fun calculateLightProjectionMatrix(): Matrix4f {
        val projViewMatrix = camera.getCalculateProjectionMatrix(z_near, z_far).mul(camera.getCalculateViewMatrix())
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = -Float.MAX_VALUE

        val corners = Array(8) { Vector3f() }
        val LightViewMatrix = calculateLightViewMatrix()
        //LightViewMatrix.invert()

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
        //println("Positon: ${Matrix4f(LightViewMatrix).invert().getColumn(3, Vector3f())} DistZ: $distZ")

        //println("Box Bounds: $minX, $maxX, $minY, $maxY, 0f, $distZ")

        val LightProjectionMatrix = Matrix4f().ortho(minX, maxX, minY, maxY, 0f, distZ * 50f)
        return LightProjectionMatrix
        //return Matrix4f().ortho(left, right, down, up, zNear, zFar);
    }

    fun setBoxBounds(right: Float, left: Float, up: Float, down: Float, near: Float, far: Float) {
        this.right = right; this.left = left; this.up = up; this.down = down; this.zNear = near; this.zFar = far
    }

    fun calculateLightViewMatrix(lightPosition: Vector3f, lightDirection: Vector3f): Matrix4f {
        return Matrix4f().lookAt(lightPosition, lightDirection, Vector3f(0f, 1f, 0f))
    }

    fun calculateLightProjectionMatrix(left: Float, right: Float, down: Float, up: Float, zNear: Float, zFar: Float): Matrix4f {
        return Matrix4f().ortho(left, right, down, up, zNear, zFar)
    }

    /*private val depthBuffer : DepthBuffer = DepthBuffer(shadowResolution, width, height);
    var zNear = 0.1f;
    var zFar = 100f;
    private var up = 15f;
    private var down = -15f;
    private var right = 15f;
    private var left = -15f;

    fun bind(shaderProgram: ShaderProgram) {
        bindDepthTexture(shaderProgram);
        bindDepthBuffer();
        configureMatrices(shaderProgram);
        GL30.glEnable(GL30.GL_DEPTH_TEST);
    }

    fun bindDepthBuffer() {
        depthBuffer.bind(shadowResolution, shadowResolution);
    }

    fun bindDepthTexture(shaderProgram: ShaderProgram) {
        depthBuffer.bindDepthTexture(shaderProgram);
    }

    fun configureMatrices(shaderProgram: ShaderProgram) {
        val viewMat = Matrix4f(calculateViewMatrix()); val projMat = Matrix4f(calculateProjectionMatrix());
        shaderProgram.setUniform("lightSpaceMatrix", Matrix4f(projMat).mul(viewMat), false);
    }

    fun unbind() {
        depthBuffer.unbind();
    }

    fun getDepthMap() = depthBuffer.getDepthMap();
    fun calculateProjectionMatrix(): Matrix4f {
        return Matrix4f().ortho(left, right, down, up, zNear, zFar);
    }
    fun calculateViewMatrix(): Matrix4f {
        return Matrix4f().lookAt(lightSource.direction, Vector3f(0f, 0f, 0f), Vector3f(0.0f, 1.0f, 0.0f));
    }*/

}