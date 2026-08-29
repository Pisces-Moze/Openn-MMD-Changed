package com.moze.openmmdchanged.player;

import com.moze.openmmdchanged.registry.ModTransfurVariants;
import com.shiroha.mmdskin.util.WaterSurfaceUtil;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.WeakHashMap;

/** 将本模组形态的持续踩水转换为稳定水面漂浮，并在靠近可上岸碰撞面时自动释放。 */
public final class WaterFloatController {
    private static final double SHORE_HORIZONTAL_MARGIN = 0.70;
    private static final Map<Player, Boolean> SYNCED_JUMP_INPUT = new WeakHashMap<>();

    private WaterFloatController() {
    }

    public static void tick(Player player, boolean jumpHeld) {
        if (!isEligibleForm(player) || !jumpHeld || player.isPassenger()
                || player.isFallFlying() || player.getAbilities().flying) {
            WaterSurfaceUtil.setSurfaceLocked(player, false);
            return;
        }

        double surfaceY = WaterSurfaceUtil.computeSurfaceY(player);
        if (!WaterSurfaceUtil.isFloating(player) || Double.isNaN(surfaceY)
                || hasReachableShore(player, surfaceY)) {
            WaterSurfaceUtil.setSurfaceLocked(player, false);
            return;
        }

        WaterSurfaceUtil.setSurfaceLocked(player, true);

        double immersion = Math.min(WaterSurfaceUtil.DEFAULT_IMMERSION_DEPTH,
                Math.max(0.0, player.getBbHeight() - 0.05));
        double targetY = surfaceY - immersion;
        if (Math.abs(player.getY() - targetY) > 1.0E-4) {
            player.setPos(player.getX(), targetY, player.getZ());
        }

        Vec3 velocity = player.getDeltaMovement();
        if (Math.abs(velocity.y) > 1.0E-5) {
            player.setDeltaMovement(velocity.x, 0.0, velocity.z);
            player.hasImpulse = true;
        }
        player.fallDistance = 0.0F;
    }

    public static void setSyncedJumpInput(Player player, boolean held) {
        synchronized (SYNCED_JUMP_INPUT) {
            if (held) {
                SYNCED_JUMP_INPUT.put(player, true);
            } else {
                SYNCED_JUMP_INPUT.remove(player);
            }
        }
    }

    public static boolean hasSyncedJumpInput(Player player) {
        synchronized (SYNCED_JUMP_INPUT) {
            return SYNCED_JUMP_INPUT.getOrDefault(player, false);
        }
    }

    private static boolean isEligibleForm(Player player) {
        return ProcessTransfur.getPlayerTransfurVariantSafe(player)
                .map(instance -> instance.getParent() == ModTransfurVariants.MMD_LATEX.get())
                .orElse(false);
    }

    private static boolean hasReachableShore(Player player, double surfaceY) {
        AABB body = player.getBoundingBox();
        AABB probe = new AABB(
                body.minX - SHORE_HORIZONTAL_MARGIN,
                surfaceY - 0.15,
                body.minZ - SHORE_HORIZONTAL_MARGIN,
                body.maxX + SHORE_HORIZONTAL_MARGIN,
                surfaceY + 1.25,
                body.maxZ + SHORE_HORIZONTAL_MARGIN);
        for (VoxelShape collision : player.level().getBlockCollisions(player, probe)) {
            if (!collision.isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
