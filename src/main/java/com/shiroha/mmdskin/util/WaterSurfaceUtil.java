package com.shiroha.mmdskin.util;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import java.util.Map;
import java.util.WeakHashMap;

/** 文件职责：识别水面漂浮状态并提供不受实体上下浮动影响的稳定锚点。 */
public final class WaterSurfaceUtil {

    /** 默认浸水深度：让头部完全露出水面，漂浮动作中的手部约一半位于水面以上。 */
    public static final float DEFAULT_IMMERSION_DEPTH = 1.25f;
    private static final Map<Entity, SurfaceSample> SURFACE_CACHE = new WeakHashMap<>();
    private static final Map<Entity, Boolean> SURFACE_LOCKS = new WeakHashMap<>();

    private WaterSurfaceUtil() {
    }

    /**
     * 计算实体所在位置的水面高度（最顶层的 Water 方块顶面 Y）。
     * 若实体不在水中 / 附近无水面，返回 {@link Double#NaN}。
     */
    public static double computeSurfaceY(Entity entity) {
        if (entity == null) {
            return Double.NaN;
        }
        BlockPos feet = entity.blockPosition();
        synchronized (SURFACE_CACHE) {
            SurfaceSample cached = SURFACE_CACHE.get(entity);
            if (cached != null && cached.tick == entity.tickCount
                    && cached.x == feet.getX() && cached.z == feet.getZ()
                    && cached.feetY == feet.getY()) {
                return cached.surfaceY;
            }
        }

        double surfaceY = scanSurfaceY(entity, feet);
        synchronized (SURFACE_CACHE) {
            SURFACE_CACHE.put(entity,
                    new SurfaceSample(entity.tickCount, feet.getX(), feet.getY(), feet.getZ(), surfaceY));
        }
        return surfaceY;
    }

    private static double scanSurfaceY(Entity entity, BlockPos feet) {
        Level level = entity.level();
        int feetX = feet.getX();
        int feetZ = feet.getZ();
        int feetY = feet.getY();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        double topWaterY = Double.NaN;
        // 漂浮只关心水面附近。限制扫描范围，避免在每个实体的渲染热路径扫描整根水柱。
        int maxDy = Math.max(4, (int) Math.ceil(entity.getBbHeight()) + 2);
        for (int dy = -1; dy <= maxDy; dy++) {
            cursor.set(feetX, feetY + dy, feetZ);
            FluidState fluid = level.getFluidState(cursor);
            boolean water = !fluid.isEmpty() && fluid.is(FluidTags.WATER);
            if (water) {
                topWaterY = feetY + dy + fluid.getHeight(level, cursor);
            } else if (!Double.isNaN(topWaterY)) {
                return topWaterY;
            }
        }
        return topWaterY;
    }

    /** 实体身体是否位于水面附近；深潜、水底站立不属于漂浮。 */
    public static boolean isNearSurface(Entity entity) {
        double surfaceY = computeSurfaceY(entity);
        if (Double.isNaN(surfaceY)) {
            return false;
        }
        double immersion = surfaceY - entity.getY();
        // 原版踩水可能在一个 tick 内把脚部略微推到水面上方，因此保留少量向上容差。
        return immersion >= -0.35 && immersion <= entity.getBbHeight() + 0.35;
    }

    /**
     * 实体是否真正漂浮：在水里且没有站在方块上（未接地）。
     * 排除明确站到方块（水底/半砖/活板门）的情况——这些时候玩家是“站”在方块上，不应触发漂浮。
     * 仅在身体靠近水面时触发，深水游泳不会错误播放漂浮动画。
     */
    public static boolean isFloating(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.onGround()) {
            return false;
        }
        return isNearSurface(entity);
    }

    /** 模型脚部的稳定世界坐标；渲染时用它抵消原版踩水产生的碰撞箱上下晃动。 */
    public static double stableModelY(Entity entity, float immersionDepth) {
        double surfaceY = computeSurfaceY(entity);
        return Double.isNaN(surfaceY) ? Double.NaN : surfaceY - Math.max(0.0f, immersionDepth);
    }

    /** 第一人称相机的稳定高度，保留正常眼高，但去除原版水面踩水的垂直晃动。 */
    public static double stableCameraY(Entity entity) {
        double feetY = stableModelY(entity, Math.min(DEFAULT_IMMERSION_DEPTH, entity.getBbHeight()));
        return Double.isNaN(feetY) ? Double.NaN : feetY + entity.getEyeHeight();
    }

    /** 由实际移动控制器发布锁定状态，渲染和相机不得只凭“靠近水面”自行锁定。 */
    public static void setSurfaceLocked(Entity entity, boolean locked) {
        if (entity == null) {
            return;
        }
        synchronized (SURFACE_LOCKS) {
            if (locked) {
                SURFACE_LOCKS.put(entity, true);
            } else {
                SURFACE_LOCKS.remove(entity);
            }
        }
    }

    public static boolean isSurfaceLocked(Entity entity) {
        synchronized (SURFACE_LOCKS) {
            return entity != null && SURFACE_LOCKS.getOrDefault(entity, false);
        }
    }

    private record SurfaceSample(int tick, int x, int feetY, int z, double surfaceY) {}
}
