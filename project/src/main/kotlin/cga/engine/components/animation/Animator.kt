package cga.engine.components.animation

import cga.engine.components.geometry.Mesh
import org.joml.Matrix4f

class Animator(val mesh: Mesh) {
    private var animationSpeed: Float = 1f
    private var currentAnimation: Animation? = null
    private var animationTime = 0f

    fun playAnimation(animation: Animation?, playbackSpeed: Float = 1f) {
        if(animation != currentAnimation) {
            animationSpeed = playbackSpeed
            currentAnimation = animation
            animationTime = 0f
        }

    }

    fun stopAnimation() {
        currentAnimation = null
        animationTime = 0f
    }


    fun update(dt: Float) {
        if(currentAnimation == null) {
            applyRestPose(mesh.rootBone!!, Matrix4f())
            return
        }

        increaseTime(dt)
        val currentPose = calculateCurrentAnimationPose()
        applyPoseToBones(currentPose, mesh.rootBone!!, Matrix4f())
    }

    private fun applyRestPose(bone: Bone, parentTransform: Matrix4f) {
        val currentLocalTransform = bone.getTransformLocal()
        val currentTransform = Matrix4f(parentTransform).mul(currentLocalTransform)

        for (child in bone.children) {
            applyRestPose(child, currentTransform)
        }
        currentTransform.mul(bone.getInverseTransform())
        bone.setAnimatedTransform(currentTransform)
    }

    private fun applyPoseToBones(currentPose: Map<String, Matrix4f>, bone: Bone, parentTransform: Matrix4f) {
        val currentLocalTransform = currentPose[bone.name] ?: bone.getTransformLocal()
        val currentTransform = Matrix4f(parentTransform).mul(currentLocalTransform)

        for (child in bone.children) {
            applyPoseToBones(currentPose, child, currentTransform)
        }
        currentTransform.mul(bone.getInverseTransform())
        bone.setAnimatedTransform(currentTransform)
    }

    private fun calculateCurrentAnimationPose(): Map<String, Matrix4f> {
        val keyFrames = getPrevAndNextKeyFrames()
        val progression = calculateProgression(keyFrames[0], keyFrames[1])
        return keyFrames[0].interpolatePoses(keyFrames[1], progression)
    }

    private fun calculateProgression(prevFrame: KeyFrame, nextFrame: KeyFrame): Float {
        if(this.animationTime <= prevFrame.timestamp) return 0f
        val totalTime = nextFrame.timestamp - prevFrame.timestamp
        val currentTime = this.animationTime - prevFrame.timestamp
        return currentTime / totalTime
    }

    private fun getPrevAndNextKeyFrames(): Array<KeyFrame> {
        val allFrames = currentAnimation!!.keyFrames
        var prevFrame = allFrames[0]
        var nextFrame = allFrames[0]
        for (i in 1 until allFrames.size) {
            nextFrame = allFrames[i]
            if(nextFrame.timestamp > animationTime) break
            prevFrame = allFrames[i]
        }
        return arrayOf(prevFrame, nextFrame)
    }

    private fun increaseTime(dt: Float) {
        animationTime += dt * animationSpeed
        if(animationTime > currentAnimation!!.duration) this.animationTime %= currentAnimation!!.duration
    }

}
