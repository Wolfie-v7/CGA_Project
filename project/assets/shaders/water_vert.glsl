#version 330 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoords;
layout (location = 2) in vec3 normal;

uniform mat4 model_matrix;
uniform mat4 view_matrix;
uniform mat4 projection_matrix;
uniform mat4 view_normal_matrix;
uniform vec2 tcMul;
uniform vec3 cameraPosition;
uniform vec3 lightPosition;


out struct VertexData
{
    vec3 position;
    vec2 textureCoordinates;
    vec3 normal;
    vec3 toCamera;
    vec3 toLight;

} vertexData;

out vec4 clipSpace;


void main() {
    vec4 worldPosition = model_matrix * vec4(position, 1.0);
    vec4 viewPosition = view_matrix * worldPosition;
    clipSpace = projection_matrix * viewPosition;
    gl_Position = clipSpace;
    vertexData.position = viewPosition.xyz;
    vertexData.normal = (view_normal_matrix * vec4(normal, 1.0)).xyz;
    vertexData.textureCoordinates = tcMul * texCoords;
    vertexData.toCamera = cameraPosition - worldPosition.xyz;
    vertexData.toLight = worldPosition.xyz - lightPosition;
}
