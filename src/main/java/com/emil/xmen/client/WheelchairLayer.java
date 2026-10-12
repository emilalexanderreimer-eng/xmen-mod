package com.emil.xmen.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.entity.MutantNpc;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Professor X's wheelchair, drawn under him while he sits. Sizes are in pixels like the player model. */
public class WheelchairLayer extends RenderLayer<MutantNpc, PlayerModel<MutantNpc>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "wheelchair"), "main");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "textures/entity/wheelchair.png");
    /** How far (in blocks) a sitting character is lowered so he sits on the seat. */
    public static final float SINK = 0.35f;
    private static final float GROUND = 24 + SINK * 16;

    private final ModelPart chair;

    public WheelchairLayer(RenderLayerParent<MutantNpc, PlayerModel<MutantNpc>> parent, ModelPart chair) {
        super(parent);
        this.chair = chair;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder frame = CubeListBuilder.create().texOffs(0, 0)
                .addBox(-5, 12.2f, -6, 10, 1.5f, 9)          // seat
                .texOffs(0, 0).addBox(-5, -1, 2.2f, 10, 14, 1.5f)  // backrest
                .texOffs(40, 0).addBox(-6, 7, -5, 1, 1, 8)    // armrests
                .texOffs(40, 0).addBox(5, 7, -5, 1, 1, 8)
                .texOffs(40, 10).addBox(-6, 8, 1, 1, 6, 1)    // armrest posts
                .texOffs(40, 10).addBox(5, 8, 1, 1, 6, 1)
                .texOffs(40, 10).addBox(-6, 8, -5, 1, 6, 1)
                .texOffs(40, 10).addBox(5, 8, -5, 1, 6, 1)
                .texOffs(40, 0).addBox(-6, 13.6f, -6, 12, 1, 1) // frame under the seat
                .texOffs(40, 0).addBox(-6, 13.6f, 2, 12, 1, 1)
                .texOffs(40, 10).addBox(-4.5f, 14, -12, 1, 1, 7) // legs down to the footrest
                .texOffs(40, 10).addBox(3.5f, 14, -12, 1, 1, 7)
                .texOffs(0, 16).addBox(-4.5f, 14.5f, -14, 9, 1, 3) // footrest
                .texOffs(40, 10).addBox(-5.5f, -2, 3.5f, 1, 1, 3) // push handles
                .texOffs(40, 10).addBox(4.5f, -2, 3.5f, 1, 1, 3)
                .texOffs(40, 10).addBox(-4.5f, 14.5f, -9, 1, GROUND - 16.5f, 1) // caster forks
                .texOffs(40, 10).addBox(3.5f, 14.5f, -9, 1, GROUND - 16.5f, 1);
        root.addOrReplaceChild("frame", frame, PartPose.ZERO);
        float wheelY = GROUND - 9;
        for (int side = -1; side <= 1; side += 2) {
            for (int turn = 0; turn < 2; turn++) {
                root.addOrReplaceChild("wheel_" + side + "_" + turn, CubeListBuilder.create().texOffs(0, 26)
                        .addBox(-0.5f, -9, -9, 1, 18, 18),
                        PartPose.offsetAndRotation(side * 8.7f, wheelY, 0.5f, turn * (float) (Math.PI / 4), 0, 0));
                root.addOrReplaceChild("caster_" + side + "_" + turn, CubeListBuilder.create().texOffs(40, 26)
                        .addBox(-0.5f, -2, -2, 1, 4, 4),
                        PartPose.offsetAndRotation(side * 4, GROUND - 2, -8.5f, turn * (float) (Math.PI / 4), 0, 0));
            }
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, MutantNpc entity, float limbSwing,
            float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!entity.isSitting()) {
            return;
        }
        chair.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light,
                OverlayTexture.NO_OVERLAY);
    }
}
