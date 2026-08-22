#version 330 core

in vec2 texCoord0;

uniform sampler2D Sampler0;
uniform float Intensity;
uniform float Opacity;
uniform int CyanMask;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 texColor = texture(Sampler0, texCoord0);
    float mask = 1.0;
    if (CyanMask != 0) {
        float hi = max(texColor.r, max(texColor.g, texColor.b));
        float lo = min(texColor.r, min(texColor.g, texColor.b));
        float chroma = hi - lo;
        float cyanHue = smoothstep(texColor.r * 1.08 + 0.015,
                texColor.r * 1.30 + 0.035, min(texColor.g, texColor.b));
        mask = smoothstep(0.16, 0.34, chroma)
                * smoothstep(0.42, 0.70, hi) * cyanHue;
    }
    float alpha = texColor.a * Opacity * mask;
    if (alpha < 0.004) {
        discard;
    }

    fragColor = vec4(texColor.rgb * Intensity * mask, alpha);
}
