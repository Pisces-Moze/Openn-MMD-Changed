package com.shiroha.mmdskin.render.material;

import java.util.Locale;

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
    public boolean ownsTexture = false;

    public boolean hasEmission() {
        return emissiveTex > 0;
    }

    public float emissionStrength() {
        return isFacialFeature() ? 0.82f : 0.62f;
    }

    public float unlitStrength() {
        return isFacialFeature() ? 0.92f : 0.0f;
    }

    /** Material-name profile used by the lilToon compatibility renderer. */
    public float matCapStrength() {
        if (isFacialFeature()) return 0.035f;
        if (containsAny(name, texturePath, "hair", "髪", "头发", "ヘア")) return 0.22f;
        if (containsAny(name, texturePath, "cloth", "dress", "衣", "服")) return 0.12f;
        return 0.075f;
    }

    public static String emissionTexturePath(String baseTexturePath) {
        if (baseTexturePath == null || baseTexturePath.isEmpty()) {
            return "";
        }
        int dot = baseTexturePath.lastIndexOf('.');
        return dot > baseTexturePath.lastIndexOf('/') && dot > baseTexturePath.lastIndexOf('\\')
                ? baseTexturePath.substring(0, dot) + "_emi.png"
                : baseTexturePath + "_emi.png";
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
