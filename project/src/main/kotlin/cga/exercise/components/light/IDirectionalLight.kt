package cga.exercise.components.light

import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f

interface IDirectionalLight : ILight {
    fun bind(shaderProgram: ShaderProgram, name: String, viewMatrix : Matrix4f);
}