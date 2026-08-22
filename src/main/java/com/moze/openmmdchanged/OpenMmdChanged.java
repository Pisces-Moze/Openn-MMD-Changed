package com.moze.openmmdchanged;

import com.mojang.logging.LogUtils;
import com.moze.openmmdchanged.client.MmdAssetInstaller;
import com.moze.openmmdchanged.registry.ModEntities;
import com.moze.openmmdchanged.registry.ModItems;
import com.moze.openmmdchanged.registry.ModTransfurVariants;
import com.shiroha.mmdskin.MmdSkinClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(OpenMmdChanged.MODID)
public final class OpenMmdChanged {
    public static final String MODID = "openmmdchanged";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OpenMmdChanged(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModEntities.REGISTRY.register(modBus);
        ModItems.REGISTRY.register(modBus);
        ModTransfurVariants.REGISTRY.register(modBus);
        modBus.addListener(OpenMmdChanged::registerAttributes);
        modBus.addListener(OpenMmdChanged::addCreativeTabItems);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.MMD_LATEX.get(), ModEntities.createMmdLatexAttributes().build());
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.MMD_LATEX_SPAWN_EGG.get());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientEvents {
        private ClientEvents() {
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            com.moze.openmmdchanged.client.render.ModEntityRenderers.register(event);
        }

        @SubscribeEvent
        public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            com.moze.openmmdchanged.client.render.ModEntityRenderers.registerLayers(event);
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MmdAssetInstaller.installBundledAssets();
                MmdSkinClient.initClient();
            });
        }
    }
}
