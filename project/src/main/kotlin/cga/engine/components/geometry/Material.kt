package cga.engine.components.geometry

import cga.engine.components.shader.ShaderProgram
import cga.engine.components.texture.Texture2D
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.opengl.GL30

open class Material(var diff: Texture2D,
                    var emit: Texture2D,
                    var specular: Texture2D,
                    var shininess: Float = 50.0f,
                    var tcMultiplier : Vector2f = Vector2f(1.0f)) {

    open var emissionColor : Vector3f = Vector3f(1f);
    var normalMap: Texture2D = Texture2D("project/assets/textures/TerrainTextures/default_norm.png", false);

    init {
    }
    constructor(diff: Texture2D,
                emit: Texture2D,
                specular: Texture2D,
                normalMap: Texture2D,
                shininess: Float,
                tcMultiplier: Vector2f) : this(diff, emit, specular, shininess, tcMultiplier) {
        this.normalMap = normalMap;
    }


    fun bind(shaderProgram: ShaderProgram) {
        // todo 3.2
        diff.bind(GL30.GL_TEXTURE0 + 0);
        emit.bind(GL30.GL_TEXTURE0 + 1);
        specular.bind(GL30.GL_TEXTURE0 + 2);
        normalMap.bind(GL30.GL_TEXTURE0 + 3);

        shaderProgram.setUniform("tcMul", tcMultiplier);
        shaderProgram.setUniform("material.diffuse", 0);
        shaderProgram.setUniform("material.emission", 1);
        shaderProgram.setUniform("material.specular", 2);
        normalMap.let { shaderProgram.setUniform("material.norm", 3);}


        // for shadows on transparent textures
        (shaderProgram.setUniform("tex", 0))

        shaderProgram.setUniform("material.shininess", shininess);
        shaderProgram.setUniform("material.emissionColor", emissionColor);
    }

    fun cleanup() {
        diff.cleanup(); emit.cleanup(); specular.cleanup();
    }
}