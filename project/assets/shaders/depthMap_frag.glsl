#version 330 core
in vec2 texCoords;
uniform sampler2D tex;

void main() {

    float alpha = texture(tex, texCoords).a;
    if(alpha < 0.5) {
        discard;
    }
    gl_FragDepth = gl_FragCoord.z;
}
