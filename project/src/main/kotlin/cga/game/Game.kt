package cga.game

import cga.engine.utility.GameWindow

/*
  Created by Fabian on 16.09.2017.
 */
class Game(width: Int,
           height: Int,
           fullscreen: Boolean = false,
           vsync: Boolean = false,
           title: String = "The Path v0.0.5",
           GLVersionMajor: Int = 3,
           GLVersionMinor: Int = 3) : GameWindow(width, height, fullscreen, vsync, GLVersionMajor, GLVersionMinor, title, 8, 120.0f) {

    private val scene: Scene
    init {
        setCursorVisible(false)
        scene = Scene(this)
    }

    override fun shutdown() = scene.cleanup()
    override fun start() {
        scene.BeginPlay();
    }

    override fun update(dt: Float, t: Float) = scene.update(dt, t)

    override fun render(dt: Float, t: Float) = scene.render(dt, t)

    override fun onMouseMove(xpos: Double, ypos: Double) = scene.onMouseMove(xpos, ypos)

    override fun onMouseButton(button: Int, action: Int, mode: Int) = scene.onMouseButton(button, action, mode);

    override fun onKey(key: Int, scancode: Int, action: Int, mode: Int) = scene.onKey(key, scancode, action, mode)

}