package cga.game

import cga.engine.components.geometry.*
import cga.engine.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f

open class Actor(var world : Scene, var Mesh : IRenderable?, var count: Int = 1, val bIsInstanced: Boolean = false, var CameraArm: Transformable? = null) {
    private var SceneRoot : Transformable = Transformable();
    open var collisionMesh : Mesh? = null;
    //var WorldPosition = SceneRoot.getWorldPosition();

    open var bIsVisible = true;
    open var bIsStatic = false;
    open var moving: Boolean = false;

    open fun BeginPlay() {};
    open fun Tick(dt : Float) {};
    open fun OnDestroy() {}

    init {
        Mesh = if(bIsInstanced) RenderableInstance(Mesh as Renderable);
        else Mesh as Renderable;
        //world.registerActor(this);
    }

    open fun Destroy() {
            OnDestroy();
            world.removeActor(this);
            Mesh?.destroy()
    }
    open fun Render(shaderProgram: ShaderProgram) { if(bIsVisible) Mesh?.render(shaderProgram) }
    open fun Update(dt : Float, t : Float) {
        Mesh?.update(dt, t)

    }
    open fun setSceneRoot(newRoot : Transformable) { SceneRoot = newRoot }
    open fun getSceneRoot() = SceneRoot;
    fun getModelMatrix(): Matrix4f {
        val mesh = Mesh as Renderable
        return mesh.getWorldModelMatrix()
    }

    open fun OnKey(key: Int, scancode: Int, action: Int, mode: Int) {}
    open fun OnMouseButton(button: Int, action: Int, mode: Int) {}
    open fun OnMouseMove(xpos: Double, ypos: Double) {}

    fun getWorldPosition(): Vector3f {
        return SceneRoot.getWorldPosition()
    }


}