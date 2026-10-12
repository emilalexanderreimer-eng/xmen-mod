package com.emil.xmen.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.entity.MagnetoBoss;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws Magneto: a player-shaped man in his red suit with a purple cape, wearing his helmet. */
public class MagnetoRenderer extends HumanoidMobRenderer<MagnetoBoss, MagnetoRenderer.Model> {
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID,
            "textures/entity/magneto.png");
    private static final ResourceLocation CAPE = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID,
            "textures/entity/magneto_cape.png");

    public MagnetoRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(
                ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidArmorModel<>(context.bakeLayer(
                        ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        addLayer(new Cape(this));
    }

    @Override
    public ResourceLocation getTextureLocation(MagnetoBoss magneto) {
        return SKIN;
    }

    /** He floats instead of walking, and raises his hands when he uses his powers. */
    public static class Model extends PlayerModel<MagnetoBoss> {
        public Model(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(MagnetoBoss magneto, float limbSwing, float limbSwingAmount, float ageInTicks,
                float netHeadYaw, float headPitch) {
            super.setupAnim(magneto, 0.0f, 0.0f, ageInTicks, netHeadYaw, headPitch);
            float drift = Mth.sin(ageInTicks * 0.08f) * 0.05f;
            rightLeg.xRot = 0.12f + drift;
            leftLeg.xRot = 0.04f - drift;
            rightLeg.zRot = 0.04f;
            leftLeg.zRot = -0.04f;
            if (magneto.isCasting()) {
                float wave = Mth.sin(ageInTicks * 0.3f) * 0.08f;
                rightArm.xRot = -1.85f + wave;
                rightArm.yRot = -0.3f;
                rightArm.zRot = 0.0f;
                leftArm.xRot = -1.85f - wave;
                leftArm.yRot = 0.3f;
                leftArm.zRot = 0.0f;
            } else {
                rightArm.xRot = drift;
                rightArm.zRot = 0.22f;
                leftArm.xRot = -drift;
                leftArm.zRot = -0.22f;
            }
            leftPants.copyFrom(leftLeg);
            rightPants.copyFrom(rightLeg);
            leftSleeve.copyFrom(leftArm);
            rightSleeve.copyFrom(rightArm);
        }
    }

    /** His cape, which flutters more when he flies fast. */
    private static class Cape extends RenderLayer<MagnetoBoss, Model> {
        Cape(RenderLayerParent<MagnetoBoss, Model> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, MagnetoBoss magneto, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            double dx = magneto.getX() - magneto.xo;
            double dz = magneto.getZ() - magneto.zo;
            float speed = (float) Math.sqrt(dx * dx + dz * dz);
            float flap = 8.0f + Math.min(speed * 150.0f, 55.0f) + Mth.sin(ageInTicks * 0.15f) * 4.0f;
            pose.pushPose();
            pose.translate(0.0f, 0.0f, 0.125f);
            pose.mulPose(Axis.XP.rotationDegrees(flap));
            pose.mulPose(Axis.YP.rotationDegrees(180.0f));
            getParentModel().renderCloak(pose, buffers.getBuffer(RenderType.entitySolid(CAPE)), light,
                    OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
