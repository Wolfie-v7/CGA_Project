package cga.framework

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.joml.Vector4i

data class Vertex(val position: Vector3f,
                  val texCoord: Vector2f,
                  val normal: Vector3f,
                  val tangent: Vector3f,
                  val bitangent: Vector3f,
                  var boneIDs: Vector4i = Vector4i(-1),
                  var boneWeights: Vector4f = Vector4f(0f))