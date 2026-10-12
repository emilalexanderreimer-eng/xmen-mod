package com.emil.xmen.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.entity.Blackbird;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Draws the X-Jet: a black jet with a pointed nose, swept wings and two tilted tail fins. */
public class BlackbirdRenderer extends EntityRenderer<Blackbird> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "blackbird"), "main");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "textures/entity/blackbird.png");
    /** The model is built small (in pixels) and drawn this many times bigger. */
    private static final float SCALE = 2.4f;

    private final ModelPart jet;

    public BlackbirdRenderer(EntityRendererProvider.Context context) {
        super(context);
        jet = context.bakeLayer(LAYER);
        shadowRadius = 2.5f;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // front is -z, the bottom of the landing gear is y = 8
        CubeListBuilder body = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -3, -16, 8, 6, 36)        // fuselage
                .texOffs(0, 44).addBox(-3, -2, -22, 6, 5, 6)        // nose
                .texOffs(0, 44).addBox(-2, -1, -27, 4, 3, 5)
                .texOffs(0, 44).addBox(-1, 0, -31, 2, 2, 4)
                .texOffs(60, 64).addBox(-8.5f, -2, 2, 4, 5, 20)     // engines
                .texOffs(60, 64).addBox(4.5f, -2, 2, 4, 5, 20)
                .texOffs(0, 56).addBox(-12, 1, -4, 24, 1, 6)       // swept wings, wider towards the back
                .texOffs(0, 56).addBox(-18, 1, 2, 36, 1, 6)
                .texOffs(0, 56).addBox(-23, 1, 8, 46, 1, 6)
                .texOffs(0, 56).addBox(-27, 1, 14, 54, 1, 6)
                .texOffs(0, 64).addBox(-0.5f, 3, -18, 1, 5, 1)     // landing gear
                .texOffs(0, 64).addBox(-7, 3, 10, 1, 5, 1)
                .texOffs(0, 64).addBox(6, 3, 10, 1, 5, 1)
                .texOffs(4, 64).addBox(-1, 7, -19, 2, 1, 3)
                .texOffs(4, 64).addBox(-7.5f, 7, 9, 2, 1, 3)
                .texOffs(4, 64).addBox(5.5f, 7, 9, 2, 1, 3);
        root.addOrReplaceChild("body", body, PartPose.ZERO);
        root.addOrReplaceChild("canopy", CubeListBuilder.create().texOffs(0, 100)
                .addBox(-2.5f, -5.5f, -15, 5, 3, 12), PartPose.ZERO);
        root.addOrReplaceChild("exhaust", CubeListBuilder.create().texOffs(60, 100)
                .addBox(-8, -1.5f, 21.5f, 3, 4, 1).addBox(5, -1.5f, 21.5f, 3, 4, 1), PartPose.ZERO);
        root.addOrReplaceChild("fin_left", CubeListBuilder.create().texOffs(40, 64)
                .addBox(-0.5f, -10, 0, 1, 10, 8), PartPose.offsetAndRotation(-5, -2, 12, 0.25f, 0, -0.35f));
        root.addOrReplaceChild("fin_right", CubeListBuilder.create().texOffs(40, 64)
                .addBox(-0.5f, -10, 0, 1, 10, 8), PartPose.offsetAndRotation(5, -2, 12, 0.25f, 0, 0.35f));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void render(Blackbird jetEntity, float yaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffers, int light) {
        poseStack.pushPose();
        float bodyYaw = Mth.rotLerp(partialTicks, jetEntity.yRotO, jetEntity.getYRot());
        float pitch = Mth.lerp(partialTicks, jetEntity.xRotO, jetEntity.getXRot());
        float bank = Mth.clamp(Mth.wrapDegrees(jetEntity.getYRot() - jetEntity.yRotO) * 5f, -35f, 35f);
        poseStack.translate(0, 0.5f * SCALE / 2, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(bank));
        poseStack.translate(0, 0.5f * SCALE / 2, 0);
        poseStack.scale(-SCALE, -SCALE, SCALE);
        jet.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light,
                OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(jetEntity, yaw, partialTicks, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Blackbird entity) {
        return TEXTURE;
    }
}
