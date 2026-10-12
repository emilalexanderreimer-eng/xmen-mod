package com.emil.xmen.robot.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.robot.LaserDrone;

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

/** The laser drone: a dark metal pod with a red eye, a gun underneath and four spinning rotors. */
public class LaserDroneModel extends HierarchicalModel<LaserDrone> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "laser_drone"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart[] rotors = new ModelPart[4];

    public LaserDroneModel(ModelPart root) {
        this.root = root;
        body = root.getChild("body");
        for (int i = 0; i < 4; i++) {
            rotors[i] = body.getChild("arms").getChild("rotor" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -3, -4, 8, 6, 8)             // pod
                .texOffs(32, 0).addBox(-2, -1.5f, -5, 4, 3, 1)         // red eye
                .texOffs(32, 4).addBox(-1, 3, -2, 2, 2, 4),            // laser gun underneath
                PartPose.offset(0, 19, 0));
        // the rotor arms make an X when seen from above
        PartDefinition arms = body.addOrReplaceChild("arms", CubeListBuilder.create()
                .texOffs(0, 14).addBox(-11, -1, -1, 22, 1, 2)
                .texOffs(0, 17).addBox(-1, -1, -11, 2, 1, 22),
                PartPose.rotation(0, Mth.PI / 4, 0));
        int[][] tips = {{-10, 0}, {10, 0}, {0, -10}, {0, 10}};
        for (int i = 0; i < 4; i++) {
            arms.addOrReplaceChild("rotor" + i, CubeListBuilder.create()
                    .texOffs(24, 40).addBox(-3.5f, 0, -3.5f, 7, 0, 7)  // blurred blades
                    .texOffs(48, 0).addBox(-0.5f, 0, -0.5f, 1, 1, 1),
                    PartPose.offset(tips[i][0], -2, tips[i][1]));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(LaserDrone entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
            float headPitch) {
        body.y = 19 + Mth.sin(ageInTicks * 0.15f) * 0.8f;
        body.xRot = headPitch * Mth.DEG_TO_RAD * 0.5f;
        for (int i = 0; i < 4; i++) {
            rotors[i].yRot = ageInTicks * 1.6f * (i % 2 == 0 ? 1 : -1);
        }
    }
}
