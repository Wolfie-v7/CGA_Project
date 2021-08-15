package cga.exercise.components.camera

import cga.exercise.components.geometry.Transformable
import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector4f

class TronCamera(var Fov : Float = 1.0f, var AspectRatio : Float = 16.0f / 9.0f, var ZNear : Float = 0.1f, var ZFar : Float = 100.0f, var _parent : Transformable? = null)
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

    init {

    }

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

        Matrix4f().toString()
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

}