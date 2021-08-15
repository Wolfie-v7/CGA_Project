package cga.exercise.components.water

import cga.exercise.components.geometry.VertexAttribute
import cga.exercise.components.shader.ShaderProgram
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT
import org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT
import org.lwjgl.opengl.GL30
import java.lang.Exception
import java.nio.ByteBuffer

class WaterFrameBuffer(var width: Int, var height: Int) {
    private var fbo = 0;
    protected var colorTexture = 0;
    private var depthTexture = 0
    private var rbo = 0;


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

        colorTexture = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorTexture);
        GL30.glTexImage2D(
            GL30.GL_TEXTURE_2D, 0,
            GL30.GL_RGB, this.width, this.height, 0,
            GL30.GL_RGB, GL30.GL_UNSIGNED_BYTE, null as ByteBuffer?
        );
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0);
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_COLOR_ATTACHMENT0,
            GL30.GL_TEXTURE_2D,
            colorTexture,
            0
        );

        // Depth buffer
        depthTexture = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, depthTexture);
        GL30.glTexImage2D(
            GL30.GL_TEXTURE_2D, 0,
            GL30.GL_DEPTH_COMPONENT32, this.width, this.height, 0,
            GL30.GL_DEPTH_COMPONENT, GL30.GL_FLOAT, null as ByteBuffer?
        );
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_LINEAR);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_LINEAR);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0);
        GL30.glFramebufferTexture2D(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_DEPTH_ATTACHMENT,
            GL30.GL_TEXTURE_2D,
            depthTexture,
            0
        );

        // Render Buffer
        rbo = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbo);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT, this.width, this.height);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
        GL30.glFramebufferRenderbuffer(
            GL30.GL_FRAMEBUFFER,
            GL30.GL_DEPTH_ATTACHMENT,
            GL30.GL_RENDERBUFFER,
            rbo
        );


        if(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) throw Exception("Frame Buffer Not Complete");
        //else println("Frame Buffer COMPLETE");

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);


        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) throw Exception("Frame Buffer Not Complete");
        //else println("Frame Buffer COMPLETE");

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        // quad
        quadVAO = GL30.glGenVertexArrays();
        quadVBO = GL30.glGenBuffers();
        GL30.glBindVertexArray(quadVAO);
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, quadVBO)
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, quadVertices, GL30.GL_STATIC_DRAW)
        GL30.glEnableVertexAttribArray(0)
        GL30.glVertexAttribPointer(
            0,
            positionAtt.n,
            positionAtt.type,
            false,
            positionAtt.stride,
            positionAtt.offset.toLong()
        )
        GL30.glEnableVertexAttribArray(1)
        GL30.glVertexAttribPointer(
            1,
            texCoordsAtt.n,
            texCoordsAtt.type,
            false,
            texCoordsAtt.stride,
            texCoordsAtt.offset.toLong()
        )
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0)
        GL30.glBindVertexArray(0)
    }

    fun bind(width : Int, height : Int) {
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, 0); // make sure no texture is bound
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.fbo);
        GL30.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0); // optional, can draw multiple attachments
        GL30.glViewport(0, 0, width, height);
        GL30.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    fun unbind(width: Int, height: Int) {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glViewport(0, 0, width, height);
    }

    fun cleanup() {
        unbind(width, height);
        if(this.fbo != 0) {
            GL30.glDeleteFramebuffers(this.fbo);
            GL11.glDeleteTextures(this.colorTexture);
            GL11.glDeleteTextures(this.depthTexture);
            GL30.glDeleteRenderbuffers(this.rbo);
        }
        this.fbo = 0;
        this.depthTexture = 0
        this.colorTexture = 0
        this.rbo = 0
    }

    fun render(shaderProgram: ShaderProgram) {
        shaderProgram.use();
        GL30.glBindVertexArray(quadVAO);
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorTexture);
        GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, 6);
        GL30.glBindVertexArray(0);
    }

    fun getTexture() = colorTexture;
    fun getDepthTexture() = depthTexture
}