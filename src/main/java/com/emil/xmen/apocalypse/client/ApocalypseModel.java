package com.emil.xmen.apocalypse.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.apocalypse.ApocalypseBoss;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Apocalypse as he looks in the movie: blue-grey skin, a dark armored suit, the tall ridged helmet with fins and
 * the cables running from the back of his head, big shoulder plates and a long cape.
 */
public class ApocalypseModel extends HumanoidModel<ApocalypseBoss> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "apocalypse"), "main");

    public ApocalypseModel(ModelPart root) {
        super(root);
    }

    /** The normal player-shaped body (64x32 part of the skin) plus his armor pieces in the lower half. */
    static MeshDefinition humanoid() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8),
                PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8,
                new CubeDeformation(0.5f)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4),
                PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-3, -2, -2, 4, 12, 4),
                PartPose.offset(-5, 2, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror()
                .addBox(-1, -2, -2, 4, 12, 4), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(-1.9f, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(1.9f, 12, 0));
        return mesh;
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(0, 32).addBox(-1, -11, -4.5f, 2, 4, 10),
                PartPose.ZERO);
        head.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(24, 32).addBox(-5.6f, -9.5f, -2.5f,
                1, 6, 6), PartPose.ZERO);
        head.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(24, 32).mirror()
                .addBox(4.6f, -9.5f, -2.5f, 1, 6, 6), PartPose.ZERO);
        head.addOrReplaceChild("cables", CubeListBuilder.create().texOffs(0, 46).addBox(-3, -5, 4.2f, 1, 8, 1)
                .texOffs(0, 46).addBox(2, -5, 4.2f, 1, 8, 1), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8,
                new CubeDeformation(0.5f)), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4, 0, -2, 8, 12, 4, new CubeDeformation(0.25f)), PartPose.ZERO);
        body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(40, 41).addBox(-5, 0, 0, 10, 20, 1),
                PartPose.offsetAndRotation(0, 0.5f, 2.6f, 0.08f, 0, 0));
        body.addOrReplaceChild("tabard", CubeListBuilder.create().texOffs(4, 46).addBox(-3, 0, 0, 6, 8, 1),
                PartPose.offset(0, 11, -2.8f));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-3, -2, -2, 4, 12, 4, new CubeDeformation(0.2f)), PartPose.offset(-5, 2, 0));
        rightArm.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(38, 32).addBox(-4.2f, -3.4f, -3, 6, 3,
                6), PartPose.rotation(0, 0, 0.2f));
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16)
                .mirror().addBox(-1, -2, -2, 4, 12, 4, new CubeDeformation(0.2f)), PartPose.offset(5, 2, 0));
        leftArm.addOrReplaceChild("pad", CubeListBuilder.create().texOffs(38, 32).mirror()
                .addBox(-1.8f, -3.4f, -3, 6, 3, 6), PartPose.rotation(0, 0, -0.2f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2, 0, -2, 4, 12, 4),
                PartPose.offset(-1.9f, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror()
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(1.9f, 12, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ApocalypseBoss boss, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        super.setupAnim(boss, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (!boss.isAwake()) {
            // asleep on his feet: head bowed, arms still, breathing slowly
            head.xRot = 0.45f + Mth.sin(ageInTicks * 0.05f) * 0.02f;
            head.yRot = 0.0f;
            rightArm.xRot = 0.0f;
            leftArm.xRot = 0.0f;
            rightArm.zRot = 0.08f;
            leftArm.zRot = -0.08f;
        } else if (boss.isCasting()) {
            float wave = Mth.sin(ageInTicks * 0.3f) * 0.08f;
            rightArm.xRot = -1.9f + wave;
            rightArm.yRot = -0.25f;
            leftArm.xRot = -1.9f - wave;
            leftArm.yRot = 0.25f;
        }
        hat.copyFrom(head);
    }
}
