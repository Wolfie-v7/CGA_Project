package cga.engine.components.framebuffer

import cga.engine.components.geometry.VertexAttribute
import cga.engine.components.shader.ShaderProgram
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_TRUE
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL32
import java.lang.Exception
import java.nio.ByteBuffer

class MultSampFrameBuffer(private val width : Int, private val height : Int, private var bIsSRGB : Boolean = false, samples: Int = 4) {
    private var fbo = 0;
    private var rbo = 0;
    protected var colorBuffer0 = 0;
    protected var colorBuffer1 = 0;
    private var internalFormat = GL30.GL_RGBA16F
    lateinit var attachments : IntArray


    /* RENDER QUAD PROPERTIES */
    internal var quadVAO = 0;
    private var quadVBO = 0;
    protected var positionAtt = VertexAttribute(2, GL30.GL_FLOAT, 16, 0);
    protected var texCoordsAtt = VertexAttribute(2, GL30.GL_FLOAT, 16, 8);

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
        fbo = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        if(bIsSRGB) internalFormat = GL30.GL_SRGB8_ALPHA8

        colorBuffer0 = GL30.glGenTextures();
        GL30.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, colorBuffer0);
        GL32.glTexImage2DMultisample(
            GL32.GL_TEXTURE_2D_MULTISAMPLE, samples,
            internalFormat, this.width, this.height, true
        );
        GL30.glTexParameteri(GL32.GL_TEXTURE_2D_MULTISAMPLE, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL32.GL_TEXTURE_2D_MULTISAMPLE, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL32.GL_TEXTURE_2D_MULTISAMPLE, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL32.GL_TEXTURE_2D_MULTISAMPLE, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GL30.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, 0);
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_COLOR_ATTACHMENT0,
            GL32.GL_TEXTURE_2D_MULTISAMPLE,
            colorBuffer0,
            0
        );

        colorBuffer1 = GL30.glGenTextures();
        GL30.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, colorBuffer1);
        GL32.glTexImage2DMultisample(
            GL32.GL_TEXTURE_2D_MULTISAMPLE, samples,
            internalFormat, this.width, this.height, true
        );
        GL30.glTexParameteri(GL32.GL_TEXTURE_2D_MULTISAMPLE, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL32.GL_TEXTURE_2D_MULTISAMPLE, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL32.GL_TEXTURE_2D_MULTISAMPLE, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL32.GL_TEXTURE_2D_MULTISAMPLE, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GL30.glBindTexture(GL32.GL_TEXTURE_2D_MULTISAMPLE, 0);
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_COLOR_ATTACHMENT1,
            GL32.GL_TEXTURE_2D_MULTISAMPLE,
            colorBuffer1,
            0
        );

        attachments = intArrayOf(GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1)


        // render buffer
        rbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbo);
        GL32.glRenderbufferStorageMultisample(GL30.GL_RENDERBUFFER, samples, GL30.GL_DEPTH24_STENCIL8, this.width, this.height);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        GL30.glFramebufferRenderbuffer(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_DEPTH_STENCIL_ATTACHMENT,
            GL30.GL_RENDERBUFFER,
            rbo
        );


        if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) throw Exception("Frame Buffer Not Complete");
        //else println("Frame Buffer COMPLETE");

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        // quad
        quadVAO = GL30.glGenVertexArrays();
        quadVBO = GL30.glGenBuffers();
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
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorBuffer0);
        GL30.glActiveTexture(GL30.GL_TEXTURE1);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, bloomTexture);
        (shaderProgram.setUniform("ScreenTexture", 0))
        (shaderProgram.setUniform("BloomTexture", 1))
        GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    fun bind(width : Int, height : Int) {
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0); // make sure no texture is bound
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fbo);
        GL30.glDrawBuffers(attachments)
        //glDrawBuffer(GL_COLOR_ATTACHMENT0); // optional, can draw multiple attachments
        GL30.glViewport(0, 0, width, height);
    }

    fun unbind(width: Int, height: Int) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glViewport(0, 0, width, height);
    }

    fun blit(to: FrameBuffer) {
        GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, fbo)
        GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, to.getId())

        GL32.glReadBuffer(attachments[0])
        GL32.glDrawBuffer(to.attachments[0])
        GL32.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL32.GL_COLOR_BUFFER_BIT, GL32.GL_NEAREST)

        GL32.glReadBuffer(attachments[1])
        GL32.glDrawBuffer(attachments[1])
        GL32.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL32.GL_COLOR_BUFFER_BIT, GL32.GL_NEAREST)
    }

    fun cleanup() {
        unbind(width, height);
        if(this.fbo != 0) {
            GL30.glDeleteFramebuffers(this.fbo);
            GL11.glDeleteTextures(this.colorBuffer0);
            GL30.glDeleteRenderbuffers(this.rbo);
        }
        this.fbo = 0;
    }

    fun setTex(tex : Int) {
        this.colorBuffer0 = tex;
    }

    fun getTexture0() = colorBuffer0
    fun getTexture1() = colorBuffer1
}