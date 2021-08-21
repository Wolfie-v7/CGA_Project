package cga.game

import cga.engine.components.geometry.Renderable
import org.joml.Vector3f

class GameKey(private val _world: Scene, val _mesh: Renderable, var type: KeyType = KeyType.NONE,
              override var bIsPickable: Boolean = true
) : IGameItem, Actor(_world, _mesh) {
    enum class KeyType() { NONE, WOLF, BEAR, RABBIT }
    val gravity = Vector3f(0f, -0.5f, 0f)
    private var currentVelocity = Vector3f()
    private var mt = 0f

    init {

    }
    override fun Update(dt: Float, t: Float) {
        super.Update(dt, t)
        val terrainHeight = this.world.getWorldTerrain().getHeightAtPosition(getWorldPosition().x(), getWorldPosition().z())
        _mesh.translateGlobal(gravity)
        mt += dt
        mt.coerceIn(0f, 1f)
        addMovement(mt)
        if (getWorldPosition().y() < terrainHeight) _mesh.translateGlobal(Vector3f(0f, terrainHeight - getWorldPosition().y(), 0f))
        val player = _world.getPlayer()
        if (checkPlayerInRange(player)) {
            player?.setItem(this)
        }
        else {
            if (player?.getItem()?.equals(this) == true) player.setItem(null)
        }

        val lock = _world.getLock(this.type)
        if (checkLock(lock)) {
            //println("unlocked")
            lock?.activate(true)
        }
        else lock?.activate(false)
    }

    private fun checkPlayerInRange(player: Player?): Boolean {
        val vec = player?.getWorldPosition()?.sub(getWorldPosition())
        val playerForward = player?._mesh?.getZAxis()?.negate() ?: Vector3f()
        val dot = vec?.dot(playerForward) ?: 0f
        val distance = vec?.length() ?: 999f
        return distance <= 10f && dot < 0f
    }

    fun move(velocity: Vector3f) {
        //println("vel: $velocity")
        currentVelocity = currentVelocity.add(velocity, Vector3f())
        mt = 0f
    }

    private fun addMovement(dt: Float) {
        if (currentVelocity == Vector3f()) return
        currentVelocity.lerp(Vector3f(), dt)
        _mesh.translateGlobal(currentVelocity.mul(dt, Vector3f()))
    }

    private fun checkLock(lock: GameLock?): Boolean {
        if (lock == null) return false
        val distance = lock.getWorldPosition().sub(getWorldPosition()).length()
        return distance <= 2f
    }
}


