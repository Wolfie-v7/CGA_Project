#version 330 core

in struct VertexData
{
    vec3 position;
    vec3 normal;
} in_vertex;

uniform vec3 in_color = vec3(1.0);
//fragment shader output
out vec4 color;

void main(){
    color = vec4(in_color, 1.0);
}
