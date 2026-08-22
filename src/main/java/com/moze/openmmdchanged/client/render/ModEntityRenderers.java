package com.moze.openmmdchanged.client.render;

import com.moze.openmmdchanged.registry.ModEntities;
import net.minecraftforge.client.event.EntityRenderersEvent;

public final class ModEntityRenderers {
    private ModEntityRenderers() {
    }

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MMD_LATEX.get(), MmdLatexRenderer::new);
    }

    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ChangedMmdBridgeModel.LAYER, ChangedMmdBridgeModel::createBodyLayer);
    }
}
