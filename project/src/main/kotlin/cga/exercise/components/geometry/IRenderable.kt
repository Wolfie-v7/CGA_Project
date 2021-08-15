package cga.exercise.components.geometry

import cga.exercise.components.shader.ShaderProgram

interface IRenderable {
    fun render(shaderProgram: ShaderProgram)
    fun destroy()
    fun update(dt: Float)
}