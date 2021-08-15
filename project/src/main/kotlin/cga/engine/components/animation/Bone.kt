package cga.engine.components.animation

import org.joml.Matrix4f

class Bone (var index: Int, var name: String, var inverseBindTransform: Matrix4f, var localTransform: Matrix4f = Matrix4f(), var globalTransform: Matrix4f = Matrix4f()) {
    var children = mutableListOf<Bone>()
    var parent : Bone? = null
    private var animatedTransform = Matrix4f(localTransform).mul(inverseBindTransform)

    fun getAnimatedTransform(): Matrix4f = Matrix4f(animatedTransform)
    fun setAnimatedTransform(transform: Matrix4f) {
        animatedTransform = transform
    }
    fun getInverseTransform() = Matrix4f(inverseBindTransform)
    fun getTransformLocal() = Matrix4f(localTransform)
    fun getTransformGlobal() = Matrix4f(globalTransform)


    fun addChild(child: Bone) {
        child.parent = this
        children.add(child)
    }

    fun print() {
        print("$name: [")
        for(c in children) print("${c.name}, ")
        println("]")
        for(c in children) c.print()
    }
}