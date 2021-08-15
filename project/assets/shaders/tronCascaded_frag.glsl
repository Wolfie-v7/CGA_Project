#version 330 core

const int NUM_CASCADES = 4;

//input from vertex shader
//in vec3 viewLightPos;
//in vec3 viewPos;
in mat4 viewMat;
in float distanceToCamera;


in struct VertexData
{
    vec3 position;
    vec2 textureCoordinates;
    vec3 normal; vec4 lightSpacePosition;
} vertexData;

struct Material
{
    sampler2D diffuse;
    sampler2D emission;
    sampler2D specular;
    vec3 emissionColor;
    float shininess;
};
uniform Material material;


struct DirectionalLight
{
    vec3 direction;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};
uniform DirectionalLight dirLight;

struct PointLight
{
    vec3 position;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    //attenuation terms
    float constant;
    float linear;
    float quadratic;
};
#define MAX_POINTLIGHTS 20
uniform PointLight pointLights[MAX_POINTLIGHTS];

struct SpotLight
{
    vec3 position;
    vec3 direction;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    float constant;
    float linear;
    float quadratic;

    float innerCutoff;
    float outerCutoff;
};

#define MAX_SPOTLIGHTS 50
uniform SpotLight spotLights[MAX_SPOTLIGHTS];

/*
* Index 0 for directional light
* Index 1 to 4 for spotlights
*/
#define MAX_SHADOW_MAPS 10
uniform sampler2D shadowTextures[MAX_SHADOW_MAPS];
uniform sampler2D cascadeShadowTextures[NUM_CASCADES];
uniform float cascadeFarPlanes[NUM_CASCADES];
uniform int shadowMapsCount;
in vec4 lightSpacePositions[10];

uniform sampler2D shadowTexture;

/* FUNCTIONS */
vec3 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 viewDir, float shadowFactor); // Directional Light Calculations
vec3 calculatePointLight(PointLight light, vec3 normal, vec3 fragPos, vec3 viewDir); // Pointlight Calculations
vec3 calculateSpotLight(SpotLight light, vec3 normal, vec3 fragPos, vec3 viewDir); // Spotlight Calcualtions
float calculateShadows(vec3 normal, vec3 lightDir, sampler2D shadowTex, int index);
vec3 applyFog(  vec3  rgb,       // original color of the pixel
                float distance, // camera to point distance
                float density);


//fragment shader output
layout(location = 0) out vec4 color;
layout(location = 1) out vec4 brightColor;

void main(){
    vec3 finalColor = vec3(0.0f);
    float shadows = 1.0;
    vec3 Normal = normalize(vertexData.normal);
    vec3 ViewDir = normalize(-vertexData.position);

    //================================================================================================================

    /*
    * Calculate lights
    */
    // Point lights
    for(int i = 0; i < MAX_POINTLIGHTS; i++)
    {
        if(pointLights[i].diffuse == vec3(0.0)) continue;
        finalColor += calculatePointLight(pointLights[i], Normal, vertexData.position, ViewDir);
    }

    // Spotlights
    for(int i = 0; i < MAX_SPOTLIGHTS; i++)
    {
        if(spotLights[i].diffuse == vec3(0.0)) continue;
        finalColor += calculateSpotLight(spotLights[i], Normal, vertexData.position, ViewDir);
    }
    // Directional light


    //================================================================================================================
    /*
    * Calculate scene shadows
    */
    // Calculate Spotlights shadows
    for(int i = 1; i < shadowMapsCount; i++)
    {
        //shadows = calculateShadows(Normal, normalize(spotLights[i - 1].direction), shadowTextures[i], i);
        finalColor = shadows * finalColor;

    }
    finalColor += calculateDirectionalLight(dirLight, Normal, ViewDir, 1.0f);
    // Calculate directional light shadows
    shadows = calculateShadows(Normal, normalize(dirLight.direction), shadowTextures[0], 0);
    finalColor = shadows * finalColor;

    //================================================================================================================
    // emission
    vec3 emission_result = material.emissionColor * vec3(texture2D(material.emission, vertexData.textureCoordinates));
    finalColor += emission_result;

    // Ambient
    vec3 ambient = 0.1 * vec3(texture2D(material.diffuse, vertexData.textureCoordinates));
    finalColor += ambient;

    // Fog
    finalColor = applyFog(finalColor, distanceToCamera, 0.005);

    // output
    color = vec4(finalColor, 1.0);

    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    if(brightness > 1.0)
    brightColor = vec4(color.rgb, 1.0);
    else
    brightColor = vec4(0.0, 0.0, 0.0, 1.0);
}


/* DEFINITIONS */

vec3 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 viewDir, float shadowFactor)
{
    vec3 viewLightDir = normalize(light.direction);
    vec3 diffuse_result = light.diffuse * (max(dot(normal, viewLightDir), 0.0) * vec3(texture2D(material.diffuse, vertexData.textureCoordinates)));

    // Specular (Phong)
    //vec3 reflectDir = reflect(-viewLightDir, normal);
    //vec3 specular_result = light.specular * (pow(max(dot(direction, reflectDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates)));

    // Specular Blinn-Phong
    vec3 halfwayDir = normalize(viewLightDir + viewDir);
    vec3 specular_result = light.specular * pow(max(dot(normal, halfwayDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates));


    return (diffuse_result + specular_result) * shadowFactor;
}


vec3 calculatePointLight(PointLight light, vec3 normal, vec3 fragPos, vec3 viewDir)
{
    // Diffuse
    vec3 v_lightPos = vec3(viewMat * vec4(light.position, 1.0f));
    vec3 viewLightDir = normalize(v_lightPos - fragPos);
    vec3 diffuse_result = light.diffuse * (max(dot(normal, viewLightDir), 0.0) * vec3(texture2D(material.diffuse, vertexData.textureCoordinates)));

    // Specular (Phong)
    //vec3 reflectDir = reflect(-viewLightDir, normal);
    //vec3 specular_result = light.specular * (pow(max(dot(direction, reflectDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates)));

    // Specular Blinn-Phong
    vec3 halfwayDir = normalize(viewLightDir + viewDir);
    vec3 specular_result = light.specular * pow(max(dot(normal, halfwayDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates));


    // Attenuation calculation
    float lightDistance = length(v_lightPos - fragPos);
    float attenuation = 1.0 / (light.constant + light.linear * lightDistance + light.quadratic * (lightDistance * lightDistance));
    diffuse_result *= attenuation;
    specular_result *= attenuation;

    return (diffuse_result + specular_result);
}

vec3 calculateSpotLight(SpotLight light, vec3 normal, vec3 fragPos, vec3 viewDir)
{
    // Diffuse
    vec3 v_lightPos = vec3(viewMat * vec4(light.position, 1.0f));
    vec3 viewLightDir = normalize(v_lightPos - fragPos);
    vec3 diffuse_result = light.diffuse * (max(dot(normal, viewLightDir), 0.0) * vec3(texture2D(material.diffuse, vertexData.textureCoordinates)));

    // Specular (Phong)
    //vec3 reflectDir = reflect(-viewLightDir, normal);
    //vec3 specular_result = light.specular * (pow(max(dot(direction, reflectDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates)));

    // Specular Blinn-Phong
    vec3 halfwayDir = normalize(viewLightDir + viewDir);
    vec3 specular_result = light.specular * pow(max(dot(normal, halfwayDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates));

    float lightDistance = length(v_lightPos - fragPos);
    float attenuation = 1.0 / (light.constant + light.linear * lightDistance + light.quadratic * (lightDistance * lightDistance));
    diffuse_result *= attenuation;
    specular_result *= attenuation;

    float theta = dot(viewLightDir, normalize(-light.direction));
    float epsilon = light.innerCutoff - light.outerCutoff;
    float intensity = clamp((theta - light.outerCutoff) / epsilon, 0.0, 1.0);
    diffuse_result *= intensity;
    specular_result *= intensity;

    return (diffuse_result + specular_result);
}

float calculateShadows(vec3 normal, vec3 lightDir, sampler2D shadowTex, int index) {

    vec3 currentFragPos = lightSpacePositions[index].xyz / lightSpacePositions[index].w;
    currentFragPos = currentFragPos * 0.5 + 0.5;
    if (currentFragPos.z > 1) return 1.0;

    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowTex, 0);
    float bias = max(0.05 * (1.0 - dot(normal, lightDir)), 0.005);

    // PCF
    for(int x = -1; x <= 1; ++x)
    {
        for(int y = -1; y <= 1; ++y)
        {
            float pcfDepth = texture(shadowTex, currentFragPos.xy + vec2(x, y) * texelSize).r;
            shadow += (currentFragPos.z - bias) > pcfDepth ? 0.0 : 1.0;
        }
    }

    return shadow /= 9.0;
}

vec3 applyFog(  vec3  rgb,       // original color of the pixel
                float distance, // camera to point distance
                float density)
{
    float fogAmount = 1.0 - exp( -distance * density );
    vec3  fogColor  = vec3(0.5, 0.6, 0.7);
    return mix( rgb, fogColor, fogAmount );
}