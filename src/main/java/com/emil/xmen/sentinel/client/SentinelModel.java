package com.emil.xmen.sentinel.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.sentinel.SentinelMarkX;

import net.minecraft.client.model.HumanoidModel;
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
 * A Sentinel robot like in Days of Future Past: a smooth helmet-like head with a crest and ear discs, a broad
 * armored chest with a glowing core, a narrow waist, huge shoulder plates, long arms with heavy forearms and palm
 * emitters, and thick legs with knee plates and big feet. The renderer scales it up to giant size.
 * The texture layout (128x64) is painted by tools/make_sentinel.py and must match the boxes here.
 */
public class SentinelModel extends HumanoidModel<SentinelMarkX> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "sentinel_mark_x"), "main");

    public SentinelModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(32, 0).addBox(-1, -9.5f, -4, 2, 2, 8),
                PartPose.ZERO);
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(56, 0).addBox(-5, -6, -1.5f, 1, 3, 3),
                PartPose.ZERO);
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(56, 0).mirror()
                .addBox(4, -6, -1.5f, 1, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-5, 0, -3, 10, 7, 6), PartPose.ZERO);
        body.addOrReplaceChild("waist", CubeListBuilder.create().texOffs(32, 16).addBox(-3.5f, 7, -2, 7, 5, 4),
                PartPose.ZERO);
        body.addOrReplaceChild("core", CubeListBuilder.create().texOffs(56, 16).addBox(-1.5f, 2, -3.6f, 3, 3, 1),
                PartPose.ZERO);
        body.addOrReplaceChild("hips", CubeListBuilder.create().texOffs(64, 16).addBox(-4, 10.5f, -2.5f, 8, 2, 5),
                PartPose.ZERO);

        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(26, 32)
                .addBox(-3, -1, -2, 4, 6, 4), PartPose.offset(-6, 2, 0));
        rightArm.addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-4.5f, -3, -3.5f, 6, 5, 7), PartPose.ZERO);
        rightArm.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(42, 32)
                .addBox(-3.5f, 5, -2.5f, 5, 7, 5), PartPose.ZERO);
        rightArm.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(62, 32).addBox(-3, 12, -2, 4, 2, 4),
                PartPose.ZERO);
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(26, 32).mirror()
                .addBox(-1, -1, -2, 4, 6, 4), PartPose.offset(6, 2, 0));
        leftArm.addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(0, 32).mirror()
                .addBox(-1.5f, -3, -3.5f, 6, 5, 7), PartPose.ZERO);
        leftArm.addOrReplaceChild("forearm", CubeListBuilder.create().texOffs(42, 32).mirror()
                .addBox(-1.5f, 5, -2.5f, 5, 7, 5), PartPose.ZERO);
        leftArm.addOrReplaceChild("hand", CubeListBuilder.create().texOffs(62, 32).mirror()
                .addBox(-1, 12, -2, 4, 2, 4), PartPose.ZERO);

        leg(root, "right_leg", -2.2f, false);
        leg(root, "left_leg", 2.2f, true);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void leg(PartDefinition root, String name, float x, boolean mirror) {
        PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 48).mirror(mirror)
                .addBox(-2.5f, 0, -2.5f, 5, 6, 5), PartPose.offset(x, 12, 0));
        leg.addOrReplaceChild("knee", CubeListBuilder.create().texOffs(20, 48).mirror(mirror)
                .addBox(-2, 5, -3.2f, 4, 3, 1), PartPose.ZERO);
        leg.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(30, 48).mirror(mirror)
                .addBox(-2, 6, -2, 4, 5, 4), PartPose.ZERO);
        leg.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(46, 48).mirror(mirror)
                .addBox(-2.5f, 11, -3.5f, 5, 1, 6), PartPose.ZERO);
    }

    @Override
    public void setupAnim(SentinelMarkX sentinel, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        // giant machines take slow, heavy steps
        super.setupAnim(sentinel, limbSwing * 0.6f, Math.min(limbSwingAmount, 0.8f), ageInTicks, netHeadYaw,
                headPitch);
        if (!sentinel.isAwake()) {
            // switched off in its cradle: head down, arms hanging
            head.xRot = 0.55f;
            head.yRot = 0.0f;
            rightArm.xRot = 0.1f;
            leftArm.xRot = 0.1f;
            rightArm.zRot = 0.06f;
            leftArm.zRot = -0.06f;
            rightLeg.xRot = 0.0f;
            leftLeg.xRot = 0.0f;
        } else {
            switch (sentinel.action()) {
                case SentinelMarkX.AIM -> {
                    // palm emitter pointed at the target
                    rightArm.xRot = -1.5f + head.xRot;
                    rightArm.yRot = -0.1f + head.yRot;
                    rightArm.zRot = 0.0f;
                }
                case SentinelMarkX.RAISE -> {
                    float shake = Mth.sin(ageInTicks * 0.9f) * 0.05f;
                    rightArm.xRot = -2.8f + shake;
                    leftArm.xRot = -2.8f - shake;
                    rightArm.zRot = -0.25f;
                    leftArm.zRot = 0.25f;
                }
                case SentinelMarkX.GRAB -> {
                    rightArm.xRot = -1.35f;
                    leftArm.xRot = -1.35f;
                    rightArm.yRot = 0.35f;
                    leftArm.yRot = -0.35f;
                }
                default -> {
                    rightArm.zRot += 0.08f;
                    leftArm.zRot -= 0.08f;
                }
            }
        }
        hat.copyFrom(head);
    }
}
