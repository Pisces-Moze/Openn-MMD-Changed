package com.shiroha.mmdskin.render.material;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Loads the model-local interchange format produced by the Unity/lilToon importer. */
public final class LilToonMaterialConfig {
    public static final String FILE_NAME = "liltoon_materials.json";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    public int schemaVersion = 1;
    public String sourcePrefab = "";
    public Map<String, MaterialProfile> materials = new LinkedHashMap<>();
    private transient Map<String, MaterialProfile> lookup = Collections.emptyMap();

    public static LilToonMaterialConfig load(String modelDir) {
        if (modelDir == null || modelDir.isBlank()) return empty();
        Path path;
        try {
            path = Path.of(modelDir, FILE_NAME);
        } catch (RuntimeException ignored) {
            return empty();
        }
        if (!Files.isRegularFile(path)) return empty();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            LilToonMaterialConfig result = GSON.fromJson(reader, LilToonMaterialConfig.class);
            if (result == null || result.schemaVersion != 1) {
                LOGGER.warn("Unsupported lilToon material config: {}", path);
                return empty();
            }
            result.buildLookup();
            LOGGER.info("Loaded {} lilToon material profiles from {}", result.materials.size(), path);
            return result;
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warn("Cannot load lilToon material config {}", path, e);
            return empty();
        }
    }

    public void apply(ModelMaterial material) {
        if (material == null) return;
        MaterialProfile profile = lookup.get(normalize(material.name));
        if (profile == null) {
            profile = lookup.get(normalize(textureStem(material.texturePath)));
        }
        if (profile != null) material.applyLilToonProfile(profile);
    }

    private void buildLookup() {
        Map<String, MaterialProfile> result = new LinkedHashMap<>();
        if (materials != null) {
            materials.forEach((name, profile) -> {
                if (profile == null) return;
                result.put(normalize(name), profile);
                if (profile.aliases != null) {
                    for (String alias : profile.aliases) result.put(normalize(alias), profile);
                }
            });
        }
        lookup = result;
    }

    private static LilToonMaterialConfig empty() {
        LilToonMaterialConfig result = new LilToonMaterialConfig();
        result.lookup = Collections.emptyMap();
        return result;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String textureStem(String value) {
        if (value == null) return "";
        String normalized = value.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        if (slash >= 0) normalized = normalized.substring(slash + 1);
        int dot = normalized.lastIndexOf('.');
        return dot > 0 ? normalized.substring(0, dot) : normalized;
    }

    public static final class MaterialProfile {
        public List<String> aliases = List.of();
        public Boolean useShadow;
        public Float shadowBorder;
        public Float shadowBlur;
        public float[] shadowColor;
        public Boolean useRim;
        public Float rimBorder;
        public Float rimBlur;
        public Float rimFresnelPower;
        public Float rimIntensity;
        public float[] rimColor;
        public Boolean useMatCap;
        public Float matCapStrength;
        public Boolean useEmission;
        public Float emissionStrength;
        public Float cyanEmissionStrength;
        public String emissionTexture;
        public float[] emissionColor;
        public String normalTexture;
        public Float normalScale;
        public String cull;
        public String renderMode;
        public Float alphaCutoff;
        public Boolean useOutline;
        public Float outlineWidth;
        public float[] outlineColor;
    }
}
