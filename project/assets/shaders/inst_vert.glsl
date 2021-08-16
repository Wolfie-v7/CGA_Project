#version 330 core

const int NUM_CASCADES = 4;

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoor;
layout(location = 2) in vec3 normal;
layout(location = 3) in vec3 tangent;
layout(location = 4) in vec3 bitangent;
//layout(location = 5) in mat4 model_matrix;
layout(location = 5) in ivec4 boneIDs;
layout(location = 6) in vec4 boneWeights;
layout(location = 7) in mat4 model_matrix;



//uniforms
//uniform mat4 model_matrix;
uniform mat4 view_matrix;
uniform mat4 projection_matrix;
uniform mat4 view_normal_matrix;
//uniform mat4 model_normal_matrix;
uniform mat4 lightSpaceMatrix;
uniform mat4 lightSpaceMatrices[10];
uniform mat4 cascades[NUM_CASCADES];

uniform vec2 tcMul;
uniform bool bUseFakeLighting;

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
out vec4 cascadePositions[NUM_CASCADES];
out float distanceToCamera;
out mat3 TBN;

//
void main()
{

    vec4 worldPosition = model_matrix * vec4(position, 1.0f);
    gl_ClipDistance[0] = dot(worldPosition, clipPlane);

    vec4 proj_vec = projection_matrix * view_matrix * worldPosition;
    vec4 nor = transpose(inverse(view_matrix * model_matrix)) * vec4(normal, 0.0f);
    //if(bUseFakeLighting) nor = view_normal_matrix * vec4(0.0, 1.0, 0.0, 0.0);
    gl_Position = proj_vec ;

    vertexData.position = (view_matrix * worldPosition).xyz;
    vertexData.textureCoordinates = tcMul * texCoor;
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

    vec4 tan = view_normal_matrix * vec4(tangent, 0.0f);
    vec4 bit = view_normal_matrix * vec4(bitangent, 0.0f);

    vec3 T = normalize(vec3(tan));
    vec3 B = normalize(vec3(bit));
    vec3 N = normalize(vec3(nor));
    TBN = mat3(T, B, N);
}
