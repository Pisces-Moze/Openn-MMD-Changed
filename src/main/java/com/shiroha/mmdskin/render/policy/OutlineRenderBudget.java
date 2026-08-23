package com.shiroha.mmdskin.render.policy;

import com.shiroha.mmdskin.config.ConfigManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/** Per-frame LOD budget for the expensive CPU-submitted inverted-hull pass. */
public final class OutlineRenderBudget {
    private static final OutlineRenderBudget INSTANCE = new OutlineRenderBudget();

    private final Map<UUID, Boolean> decisions = new HashMap<>();
    private long frameKey = Long.MIN_VALUE;
    private int remoteModels;

    private OutlineRenderBudget() {}

    public static OutlineRenderBudget get() {
        return INSTANCE;
    }

    public synchronized boolean shouldRender(Entity entity) {
        if (entity == null || !ConfigManager.isToonOutlineEnabled()) return false;
        beginFrame();

        Boolean existing = decisions.get(entity.getUUID());
        if (existing != null) return existing;

        Minecraft minecraft = Minecraft.getInstance();
        boolean local = minecraft.player != null
                && minecraft.player.getUUID().equals(entity.getUUID());
        if (local) {
            decisions.put(entity.getUUID(), true);
            return true;
        }

        Entity camera = minecraft.getCameraEntity();
        float maxDistance = ConfigManager.getToonOutlineMaxDistance();
        boolean withinDistance = camera == null || maxDistance <= 0.0f
                || entity.distanceToSqr(camera) <= maxDistance * maxDistance;
        int remoteLimit = Math.max(0, ConfigManager.getMaxOutlinedModelsPerFrame() - 1);
        boolean approved = withinDistance && remoteModels < remoteLimit;
        if (approved) remoteModels++;
        decisions.put(entity.getUUID(), approved);
        return approved;
    }

    private void beginFrame() {
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        long partialTick = Float.floatToRawIntBits(minecraft.getFrameTime()) & 0xffffffffL;
        long nextKey = (gameTime << 32) ^ partialTick;
        if (nextKey == frameKey) return;
        frameKey = nextKey;
        remoteModels = 0;
        decisions.clear();
    }
}
