package com.shiroha.mmdskin.render.material;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** MMD 模型材质定义。 */
public class ModelMaterial {
    private static final String[] FACIAL_TOKENS = {
            "eye", "eyes", "eyeline", "eyelash", "eyelid", "iris", "pupil", "brow", "eyebrow",
            "mouth", "lip", "teeth", "tooth", "tongue", "gum", "lash", "highlight", "eyeshadow",
            "瞳", "目", "眉", "睫", "口", "唇", "牙", "舌", "ハイライト", "まつげ", "くち",
            "くちびる", "アイ", "アイライン", "アイラッシュ"
    };

    public int tex = 0;
    public int emissiveTex = 0;
    public boolean hasAlpha = false;
    public String name = "";
    public String texturePath = "";
    public ResourceLocation minecraftTexture;
    public ResourceLocation minecraftEmissionTexture;
    public boolean ownsTexture = false;
    public boolean lilUseShadow = true;
    /** Minimum view-independent contribution of the authored base texture. */
    public float lilBaseLightFloor = 0.0f;
    /** Direct counterpart of lilToon's _AsUnlit. */
    public float lilUnlitStrength = -1.0f;
    public float lilShadowBorder = 0.52f;
    public float lilShadowBlur = 0.075f;
    public final float[] lilShadowColor = {0.78f, 0.84f, 0.94f};
    public boolean lilUseRim = true;
    public float lilRimBorder = 0.70f;
    public float lilRimBlur = 0.20f;
    public float lilRimPower = 5.6f;
    public float lilRimIntensity = 0.02f;
    public final float[] lilRimColor = {0.30f, 0.78f, 0.92f};
    public boolean lilUseMatCap = false;
    public float lilMatCapStrength = 0.0f;
    /** Emission is opt-in per material; texture names and base colors never enable it. */
    public boolean lilUseEmission = false;
    public float lilEmissionStrength = -1.0f;
    public String lilEmissionTexture = "";
    public final float[] lilEmissionColor = {1.0f, 1.0f, 1.0f};
    public final List<EmissionLayerDefinition> lilEmissionLayers = new ArrayList<>();
    public final List<MinecraftEmissionLayer> minecraftEmissionLayers = new ArrayList<>();
    public String lilNormalTexture = "";
    public float lilNormalScale = 1.0f;
    public String lilCull = "model";
    public String lilRenderMode = "model";
    public float lilAlphaCutoff = 0.1f;
    public boolean lilUseOutline = true;
    public float lilOutlineWidth = -1.0f;
    public final float[] lilOutlineColor = {0.06f, 0.08f, 0.12f, 1.0f};

    public boolean hasEmission() {
        return emissiveTex > 0;
    }

    /** Both declarations are required so native and Minecraft render paths stay equivalent. */
    public boolean hasExplicitEmissionConfiguration() {
        return lilUseEmission
                && lilEmissionTexture != null
                && !lilEmissionTexture.isBlank()
                && !lilEmissionLayers.isEmpty();
    }

    public float emissionStrength() {
        if (!lilUseEmission) return 0.0f;
        return lilEmissionStrength >= 0.0f
                ? lilEmissionStrength
                : (isFacialFeature() ? 0.82f : 0.62f);
    }

    public float unlitStrength() {
        if (lilUnlitStrength >= 0.0f) return lilUnlitStrength;
        return isFacialFeature() ? 0.92f : 0.0f;
    }

    public float baseLightFloor() {
        return Math.max(0.0f, Math.min(1.0f, lilBaseLightFloor));
    }

    /** Material-name profile used by the lilToon compatibility renderer. */
    public float matCapStrength() {
        return lilUseMatCap ? lilMatCapStrength : 0.0f;
    }

    public void applyLilToonProfile(LilToonMaterialConfig.MaterialProfile p) {
        if (p.useShadow != null) lilUseShadow = p.useShadow;
        if (p.baseLightFloor != null) lilBaseLightFloor = clamp01(p.baseLightFloor);
        if (p.unlitStrength != null) lilUnlitStrength = clamp01(p.unlitStrength);
        if (p.shadowBorder != null) lilShadowBorder = p.shadowBorder;
        if (p.shadowBlur != null) lilShadowBlur = p.shadowBlur;
        copyRgb(p.shadowColor, lilShadowColor);
        if (p.useRim != null) lilUseRim = p.useRim;
        if (p.rimBorder != null) lilRimBorder = p.rimBorder;
        if (p.rimBlur != null) lilRimBlur = p.rimBlur;
        if (p.rimFresnelPower != null) lilRimPower = p.rimFresnelPower;
        if (p.rimIntensity != null) lilRimIntensity = p.rimIntensity;
        copyRgb(p.rimColor, lilRimColor);
        if (p.useMatCap != null) lilUseMatCap = p.useMatCap;
        if (p.matCapStrength != null) lilMatCapStrength = p.matCapStrength;
        if (p.useEmission != null) lilUseEmission = p.useEmission;
        if (p.emissionStrength != null) lilEmissionStrength = p.emissionStrength;
        if (p.emissionTexture != null) lilEmissionTexture = p.emissionTexture;
        copyRgb(p.emissionColor, lilEmissionColor);
        lilEmissionLayers.clear();
        if (p.emissionLayers != null) {
            for (LilToonMaterialConfig.EmissionLayerProfile layer : p.emissionLayers) {
                if (layer == null) continue;
                lilEmissionLayers.add(new EmissionLayerDefinition(layer));
            }
        }
        if (p.normalTexture != null) lilNormalTexture = p.normalTexture;
        if (p.normalScale != null) lilNormalScale = p.normalScale;
        if (p.cull != null) lilCull = p.cull;
        if (p.renderMode != null) lilRenderMode = p.renderMode;
        if (p.alphaCutoff != null) lilAlphaCutoff = p.alphaCutoff;
        if (p.useOutline != null) lilUseOutline = p.useOutline;
        if (p.outlineWidth != null) lilOutlineWidth = p.outlineWidth;
        copyRgba(p.outlineColor, lilOutlineColor);
    }

    public String configuredEmissionPath(String modelDir) {
        if (lilEmissionTexture == null || lilEmissionTexture.isBlank()) return "";
        return java.nio.file.Path.of(modelDir, lilEmissionTexture).toString();
    }

    private static void copyRgb(float[] source, float[] target) {
        if (source == null || source.length < 3) return;
        target[0] = source[0]; target[1] = source[1]; target[2] = source[2];
    }

    private static void copyRgba(float[] source, float[] target) {
        if (source == null || source.length < 3) return;
        target[0] = source[0]; target[1] = source[1]; target[2] = source[2];
        if (source.length >= 4) target[3] = source[3];
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    public static final class EmissionLayerDefinition {
        public final String texture;
        public final String maskMode;
        public final float strength;
        public final float[] color = {1.0f, 1.0f, 1.0f};
        public final float[] maskColor = {0.0f, 1.0f, 1.0f};
        public final float maskTolerance;
        public final float minBrightness;
        public final float maxBrightness;
        public final float minSaturation;
        public final List<float[]> uvRects = new ArrayList<>();
        public final boolean preserveSourceColor;

        private EmissionLayerDefinition(LilToonMaterialConfig.EmissionLayerProfile source) {
            texture = source.texture == null ? "$base" : source.texture;
            maskMode = source.maskMode == null ? "texture" : source.maskMode;
            strength = Math.max(0.0f, source.strength == null ? 1.0f : source.strength);
            copyRgb(source.color, color);
            copyRgb(source.maskColor, maskColor);
            maskTolerance = clamp01(source.maskTolerance == null ? 0.5f : source.maskTolerance);
            minBrightness = clamp01(source.minBrightness == null ? 0.45f : source.minBrightness);
            maxBrightness = clamp01(source.maxBrightness == null ? 1.0f : source.maxBrightness);
            minSaturation = clamp01(source.minSaturation == null ? 0.25f : source.minSaturation);
            if (source.uvRects != null) {
                for (float[] rect : source.uvRects) {
                    if (rect != null && rect.length >= 4) uvRects.add(rect.clone());
                }
            }
            preserveSourceColor = source.preserveSourceColor == null || source.preserveSourceColor;
        }
    }

    public static final class MinecraftEmissionLayer {
        public final ResourceLocation texture;
        public final float strength;
        public final float[] color;

        public MinecraftEmissionLayer(ResourceLocation texture, float strength, float[] color) {
            this.texture = texture;
            this.strength = strength;
            this.color = color.clone();
        }
    }

    public static String eyeTexturePath(String baseTexturePath) {
        if (baseTexturePath == null || baseTexturePath.isEmpty()) {
            return "";
        }
        int dot = baseTexturePath.lastIndexOf('.');
        return dot > baseTexturePath.lastIndexOf('/') && dot > baseTexturePath.lastIndexOf('\\')
                ? baseTexturePath.substring(0, dot) + "_eye.png"
                : baseTexturePath + "_eye.png";
    }

    private Boolean cachedIsFacialFeature;

    public boolean isFacialFeature() {
        if (cachedIsFacialFeature == null) {
            cachedIsFacialFeature = containsFacialToken(name) || containsFacialToken(texturePath);
        }
        return cachedIsFacialFeature;
    }

    private static boolean containsFacialToken(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        for (String token : FACIAL_TOKENS) {
            if (normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsAny(String first, String second, String... tokens) {
        String normalized = ((first == null ? "" : first) + " "
                + (second == null ? "" : second)).toLowerCase(Locale.ROOT);
        for (String token : tokens) {
            if (normalized.contains(token.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
