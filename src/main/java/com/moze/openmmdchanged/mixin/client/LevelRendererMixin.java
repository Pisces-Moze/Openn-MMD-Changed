package com.moze.openmmdchanged.mixin.client;

import com.shiroha.mmdskin.player.runtime.FirstPersonManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Makes Minecraft render the local Changed/MMD form while in first person. */
@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class LevelRendererMixin {
    @Redirect(
            method = "renderLevel",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z", ordinal = 0),
            require = 0)
    private boolean openmmdchanged$renderLocalMmdForm(Camera camera) {
        if (camera.getEntity() instanceof Player && FirstPersonManager.shouldRenderFirstPerson()) {
            return camera.getXRot() >= 0.0F;
        }
        return camera.isDetached();
    }
}
