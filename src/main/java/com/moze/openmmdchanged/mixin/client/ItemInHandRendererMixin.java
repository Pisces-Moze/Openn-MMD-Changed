package com.moze.openmmdchanged.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.shiroha.mmdskin.player.runtime.FirstPersonManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppresses only the vanilla/Changed arm; held items continue to render normally. */
@Mixin(value = ItemInHandRenderer.class, priority = 850)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true, require = 0)
    private void openmmdchanged$useFullMmdFirstPerson(float partialTick, PoseStack poseStack,
                                                       MultiBufferSource.BufferSource buffer,
                                                       LocalPlayer player, int packedLight,
                                                       CallbackInfo callback) {
        if (FirstPersonManager.shouldRenderFirstPerson()) {
            callback.cancel();
        }
    }
}
