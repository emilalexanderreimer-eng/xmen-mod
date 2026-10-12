package com.emil.xmen.robot.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.robot.Sentinel;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Sentinel from Days of Future Past: a broad purple chest, huge shoulder pads, long arms with big hands, a thin
 * waist and a round head with a silver face plate and glowing eyes. Texture layout: tools/robots.py.
 */
public class SentinelModel extends HierarchicalModel<Sentinel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "sentinel"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public SentinelModel(ModelPart root) {
        this.root = root;
        head = root.getChild("head");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-11, -30, -7, 22, 18, 14)         // chest
                .texOffs(38, 68).addBox(-7, -28, -8, 14, 10, 1)        // chest plate
                .texOffs(40, 32).addBox(-6, -12, -4, 12, 8, 8),        // waist
                PartPose.ZERO);
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(68, 68).addBox(-3, -3, -3, 6, 3, 6)           // neck
                .texOffs(72, 0).addBox(-6, -15, -6.5f, 12, 12, 13)     // head
                .texOffs(0, 82).addBox(-4, -11, -7.5f, 8, 6, 1)        // face plate with the eyes
                .texOffs(92, 68).addBox(-2, -17, -4, 4, 2, 8),         // crest on top
                PartPose.offset(0, -30, 0));
        for (int side : new int[] {-1, 1}) {
            CubeListBuilder arm = CubeListBuilder.create().mirror(side > 0)
                    .texOffs(0, 32).addBox(-5, -4, -5, 10, 7, 10)      // shoulder pad
                    .texOffs(60, 50).addBox(-3, 3, -3, 6, 11, 6)       // upper arm
                    .texOffs(80, 32).addBox(-3.5f, 14, -3.5f, 7, 10, 7) // forearm
                    .texOffs(84, 50).addBox(-4, 24, -4, 8, 6, 8);      // hand
            root.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", arm, PartPose.offset(side * 15, -27, 0));
            CubeListBuilder leg = CubeListBuilder.create().mirror(side > 0)
                    .texOffs(0, 50).addBox(-3.5f, 0, -3.5f, 7, 11, 7)  // thigh
                    .texOffs(28, 50).addBox(-4, 11, -4, 8, 10, 8)      // shin
                    .texOffs(0, 68).addBox(-4.5f, 21, -6, 9, 3, 10);   // foot
            root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", leg, PartPose.offset(side * 5, -4, 0));
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Sentinel entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        // slow, heavy steps
        float walk = limbSwing * 0.45f;
        rightLeg.xRot = Mth.cos(walk) * 0.9f * limbSwingAmount;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 0.9f * limbSwingAmount;
        rightArm.xRot = Mth.cos(walk + Mth.PI) * 0.6f * limbSwingAmount;
        leftArm.xRot = Mth.cos(walk) * 0.6f * limbSwingAmount;
        rightArm.zRot = 0.08f + Mth.sin(ageInTicks * 0.05f) * 0.02f;
        leftArm.zRot = -rightArm.zRot;
        if (entity.isCharging()) {
            // leans its head forward while the eyes charge
            head.xRot = Math.max(head.xRot, 0.15f);
        }
        if (attackTime > 0) {
            // a big overhead smash with both arms
            float swing = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -2.2f * swing;
            leftArm.xRot = -2.2f * swing;
        }
    }
}
