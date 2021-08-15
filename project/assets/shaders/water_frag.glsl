#version 330 core
in vec4 clipSpace;
in struct VertexData
{
    vec3 position;
    vec2 textureCoordinates;
    vec3 normal;
    vec3 toCamera;
    vec3 toLight;

} vertexData;

uniform sampler2D reflectionTexture;
uniform sampler2D refractionTexture;
uniform sampler2D waterSpec;
uniform sampler2D normalMap;
uniform sampler2D DuDvMap;
uniform float distortionStrength;
uniform float distFactor;
uniform float shininess;
uniform vec3 lightSpec;


layout(location = 0) out vec4 colorOutput;
layout(location = 1) out vec4 brightColor;

void main() {

    vec4 finalColor = vec4(0.0);
    vec2 ndc = (clipSpace.xy / clipSpace.w);
    ndc = ndc / 2 + 0.5;
    vec2 refractTexCoords = vec2(ndc);
    vec2 reflectTexCoords = vec2(ndc.x, -ndc.y);



    vec2 distTexCoords = texture(DuDvMap, vec2(vertexData.textureCoordinates.x + distFactor, vertexData.textureCoordinates.y)).rg * 0.5;
    distTexCoords = vertexData.textureCoordinates + vec2(distTexCoords.x, distTexCoords.y + distFactor);

    vec2 finalDistortion = (texture(DuDvMap, distTexCoords).rg * 2.0 - 1.0) * distortionStrength;

    refractTexCoords += finalDistortion;
    reflectTexCoords += finalDistortion;

    refractTexCoords = clamp(refractTexCoords, 0.001, 0.999);
    reflectTexCoords.x = clamp(reflectTexCoords.x, 0.001, 0.999);
    reflectTexCoords.y = clamp(reflectTexCoords.y, -0.999, -0.001);


    vec4 reflectionColor = texture2D(reflectionTexture, reflectTexCoords);
    vec4 refractionColor = texture2D(refractionTexture, refractTexCoords);

    vec4 normalMapColor = texture(normalMap, distTexCoords);
    vec3 normal = vec3(normalMapColor.r * 2.0 - 1.0, normalMapColor.b, normalMapColor.g * 2.0 - 1.0);
    normal = normalize(normal);

    vec3 viewDir = normalize(vertexData.toCamera);
    float refractFactor = dot(viewDir, vec3(0, 1, 0));
    refractFactor = pow(refractFactor, 0.3);

    vec3 viewLightDir = normalize(-vertexData.toLight);
    vec3 halfwayDir = normalize(viewLightDir + viewDir);
    vec3 specular_result = lightSpec * pow(max(dot(normal, halfwayDir), 0.0), shininess) * vec3(texture2D(waterSpec, distTexCoords));


    finalColor = mix(reflectionColor, refractionColor, refractFactor);
    colorOutput = mix(finalColor, vec4(0.0, 0.3, 0.4, 1.0), 0.3) + vec4(specular_result, 1.0);

    float brightness = dot(colorOutput.rgb, vec3(0.2126, 0.7152, 0.0722));
    if(brightness > 1.0)
    brightColor = vec4(colorOutput.rgb, 1.0);
    else
    brightColor = vec4(0.0, 0.0, 0.0, 1.0);
    //colorOutput = normalMapColor;
    //color = vec4(0.0, 1.0, 1.0, 1.0);
    //color = reflectionColor;
}
