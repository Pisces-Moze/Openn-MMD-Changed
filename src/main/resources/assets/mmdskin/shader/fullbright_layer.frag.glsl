#version 330 core

in vec2 texCoord0;

uniform sampler2D Sampler0;
uniform float Intensity;
uniform float Opacity;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    float alpha = texColor.a * Opacity;
    if (alpha < 0.004) {
        discard;
    }

    fragColor = vec4(texColor.rgb * Intensity, alpha);
}
