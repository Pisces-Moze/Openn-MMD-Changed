package com.moze.openmmdchanged.mixin;

import com.moze.openmmdchanged.player.WaterFloatController;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在原版水中移动结算后应用玩家水面踩水锁定，客户端和服务端使用同一规则。 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow protected boolean jumping;

    @Inject(method = "tick", at = @At("TAIL"))
    private void openmmdchanged$stabilizeWaterFloat(CallbackInfo callback) {
        if ((Object) this instanceof Player player) {
            WaterFloatController.tick(player,
                    this.jumping || WaterFloatController.hasSyncedJumpInput(player));
        }
    }
}
