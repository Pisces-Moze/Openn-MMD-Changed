package com.moze.openmmdchanged.client;

import com.moze.openmmdchanged.OpenMmdChanged;
import com.shiroha.mmdskin.asset.catalog.ModelCatalogEntry;
import com.shiroha.mmdskin.config.PathConstants;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Extracts immutable, mod-owned MMD assets into the directory consumed by MMDSkin. */
public final class MmdAssetInstaller {
    public static final String MODEL_FOLDER = "openmmdchanged.mmd_latex";
    private static final String RESOURCE_ROOT = "/assets/openmmdchanged/mmd/mmd_latex/";
    private static final String MANIFEST = RESOURCE_ROOT + "assets.list";

    private MmdAssetInstaller() {
    }

    public static void installBundledAssets() {
        Path targetRoot = PathConstants.getModelDir(MODEL_FOLDER).toPath();
        try (InputStream stream = MmdAssetInstaller.class.getResourceAsStream(MANIFEST)) {
            if (stream == null) {
                OpenMmdChanged.LOGGER.warn("Missing bundled MMD asset manifest: {}", MANIFEST);
                return;
            }

            Files.createDirectories(targetRoot);
            int installed = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                for (String rawLine; (rawLine = reader.readLine()) != null;) {
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    Path relative = Path.of(line).normalize();
                    if (relative.isAbsolute() || relative.startsWith("..")) {
                        throw new IOException("Unsafe path in MMD asset manifest: " + line);
                    }

                    try (InputStream asset = MmdAssetInstaller.class.getResourceAsStream(RESOURCE_ROOT + line.replace('\\', '/'))) {
                        if (asset == null) {
                            OpenMmdChanged.LOGGER.warn("Bundled MMD asset is listed but missing: {}", line);
                            continue;
                        }
                        Path target = targetRoot.resolve(relative).normalize();
                        if (!target.startsWith(targetRoot)) {
                            throw new IOException("MMD asset escaped target directory: " + line);
                        }
                        Files.createDirectories(target.getParent());
                        Files.copy(asset, target, StandardCopyOption.REPLACE_EXISTING);
                        installed++;
                    }
                }
            }

            ModelCatalogEntry.invalidateCache();
            if (installed == 0) {
                OpenMmdChanged.LOGGER.info("No PMX/PMD assets are bundled yet for {}", MODEL_FOLDER);
            } else {
                OpenMmdChanged.LOGGER.info("Installed {} bundled MMD assets for {}", installed, MODEL_FOLDER);
            }
        } catch (IOException exception) {
            OpenMmdChanged.LOGGER.error("Failed to install bundled MMD assets for " + MODEL_FOLDER, exception);
        }
    }
}
