package cga.exercise.components.animation

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f

class BoneTransform(var position: Vector3f, var rotation: Quaternionf, var scaling: Vector3f) {
    fun getLocalTransform(): Matrix4f {
        val output = Matrix4f()
        output.translate(position)
        output.rotate(rotation)
        output.scale(scaling)
        return output
    }


    fun interpolateTransform(t2: BoneTransform, progression: Float): BoneTransform {
        val pos = Vector3f(position).lerp(t2.position, progression)
        val rot = Quaternionf(rotation).slerp(t2.rotation, progression)
        val scl = Vector3f(scaling).lerp(t2.scaling, progression);
        return BoneTransform(pos, rot, scl);
    }
}
