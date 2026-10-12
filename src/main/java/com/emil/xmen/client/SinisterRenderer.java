package com.emil.xmen.client;

import java.util.UUID;

import com.emil.xmen.XMenMod;
import com.emil.xmen.entity.SinisterLook;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;

/**
 * Draws Mister Sinister and his clones: a tall, chalk-white man in a black suit with red diamonds on his forehead
 * and chest, glowing red eyes, and a long tattered black cape. While he is shapeshifted he looks exactly like the
 * player he copied.
 */
public class SinisterRenderer<T extends Monster & SinisterLook>
        extends HumanoidMobRenderer<T, SinisterRenderer.Model<T>> {
    static final ResourceLocation SINISTER = texture("mister_sinister");
    static final ResourceLocation CLONE = texture("sinister_clone");
    private static final ResourceLocation CAPE = texture("mister_sinister_cape");
    private static final RenderType GLOW = RenderType.eyes(texture("mister_sinister_glow"));

    private final ResourceLocation skin;

    public SinisterRenderer(EntityRendererProvider.Context context, ResourceLocation skin) {
        super(context, new Model<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        this.skin = skin;
        addLayer(new Cape<>(this));
        addLayer(new Glow<>(this));
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "textures/entity/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T sinister) {
        UUID disguise = sinister.getDisguise();
        if (disguise == null) {
            return skin;
        }
        var connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null : connection.getPlayerInfo(disguise);
        return info != null ? info.getSkin().texture() : DefaultPlayerSkin.get(disguise).texture();
    }

    @Override
    protected void scale(T sinister, PoseStack pose, float partialTick) {
        if (sinister.getDisguise() == null) {
            pose.scale(1.06f, 1.06f, 1.06f);   // he towers over people a little
        }
    }

    /** Stands tall with his hands behind his back, points a hand when he blasts, and sleeps in a tank. */
    public static class Model<T extends Monster & SinisterLook> extends PlayerModel<T> {
        public Model(ModelPart root) {
            super(root, false);
        }

        @Override
        public void setupAnim(T sinister, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
                float headPitch) {
            super.setupAnim(sinister, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (sinister.isDormant()) {
                float drift = Mth.sin(ageInTicks * 0.05f) * 0.04f;
                head.xRot = 0.55f;
                head.yRot = 0.0f;
                rightArm.xRot = 0.15f + drift;
                leftArm.xRot = 0.15f - drift;
                rightArm.zRot = 0.08f;
                leftArm.zRot = -0.08f;
                rightLeg.xRot = 0.05f;
                leftLeg.xRot = -0.05f;
            } else if (sinister.isCasting()) {
                rightArm.xRot = -1.55f + head.xRot;
                rightArm.yRot = -0.1f + head.yRot;
                rightArm.zRot = 0.0f;
                leftArm.xRot = -0.9f;
                leftArm.yRot = 0.4f;
            } else if (attackTime <= 0 && limbSwingAmount < 0.3f && sinister.getDisguise() == null) {
                // hands clasped behind his back, like a doctor looking over his work
                rightArm.xRot = 0.55f;
                rightArm.zRot = 0.25f;
                leftArm.xRot = 0.55f;
                leftArm.zRot = -0.25f;
            }
            hat.copyFrom(head);
            leftPants.copyFrom(leftLeg);
            rightPants.copyFrom(rightLeg);
            leftSleeve.copyFrom(leftArm);
            rightSleeve.copyFrom(rightArm);
            jacket.copyFrom(body);
        }
    }

    /** His long black cape with a red lining and a ragged bottom edge. */
    private static class Cape<T extends Monster & SinisterLook> extends RenderLayer<T, Model<T>> {
        Cape(RenderLayerParent<T, Model<T>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T sinister, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!sinister.hasCape() || sinister.isInvisible()) {
                return;
            }
            float flap = 6.0f + Math.min(limbSwingAmount * 40.0f, 40.0f) + Mth.sin(ageInTicks * 0.1f) * 2.0f;
            pose.pushPose();
            pose.translate(0.0f, 0.0f, 0.125f);
            pose.mulPose(Axis.XP.rotationDegrees(flap));
            pose.mulPose(Axis.YP.rotationDegrees(180.0f));
            getParentModel().renderCloak(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CAPE)), light,
                    OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    /** Red eyes and diamonds that glow in the dark (not while sleeping or shapeshifted). */
    private static class Glow<T extends Monster & SinisterLook> extends EyesLayer<T, Model<T>> {
        Glow(RenderLayerParent<T, Model<T>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T sinister, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!sinister.isDormant() && sinister.getDisguise() == null && !sinister.isInvisible()) {
                super.render(pose, buffers, light, sinister, limbSwing, limbSwingAmount, partialTick, ageInTicks,
                        netHeadYaw, headPitch);
            }
        }

        @Override
        public RenderType renderType() {
            return GLOW;
        }
    }
}
