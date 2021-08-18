package cga.engine.components.animation

import cga.engine.components.geometry.Mesh
import org.joml.Matrix4f

class Animator(val mesh: Mesh) {
    private var animationSpeed: Float = 1f
    private var currentAnimation: Animation? = null
    private var animationTime = 0f
    private var looping: Boolean = false

    fun playAnimation(animation: Animation?, playbackSpeed: Float = 1f, loop: Boolean = false) {
        if(animation != currentAnimation) {
            animationSpeed = playbackSpeed
            currentAnimation = animation
            animationTime = 0f
            looping = loop
        }

    }

    fun stopAnimation() {
        animationSpeed = 1f
        currentAnimation = null
        animationTime = 0f
        looping = false
    }


    fun update(dt: Float) {
        increaseTime(dt)
        if(currentAnimation == null) {
            mesh.rootBone?.let { applyRestPose(it, Matrix4f()) }
            return
        }
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
        if (currentAnimation == null) return
        animationTime += dt * animationSpeed
        if(animationTime > currentAnimation!!.duration) {
            if (looping) this.animationTime %= currentAnimation!!.duration
            else stopAnimation()
        }
    }

}
