package com.moze.openmmdchanged.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.moze.openmmdchanged.OpenMmdChanged;
import com.moze.openmmdchanged.entity.MmdLatexEntity;
import net.ltxprogrammer.changed.client.renderer.animate.AnimatorPresets;
import net.ltxprogrammer.changed.client.renderer.animate.HumanoidAnimator;
import net.ltxprogrammer.changed.client.renderer.model.AdvancedHumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.HumanoidArm;

/** Lightweight humanoid used only by Changed's gradual transfur and hand rendering. */
public final class ChangedMmdBridgeModel extends AdvancedHumanoidModel<MmdLatexEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(OpenMmdChanged.id("mmd_latex_bridge"), "main");

    private final ModelPart head;
    private final ModelPart torso;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final HumanoidAnimator<MmdLatexEntity, ChangedMmdBridgeModel> animator;

    public ChangedMmdBridgeModel(ModelPart root) {
        super(root);
        head = root.getChild("Head");
        torso = root.getChild("Torso");
        rightArm = root.getChild("RightArm");
        leftArm = root.getChild("LeftArm");
        rightLeg = root.getChild("RightLeg");
        leftLeg = root.getChild("LeftLeg");
        animator = HumanoidAnimator.of(this).hipOffset(-1.5F).legLength(10.5F)
                .addPreset(AnimatorPresets.humanLike(head, torso, leftArm, rightArm, leftLeg, rightLeg));
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -8, -4, 8, 8, 8, CubeDeformation.NONE), PartPose.ZERO);
        root.addOrReplaceChild("Torso", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4, 0, -2, 8, 12, 4, CubeDeformation.NONE), PartPose.ZERO);
        root.addOrReplaceChild("RightArm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-3, -2, -2, 4, 12, 4, CubeDeformation.NONE), PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("LeftArm", CubeListBuilder.create().texOffs(32, 48)
                .addBox(-1, -2, -2, 4, 12, 4, CubeDeformation.NONE), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("RightLeg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -2, 4, 12, 4, CubeDeformation.NONE), PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild("LeftLeg", CubeListBuilder.create().texOffs(16, 48)
                .addBox(-2, 0, -2, 4, 12, 4, CubeDeformation.NONE), PartPose.offset(2, 12, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart getArm(HumanoidArm side) {
        return side == HumanoidArm.LEFT ? leftArm : rightArm;
    }

    @Override
    public ModelPart getLeg(HumanoidArm side) {
        return side == HumanoidArm.LEFT ? leftLeg : rightLeg;
    }

    @Override
    public ModelPart getHead() {
        return head;
    }

    @Override
    public ModelPart getTorso() {
        return torso;
    }

    @Override
    public HumanoidAnimator<MmdLatexEntity, ChangedMmdBridgeModel> getAnimator(MmdLatexEntity entity) {
        return animator;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        head.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
        torso.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
        rightArm.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
        leftArm.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
        rightLeg.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
        leftLeg.render(poseStack, buffer, light, overlay, red, green, blue, alpha);
    }
}
