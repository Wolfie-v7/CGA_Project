#version 330 core
in vec2 TexCoords;

uniform sampler2D ScreenTexture;
uniform sampler2D BloomTexture;

uniform bool negative;
uniform bool grayscale;
const float gamma = 2.2;
const float exposure = 1.0;

out vec4 color;
void main() {

    vec3 outputColor = texture(ScreenTexture, TexCoords).rgb;
    vec3 bloomColor = texture(BloomTexture, TexCoords).rgb;

    outputColor += bloomColor;
    if(negative) {
        outputColor = 1.0 - outputColor;
    }
    else if (grayscale) {
        outputColor = vec3(outputColor.r * 0.2126 + outputColor.g * 0.7152 + outputColor.b * 0.0722);
    }

    outputColor = vec3(1.0) - exp(-outputColor * exposure);

    outputColor = pow(outputColor, vec3(1.0 / gamma));

    //color = outputColor;
    //color.a = outputColor.a;
    color = vec4(outputColor, 1.0);

}
