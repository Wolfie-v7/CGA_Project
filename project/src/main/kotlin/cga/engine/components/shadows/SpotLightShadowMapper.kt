package cga.engine.components.shadows

import cga.engine.components.light.SpotLight
import org.joml.Matrix4f

class SpotLightShadowMapper(private val shadowResolution: Int, private var width: Int, private var height: Int, private val lightSource: SpotLight) : ShadowMapper(shadowResolution, width, height, lightSource) {
    private var AspectRatio: Float = 1f
    private var Fov: Float = (lightSource.getOuterAngle() * 2);
    override var zNear: Float = 1f;
    override var zFar: Float = 50f
    override fun calculateLightViewMatrix(): Matrix4f {
        return Matrix4f(lightSource.getWorldModelMatrix()).invert();
    }

    override fun calculateLightProjectionMatrix(): Matrix4f {
        return Matrix4f().perspective(Fov, AspectRatio, zNear, zFar);
    }
}