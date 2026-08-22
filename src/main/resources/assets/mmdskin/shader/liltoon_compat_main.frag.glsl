#version 330 core

// Minecraft/OpenGL compatibility port inspired by lilToon's avatar material model.
// Unity-specific lighting, ShaderLab passes and render-pipeline macros are replaced
// with explicit GLSL inputs supplied by the embedded MMD renderer.

in vec2 texCoord0;
in vec3 viewNormal;
in vec3 viewPos;
in vec3 viewLightDir;

uniform sampler2D Sampler0;
uniform float LightIntensity;
uniform float RimPower;
uniform float RimIntensity;
uniform vec3 ShadowColor;
uniform float SpecularPower;
uniform float SpecularIntensity;
uniform float AlphaCutoff;
uniform float MaterialUnlit;
uniform float EmissionStrength;
uniform float ShadowBorder;
uniform float ShadowBlur;
uniform float MatCapStrength;
uniform vec3 RimColor;
uniform float HurtFactor;
uniform int UseShadow;
uniform int UseRim;
uniform float RimBorder;
uniform float RimBlur;

layout(location = 0) out vec4 fragColor;

float saturate(float v) {
    return clamp(v, 0.0, 1.0);
}

vec3 softLight(vec3 base, vec3 blend) {
    return mix(
        2.0 * base * blend + base * base * (1.0 - 2.0 * blend),
        sqrt(max(base, vec3(0.0))) * (2.0 * blend - 1.0) + 2.0 * base * (1.0 - blend),
        step(vec3(0.5), blend)
    );
}

void main() {
    vec4 mainTex = texture(Sampler0, texCoord0);
    if (mainTex.a < AlphaCutoff) discard;

    vec3 albedo = mainTex.rgb;
    vec3 n = normalize(viewNormal);
    vec3 l = normalize(viewLightDir);
    vec3 v = normalize(-viewPos);
    float ndl = dot(n, l) * 0.5 + 0.5;

    // lilToon-like controllable shadow border. Keeping the tint multiplicative
    // preserves the authored blue/green body colour in both daylight and night.
    float border = clamp(ShadowBorder, 0.02, 0.98);
    float blur = max(ShadowBlur, 0.002);
    float shadowStep = UseShadow != 0
            ? smoothstep(border - blur, border + blur, ndl)
            : 1.0;
    float environmentLight = clamp(LightIntensity, 0.0, 1.0);
    // The base layer follows Minecraft's environment light. Dedicated emission
    // textures are rendered in a separate additive pass and remain fullbright.
    float baseLight = mix(0.16, 1.0, environmentLight);
    float litBand = shadowStep * mix(0.025, 0.10, environmentLight);
    vec3 lightTint = mix(clamp(ShadowColor, vec3(0.0), vec3(1.0)), vec3(1.0), 0.88);
    float materialLight = mix(baseLight, 1.0, saturate(MaterialUnlit));
    vec3 color = albedo * materialLight
            + albedo * lightTint * litBand * (1.0 - saturate(MaterialUnlit));

    // View-space MatCap approximation. This requires no Unity reflection probe
    // and remains deterministic in inventory and first-person rendering.
    vec2 matUv = n.xy * 0.5 + 0.5;
    float matBand = pow(saturate(1.0 - length(matUv - vec2(0.42, 0.62)) * 1.75), 4.0);
    vec3 matTint = mix(albedo, softLight(albedo, vec3(0.72, 0.88, 1.0)), 0.75);
    color += matTint * matBand * MatCapStrength * shadowStep;

    vec3 h = normalize(l + v);
    float spec = pow(saturate(dot(n, h)), max(SpecularPower, 1.0));
    float specBand = smoothstep(0.42, 0.58, spec);
    color += vec3(0.82, 0.94, 1.0) * specBand * SpecularIntensity;

    float fresnel = pow(1.0 - saturate(dot(n, v)), max(RimPower, 0.5));
    float rimEdge = smoothstep(
            clamp(RimBorder - RimBlur, 0.0, 1.0),
            clamp(RimBorder + RimBlur, 0.001, 1.0),
            fresnel);
    float rim = (UseRim != 0 ? rimEdge : 0.0) * RimIntensity;
    color += RimColor * rim;

    // Optional, material-scoped fluorescent coating. It is disabled by default
    // and only extracts saturated cyan from the base texture when explicitly
    // requested by liltoon_materials.json.
    float hi = max(albedo.r, max(albedo.g, albedo.b));
    float lo = min(albedo.r, min(albedo.g, albedo.b));
    float chroma = hi - lo;
    float cyanHue = smoothstep(albedo.r * 1.08 + 0.015,
            albedo.r * 1.30 + 0.035, min(albedo.g, albedo.b));
    float cyanMask = smoothstep(0.16, 0.34, chroma)
            * smoothstep(0.42, 0.70, hi) * cyanHue;
    color += albedo * cyanMask * max(EmissionStrength, 0.0) * 1.20;

    color = mix(color, vec3(max(color.r, 0.72), color.g * 0.32, color.b * 0.32),
                saturate(HurtFactor));
    fragColor = vec4(color, mainTex.a);
}
