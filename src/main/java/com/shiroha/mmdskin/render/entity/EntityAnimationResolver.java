package com.shiroha.mmdskin.render.entity;

import com.shiroha.mmdskin.model.runtime.ManagedModel;
import com.shiroha.mmdskin.player.runtime.EntityAnimState;
import com.shiroha.mmdskin.render.scene.MutableRenderPose;
import com.shiroha.mmdskin.util.WaterSurfaceUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * 通用实体动画状态解析器。
 */
public final class EntityAnimationResolver {

    private EntityAnimationResolver() {
    }

    public static void resolve(Entity entity, ManagedModel model,
                                float entityYaw, float tickDelta, MutableRenderPose params) {

        if (entity instanceof LivingEntity living) {
            params.bodyYaw = Mth.rotLerp(tickDelta, living.yBodyRotO, living.yBodyRot);
        } else {
            params.bodyYaw = entityYaw;
        }
        params.bodyPitch = 0.0f;
        params.translation.zero();

        if (entity instanceof LivingEntity living) {
            if (living.getHealth() <= 0.0f) {
                changeAnimOnce(model, EntityAnimState.State.Die, 0);
                return;
            }
            if (living.isSleeping()) {
                params.bodyYaw = living.getBedOrientation().toYRot() + 180.0f;
                params.bodyPitch = model.renderProperties().sleepingPitch();
                params.translation.set(model.renderProperties().sleepingTranslation());
                changeAnimOnce(model, EntityAnimState.State.Sleep, 0);
                return;
            }
        }

        boolean hasMovement = entity.getX() - entity.xo != 0.0f
                           || entity.getZ() - entity.zo != 0.0f;

        if (entity.isVehicle() && hasMovement) {
            changeAnimOnce(model, EntityAnimState.State.Driven, 0);
        } else if (entity.isVehicle()) {
            changeAnimOnce(model, EntityAnimState.State.Ridden, 0);
        } else if (WaterSurfaceUtil.isSurfaceLocked(entity)) {
            applyFloatingPose(entity, model, tickDelta, params);
        } else if (entity.isSwimming() && hasMovement) {
            changeAnimOnce(model, EntityAnimState.State.Swim, 0);
        } else if (WaterSurfaceUtil.isFloating(entity)) {
            applyFloatingPose(entity, model, tickDelta, params);
        } else if (entity instanceof LivingEntity living && living.isShiftKeyDown()) {
            changeAnimOnce(model, EntityAnimState.State.Sneak, 0);
            // 移动时从当前帧继续播放；停下时保留当前帧，不跳回起手帧。
            model.modelInstance().setLayerLoop(0, hasMovement);
            if (hasMovement) {
                model.modelInstance().resumeLayer(0);
            } else {
                model.modelInstance().pauseLayer(0);
            }
        } else if (hasMovement && entity.getVehicle() == null
                && entity instanceof LivingEntity living && living.isSprinting()) {
            changeAnimOnce(model, EntityAnimState.State.Sprint, 0);
        } else if (hasMovement && entity.getVehicle() == null) {
            changeAnimOnce(model, EntityAnimState.State.Walk, 0);
        } else {
            changeAnimOnce(model, EntityAnimState.State.Idle, 0);
        }
    }

    private static void applyFloatingPose(Entity entity, ManagedModel model,
                                          float tickDelta, MutableRenderPose params) {
        params.bodyPitch = model.renderProperties().floatingPitch();
        params.translation.set(model.renderProperties().floatingTranslation());
        if (WaterSurfaceUtil.isSurfaceLocked(entity)) {
            double stableY = WaterSurfaceUtil.stableModelY(entity, model.renderProperties().floatingDepth());
            if (!Double.isNaN(stableY)) {
                double interpolatedY = Mth.lerp(tickDelta, entity.yo, entity.getY());
                params.translation.y += (float) (stableY - interpolatedY);
            }
        }
        model.modelInstance().setLayerLoop(0, true);
        model.modelInstance().resumeLayer(0);
        changeAnimOnce(model, EntityAnimState.State.Float, 0);
    }

    private static void changeAnimOnce(ManagedModel model,
                                        EntityAnimState.State targetState, int layer) {
        if (model.entityState().stateLayers[layer] != targetState) {
            String property = EntityAnimState.getPropertyName(targetState);
            long anim = model.animationLibrary().animation(property);
            if (anim == 0 && targetState != EntityAnimState.State.Idle) {
                // 动画缺失：保留当前动画，避免切换到空动画导致模型定格
                return;
            }
            model.entityState().stateLayers[layer] = targetState;
            model.modelInstance().resumeLayer(layer);
            model.modelInstance().changeAnim(anim, layer);
        }
    }
}
