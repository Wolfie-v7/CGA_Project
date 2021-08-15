package cga.engine.components.light

import cga.engine.components.geometry.Transformable
import cga.engine.components.shader.ShaderProgram
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.acos

/**
 * @property direction: where the Spotlight is pointing at
 * @property innerCutoff: the Cos of the angle the Spotlight makes before the light starts falling off
 * @property outerCutoff: the Cos of the angle the Spotlight makes before the light disappears
 */
class SpotLight(var direction : Vector3f = Vector3f(),
                private var innerCutoff : Float = 0.0f,
                private var outerCutoff : Float = 0.0f,
                var _ambient : Vector3f = Vector3f(),
                var _diffuse : Vector3f = Vector3f(),
                private var _specular : Vector3f = Vector3f(),
                var __parent : Transformable? = null) : PointLight(_ambient, _diffuse, _specular, __parent), ISpotLight {

    override var bIsSpotLight: Boolean = true;

    override fun bind(shaderProgram: ShaderProgram, name: String, viewMatrix: Matrix4f) {
        super.bind(shaderProgram, name);
        var viewDirection = Vector4f(direction, 0.0f).mul(getWorldModelMatrix());
        viewDirection.mul(viewMatrix);
        shaderProgram.setUniform("$name.direction", Vector3f(viewDirection.x, viewDirection.y, viewDirection.z));
        shaderProgram.setUniform("$name.innerCutoff", innerCutoff);
        shaderProgram.setUniform("$name.outerCutoff", outerCutoff);

    }

    fun getInnerAngle() = acos(innerCutoff);
    fun getOuterAngle() = acos(outerCutoff);
}