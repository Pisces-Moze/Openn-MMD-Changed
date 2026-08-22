package com.moze.openmmdchanged.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.moze.openmmdchanged.client.MmdAssetInstaller;
import com.moze.openmmdchanged.entity.MmdLatexEntity;
import com.shiroha.mmdskin.asset.catalog.ModelCatalogEntry;
import com.shiroha.mmdskin.render.entity.MmdSkinRenderer;
import net.ltxprogrammer.changed.client.renderer.AdvancedHumanoidRenderer;
import net.ltxprogrammer.changed.client.renderer.model.armor.ArmorHumanModel;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/** Changed-compatible renderer that swaps to MMDSkin after the vanilla transfur animation. */
public final class MmdLatexRenderer
        extends AdvancedHumanoidRenderer<MmdLatexEntity, ChangedMmdBridgeModel> {
    private final MmdSkinRenderer<MmdLatexEntity> mmdRenderer;

    public MmdLatexRenderer(EntityRendererProvider.Context context) {
        super(context, new ChangedMmdBridgeModel(context.bakeLayer(ChangedMmdBridgeModel.LAYER)),
                ArmorHumanModel.MODEL_SET, 0.5F);
        mmdRenderer = new MmdSkinRenderer<>(context, MmdAssetInstaller.MODEL_FOLDER);
    }

    @Override
    public void render(MmdLatexEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        Player player = entity.getUnderlyingPlayer();
        boolean invalidPlayerForm = player != null
                && (player.isDeadOrDying() || player.isRemoved() || entity.isRemoved());
        if (isGraduallyTransfurring(entity, partialTick) || !hasBundledModel() || invalidPlayerForm) {
            super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
        } else {
            mmdRenderer.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
        }
    }

    private static boolean hasBundledModel() {
        return ModelCatalogEntry.findByFolderName(MmdAssetInstaller.MODEL_FOLDER) != null;
    }

    private static boolean isGraduallyTransfurring(MmdLatexEntity entity, float partialTick) {
        Player player = entity.getUnderlyingPlayer();
        if (player == null) {
            return false;
        }
        return ProcessTransfur.getPlayerTransfurVariantSafe(player)
                .filter(instance -> instance.getChangedEntity() == entity)
                .map(instance -> instance.getTransfurProgression(partialTick) < 1.0F)
                .orElse(false);
    }

    @Override
    public ResourceLocation getTextureLocation(MmdLatexEntity entity) {
        if (entity.getUnderlyingPlayer() instanceof AbstractClientPlayer player) {
            return player.getSkinTextureLocation();
        }
        return DefaultPlayerSkin.getDefaultSkin(entity.getUUID());
    }
}
