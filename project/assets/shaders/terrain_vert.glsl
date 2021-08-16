#version 330 core

const int NUM_CASCADES = 4;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoor;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec3 tangent;
layout(location = 4) in vec3 bitangent;


//uniforms
uniform mat4 model_matrix;
uniform mat4 view_matrix;
uniform mat4 projection_matrix;
uniform mat4 view_normal_matrix;
uniform mat4 model_normal_matrix;
uniform mat4 lightSpaceMatrix;
uniform mat4 lightSpaceMatrices[10];
uniform mat4 cascades[NUM_CASCADES];
uniform vec4 clipPlane;

out struct VertexData
{
    vec4 lightSpacePosition;
    vec3 position;
    vec2 textureCoordinates;
    vec3 normal;

} vertexData;


uniform int NUM_SHADOWCASTERS;
out mat4 viewMat;
out vec4 lightSpacePositions[10];
out vec4 cascadePositions[NUM_CASCADES];
out float distanceToCamera;
out mat3 fs_TBN;



//
void main()
{

    vec4 worldPosition = model_matrix * vec4(position, 1.0f);
    gl_ClipDistance[0] = dot(worldPosition, clipPlane);
    vec4 proj_vec = projection_matrix * view_matrix * worldPosition;
    vec4 tan = view_normal_matrix * vec4(tangent, 0.0f);
    vec4 bit = view_normal_matrix * vec4(bitangent, 0.0f);
    vec4 nor = view_normal_matrix * vec4(normal, 0.0f);

    vec3 T = normalize(vec3(tan));
    vec3 B = normalize(vec3(bit));
    vec3 N = normalize(vec3(nor));
    //mat3 TBN = transpose(mat3(T, B, N));
    gl_Position = proj_vec ;

    vertexData.position = vec3((view_matrix * model_matrix) * vec4(position, 1.0f));
    vertexData.textureCoordinates = texCoor;
    vertexData.normal = nor.xyz;
    vertexData.lightSpacePosition = lightSpaceMatrix * model_matrix * vec4(position, 1.0f);

    for(int i = 0; i < NUM_SHADOWCASTERS; i++)
    {
        lightSpacePositions[i] = lightSpaceMatrices[i] * model_matrix * vec4(position, 1.0f);
    }

    for (int i = 0; i < NUM_CASCADES; i++)
    {
        cascadePositions[i] = cascades[i] * model_matrix * vec4(position, 1.0f);
    }

    viewMat = view_matrix;
    distanceToCamera  = length((view_matrix * model_matrix * vec4(position, 1.0f)).xyz);
    fs_TBN = mat3(T, B, N);


}

