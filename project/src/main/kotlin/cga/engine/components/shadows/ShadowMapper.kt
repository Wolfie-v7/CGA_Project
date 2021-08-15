package cga.engine.components.shadows

import cga.engine.components.framebuffer.DepthBuffer
import cga.engine.components.light.ILight
import cga.engine.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.lwjgl.opengl.GL30

abstract class ShadowMapper(private var shadowResolution : Int, private var width : Int, private var height : Int, private val lightSource : ILight) {

    private val depthBuffer : DepthBuffer = DepthBuffer(shadowResolution, width, height);
    protected abstract var zNear: Float;
    protected abstract var zFar: Float;

    fun bindDepthBuffer() {
        depthBuffer.bind(shadowResolution, shadowResolution);
    }
    fun bindDepthTexture(shaderProgram: ShaderProgram, index : Int = 0) {
        depthBuffer.bindDepthTexture(shaderProgram, index);
    }

    fun bindCascadeDepthTexture(shaderProgram: ShaderProgram, index : Int = 0) {
        depthBuffer.bindCascadeDepthTexture(shaderProgram, index);
    }

    fun bind(shaderProgram: ShaderProgram) {
        bindDepthBuffer(); bindDepthTexture(shaderProgram);  configureMatrices(shaderProgram); GL30.glCullFace(GL30.GL_FRONT);
        GL30.glEnable(GL30.GL_DEPTH_TEST); // Make sure Depth Test is enabled when rendering to depth texture
    }

    fun bindCascade(shaderProgram: ShaderProgram, arrayName: String, index: Int) {
        bindDepthBuffer(); bindCascadeDepthTexture(shaderProgram, index);  configureMatrices(shaderProgram, arrayName, index); GL30.glCullFace(GL30.GL_FRONT);
        GL30.glEnable(GL30.GL_DEPTH_TEST); // Make sure Depth Test is enabled when rendering to depth texture
    }

    fun unbind() {
        depthBuffer.unbind();
        GL30.glCullFace(GL30.GL_BACK);
    }

    abstract fun calculateLightViewMatrix() : Matrix4f
    abstract fun calculateLightProjectionMatrix() : Matrix4f
    fun configureMatrices(shaderProgram: ShaderProgram, index : Int = 0) : Boolean
    {
        val viewMat = Matrix4f(calculateLightViewMatrix()); val projectionMat = Matrix4f(calculateLightProjectionMatrix());
        val lightSpaceMatrix = Matrix4f(projectionMat).mul(viewMat);
        shaderProgram.setUniform("lightSpaceMatrix", lightSpaceMatrix, false);
        return shaderProgram.setUniform("lightSpaceMatrices[$index]", lightSpaceMatrix, false);
    }

    fun configureMatrices(shaderProgram: ShaderProgram, arrayName: String, index : Int = 0) : Boolean
    {
        val viewMat = Matrix4f(calculateLightViewMatrix()); val projectionMat = Matrix4f(calculateLightProjectionMatrix());
        val lightSpaceMatrix = Matrix4f(projectionMat).mul(viewMat);
        shaderProgram.setUniform("lightSpaceMatrix", lightSpaceMatrix, false);
        return shaderProgram.setUniform("$arrayName[$index]", lightSpaceMatrix, false);
    }

    fun getDepthMap() = depthBuffer.getDepthMap();
    fun bindTex(shaderProgram: ShaderProgram) {
        depthBuffer.bindDepthTexture(shaderProgram);
    }

    fun cleanup()
    {
        depthBuffer.cleanup();
    }

}