package com.moze.openmmdchanged.mixin.client;

import com.shiroha.mmdskin.config.RuntimeConfigPort;
import com.shiroha.mmdskin.config.RuntimeConfigPortHolder;
import com.shiroha.mmdskin.player.runtime.FirstPersonManager;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** MC-MMD-rust desktop first-person eye-bone camera path. */
@Mixin(value = Camera.class, priority = 900)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "setup", at = @At("TAIL"))
    private void openmmdchanged$useMmdEyeCamera(BlockGetter level, Entity entity,
                                                boolean detached, boolean mirrored,
                                                float partialTick, CallbackInfo callback) {
        if (!FirstPersonManager.isEyeCameraActive()
                || !FirstPersonManager.isEyeBoneValid() || detached) {
            return;
        }

        Vec3 eye = FirstPersonManager.getRotatedEyePosition(entity, partialTick);
        float yaw = entity.getViewYRot(partialTick);
        float pitch = entity.getViewXRot(partialTick);
        float pitchRad = pitch * ((float) Math.PI / 180F);
        float yawRad = yaw * ((float) Math.PI / 180F);
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        RuntimeConfigPort config = RuntimeConfigPortHolder.get();
        double forward = config.getFirstPersonCameraForwardOffset();
        double vertical = config.getFirstPersonCameraVerticalOffset();
        double offsetY = vertical * cosPitch - forward * sinPitch;
        double horizontal = vertical * sinPitch + forward * cosPitch;
        Vec3 finalPosition = new Vec3(
                eye.x + sinYaw * -horizontal,
                eye.y + offsetY,
                eye.z + cosYaw * horizontal);
        FirstPersonManager.setLastCameraPos(finalPosition);
        this.setPosition(finalPosition.x, finalPosition.y, finalPosition.z);
        this.setRotation(yaw, pitch);
    }
}
