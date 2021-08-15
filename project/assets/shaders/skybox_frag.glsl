#version 330 core

in vec3 TexCoords;

uniform samplerCube skybox;
//fragment shader output
out vec4 color;

void main(){
    color = texture(skybox, TexCoords);
}
