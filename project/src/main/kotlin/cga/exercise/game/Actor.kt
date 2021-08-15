package cga.exercise.game

import cga.exercise.components.geometry.*
import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f

abstract class Actor(var world : Scene, var Mesh : IRenderable?, count: Int = 1, val bIsInstanced: Boolean = false) {
    private var SceneRoot : Transformable = Transformable();
    open var collisionMesh : Mesh? = null;
    //var WorldPosition = SceneRoot.getWorldPosition();

    open var bIsVisible = true;
    open var bIsStatic = false;

    open fun BeginPlay() {};
    open fun Tick(dt : Float) {};
    open fun OnDestroy() {}

    init {
        Mesh = if(bIsInstanced) RenderableInstance(Mesh as Renderable);
        else Mesh as Renderable;
        //world.registerActor(this);
    }

    open fun Destroy() {
        if(world.contains(this)) {
            OnDestroy(); world.removeActor(this); Mesh?.destroy()
        }
    }
    open fun Render(shaderProgram: ShaderProgram) { if(bIsVisible) Mesh?.render(shaderProgram) }
    open fun Update(dt : Float, t : Float) { Mesh?.update(dt) }
    open fun setSceneRoot(newRoot : Transformable) { SceneRoot = newRoot }
    open fun getSceneRoot() = SceneRoot;
    fun getModelMatrix(): Matrix4f {
        val mesh = Mesh as Renderable
        return mesh.getWorldModelMatrix()
    }

    fun getWorldPosition(): Vector3f {
        return SceneRoot.getWorldPosition()
    }


}