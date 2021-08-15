#version 330 core
out vec4 color;

uniform sampler2D shadowMap;

in vec2 TexCoords;

void main() {
    float depthValue = texture(shadowMap, TexCoords).r;
    color = vec4(vec3(depthValue), 1.0f);
    //color = vec4(vec3(1.0, 0.0, 1.0), 1.0);
    //color = texture(shadowMap, TexCoords);
}
