package com.emil.xmen.apocalypse.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.apocalypse.ApocalypseServant;
import com.emil.xmen.apocalypse.ServantKind;

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

/** The Horsemen and cultists: a person-shaped body plus Storm's mohawk, Archangel's metal wings, Psylocke's blade. */
public class ServantModel extends HumanoidModel<ApocalypseServant> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "apocalypse_servant"), "main");

    private final ModelPart mohawk;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart blade;

    public ServantModel(ModelPart root) {
        super(root);
        mohawk = head.getChild("mohawk");
        rightWing = body.getChild("right_wing");
        leftWing = body.getChild("left_wing");
        blade = rightArm.getChild("blade");
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = ApocalypseModel.humanoid();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        head.addOrReplaceChild("mohawk", CubeListBuilder.create().texOffs(0, 32).addBox(-1, -10.5f, -4.5f, 2, 3, 10),
                PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-4, 0, -2, 8, 12, 4), PartPose.ZERO);
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 45).addBox(-16, -6, 0, 16, 18, 1),
                PartPose.offsetAndRotation(-1.5f, 2, 2.2f, 0, 0.45f, -0.2f));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 45).mirror()
                .addBox(0, -6, 0, 16, 18, 1), PartPose.offsetAndRotation(1.5f, 2, 2.2f, 0, -0.45f, 0.2f));
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-3, -2, -2, 4, 12, 4), PartPose.offset(-5, 2, 0));
        rightArm.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(34, 45).addBox(-1.5f, 9, -11, 1, 2, 10),
                PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(ApocalypseServant servant, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        super.setupAnim(servant, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ServantKind kind = servant.kind();
        mohawk.visible = kind == ServantKind.STORM;
        blade.visible = kind == ServantKind.PSYLOCKE;
        boolean wings = kind == ServantKind.ARCHANGEL;
        rightWing.visible = wings;
        leftWing.visible = wings;
        if (wings) {
            float flap = servant.isNoGravity() ? Mth.sin(ageInTicks * 0.35f) * 0.35f : Mth.sin(ageInTicks * 0.05f)
                    * 0.05f;
            float open = servant.isNoGravity() ? 0.15f : 0.45f;
            rightWing.yRot = open + flap;
            leftWing.yRot = -open - flap;
            if (servant.isNoGravity()) {
                rightLeg.xRot = 0.15f;
                leftLeg.xRot = 0.05f;
            }
        }
        if (kind == ServantKind.MAGNETO && servant.isAggressive()) {
            rightArm.xRot = -1.6f + Mth.sin(ageInTicks * 0.2f) * 0.1f;
        }
    }
}
