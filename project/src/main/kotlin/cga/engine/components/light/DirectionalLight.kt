package cga.engine.components.light

import cga.engine.components.geometry.Transformable
import cga.engine.components.shader.ShaderProgram
import cga.engine.components.texture.Texture2D
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f

class DirectionalLight(val ambient : Vector3f,
                       val diffuse : Vector3f,
                       val specular : Vector3f,
                       internal var direction : Vector3f) : Transformable(Matrix4f(), null),IDirectionalLight {
    override fun bind(shaderProgram: ShaderProgram, name: String, viewMatrix : Matrix4f) {
        var viewDirection = Vector4f(direction, 0.0f).mul(viewMatrix);
        shaderProgram.setUniform("$name.direction", Vector3f(viewDirection.x, viewDirection.y, viewDirection.z));
        //shaderProgram.setUniform("$name.direction", direction);
        shaderProgram.setUniform("$name.ambient", ambient);
        shaderProgram.setUniform("$name.diffuse", diffuse);
        shaderProgram.setUniform("$name.specular", specular);

    }

    //val position: Vector3f? = direction.mul(10f);
    override var bIsSpotLight = false;
}