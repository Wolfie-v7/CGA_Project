package cga.game

import cga.engine.components.geometry.Renderable
import org.joml.Vector3f

class Player(var _world: Scene, var _mesh: Renderable) : Actor(_world, _mesh) {

    private val gravity = Vector3f(0f, -0.9f, 0f)
    init {

    }
    override fun Update(dt: Float, t: Float) {
        super.Update(dt, t)
        val terrainHeight = this.world.getWorldTerrain().getHeightAtPosition(getWorldPosition().x(), getWorldPosition().z())
        _mesh.translateGlobal(gravity)
        if (getWorldPosition().y() < terrainHeight) _mesh.translateGlobal(Vector3f(0f, terrainHeight - getWorldPosition().y(), 0f))

    }
}