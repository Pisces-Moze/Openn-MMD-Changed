package com.shiroha.mmdskin.texture.runtime;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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
        if (filename == null || filename.isBlank()) return null;
        try {
            Path path = Path.of(filename);
            if (!path.isAbsolute() && modelDirectory != null && !modelDirectory.isBlank()) {
                path = Path.of(modelDirectory).resolve(path);
            }
            String key = path.toAbsolutePath().normalize().toString();
            return CACHE.computeIfAbsent(key, MinecraftTextureRegistry::load);
        } catch (RuntimeException exception) {
            LOGGER.warn("Invalid PMX texture path: {}", filename);
            return null;
        }
    }

    private static ResourceLocation load(String filename) {
        try (InputStream stream = Files.newInputStream(Path.of(filename))) {
            DynamicTexture texture = new DynamicTexture(NativeImage.read(stream));
            return Minecraft.getInstance().getTextureManager().register(
                    "pmx_" + Integer.toUnsignedString(filename.hashCode(), 36), texture);
        } catch (Exception exception) {
            LOGGER.warn("Cannot register PMX texture with Minecraft: {}", filename);
            return null;
        }
    }
}
