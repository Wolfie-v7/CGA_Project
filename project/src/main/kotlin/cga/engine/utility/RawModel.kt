package cga.engine.utility

data class RawModel(
        var meshes: MutableList<RawMesh> = mutableListOf(),
        var materials: MutableList<RawMaterial> = mutableListOf(),
        var textures: MutableList<String> = mutableListOf()
)