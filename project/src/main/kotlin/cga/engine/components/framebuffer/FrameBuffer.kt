package cga.engine.components.framebuffer

import cga.engine.components.geometry.VertexAttribute
import cga.engine.components.shader.ShaderProgram
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL30.*

import java.lang.Exception
import java.nio.ByteBuffer

open class FrameBuffer(private val width : Int, private val height : Int, private var bIsSRGB : Boolean = false) {
    private var fbo = 0;
    private var rbo = 0;
    protected var colorBuffer0 = 0;
    protected var colorBuffer1 = 0;
    private var internalFormat = GL_RGBA16F
    lateinit var attachments : IntArray


    /* RENDER QUAD PROPERTIES */
    internal var quadVAO = 0;
    private var quadVBO = 0;
    protected var positionAtt = VertexAttribute(2, GL_FLOAT, 16, 0);
    protected var texCoordsAtt = VertexAttribute(2, GL_FLOAT, 16, 8);

    private val quadVertices = floatArrayOf(
            //Positions    //Texture Coordinates
            -1.0f,  1.0f,  0.0f, 1.0f,
            -1.0f, -1.0f,  0.0f, 0.0f,
             1.0f, -1.0f,  1.0f, 0.0f,

            -1.0f,  1.0f,  0.0f, 1.0f,
             1.0f, -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f,  1.0f, 1.0f
    )


    init {
        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        if(bIsSRGB) internalFormat = GL30.GL_SRGB8_ALPHA8

        colorBuffer0 = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorBuffer0);
        glTexImage2D(GL_TEXTURE_2D, 0,
                internalFormat, this.width, this.height, 0,
                GL_RGBA, GL_FLOAT, null as ByteBuffer?);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorBuffer0, 0);

        colorBuffer1 = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorBuffer1);
        glTexImage2D(GL_TEXTURE_2D, 0,
            internalFormat, this.width, this.height, 0,
            GL_RGBA, GL_FLOAT, null as ByteBuffer?);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_2D, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT1, GL_TEXTURE_2D, colorBuffer1, 0);

        attachments = intArrayOf(GL_COLOR_ATTACHMENT0, GL_COLOR_ATTACHMENT1)


        // render buffer
        rbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, this.width, this.height);
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_RENDERBUFFER, rbo);


        if(glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) throw Exception("Frame Buffer Not Complete");
        //else println("Frame Buffer COMPLETE");

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // quad
        quadVAO = glGenVertexArrays();
        quadVBO = glGenBuffers();
        GL30.glBindVertexArray(quadVAO);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, quadVBO)
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, quadVertices, GL30.GL_STATIC_DRAW)
        GL30.glEnableVertexAttribArray(0)
        GL30.glVertexAttribPointer(0, positionAtt.n, positionAtt.type, false, positionAtt.stride, positionAtt.offset.toLong())
        GL30.glEnableVertexAttribArray(1)
        GL30.glVertexAttribPointer(1, texCoordsAtt.n, texCoordsAtt.type, false, texCoordsAtt.stride, texCoordsAtt.offset.toLong())
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)


    }

    fun render(shaderProgram: ShaderProgram, bloomTexture: Int) {
        shaderProgram.use();
        GL30.glBindVertexArray(quadVAO);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorBuffer0);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, bloomTexture);
        (shaderProgram.setUniform("ScreenTexture", 0))
        (shaderProgram.setUniform("BloomTexture", 1))
        GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    fun bind(width : Int, height : Int) {
        glBindTexture(GL_TEXTURE_2D, 0); // make sure no texture is bound
        glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
        glDrawBuffers(attachments)
        //glDrawBuffer(GL_COLOR_ATTACHMENT0); // optional, can draw multiple attachments
        glViewport(0,0, width, height);
    }

    fun unbind(width: Int, height: Int) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
    }

    fun cleanup() {
        unbind(width, height);
        if(this.fbo != 0) {
            glDeleteFramebuffers(this.fbo);
            GL11.glDeleteTextures(this.colorBuffer0);
            glDeleteRenderbuffers(this.rbo);
        }
        this.fbo = 0;
    }

    fun setTex(tex : Int) {
        this.colorBuffer0 = tex;
    }

    fun getTexture0() = colorBuffer0
    fun getTexture1() = colorBuffer1
}