package com.moze.openmmdchanged;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.moze.openmmdchanged.client.MmdAssetInstaller;
import com.moze.openmmdchanged.registry.ModEntities;
import com.moze.openmmdchanged.registry.ModItems;
import com.moze.openmmdchanged.registry.ModTransfurVariants;
import com.moze.openmmdchanged.network.ModNetwork;
import com.moze.openmmdchanged.player.WaterFloatController;
import com.shiroha.mmdskin.MmdSkinClient;
import com.shiroha.mmdskin.model.runtime.ModelRequestKey;
import com.shiroha.mmdskin.render.bootstrap.ClientRenderRuntime;
import com.shiroha.mmdskin.ui.wheel.MorphWheelScreen;
import com.shiroha.mmdskin.ui.wheel.service.DefaultMorphWheelService;
import net.ltxprogrammer.changed.init.ChangedTabs;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.lwjgl.glfw.GLFW;

@Mod(OpenMmdChanged.MODID)
public final class OpenMmdChanged {
    public static final String MODID = "openmmdchanged";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OpenMmdChanged(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModEntities.REGISTRY.register(modBus);
        ModItems.REGISTRY.register(modBus);
        ModTransfurVariants.REGISTRY.register(modBus);
        ModNetwork.init();
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
        if (event.getTabKey().equals(ChangedTabs.TAB_CHANGED_ENTITIES.getKey())) {
            event.accept(ModItems.MMD_LATEX_SPAWN_EGG.get());
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientEvents {
        public static final KeyMapping EXPRESSION_WHEEL_KEY = new KeyMapping(
                "key.mmdskin.morph_wheel",
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "key.categories.mmdskin");

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
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(ClientEvents.EXPRESSION_WHEEL_KEY);
        }

        @SubscribeEvent
        public static void clientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                MmdAssetInstaller.installBundledAssets();
                MmdSkinClient.initClient();
                DefaultMorphWheelService.setCurrentModelResolver(player ->
                        ProcessTransfur.getPlayerTransfurVariantSafe(player)
                                .map(instance -> instance.getChangedEntity())
                                .map(entity -> ClientRenderRuntime.get().modelRepository().acquire(
                                        ModelRequestKey.mob(entity, MmdAssetInstaller.MODEL_FOLDER)))
                                .orElse(null));
            });
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeClientEvents {
        private static boolean expressionWheelKeyWasDown;
        private static boolean waterFloatKeyWasDown;

        private ForgeClientEvents() {
        }

        @SubscribeEvent
        public static void clientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                expressionWheelKeyWasDown = false;
                waterFloatKeyWasDown = false;
                return;
            }
            ClientRenderRuntime.get().modelRepository().tick();
            boolean jumpHeld = minecraft.player.input.jumping;
            WaterFloatController.tick(minecraft.player, jumpHeld);
            if (jumpHeld != waterFloatKeyWasDown) {
                ModNetwork.sendWaterFloatInput(jumpHeld);
                waterFloatKeyWasDown = jumpHeld;
            }
            if (minecraft.screen == null || minecraft.screen instanceof MorphWheelScreen) {
                boolean keyDown = ClientEvents.EXPRESSION_WHEEL_KEY.isDown();
                if (keyDown && !expressionWheelKeyWasDown && minecraft.screen == null) {
                    minecraft.setScreen(new MorphWheelScreen(ClientEvents.EXPRESSION_WHEEL_KEY));
                }
                expressionWheelKeyWasDown = keyDown;
            } else {
                expressionWheelKeyWasDown = false;
            }
        }
    }
}
