#version 330 core

const int NUM_CASCADES = 4;

//input from vertex shader
//in vec3 viewLightPos;
//in vec3 viewPos;
in mat4 viewMat;
in float distanceToCamera;

in struct VertexData
{
    vec4 lightSpacePosition;
    vec3 position;
    vec2 textureCoordinates;
    vec3 normal;
} vertexData;

struct Material
{
    vec2 tcMul;
    sampler2D diffuse;
    sampler2D emission;
    sampler2D specular;
    vec3 emissionColor;
    sampler2D normal;
    float shininess;
};

uniform Material backgroundMat;
uniform Material rMaterial;
uniform Material gMaterial;
uniform Material bMaterial;
uniform sampler2D blendMap;

struct Blend
{
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    vec3 emission;
    vec3 normal;
    vec3 emissionColor;
    float shininess;
};
Blend blendedTex;

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
* Index 1 to 9 for spotlights
*/
#define MAX_SHADOW_MAPS 10
uniform sampler2D shadowTextures[MAX_SHADOW_MAPS];
uniform sampler2D cascadeShadowTextures[NUM_CASCADES]; // cascaded shadow maps
uniform float cascadeFarPlanes[NUM_CASCADES];
uniform int shadowMapsCount;
in vec4 lightSpacePositions[10];
in vec4 cascadePositions[NUM_CASCADES];
in mat3 fs_TBN;

uniform sampler2D shadowTexture;
uniform sampler2D noiseTexture;

/* FUNCTIONS */
vec4 hash4( vec2 p ) {
    return fract(sin(vec4( 1.0+dot(p,vec2(37.0,17.0)),
    2.0+dot(p,vec2(11.0,47.0)),
    3.0+dot(p,vec2(41.0,29.0)),
    4.0+dot(p,vec2(23.0,31.0))))*103.0);
}
float sum( vec3 v ) { return v.x+v.y+v.z; }
vec3 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 viewDir, float shadowFactor); // Directional Light Calculations
vec3 calculatePointLight(PointLight light, vec3 normal, vec3 fragPos, vec3 viewDir); // Pointlight Calculations
vec3 calculateSpotLight(SpotLight light, vec3 normal, vec3 fragPos, vec3 viewDir); // Spotlight Calcualtions
float calculateShadows(vec3 normal, vec3 lightDir, sampler2D shadowTex, int index);
float calculateCascadedShadows(vec3 normal, vec3 lightDir, sampler2D shadowTex, int index);
void blendTextures();
vec3 textureUntile(sampler2D samp, vec2 uv);
vec4 textureUntileAlt(sampler2D samp, vec2 uv);
vec3 textureUntileCheap(sampler2D samp, vec2 uv);
vec3 applyFog(  vec3  rgb,       // original color of the pixel
                float distance, // camera to point distance
                float density);


//fragment shader output
layout(location = 0) out vec4 color;
layout(location = 1) out vec4 brightColor;

void main(){
    vec3 finalColor = vec3(0.0f);
    float shadows = 1.0;
    vec3 Normal_s = normalize(vertexData.normal);
    vec3 ViewDir = normalize(-vertexData.position);
    //vec3 ViewDir = normalize(-vertexData.tangentPosition);

    int cascade = 3;
    for (int i = 0; i < NUM_CASCADES; i++)
    {
        if ( abs(vertexData.position.z) < cascadeFarPlanes[i] )
        {
            cascade = i;
            break;
        }
    }

    //cascade = 1;

    //================================================================================================================

    blendTextures(); // use the blend map to blend between terrain textures

    //================================================================================================================

    vec3 Normal = blendedTex.normal;
    Normal = Normal * 2.0 - 1.0;
    Normal = fs_TBN * Normal;
    Normal = normalize(Normal);
    /*
    * Calculate lights
    */
    // Point lights
    for(int i = 0; i < MAX_POINTLIGHTS; i++)
    {
        if(pointLights[i].diffuse == vec3(0.0)) continue;
        finalColor += calculatePointLight(pointLights[i], Normal_s, vertexData.position, ViewDir);
    }

    // Spotlights
    for(int i = 0; i < 10; i++)
    {
        if(spotLights[i].diffuse == vec3(0.0)) continue;
        finalColor += calculateSpotLight(spotLights[i], Normal_s, vertexData.position, ViewDir);
    }
    // Directional light
    finalColor += calculateDirectionalLight(dirLight, Normal_s, ViewDir, 1.0f);


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

    // Calculate directional light shadows
    //shadows = calculateShadows(Normal_s, normalize(dirLight.direction), shadowTextures[0], 0);
    shadows = calculateCascadedShadows(Normal_s, normalize(dirLight.direction), cascadeShadowTextures[cascade], cascade);
    finalColor = shadows * finalColor;


    //================================================================================================================
    // emission
    //vec3 emission_result = blendedTex.emissionColor * vec3(texture2D(material.emission, vertexData.textureCoordinates));
    //finalColor += emission_result;
    finalColor += blendedTex.emission;

    // Ambient
    //vec3 ambient = 0.2 * vec3(texture2D(material.diffuse, vertexData.textureCoordinates));
    //finalColor += ambient;
    finalColor += blendedTex.ambient;

    //Fog
    finalColor = applyFog(finalColor, distanceToCamera, 0.0);

    // output
    color = vec4(finalColor, 1.0);

    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    if(brightness > 1.0)
    brightColor = vec4(color.rgb, 1.0);
    else
    brightColor = vec4(0.0, 0.0, 0.0, 1.0);
}


/* DEFINITIONS */
void blendTextures()
{
    vec4 blendMapColor = texture(blendMap, vertexData.textureCoordinates);
    float backgroundAmount = 1 - (blendMapColor.r + blendMapColor.g + blendMapColor.b);
    vec3 finalDiffuse = vec3(0);
    vec3 finalSpecular = vec3(0);
    vec3 finalEmission = vec3(0);
    vec3 finalNormal = vec3(0);
    float finalShininess = 10;

    // Background Texture
    vec2 bgTCoords = backgroundMat.tcMul * vertexData.textureCoordinates;
    vec3 bgDiffuse = vec3(textureUntileAlt(backgroundMat.diffuse, bgTCoords)) * backgroundAmount;
    vec3 bgSpecular = vec3(textureUntileAlt(backgroundMat.specular, bgTCoords)) * backgroundAmount;
    vec3 bgEmission = backgroundMat.emissionColor * vec3(textureUntileAlt(backgroundMat.emission, bgTCoords));
    vec3 bgNormal = textureUntileAlt(backgroundMat.normal, bgTCoords).rgb * backgroundAmount;
    //bgNormal = normalize(bgNormal * 2.0 - 1.0);

    // R Texture
    vec2 rTCoords = rMaterial.tcMul * vertexData.textureCoordinates;
    vec3 rDiffuse = vec3(textureUntileAlt(rMaterial.diffuse, rTCoords)) * blendMapColor.r;
    vec3 rSpecular = vec3(textureUntileAlt(rMaterial.specular, rTCoords)) * blendMapColor.r;
    vec3 rEmission = rMaterial.emissionColor * vec3(textureUntileAlt(rMaterial.emission, rTCoords));
    vec3 rNormal = textureUntileAlt(rMaterial.normal, rTCoords).rgb * blendMapColor.r;
    rNormal = normalize(rNormal * 2.0 - 1.0);

    // G Texture
    vec2 gTCoords = gMaterial.tcMul * vertexData.textureCoordinates;
    vec3 gDiffuse = vec3(textureUntileAlt(gMaterial.diffuse, gTCoords)) * blendMapColor.g;
    vec3 gSpecular = vec3(textureUntileAlt(gMaterial.specular, gTCoords)) * blendMapColor.g;
    vec3 gEmission = gMaterial.emissionColor * vec3(textureUntileAlt(gMaterial.emission, gTCoords));
    vec3 gNormal = textureUntileAlt(gMaterial.normal, gTCoords).rgb * blendMapColor.g;
    gNormal = normalize(gNormal * 2.0 - 1.0);

    // B Texture
    vec2 bTCoords = bMaterial.tcMul * vertexData.textureCoordinates;
    vec3 bDiffuse = vec3(textureUntileAlt(bMaterial.diffuse, bTCoords)) * blendMapColor.b;
    vec3 bSpecular = vec3(textureUntileAlt(bMaterial.specular, bTCoords)) * blendMapColor.b;
    vec3 bEmission = bMaterial.emissionColor * vec3(textureUntileAlt(bMaterial.emission, bTCoords));
    vec3 bNormal = textureUntileAlt(bMaterial.normal, bTCoords).rgb * blendMapColor.b;
    bNormal = normalize(bNormal * 2.0 - 1.0);


    // Mixing
    finalDiffuse = bgDiffuse + rDiffuse + gDiffuse + bDiffuse;
    finalSpecular = bgSpecular + rSpecular + gSpecular + bSpecular;
    finalEmission = bgEmission + rEmission + gEmission + bEmission;
    finalNormal = bgNormal + rNormal + gNormal + bNormal;
    finalShininess = (backgroundMat.shininess + rMaterial.shininess + gMaterial.shininess + bMaterial.shininess) / 4;


    // Output
    blendedTex.ambient = 0.1 * finalDiffuse;
    blendedTex.diffuse = finalDiffuse;
    blendedTex.specular = finalSpecular;
    blendedTex.normal = bgNormal;
    blendedTex.emission = finalEmission;
    blendedTex.shininess = finalShininess;
}


vec3 calculateDirectionalLight(DirectionalLight light, vec3 normal, vec3 viewDir, float shadowFactor)
{
    vec3 viewLightDir = normalize(light.direction);
    //vec3 diffuse_result = light.diffuse * (max(dot(normal, viewLightDir), 0.0) * vec3(texture2D(material.diffuse, vertexData.textureCoordinates)));
    vec3 diffuse_result = light.diffuse * (max(dot(normal, viewLightDir), 0.0) * blendedTex.diffuse);

    // Specular (Phong)
    //vec3 reflectDir = reflect(-viewLightDir, normal);
    //vec3 specular_result = light.specular * (pow(max(dot(direction, reflectDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates)));

    // Specular Blinn-Phong
    vec3 halfwayDir = normalize(viewLightDir + viewDir);
    //vec3 specular_result = light.specular * pow(max(dot(normal, halfwayDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates));
    vec3 specular_result = light.specular * pow(max(dot(normal, halfwayDir), 0.0), blendedTex.shininess) * blendedTex.specular;

    return (diffuse_result + specular_result);
}


vec3 calculatePointLight(PointLight light, vec3 normal, vec3 fragPos, vec3 viewDir)
{
    // Diffuse
    vec3 v_lightPos = vec3(viewMat * vec4(light.position, 1.0f));
    vec3 viewLightDir = normalize(v_lightPos - fragPos);
    //vec3 diffuse_result = light.diffuse * (max(dot(normal, viewLightDir), 0.0) * vec3(texture2D(material.diffuse, vertexData.textureCoordinates)));
    vec3 diffuse_result = light.diffuse * (max(dot(normal, viewLightDir), 0.0) * blendedTex.diffuse);

    // Specular (Phong)
    //vec3 reflectDir = reflect(-viewLightDir, normal);
    //vec3 specular_result = light.specular * (pow(max(dot(direction, reflectDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates)));

    // Specular Blinn-Phong
    vec3 halfwayDir = normalize(viewLightDir + viewDir);
    //vec3 specular_result = light.specular * pow(max(dot(normal, halfwayDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates));
    vec3 specular_result = light.specular * pow(max(dot(normal, halfwayDir), 0.0), blendedTex.shininess) * blendedTex.specular;

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
    vec3 v_lightPos =  vec3(viewMat * vec4(light.position, 1.0f));
    vec3 viewLightDir = normalize(v_lightPos - fragPos);
    //vec3 diffuse_result = light.diffuse * (max(dot(normal, viewLightDir), 0.0) * vec3(texture2D(material.diffuse, vertexData.textureCoordinates)));
    vec3 diffuse_result = light.diffuse * (max(dot(normal, viewLightDir), 0.0) * blendedTex.diffuse);

    // Specular (Phong)
    //vec3 reflectDir = reflect(-viewLightDir, normal);
    //vec3 specular_result = light.specular * (pow(max(dot(direction, reflectDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates)));

    // Specular Blinn-Phong
    vec3 halfwayDir = normalize(viewLightDir + viewDir);
    //vec3 specular_result = light.specular * pow(max(dot(normal, halfwayDir), 0.0), material.shininess) * vec3(texture2D(material.specular, vertexData.textureCoordinates));
    vec3 specular_result = light.specular * pow(max(dot(normal, halfwayDir), 0.0), blendedTex.shininess) * blendedTex.specular;

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
    float bias = max(0.0005 * (1.0 - dot(normal, lightDir)), 0.00005);

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

float calculateCascadedShadows(vec3 normal, vec3 lightDir, sampler2D shadowTex, int index) {

    vec3 currentFragPos = cascadePositions[index].xyz / cascadePositions[index].w;
    currentFragPos = currentFragPos * 0.5 + 0.5;
    if (currentFragPos.z > 1) return 1.0;

    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowTex, 0);
    float bias = max(0.0005 * (1.0 - dot(normal, lightDir)), 0.00005);

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



/*
* Voronoi pattern
* Three Variants with different complexities
*/

vec3 textureUntile(sampler2D samp, vec2 uv)
{
    vec2 p = floor( uv );
    vec2 f = fract( uv );

    // derivatives (for correct mipmapping)
    vec2 ddx = dFdx( uv );
    vec2 ddy = dFdy( uv );

    vec3 va = vec3(0.0);
    float w1 = 0.0;
    float w2 = 0.0;
    for( int j=-1; j<=1; j++ )
        for( int i=-1; i<=1; i++ )
        {
            vec2 g = vec2( float(i),float(j) );
            vec4 o = hash4( p + g );
            vec2 r = g - f + o.xy;
            float d = dot(r,r);
            float w = exp(-5.0*d );
            vec3 c = textureGrad( samp, uv + o.zw, ddx, ddy ).xyz;
            va += w*c;
            w1 += w;
            w2 += w*w;
        }

    // normal averaging --> lowers contrasts
    //return va/w1;

    // contrast preserving average
    float mean = textureGrad( samp, uv, ddx*16.0, ddy*16.0 ).x;
    vec3 res = mean + (va-w1*mean)/sqrt(w2);
    return mix( va/w1, res, 1.0 );
}

vec4 textureUntileAlt(sampler2D samp, vec2 uv)
{
vec2 iuv = floor( uv );
vec2 fuv = fract( uv );

// generate per-tile transform
vec4 ofa = hash4( iuv + vec2(0.0,0.0) );
vec4 ofb = hash4( iuv + vec2(1.0,0.0) );
vec4 ofc = hash4( iuv + vec2(0.0,1.0) );
vec4 ofd = hash4( iuv + vec2(1.0,1.0) );


vec2 ddx = dFdx( uv );
vec2 ddy = dFdy( uv );

// transform per-tile uvs
ofa.zw = sign(ofa.zw-0.5);
ofb.zw = sign(ofb.zw-0.5);
ofc.zw = sign(ofc.zw-0.5);
ofd.zw = sign(ofd.zw-0.5);

// uv's, and derivarives (for correct mipmapping)
vec2 uva = uv*ofa.zw + ofa.xy; vec2 ddxa = ddx*ofa.zw; vec2 ddya = ddy*ofa.zw;
vec2 uvb = uv*ofb.zw + ofb.xy; vec2 ddxb = ddx*ofb.zw; vec2 ddyb = ddy*ofb.zw;
vec2 uvc = uv*ofc.zw + ofc.xy; vec2 ddxc = ddx*ofc.zw; vec2 ddyc = ddy*ofc.zw;
vec2 uvd = uv*ofd.zw + ofd.xy; vec2 ddxd = ddx*ofd.zw; vec2 ddyd = ddy*ofd.zw;

// fetch and blend
vec2 b = smoothstep(0.25,0.75,fuv);

return mix( mix( textureGrad( samp, uva, ddxa, ddya ),
textureGrad( samp, uvb, ddxb, ddyb ), b.x ),
mix( textureGrad( samp, uvc, ddxc, ddyc ),
textureGrad( samp, uvd, ddxd, ddyd ), b.x), b.y );
}

vec3 textureUntileCheap(sampler2D samp, vec2 uv)
{
    float k = texture( noiseTexture, uv ).x; // cheap (cache friendly) lookup

    vec2 duvdx = dFdx( uv );
    vec2 duvdy = dFdx( uv );

    float l = k * 8.0;
    float f = fract(l);

    /*#if 1
    float ia = floor(l); // my method
    float ib = ia + 1.0;
    #else*/
    float ia = floor(l+0.5); // suslik's method (see comments)
    float ib = floor(l);
    f = min(f, 1.0-f)*2.0;
    //#endif

    vec2 offa = hash4(vec2(3.0,7.0)*ia).xy; // can replace with any other hash
    vec2 offb = hash4(vec2(3.0,7.0)*ib).xy; // can replace with any other hash

    vec3 cola = textureGrad( samp, uv + offa, duvdx, duvdy ).xyz;
    vec3 colb = textureGrad( samp, uv + offb, duvdx, duvdy ).xyz;

    return mix( cola, colb, smoothstep(0.2,0.8,f-0.1*sum(cola-colb)) );
}

vec3 applyFog(  vec3  rgb,       // original color of the pixel
                float distance, // camera to point distance
                float density)
{
    float gradient = 0.5;
    float fogAmount = 1.0 - clamp(exp(-pow((distance * density), gradient)), 0.0, 1.0);
    vec3  fogColor  = vec3(0.4, 0.6, 0.7);
    return mix( rgb, fogColor, fogAmount );
    //return rgb*(1.0-exp(-distance*density)) + fogColor*exp(-distance*density);
}