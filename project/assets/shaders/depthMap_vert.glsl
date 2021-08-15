#version 330 core

const int MAX_BONES = 15;
const int MAX_WEIGHTS = 4;
const int MAX_CASCADES = 4;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 in_texCoor;
layout(location = 2) in vec3 normal;
layout(location = 5) in ivec4 boneIDs;
layout(location = 6) in vec4 boneWeights;
layout(location = 7) in mat4 inst_model_matrix;

uniform mat4 model_matrix;
uniform mat4 lightSpaceMatrix;
uniform mat4 lightView;
uniform mat4 lightProjection;
uniform int bIsInstanced;
uniform mat4 boneMatrices[15];

out vec2 texCoords;


void main() {

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

    if(bIsInstanced == 1) gl_Position = lightSpaceMatrix * inst_model_matrix * posePosition;
    else gl_Position = lightSpaceMatrix * model_matrix * posePosition;

    texCoords = in_texCoor;
    //gl_Position = lightProjection * lightView * model_matrix * vec4(position, 1.0f);
}