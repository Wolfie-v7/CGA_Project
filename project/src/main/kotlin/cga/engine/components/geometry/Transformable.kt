package cga.engine.components.geometry

import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.sin


open class Transformable(var modelMatrix: Matrix4f = Matrix4f(), var parent: Transformable? = null) {

    var pitch = 0f
    var yaw = 0f
    var roll = 0f
    /**
     * Rotates object around its own origin.
     * @param pitch radiant angle around x-axis ccw
     * @param yaw radiant angle around y-axis ccw
     * @param roll radiant angle around z-axis ccw
     */
    fun rotateLocal(pitch: Float, yaw: Float, roll: Float) {

        this.pitch += pitch; this.yaw += yaw; this.roll += roll
        modelMatrix.rotateXYZ(pitch, yaw, roll);
    }

    /**
     * Rotates object around given rotation center.
     * @param pitch radiant angle around x-axis ccw
     * @param yaw radiant angle around y-axis ccw
     * @param roll radiant angle around z-axis ccw
     * @param altMidpoint rotation center
     */
    fun rotateAroundPoint(pitch: Float, yaw: Float, roll: Float, altMidpoint: Vector3f) {
        /*val rotMatX = Matrix4f(
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, cos(pitch), sin(pitch), 0.0f,
                0.0f, -sin(pitch), cos(pitch), 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f);
        val rotMatY = Matrix4f(
                cos(yaw), 0.0f, -sin(yaw), 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                sin(yaw), 0.0f, cos(yaw), 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f);
        val rotMatZ = Matrix4f(
                cos(roll), sin(roll), 0.0f, 0.0f,
                -sin(roll), cos(roll), 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f

        );
        val rotMat = rotMatX.mul(rotMatY.mul(rotMatZ)); // Rotation Matrix


        altMidpoint.mul(-1.0f); //println(altMidpoint);
        this.translateGlobal(altMidpoint);


        modelMatrix.set(rotMat.mul(modelMatrix));

        //modelMatrix.rotate(pitch, 1.0f, 0.0f, 0.0f);
        //modelMatrix.rotate(yaw, 0.0f, 1.0f, 0.0f);
        //modelMatrix.rotate(roll, 0.0f, 0.0f, 1.0f);

        altMidpoint.mul(-1.0f);
        this.translateGlobal(altMidpoint);*/

        this.pitch += pitch; this.yaw += yaw; this.roll += roll
        modelMatrix = Matrix4f().translate(altMidpoint).rotateXYZ(pitch, yaw, roll).translate(Vector3f(altMidpoint).negate()).mul(modelMatrix)

    }

    /**
     * Translates object based on its own coordinate system.
     * @param deltaPos delta positions
     */
    fun translateLocal(deltaPos: Vector3f) {
        modelMatrix.translate(deltaPos);
        /*val vec4 = Vector4f(deltaPos.x, deltaPos.y, deltaPos.z, 1.0f);
        val tMatrix = Matrix4f().setColumn(3, vec4);
        modelMatrix.mul(tMatrix); //println(modelMatrix);*/

    }

    /**
     * Translates object based on its parent coordinate system.
     * Hint: global operations will be left-multiplied
     * @param deltaPos delta positions (x, y, z)
     */
    fun translateGlobal(deltaPos: Vector3f) {
        val tMat4f = Matrix4f();
        if (parent != null) tMat4f.mul(parent!!.modelMatrix)
        tMat4f.translate(deltaPos);
        tMat4f.mul(modelMatrix);

        modelMatrix.set(tMat4f);

    }

    /**
     * Scales object related to its own origin
     * @param scale scale factor (x, y, z)
     */
    fun scaleLocal(scale: Vector3f) {
        val pos = getPosition();
        pos.mul(-1.0f);
        this.translateGlobal(pos);
        modelMatrix.scale(scale);
        pos.mul(-1.0f);
        this.translateGlobal(pos);

    }

    /**
     * Returns position based on aggregated translations.
     * Hint: last column of model matrix
     * @return position
     */
    fun getPosition(): Vector3f {
        val x = modelMatrix.get(3, 0);
        val y = modelMatrix.get(3, 1);
        val z = modelMatrix.get(3, 2);
        return Vector3f(x, y, z);
    }

    /**
     * Returns position based on aggregated translations incl. parents.
     * Hint: last column of world model matrix
     * @return position
     */
    fun getWorldPosition(): Vector3f {
        val output = Vector4f()
        getWorldModelMatrix().getColumn(3, output)
        return Vector3f(output.x, output.y, output.z);
    }

    /**
     * Returns x-axis of object coordinate system
     * Hint: first normalized column of model matrix
     * @return x-axis
     */
    fun getXAxis(): Vector3f {
        val mat = modelMatrix;
        val x = mat.get(0,0);
        val y = mat.get(0,1);
        val z = mat.get(0,2);
        val vec = Vector3f(x, y, z);
        return vec.normalize();
    }

    /**
     * Returns y-axis of object coordinate system
     * Hint: second normalized column of model matrix
     * @return y-axis
     */
    fun getYAxis(): Vector3f {
        val mat = modelMatrix;
        val x = mat.get(1,0);
        val y = mat.get(1,1);
        val z = mat.get(1,2);
        val vec = Vector3f(x, y, z);
        return vec.normalize();
    }

    /**
     * Returns z-axis of object coordinate system
     * Hint: third normalized column of model matrix
     * @return z-axis
     */
    fun getZAxis(): Vector3f {
        val mat = modelMatrix;
        val x = mat.get(2,0);
        val y = mat.get(2,1);
        val z = mat.get(2,2);
        val vec = Vector3f(x, y, z);
        return vec.normalize();
    }

    /**
     * Returns x-axis of world coordinate system
     * Hint: first normalized column of world model matrix
     * @return x-axis
     */
    fun getWorldXAxis(): Vector3f {
        val output = Vector4f()
        getWorldModelMatrix().getColumn(0, output)
        val vec = Vector3f(output.x, output.y, output.z);
        return vec.normalize();
    }

    /**
     * Returns y-axis of world coordinate system
     * Hint: second normalized column of world model matrix
     * @return y-axis
     */
    fun getWorldYAxis(): Vector3f {
        val output = Vector4f()
        getWorldModelMatrix().getColumn(1, output)
        val vec = Vector3f(output.x, output.y, output.z);
        return vec.normalize();
    }

    /**
     * Returns z-axis of world coordinate system
     * Hint: third normalized column of world model matrix
     * @return z-axis
     */
    fun getWorldZAxis(): Vector3f {
        val output = Vector4f()
        getWorldModelMatrix().getColumn(2, output)
        val vec = Vector3f(output.x, output.y, output.z);
        return vec.normalize();
    }


    /**
     * Returns multiplication of world and object model matrices.
     * Multiplication has to be recursive for all parents.
     * Hint: scene graph
     * @return world modelMatrix
     */
    fun getWorldModelMatrix(): Matrix4f {

        /*val res = Matrix4f();
        val par = parent;

        if (par != null) res.set(par.getWorldModelMatrix());
        res.mul(modelMatrix);

        return res;*/

        val worldMatrix = Matrix4f(getLocalModelMatrix());
        parent?.getWorldModelMatrix()?.mul(modelMatrix, worldMatrix);
        return worldMatrix;

    }

    /**
     * Returns object model matrix
     * @return modelMatrix
     */
    fun getLocalModelMatrix(): Matrix4f {

        return Matrix4f(modelMatrix);
    }

    fun attachParent(newParent : Transformable?) { this.parent = newParent; }
    fun invertPitch() {

        modelMatrix.m10(modelMatrix.m10() * -1)
        modelMatrix.m01(modelMatrix.m01() * -1)
        modelMatrix.m00(modelMatrix.m00())

        modelMatrix.m12(modelMatrix.m12() * -1)
        modelMatrix.m21(modelMatrix.m21() * -1)

    }

}