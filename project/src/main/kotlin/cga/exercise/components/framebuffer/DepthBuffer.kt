package cga.exercise.components.framebuffer

import cga.exercise.components.geometry.VertexAttribute
import cga.exercise.components.light.ILight
import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import java.lang.Exception
import java.nio.ByteBuffer

class DepthBuffer(private val resolution: Int = 1024, private val screenWidth: Int, private val screenHeight: Int) {


    private var fbo = 0;
    private var depthMap = 0;




    init {
        // FBO ID
        fbo = GL30.glGenFramebuffers();

        // Depth Map
        depthMap = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, depthMap);
        GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0,
                GL30.GL_DEPTH_COMPONENT, resolution, resolution, 0,
                GL30.GL_DEPTH_COMPONENT, GL30.GL_FLOAT, null as ByteBuffer?);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_BORDER);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_BORDER);
        GL30.glTexParameterfv(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_BORDER_COLOR, floatArrayOf(1f, 1f, 1f, 1f));
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0);


        // Attach Depth Texture to Frame Buffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_TEXTURE_2D, depthMap, 0);
        GL30.glDrawBuffer(GL30.GL_NONE);
        //GL30.glReadBuffer(GL30.GL_NONE);
        if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) throw Exception("Frame Buffer Not Complete");
        //else println("Depth Map COMPLETE");

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);


    }


    fun getBufferId() = fbo;
    fun getDepthMap() = depthMap;

    // Binds Depth Buffer
    // Sets Viewport to the desired width and height
    // Clears the depth buffer
    fun bind(width : Int, height : Int) {
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GL30.glViewport(0, 0, width, height);
        GL30.glClear(GL30.GL_DEPTH_BUFFER_BIT);
    }
    fun bindDepthTexture(shaderProgram: ShaderProgram, index : Int = 0) {

        GL30.glActiveTexture(GL30.GL_TEXTURE0 + 14 + index);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, depthMap);
        //println(shaderProgram.setUniform("shadowTexture", 3));
        shaderProgram.setUniform("shadowTextures[$index]", 14 + index);

        //println(depthMap)
    }

    fun bindCascadeDepthTexture(shaderProgram: ShaderProgram, index : Int = 0) {

        GL30.glActiveTexture(GL30.GL_TEXTURE0 + 14 + index);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, depthMap);
        //println(shaderProgram.setUniform("shadowTexture", 3));
        shaderProgram.setUniform("cascadeShadowTextures[$index]", 14 + index);

        //println(depthMap)
    }


    fun unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glViewport(0, 0, this.screenWidth, this.screenHeight);
    }

    fun cleanup() {
        unbind();
        if(this.fbo != 0) {
            GL30.glDeleteFramebuffers(this.fbo);
            GL11.glDeleteTextures(this.depthMap);
        }
        this.fbo = 0;
    }
}