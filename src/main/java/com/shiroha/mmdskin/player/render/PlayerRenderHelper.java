package com.shiroha.mmdskin.player.render;

import com.shiroha.mmdskin.model.runtime.ManagedModel;
import com.shiroha.mmdskin.model.runtime.ModelRenderProperties;
import com.shiroha.mmdskin.player.runtime.FirstPersonManager;
import com.shiroha.mmdskin.render.scene.MutableRenderPose;
import com.shiroha.mmdskin.util.WaterSurfaceUtil;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;

/** 文件职责：集中计算玩家模型渲染姿态与模型属性读取。 */
public final class PlayerRenderHelper {

    private PlayerRenderHelper() {}

    public static MutableRenderPose calculateMutableRenderPose(AbstractClientPlayer player, ManagedModel modelData, float tickDelta) {
        MutableRenderPose params = new MutableRenderPose();
        ModelRenderProperties renderProperties = modelData.renderProperties();
        float vrBodyYaw = FirstPersonManager.vrRuntime().getBodyYawDegrees(player, tickDelta);
        params.bodyYaw = Float.isFinite(vrBodyYaw) ? vrBodyYaw : player.yBodyRot;
        params.bodyPitch = 0.0f;
        params.translation.zero();

        if (player.isFallFlying()) {
            params.bodyPitch = player.getXRot() + renderProperties.flyingPitch();
            params.translation.set(renderProperties.flyingTranslation());
        } else if (player.isSleeping()) {
            params.bodyYaw = player.getBedOrientation().toYRot() + 180.0f;
            params.bodyPitch = renderProperties.sleepingPitch();
            params.translation.set(renderProperties.sleepingTranslation());
        } else if (player.isSwimming() && hasMovement(player)) {
            params.bodyPitch = player.getXRot() + renderProperties.swimmingPitch();
            params.translation.set(renderProperties.swimmingTranslation());
        } else if (WaterSurfaceUtil.isFloating(player)) {
            // 漂浮：把模型钉在水面（身体在水中、头/手露出），无视碰撞箱上下浮动。
            params.bodyPitch = renderProperties.floatingPitch();
            params.translation.set(renderProperties.floatingTranslation());
            double stableY = WaterSurfaceUtil.isSurfaceLocked(player)
                    ? WaterSurfaceUtil.stableModelY(player, renderProperties.floatingDepth())
                    : Double.NaN;
            if (!Double.isNaN(stableY)) {
                // 用稳定脚部锚点抵消原版长按跳跃踩水时的碰撞箱起伏。
                double interpolatedY = Mth.lerp(tickDelta, player.yo, player.getY());
                params.translation.y += (float) (stableY - interpolatedY);
            }
        } else if (player.isVisuallyCrawling()) {
            params.bodyPitch = renderProperties.crawlingPitch();
            params.translation.set(renderProperties.crawlingTranslation());
        }

        return params;
    }

    private static boolean hasMovement(AbstractClientPlayer player) {
        return player.getX() - player.xo != 0.0f || player.getZ() - player.zo != 0.0f;
    }

    public static float[] getModelSize(ManagedModel modelData) {
        return new float[] {
                modelData.renderProperties().modelScale(),
                modelData.renderProperties().inventoryScale()
        };
    }
}
