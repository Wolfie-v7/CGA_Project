package cga.engine.components.shader

import org.joml.*
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import java.nio.FloatBuffer
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by Fabian on 16.09.2017.
 */
class ShaderProgram(vertexShaderPath: String, fragmentShaderPath: String) {
    var programID: Int = 0
    //Matrix buffers for setting matrix uniforms. Prevents allocation for each uniform
    private val m4x4buf : FloatBuffer = BufferUtils.createFloatBuffer(16)

    // Vector buffers
    private val vec2buf : FloatBuffer = BufferUtils.createFloatBuffer(2);
    private val vec3buf : FloatBuffer = BufferUtils.createFloatBuffer(3);
    private val vec4buf : FloatBuffer = BufferUtils.createFloatBuffer(4);
    /**
     * Sets the active shader program of the OpenGL render pipeline to this shader
     * if this isn't already the currently active shader
     */
    fun use() {
        val curprog = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)
        if (curprog != programID) GL20.glUseProgram(programID)

    }

    /**
     * Frees the allocated OpenGL objects
     */
    fun cleanup() {
        GL20.glDeleteProgram(programID)
    }

    //setUniform() functions are added later during the course
    // float vector uniforms
    /**
     * Sets a single float uniform
     * @param name  Name of the uniform variable in the shader
     * @param value Value
     * @return returns false if the uniform was not found in the shader
     */
    fun setUniform(name: String, value: Float): Boolean {
        if (programID == 0) return false
        val loc = GL20.glGetUniformLocation(programID, name)
        if (loc != -1) {
            GL20.glUniform1f(loc, value)
            return true
        }
        return false
    }

    fun setUniform(name: String, value: Matrix4f, transpose: Boolean) : Boolean
    {
        if (programID == 0) return false;
        val loc = GL30.glGetUniformLocation(programID, name);
        value.get(m4x4buf);
        if(loc != -1)
        {
            GL30.glUniformMatrix4fv(loc, transpose, m4x4buf);
            return true;
        }
        return false;
    }

    fun setUniform(name: String, value: Int): Boolean {
        if (programID == 0) { println("Program ID is invalid."); return false}
        val loc = GL20.glGetUniformLocation(programID, name)
        if (loc != -1) {
            GL20.glUniform1i(loc, value)
            return true
        }
        return false
    }

    fun setUniform(name: String, value: Vector2f): Boolean {
        if (programID == 0) return false;
        val loc = GL20.glGetUniformLocation(programID, name);
        value.get(vec2buf)
        if (loc != -1) {
            GL20.glUniform2fv(loc, vec2buf);
            return true
        }
        return false

    }

    fun setUniform(name: String, value: Vector3f) : Boolean {
        if (programID == 0) return false;
        val loc = GL20.glGetUniformLocation(programID, name);
        value.get(vec3buf);
        if(loc != -1) {
            GL20.glUniform3fv(loc, vec3buf);
            return true;
        }
        return false;
    }

    fun setUniform(name: String, value: Vector4f) : Boolean {
        if (programID == 0) return false;
        val loc = GL20.glGetUniformLocation(programID, name);
        value.get(vec4buf);
        if(loc != -1) {
            GL20.glUniform4fv(loc, vec4buf);
            return true;
        }
        return false;
    }

    fun setUniform(name: String, value: Boolean) : Boolean{
        if (programID == 0) { println("Program ID is invalid."); return false}
        val loc = GL20.glGetUniformLocation(programID, name)
        if (loc != -1) {
            val v = if(value) 1 else 0;
            GL20.glUniform1i(loc, v)
            return true
        }
        return false
    }


    /**
     * Creates a shader object from vertex and fragment shader paths
     * @param vertexShaderPath      vertex shader path
     * @param fragmentShaderPath    fragment shader path
     * @throws Exception if shader compilation failed, an exception is thrown
     */
    init {
        val vPath = Paths.get(vertexShaderPath)
        val fPath = Paths.get(fragmentShaderPath)
        val vSource = String(Files.readAllBytes(vPath))
        val fSource = String(Files.readAllBytes(fPath))
        val vShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER)
        if (vShader == 0) throw Exception("Vertex shader object couldn't be created.")
        val fShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER)
        if (fShader == 0) {
            GL20.glDeleteShader(vShader)
            throw Exception("Fragment shader object couldn't be created.")
        }
        GL20.glShaderSource(vShader, vSource)
        GL20.glShaderSource(fShader, fSource)
        GL20.glCompileShader(vShader)
        if (GL20.glGetShaderi(vShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            val log = GL20.glGetShaderInfoLog(vShader)
            GL20.glDeleteShader(fShader)
            GL20.glDeleteShader(vShader)
            throw Exception("Vertex shader compilation failed:\n$log")
        }
        GL20.glCompileShader(fShader)
        if (GL20.glGetShaderi(fShader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            val log = GL20.glGetShaderInfoLog(fShader)
            GL20.glDeleteShader(fShader)
            GL20.glDeleteShader(vShader)
            throw Exception("Fragment shader compilation failed:\n$log")
        }
        programID = GL20.glCreateProgram()
        if (programID == 0) {
            GL20.glDeleteShader(vShader)
            GL20.glDeleteShader(fShader)
            throw Exception("Program object creation failed.")
        }
        GL20.glAttachShader(programID, vShader)
        GL20.glAttachShader(programID, fShader)
        GL20.glLinkProgram(programID)
        if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            val log = GL20.glGetProgramInfoLog(programID)
            GL20.glDetachShader(programID, vShader)
            GL20.glDetachShader(programID, fShader)
            GL20.glDeleteShader(vShader)
            GL20.glDeleteShader(fShader)
            throw Exception("Program linking failed:\n$log")
        }
        GL20.glDetachShader(programID, vShader)
        GL20.glDetachShader(programID, fShader)
        GL20.glDeleteShader(vShader)
        GL20.glDeleteShader(fShader)
    }
}