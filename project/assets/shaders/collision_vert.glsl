#version 330 core

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;

uniform mat4 model_matrix;
uniform mat4 view_matrix;
uniform mat4 projection_matrix;

out struct VertexData
{
    vec3 position;
    vec3 normal;
} out_vertex;


void main(){

    vec4 outPos = projection_matrix * view_matrix * model_matrix * vec4(position, 1.0);
    gl_Position = outPos;
    out_vertex.position = outPos.xyz;
    out_vertex.normal = normal;
}
