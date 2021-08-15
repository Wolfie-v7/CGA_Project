package cga.exercise.components.framebuffer

import cga.exercise.components.geometry.VertexAttribute
import cga.exercise.components.shader.ShaderProgram
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import java.lang.Exception
import java.nio.ByteBuffer

class PingPongBuffer(private val width : Int, private val height : Int) {
    private var fbo0 = 0;
    private var fbo1 = 0;
    private var rbo = 0;
    protected var colorBuffer0 = 0;
    protected var colorBuffer1 = 0;
    private var internalFormat = GL30.GL_RGBA16F

    private val frameBuffers = arrayOf(fbo0, fbo1)
    private val colorAttachments = arrayOf(colorBuffer0, colorBuffer1)
    private var blurTexture = 0;


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
        fbo0 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo0);

        colorBuffer0 = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorBuffer0);
        GL30.glTexImage2D(
            GL30.GL_TEXTURE_2D, 0,
            internalFormat, this.width, this.height, 0,
            GL30.GL_RGBA, GL30.GL_FLOAT, null as ByteBuffer?
        );
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0);
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_COLOR_ATTACHMENT0,
            GL30.GL_TEXTURE_2D,
            colorBuffer0,
            0
        );

        fbo1 = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo1);

        colorBuffer1 = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorBuffer1);
        GL30.glTexImage2D(
            GL30.GL_TEXTURE_2D, 0,
            internalFormat, this.width, this.height, 0,
            GL30.GL_RGBA, GL30.GL_FLOAT, null as ByteBuffer?
        );
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0);
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_COLOR_ATTACHMENT0,
            GL30.GL_TEXTURE_2D,
            colorBuffer1,
            0
        );


        // render buffer
        rbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbo);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, this.width, this.height);
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

    fun render(shaderProgram: ShaderProgram, baseTexture : Int, iterations : Int = 2) {
        var horizonal : Boolean = true
        var firstIteration: Boolean = true
        shaderProgram.use();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0)
        for(i in 0 until iterations) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, if(horizonal) fbo1 else fbo0)
            shaderProgram.setUniform("horizontal", horizonal)
            GL30.glBindVertexArray(quadVAO);
            GL30.glActiveTexture(GL30.GL_TEXTURE0);
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, if(firstIteration) baseTexture else {
                if (horizonal) colorBuffer0 else colorBuffer1
            })

            GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, 6);
            GL30.glBindVertexArray(0);

            horizonal = !horizonal

            if(firstIteration) firstIteration = !firstIteration

        }
        //blurTexture = colorAttachments[if(horizonal) 0 else 1]
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)

    }

    fun renderBloom(shaderProgram: ShaderProgram, tex: Int) {
        shaderProgram.use();
        GL30.glBindVertexArray(quadVAO);
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, tex);
        //shaderProgram.setUniform("ScreenTexture", 0)
        //shaderProgram.setUniform("BloomTexture", 1)
        GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    fun bind(width : Int, height : Int) {
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0); // make sure no texture is bound
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fbo0);
        //glDrawBuffer(GL_COLOR_ATTACHMENT0); // optional, can draw multiple attachments
        GL30.glViewport(0, 0, width, height);
    }

    fun unbind(width: Int, height: Int) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glViewport(0, 0, width, height);
    }

    fun cleanup() {
        unbind(width, height);
        if(this.fbo0 != 0) {
            GL30.glDeleteFramebuffers(this.fbo0);
            GL11.glDeleteTextures(this.colorBuffer0);
            GL30.glDeleteRenderbuffers(this.rbo);
        }
        this.fbo0 = 0;
    }

    fun setTex(tex : Int) {
        this.colorBuffer0 = tex;
    }

    fun getBlurTexture() = colorBuffer1

}