package cga.exercise.components.light;

import cga.exercise.components.geometry.Transformable;
import cga.exercise.components.shader.ShaderProgram;
import org.joml.Matrix4f
import org.joml.Vector3f;


open class PointLight(var ambient : Vector3f = Vector3f(),
                      var diffuse : Vector3f = Vector3f(),
                      var specular : Vector3f = Vector3f(),
                      var _parent : Transformable? = null)
    : Transformable(Matrix4f(), _parent), IPointLight {

    protected var constant = 1.0f;
    protected var linear = 0.09f;
    protected var quadratic = 0.032f;



    override fun bind(shaderProgram: ShaderProgram, name: String) {
        shaderProgram.setUniform("$name.position", getWorldPosition());
        shaderProgram.setUniform("$name.diffuse", diffuse);
        shaderProgram.setUniform("$name.specular", specular);

        shaderProgram.setUniform("$name.constant", constant);
        shaderProgram.setUniform("$name.linear", linear);
        shaderProgram.setUniform("$name.quadratic", quadratic);
    }

    fun setAttenuation(_constant: Float, _linear: Float, _quadratic: Float) {
        constant = _constant; linear = _linear; quadratic = _quadratic;
    }
}