package com.emil.xmen.robot.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.robot.TrainingRobot;

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

/** The Danger Room robot: a silver humanoid with a blue visor, an antenna and armoured shoulders. */
public class TrainingRobotModel extends HierarchicalModel<TrainingRobot> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "training_robot"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public TrainingRobotModel(ModelPart root) {
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
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                .texOffs(32, 0).addBox(-3.5f, -5, -4.5f, 7, 2, 1)      // visor
                .texOffs(48, 0).addBox(2, -12, -0.5f, 1, 4, 1),        // antenna
                PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4, 0, -2.5f, 8, 12, 5)
                .texOffs(26, 16).addBox(-2, 3, -3, 4, 4, 1),          // glowing core
                PartPose.ZERO);
        for (int side : new int[] {-1, 1}) {
            root.addOrReplaceChild(side < 0 ? "right_arm" : "left_arm", CubeListBuilder.create().mirror(side > 0)
                    .texOffs(40, 16).addBox(-2, -2, -2, 4, 12, 4)
                    .texOffs(36, 34).addBox(-2.5f, -2.5f, -2.5f, 5, 4, 5),   // shoulder armour
                    PartPose.offset(side * 6, 2, 0));
            root.addOrReplaceChild(side < 0 ? "right_leg" : "left_leg", CubeListBuilder.create().mirror(side > 0)
                    .texOffs(0, 34).addBox(-2, 0, -2, 4, 12, 4),
                    PartPose.offset(side * 2, 12, 0));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(TrainingRobot entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float walk = limbSwing * 0.6662f;
        rightLeg.xRot = Mth.cos(walk) * 1.4f * limbSwingAmount;
        leftLeg.xRot = Mth.cos(walk + Mth.PI) * 1.4f * limbSwingAmount;
        boolean fighting = entity.isAggressive();
        // fists up like a boxer when it is fighting
        float guard = fighting ? -1.1f : 0;
        rightArm.xRot = guard + Mth.cos(walk + Mth.PI) * (fighting ? 0.3f : 1.0f) * limbSwingAmount;
        leftArm.xRot = guard + Mth.cos(walk) * (fighting ? 0.3f : 1.0f) * limbSwingAmount;
        rightArm.yRot = fighting ? -0.3f : 0;
        leftArm.yRot = fighting ? 0.3f : 0;
        rightArm.zRot = 0;
        leftArm.zRot = 0;
        if (attackTime > 0) {
            float swing = Mth.sin(attackTime * Mth.PI);
            rightArm.xRot = -1.6f * swing - 0.4f;
        }
    }
}
