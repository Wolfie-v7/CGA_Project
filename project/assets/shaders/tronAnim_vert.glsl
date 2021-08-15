#version 430 core

const int MAX_BONES = 15;
const int MAX_WEIGHTS = 4;
const int NUM_CASCADES = 4;

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
uniform mat4 cascades[NUM_CASCADES];

uniform mat4 boneMatrix0;
uniform mat4 boneMatrix1;
uniform mat4 boneMatrices[15];
uniform int NUM_SHADOWCASTERS;
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


out mat4 viewMat;
out vec4 lightSpacePositions[10];
out vec4 cascadePositions[NUM_CASCADES];
out float distanceToCamera;

//
void main()
{

    vec4 posePosition = vec4(0.0);
    vec4 poseNormal = vec4(0.0);

    for (int i = 0; i < MAX_WEIGHTS; i++)
    {

        if(boneIDs[i] == -1 && i == 0)
        {
            posePosition = vec4(position, 1.0f);
            poseNormal = vec4(normal, 0.0);
            break;
        }
        if(boneIDs[i] == -1) continue;

        //if(boneIDs[i] >= MAX_BONES)
        //{
        //    posePosition = vec4(position, 1.0f);
        //    break;
        //}
        mat4 boneTransform = boneMatrices[boneIDs[i]];
        vec4 currentPosePosition = boneTransform * vec4(position, 1.0);
        posePosition += currentPosePosition * boneWeights[i];

        vec4 currentNormal = boneTransform * vec4(normal, 0.0);
        poseNormal += currentNormal;
    }



    vec4 worldPosition = model_matrix * posePosition;
    gl_ClipDistance[0] = dot(worldPosition, clipPlane);
    vec4 proj_vec = projection_matrix * view_matrix * worldPosition;
    vec4 nor = view_normal_matrix * poseNormal;
    gl_Position = proj_vec ;

    vertexData.position = vec3(view_matrix * model_matrix * posePosition);
    vertexData.textureCoordinates = tcMul * texCoor;
    vertexData.normal = nor.xyz;
    vertexData.lightSpacePosition = lightSpaceMatrix * model_matrix * posePosition;
    for(int i = 0; i < NUM_SHADOWCASTERS; i++)
    {
        lightSpacePositions[i] = lightSpaceMatrices[i] * model_matrix * posePosition;
    }

    for (int i = 0; i < NUM_CASCADES; i++)
    {
        cascadePositions[i] = cascades[i] * model_matrix * posePosition;
    }

    viewMat = view_matrix;
    distanceToCamera  = length((view_matrix * model_matrix * posePosition).xyz);
}
