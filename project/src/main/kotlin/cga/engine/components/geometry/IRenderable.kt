package cga.engine.components.geometry

import cga.engine.components.shader.ShaderProgram

interface IRenderable {
    fun render(shaderProgram: ShaderProgram)
    fun destroy()
    fun update(dt: Float)
}