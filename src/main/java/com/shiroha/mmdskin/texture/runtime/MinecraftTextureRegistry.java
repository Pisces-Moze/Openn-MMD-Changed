package com.shiroha.mmdskin.texture.runtime;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Registers model-local textures with Minecraft so Iris/Oculus can see them. */
public final class MinecraftTextureRegistry {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();

    private MinecraftTextureRegistry() {}

    public static ResourceLocation get(String filename, String modelDirectory) {
        return getFiltered(filename, modelDirectory, "texture", null, 0.0f, 0.0f, 0.0f);
    }

    public static ResourceLocation getFiltered(String filename, String modelDirectory,
                                               String maskMode, float[] maskColor,
                                               float tolerance, float minBrightness,
                                               float minSaturation) {
        if (filename == null || filename.isBlank()) return null;
        try {
            Path path = Path.of(filename);
            if (!path.isAbsolute() && modelDirectory != null && !modelDirectory.isBlank()) {
                path = Path.of(modelDirectory).resolve(path);
            }
            String resolved = path.toAbsolutePath().normalize().toString();
            String mode = maskMode == null ? "texture" : maskMode.trim().toLowerCase();
            String key = resolved + "|" + mode + "|" + Arrays.toString(maskColor)
                    + "|" + tolerance + "|" + minBrightness + "|" + minSaturation;
            return CACHE.computeIfAbsent(key, ignored -> load(resolved, mode, maskColor,
                    tolerance, minBrightness, minSaturation));
        } catch (RuntimeException exception) {
            LOGGER.warn("Invalid PMX texture path: {}", filename);
            return null;
        }
    }

    private static ResourceLocation load(String filename, String maskMode, float[] maskColor,
                                         float tolerance, float minBrightness,
                                         float minSaturation) {
        try (InputStream stream = Files.newInputStream(Path.of(filename))) {
            NativeImage image = NativeImage.read(stream);
            if (!"texture".equals(maskMode) && !"none".equals(maskMode)) {
                filterEmission(image, maskMode, maskColor, tolerance,
                        minBrightness, minSaturation);
            }
            DynamicTexture texture = new DynamicTexture(image);
            return Minecraft.getInstance().getTextureManager().register(
                    "pmx_" + Integer.toUnsignedString(filename.hashCode(), 36), texture);
        } catch (Exception exception) {
            LOGGER.warn("Cannot register PMX texture with Minecraft: {}", filename);
            return null;
        }
    }

    private static void filterEmission(NativeImage image, String mode, float[] requestedColor,
                                       float tolerance, float minBrightness,
                                       float minSaturation) {
        float[] target = "cyan".equals(mode)
                ? new float[] {0.0f, 1.0f, 1.0f}
                : normalizedColor(requestedColor);
        float allowedDistance = Math.max(0.01f, tolerance);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int red = pixel & 0xff;
                int green = (pixel >>> 8) & 0xff;
                int blue = (pixel >>> 16) & 0xff;
                int alpha = (pixel >>> 24) & 0xff;
                float max = Math.max(red, Math.max(green, blue)) / 255.0f;
                float min = Math.min(red, Math.min(green, blue)) / 255.0f;
                float saturation = max <= 0.0f ? 0.0f : (max - min) / max;
                float scale = Math.max(1.0f, Math.max(red, Math.max(green, blue)));
                float nr = red / scale;
                float ng = green / scale;
                float nb = blue / scale;
                float distance = (float) Math.sqrt(square(nr - target[0])
                        + square(ng - target[1]) + square(nb - target[2]));
                if (alpha == 0 || max < minBrightness || saturation < minSaturation
                        || distance > allowedDistance) {
                    image.setPixelRGBA(x, y, 0);
                }
            }
        }
    }

    private static float[] normalizedColor(float[] color) {
        if (color == null || color.length < 3) return new float[] {0.0f, 1.0f, 1.0f};
        float max = Math.max(0.0001f, Math.max(color[0], Math.max(color[1], color[2])));
        return new float[] {color[0] / max, color[1] / max, color[2] / max};
    }

    private static float square(float value) {
        return value * value;
    }
}
