#version 330 core

layout(location = 0) in vec3 Position;
layout(location = 2) in vec2 UV0;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform float DepthBias;

out vec2 texCoord0;

void main() {
    vec4 clipPosition = ProjMat * ModelViewMat * vec4(Position, 1.0);
    clipPosition.z -= DepthBias * clipPosition.w;
    gl_Position = clipPosition;
    texCoord0 = UV0;
}
