package cga.engine.components.camera

import cga.engine.components.geometry.Transformable
import cga.engine.components.shader.ShaderProgram
import cga.engine.components.terrain.Terrain
import org.joml.Math
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.tan

class ThirdPersonCamera(var Fov : Float = 1.0f, var AspectRatio : Float = 16.0f / 9.0f, var ZNear : Float = 0.1f, var ZFar : Float = 100.0f, var _parent : Transformable? = null)
    : ICamera, Transformable(Matrix4f(), _parent) {

    private var CameraPos = Vector4f(0.0f, 0.0f, 0.0f,1.0f);
    private var CameraForward = Vector4f(0.0f, 0.0f, -1.0f, 1.0f);
    private var CameraUp = Vector4f(0.0f, 1.0f, 0.0f, 1.0f);
    private var mat = Matrix4f(
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, -1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
    );

    val MAX_PITCH = 0.99f
    val MIN_PITCH = -0.99f

    private val originalFOV = Fov
    private var zooming: Boolean = false
    private var mt = 0f
    private val zoomFOV = Fov * 0.7f
    private var RADIUS = 0f
    private var halfNear = ZNear * tan(Fov)

    override fun getCalculateViewMatrix(): Matrix4f {

        // Using LookAt() function. Creates the problem where World flips 180 degrees when the Camera passes through its
        // original position (?)


        /*val temp4 = Matrix4f(getWorldModelMatrix());
        val newPos = Vector4f();
        val newForward = Vector4f();
        val newUp = Vector4f();
        CameraForward.mul(temp4, newForward); //println(newForward);
        CameraUp.mul(temp4, newUp); //println(newUp);
        temp4.getColumn(3, newPos); //println(newPos);

        return Matrix4f().lookAt(newPos.x, newPos.y, newPos.z, newForward.x, newForward.y, newForward.z, newUp.x, newUp.y, newUp.z);
        */

        //Matrix4f().toString()
        // Using the inverse of the World Model Matrix. Works fine
        return Matrix4f(getWorldModelMatrix()).invert();

    }

    override fun getCalculateProjectionMatrix(): Matrix4f = Matrix4f().perspective(Fov, AspectRatio, ZNear, ZFar);
    fun getCalculateProjectionMatrix(zNear: Float, zFar: Float): Matrix4f = Matrix4f().perspective(Fov, AspectRatio, zNear, zFar);

    override fun bind(shader: ShaderProgram) {

        val projMat = Matrix4f(getCalculateProjectionMatrix());
        val viewMat = Matrix4f(getCalculateViewMatrix());
        val invViewMat = Matrix4f(viewMat).invert()

        shader.setUniform("projection_matrix", projMat, false);
        shader.setUniform("view_matrix", viewMat, false);
        //shader.setUniform("norm_matrix", invViewMat, true);
    }

    fun update(dt: Float, t: Float, terrain: Terrain) {
        updateTerrainCollision(terrain.getHeightAtPosition(getWorldPosition().x(), getWorldPosition().z()), dt)
        updateZoom(zooming, dt)
    }

    /**
     * rotate the camera locally within X-Axis limits
     */
    override fun rotateLocal(pitch: Float, yaw: Float, roll: Float) {
        val dot = getZAxis().dot(Vector3f(0f, 1f, 0f))

        // Check if the rotation is still in Range MIN_PITCH-MAX_PITCH
        if (dot in MIN_PITCH .. MAX_PITCH) super.rotateLocal(pitch, yaw, roll)
        else {

            // On Range limit, check if we are rotating back into range
            val mat = Matrix4f(modelMatrix).rotateXYZ(pitch, yaw, roll)
            if (mat.getColumn(2, Vector3f()).dot(Vector3f(0f, 1f, 0f)) in min(dot, -dot) .. max(dot, -dot)) super.rotateLocal(pitch, yaw, roll)
        }
    }

    /**
     * Same with rotateLocal() but around a point that is different from teh camera's position
     */
    override fun rotateAroundPoint(pitch: Float, yaw: Float, roll: Float, altMidpoint: Vector3f) {
        val dot = getZAxis().dot(Vector3f(0f, 1f, 0f))
        if (dot in MIN_PITCH .. MAX_PITCH) super.rotateAroundPoint(pitch, yaw, roll, altMidpoint)
        else {
            val mat = Matrix4f(modelMatrix).rotateXYZ(pitch, yaw, roll)
            if (mat.getColumn(2, Vector3f()).dot(Vector3f(0f, 1f, 0f)) in min(dot, -dot) .. max(dot, -dot)) super.rotateAroundPoint(pitch, yaw, roll, altMidpoint)
        }
    }

    fun zoomIN(hold: Boolean) {
        zooming = hold
    }

    private fun updateZoom(zoom: Boolean, dt: Float) {
        if (zooming) {
            if (mt < 1f) {
                mt += dt * 2
                mt = mt.coerceIn(0f, 1f)
                Fov = Math.fma(zoomFOV - originalFOV, mt, originalFOV)
            }
        }
        else {
            if (mt > 0f) {
                mt -= dt * 2
                mt = mt.coerceIn(0f, 1f)
                Fov = Math.fma(zoomFOV - originalFOV, mt, originalFOV)
            }
        }
    }

    private fun updateTerrainCollision(terrainHeight: Float, dt: Float) {
        if (getWorldPosition().y() - halfNear < terrainHeight) {
            translateLocal(Vector3f(0f, 0f, -abs(terrainHeight - getWorldPosition().y() + halfNear)))
        }

        if ((_parent?.getWorldPosition()?.sub(getWorldPosition())?.length() ?: 0f) < RADIUS) {
            translateLocal(Vector3f(0f, 0f, RADIUS * dt))
        }

    }

    fun setRadius(length: Float) {
        RADIUS = length
    }


}