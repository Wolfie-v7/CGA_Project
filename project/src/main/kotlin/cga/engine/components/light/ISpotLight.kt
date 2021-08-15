package cga.engine.components.light

import cga.engine.components.shader.ShaderProgram
import org.joml.Matrix4f

interface ISpotLight : ILight {
    fun bind(shaderProgram: ShaderProgram, name: String, viewMatrix: Matrix4f)
}