package cga.engine.components.geometry

import cga.engine.components.animation.Animation
import cga.engine.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30

class Renderable(var MeshList : MutableList<Mesh>, var _modelMatrix : Matrix4f = Matrix4f(), var _parent : Transformable? = null)
    : IRenderable, Transformable(_modelMatrix, _parent)
{

    val animations = mutableListOf<Animation>()
    var currentAnimation = -1

    override fun render(shaderProgram: ShaderProgram) {

        shaderProgram.setUniform("model_matrix", getWorldModelMatrix(), false);
        //println(animations.size)
        for (mesh in MeshList) {
            if(mesh.bHasTransparency) GL30.glDisable(GL30.GL_CULL_FACE);
            mesh.render(shaderProgram)
            GL11.glEnable(GL11.GL_CULL_FACE)
            GL11.glCullFace(GL11.GL_BACK)
        };
    }

    override fun destroy() {
        for (mesh in MeshList) mesh.cleanup()
    }

    fun renderNoMat(shaderProgram: ShaderProgram) {
        shaderProgram.setUniform("model_matrix", getWorldModelMatrix(), false);
        for (mesh in MeshList) {
            if(mesh.bHasTransparency) GL30.glDisable(GL30.GL_CULL_FACE);
            mesh.renderNoMat(shaderProgram)
            GL11.glEnable(GL11.GL_CULL_FACE)
            GL11.glCullFace(GL11.GL_BACK)
        };
    }

    override fun update(dt : Float) {
        for (mesh in MeshList) mesh.update(dt)
    }

    fun playAnimation(index: Int, playbackSpeed: Float = 1f, loop: Boolean = false) {
        if (index !in animations.indices) return
        currentAnimation = index
        for (m in MeshList) m.playAnimation(animations[index] ?: null, playbackSpeed, loop)
    }

    fun stopAnimation() {
        currentAnimation = -1
        for (m in MeshList) m.stopAnimation()
    }

    fun stopAnimation(index: Int) {
        if (currentAnimation == index) {
            currentAnimation = -1
            for (m in MeshList) m.stopAnimation()
        }
    }

    fun addAnimation(animation: Animation) = animations.add(animation)
    fun addAnimation(animations: MutableList<Animation>) : Boolean {
        for (a in animations) if(!this.animations.add(a)) return false
        return true
    }

    fun setDrawLine(line: Boolean) {
        for (mesh in MeshList) mesh.bDrawLines = line
    }

    fun setMaterial(material: Material) {
        for (mesh in MeshList) mesh.material = material
    }
}