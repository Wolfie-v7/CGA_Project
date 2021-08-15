package cga.engine.components.terrain

import cga.engine.components.geometry.Material
import cga.engine.components.shader.ShaderProgram
import cga.engine.components.texture.Texture2D

import org.lwjgl.opengl.GL30

class TerrainMaterial(var blendMap : Texture2D,
                      private val rMaterial: Material,
                      private val gMaterial: Material,
                      private val bMaterial: Material,
                      private val backgroundMat : Material) {
    private final val texUnit = GL30.GL_TEXTURE0;
    private var increment = 0;
    private val noiseTexture = Texture2D("project/assets/textures/TerrainTextures/noiseTexture.png", false);


    fun bind(shaderProgram: ShaderProgram) {
        increment = 0;
        rMaterial.diff.bind(texUnit + 0);
        rMaterial.specular.bind(texUnit + 1);
        gMaterial.diff.bind(texUnit + 2);
        gMaterial.specular.bind(texUnit + 3);
        bMaterial.diff.bind(texUnit + 4);
        bMaterial.specular.bind(texUnit + 5);
        backgroundMat.diff.bind(texUnit + 6);
        backgroundMat.specular.bind(texUnit + 7);
        rMaterial.normalMap.bind(texUnit + 8)
        gMaterial.normalMap.bind(texUnit + 9)
        bMaterial.normalMap.bind(texUnit + 10)
        backgroundMat.normalMap.bind(texUnit + 11)
        blendMap.bind(texUnit + 12);
        noiseTexture.bind(texUnit + 13);

        (shaderProgram.setUniform("rMaterial.diffuse", 0));
        (shaderProgram.setUniform("rMaterial.specular", 1));
        (shaderProgram.setUniform("gMaterial.diffuse", 2));
        (shaderProgram.setUniform("gMaterial.specular", 3));
        (shaderProgram.setUniform("bMaterial.diffuse", 4));
        (shaderProgram.setUniform("bMaterial.specular", 5));
        (shaderProgram.setUniform("backgroundMat.diffuse", 6));
        (shaderProgram.setUniform("backgroundMat.specular", 7));
        (shaderProgram.setUniform("rMaterial.normal", 8));
        (shaderProgram.setUniform("gMaterial.normal", 9));
        (shaderProgram.setUniform("bMaterial.normal", 10));
        (shaderProgram.setUniform("backgroundMat.normal", 11));

        (shaderProgram.setUniform("rMaterial.tcMul", rMaterial.tcMultiplier));
        (shaderProgram.setUniform("rMaterial.shininess", rMaterial.shininess));
        (shaderProgram.setUniform("gMaterial.tcMul", gMaterial.tcMultiplier));
        (shaderProgram.setUniform("gMaterial.shininess", gMaterial.shininess));
        (shaderProgram.setUniform("bMaterial.tcMul", bMaterial.tcMultiplier));
        (shaderProgram.setUniform("bMaterial.shininess", bMaterial.shininess));
        (shaderProgram.setUniform("backgroundMat.tcMul", backgroundMat.tcMultiplier));
        (shaderProgram.setUniform("backgroundMat.shininess", backgroundMat.shininess));


        //bindMaterial("backgroundMat", backgroundMat, shaderProgram);
        //bindMaterial("rMaterial", rMaterial, shaderProgram);
        //bindMaterial("gMaterial", gMaterial, shaderProgram);
        //bindMaterial("bMaterial", bMaterial, shaderProgram);


        (shaderProgram.setUniform("blendMap", 12));
        (shaderProgram.setUniform("noiseTexture", 13));

    }

    private fun bindMaterial(name : String, material : Material, shaderProgram: ShaderProgram) {
        material.diff.bind(texUnit+ increment); println(increment)
        shaderProgram.setUniform("$name.diffuse", increment); increment++
        material.specular.bind(texUnit + increment);println(increment)
        shaderProgram.setUniform("$name.specular", increment); increment++
        (shaderProgram.setUniform("$name.tcMul", material.tcMultiplier));
        (shaderProgram.setUniform("$name.shininess", material.shininess));
        /*material.emit.bind(texUnit + increment);println(increment)
        shaderProgram.setUniform("$name.emission", increment); increment++


        (shaderProgram.setUniform("$name.emissionColor", material.emissionColor));*/
    }

    fun cleanup() {
        blendMap.cleanup();
        rMaterial.cleanup();
        gMaterial.cleanup();
        bMaterial.cleanup();
        backgroundMat.cleanup();
    }
}