#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoor;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec3 tangent;
layout(location = 4) in vec3 bitangent;
layout(location = 5) in ivec4 boneIDs;
layout(location = 6) in vec4 boneWeights;

//uniforms
uniform mat4 model_matrix;
uniform mat4 view_matrix;
uniform mat4 projection_matrix;
uniform mat4 view_normal_matrix;
uniform mat4 model_normal_matrix;
uniform mat4 lightSpaceMatrix;
uniform mat4 lightSpaceMatrices[10];

uniform vec2 tcMul;
uniform int bUseNormalMap;

uniform vec4 clipPlane;


out struct VertexData
{
    vec3 position;
    vec2 textureCoordinates;
    vec3 normal;
    vec4 lightSpacePosition;

} vertexData;

uniform int NUM_SHADOWCASTERS;
out mat4 viewMat;
out vec4 lightSpacePositions[10];
out float distanceToCamera;

//
void main()
{

    vec4 worldPosition = model_matrix * vec4(position, 1.0f);
    gl_ClipDistance[0] = dot(worldPosition, clipPlane);
    vec4 proj_vec = projection_matrix * view_matrix * worldPosition;
    vec4 nor = view_normal_matrix * vec4(normal, 0.0f);
    gl_Position = proj_vec ;

    vertexData.position = vec3((view_matrix * model_matrix) * vec4(position.xyz, 1.0f));
    vertexData.textureCoordinates = tcMul * texCoor;
    vertexData.normal = nor.xyz;
    vertexData.lightSpacePosition = lightSpaceMatrix * model_matrix * vec4(position, 1.0f);
    for(int i = 0; i < NUM_SHADOWCASTERS; i++)
    {
        lightSpacePositions[i] = lightSpaceMatrices[i] * model_matrix * vec4(position, 1.0f);
    }
    viewMat = view_matrix;
    distanceToCamera  = length((view_matrix * model_matrix * vec4(position, 1.0f)).xyz);
}
