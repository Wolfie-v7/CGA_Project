package cga.game

import cga.engine.components.geometry.Renderable
import cga.utility.ModelLoader
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*

class Player(var _world: Scene, var _mesh: Renderable) : Actor(_world, _mesh) {

    private var running: Boolean = false
    private val MIN_SPEED = 0f
    private val MAX_SPEED = 500f
    private val gravity = Vector3f(0f, -0.9f, 0f)
    private var currentVelocity = Vector3f()
    init {

    }
    override fun Update(dt: Float, t: Float) {
        super.Update(dt, t)
        val terrainHeight = this.world.getWorldTerrain().getHeightAtPosition(getWorldPosition().x(), getWorldPosition().z())
        _mesh.translateGlobal(gravity)
        if (getWorldPosition().y() < terrainHeight) _mesh.translateGlobal(Vector3f(0f, terrainHeight - getWorldPosition().y(), 0f))

        addMovement(dt)

        /**
         * Update animations
         */
        playRunningAnimation(running)
        if(_world.getKeyState(GLFW_KEY_X)) _mesh.playAnimation(1)


    }

    override fun OnKey(key: Int, scancode: Int, action: Int, mode: Int) {

        when(key) {
            GLFW_KEY_K -> Destroy()

        }

    }

    override fun OnMouseButon(button: Int, action: Int, mode: Int) {
        when(button) {
            GLFW_MOUSE_BUTTON_LEFT -> _world.spawnActor(Actor(_world,
                ModelLoader.loadModel("project/assets/models/ball/ball.obj", 0f, 0f, 0f)),
                _mesh.getWorldPosition().add(Vector3f(0f, 0f, 10f)),
                Vector3f()
            )
        }
    }

    /**
     * @return the current velocity of this actor
     */
    private fun moveForward(speed: Float = 0f, direction: Int = 1) : Vector3f{
        val velocity = speed.coerceIn(MIN_SPEED .. MAX_SPEED) * direction.coerceIn(-1 .. 1)
        return Vector3f(0f, 0f, velocity)
    }

    private fun addMovement(dt: Float) {
        currentVelocity =
            if (_world.getKeyState(GLFW_KEY_W)) moveForward(10f,1)
            else if (_world.getKeyState(GLFW_KEY_S)) moveForward(2f, -1)
            else Vector3f()

        if (_world.getKeyState(GLFW_KEY_D)) _mesh.rotateLocal(0f, Math.toRadians(dt * -20.0).toFloat(), 0f)
        else if (_world.getKeyState(GLFW_KEY_A)) _mesh.rotateLocal(0f, Math.toRadians(dt * 20.0).toFloat(), 0f)

        running = currentVelocity.z() > 0f
        _mesh.translateLocal(currentVelocity.mul(-dt))
    }


    private fun attack() {}

    private fun playRunningAnimation(running: Boolean = false) {
        if (running) _mesh.playAnimation(0, 4f, loop = true)
        else _mesh.stopAnimation(0)
    }

}