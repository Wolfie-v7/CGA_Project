package cga.exercise.components.animation

import org.joml.Matrix4f

class KeyFrame(var timestamp: Float, val pose: Map<String, BoneTransform>) {
    fun interpolatePoses(otherFrame: KeyFrame, progression: Float): Map<String, Matrix4f> {

        val currentPose = HashMap<String, Matrix4f>()
        for(boneName in pose.keys) {
            val transform = pose[boneName]
            val otherTransform = otherFrame.pose[boneName]
            val currentTransform = otherTransform?.let { transform?.interpolateTransform(it, progression) }
            if (currentTransform != null) {
                currentPose[boneName] = currentTransform.getLocalTransform()
            }
        }
        return currentPose
    }

}
