package cga.framework

import cga.exercise.components.animation.Bone

data class RawMesh(
    var vertices: MutableList<Vertex> = mutableListOf(),
    var indices: MutableList<Int> = mutableListOf(),
    var materialIndex: Int = 0,
    var rootBone : Bone? = null,
    var boneCount : Int = 0
)
