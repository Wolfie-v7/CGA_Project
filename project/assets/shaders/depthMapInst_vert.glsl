#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texCoor;
layout(location = 2) in vec3 normal;
layout(location = 3) in mat4 model_matrix;

//uniform mat4 model_matrix;
uniform mat4 lightSpaceMatrix;
uniform mat4 lightView;
uniform mat4 lightProjection;



void main() {
    gl_Position = lightSpaceMatrix * model_matrix * vec4(position, 1.0f);
    //gl_Position = lightProjection * lightView * model_matrix * vec4(position, 1.0f);
}