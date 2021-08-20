package cga.game

import cga.engine.components.geometry.Renderable
import org.joml.Vector3f

class GameLock(private val _world: Scene,
               val _mesh: Renderable,
               var type: GameKey.KeyType = GameKey.KeyType.NONE,
               override var bIsPickable: Boolean = false
): IGameItem, Actor(_world, _mesh) {
    private var activated: Boolean = false;
    private var mt = 0f
    private val gravity = Vector3f(0f, -0.9f, 0f)

    init {
        if(!activated) _mesh.MeshList[0].material?.emissionColor = Vector3f(0.01f)
    }

    override fun Update(dt: Float, t: Float) {
        super.Update(dt, t)

        val terrainHeight = this.world.getWorldTerrain().getHeightAtPosition(getWorldPosition().x(), getWorldPosition().z())
        _mesh.translateGlobal(gravity)
        if (getWorldPosition().y() < terrainHeight) _mesh.translateGlobal(Vector3f(0f, terrainHeight - getWorldPosition().y(), 0f))
        checkActivated(dt)

    }

    fun activate(activate: Boolean) {
        if (activated != activate) {
            //mt = 0f
            activated = activate
        }
    }

    fun checkActivated(dt: Float) {
        if (activated) {
            if (mt < 1f) {
                mt += dt * 0.2f
                mt = mt.coerceIn(0f, 1f)
                _mesh.MeshList[0].material?.emissionColor?.lerp(Vector3f(1f), mt)
            }

        }
        else {
            mt -= dt * 0.5f
            mt = mt.coerceIn(0f, 1f)
            _mesh.MeshList[0].material?.emissionColor?.lerp(Vector3f(0f), 1f - mt)
        }
    }

}