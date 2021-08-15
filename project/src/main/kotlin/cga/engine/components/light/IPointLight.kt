package cga.engine.components.light

import cga.engine.components.shader.ShaderProgram

interface IPointLight {
    fun bind(shaderProgram: ShaderProgram, name: String)
}