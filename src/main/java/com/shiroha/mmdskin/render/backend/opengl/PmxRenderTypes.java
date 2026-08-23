package com.shiroha.mmdskin.render.backend.opengl;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Minecraft-managed triangle RenderTypes for PMX materials. */
final class PmxRenderTypes extends RenderType {
    private static final int BUFFER_SIZE = 1 << 18;
    private static final Map<Key, RenderType> CACHE = new ConcurrentHashMap<>();

    private PmxRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode,
                           int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                           Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    static RenderType base(ResourceLocation texture, boolean cull, String renderMode, float alphaCutoff) {
        Blend blend = Blend.from(renderMode, alphaCutoff);
        return CACHE.computeIfAbsent(new Key(texture, Layer.BASE, blend, cull), PmxRenderTypes::create);
    }

    static RenderType emission(ResourceLocation texture, boolean cull) {
        return CACHE.computeIfAbsent(new Key(texture, Layer.EMISSION, Blend.ADDITIVE, cull), PmxRenderTypes::create);
    }

    private static RenderType create(Key key) {
        RenderType.CompositeState.CompositeStateBuilder builder = RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(key.texture, false, false))
                .setCullState(key.cull ? CULL : NO_CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST);

        switch (key.layer) {
            case BASE -> builder
                    .setShaderState(key.blend == Blend.TRANSLUCENT
                            ? RENDERTYPE_ENTITY_TRANSLUCENT_SHADER
                            : RENDERTYPE_ENTITY_CUTOUT_SHADER)
                    .setTransparencyState(key.blend == Blend.TRANSLUCENT
                            ? TRANSLUCENT_TRANSPARENCY : NO_TRANSPARENCY)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_DEPTH_WRITE);
            case EMISSION -> builder
                    .setShaderState(RENDERTYPE_EYES_SHADER)
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .setWriteMaskState(COLOR_WRITE);
        }

        return RenderType.create(
                "mmd_pmx_" + key.layer.name().toLowerCase(),
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.TRIANGLES,
                BUFFER_SIZE,
                false,
                key.layer != Layer.BASE || key.blend == Blend.TRANSLUCENT,
                builder.createCompositeState(false));
    }

    private enum Layer { BASE, EMISSION }

    private enum Blend {
        OPAQUE, CUTOUT, TRANSLUCENT, ADDITIVE;

        private static Blend from(String renderMode, float alphaCutoff) {
            String mode = renderMode == null ? "" : renderMode.trim().toLowerCase();
            if (mode.contains("trans")) return TRANSLUCENT;
            if (mode.contains("cutout") || mode.contains("clip") || alphaCutoff >= 0.0f) return CUTOUT;
            return OPAQUE;
        }
    }

    private record Key(ResourceLocation texture, Layer layer, Blend blend, boolean cull) {}
}
