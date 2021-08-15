#version 330 core

layout(location = 0) in vec3 position;

uniform mat4 view_matrix;
uniform mat4 projection_matrix;

uniform vec4 clipPlane;
out vec3 TexCoords;

void main(){

    vec4 worldPosition = vec4(position, 1.0f);
    gl_ClipDistance[0] = dot(worldPosition, clipPlane);

    gl_Position = (projection_matrix * view_matrix * vec4(position, 1.0f)).xyww;
    TexCoords =  position;

}
