package com.emil.xmen.client;

import com.emil.xmen.XMenMod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * The 3D shape of Magneto's helmet when it's worn: a rounded dome with a ridge running over the top and down the
 * forehead, cheek guards that frame the face, a neck guard at the back and small fins at the temples.
 * Texture: textures/models/armor/magneto_layer_1.png (128x64, drawn by tools/make_magneto.py).
 */
public final class MagnetoHelmetModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "magneto_helmet"), "main");

    private MagnetoHelmetModel() {
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0f);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5, -9, -5, 10, 3, 10)      // dome
                .texOffs(40, 0).addBox(-5, -6, 1, 10, 7, 4)       // back of the head and neck guard
                .texOffs(68, 0).addBox(4, -6, -5, 1, 7, 6)        // left cheek guard
                .texOffs(82, 0).addBox(-5, -6, -5, 1, 7, 6)       // right cheek guard
                .texOffs(96, 0).addBox(-4, -6, -5, 8, 1, 1)       // brow
                .texOffs(96, 4).addBox(3, -3, -5, 1, 4, 1)        // jaw guards
                .texOffs(100, 4).addBox(-4, -3, -5, 1, 4, 1)
                .texOffs(0, 16).addBox(-1, -10, -6, 2, 1, 11)     // ridge over the top
                .texOffs(26, 16).addBox(-1, -9, -6, 2, 4, 1)      // ridge down the forehead
                .texOffs(32, 16).addBox(5, -8, -3, 1, 3, 4)       // temple fins
                .texOffs(42, 16).addBox(-6, -8, -3, 1, 3, 4),
                PartPose.ZERO);
        CubeListBuilder nothing = CubeListBuilder.create();
        root.addOrReplaceChild("hat", nothing, PartPose.ZERO);
        root.addOrReplaceChild("body", nothing, PartPose.ZERO);
        root.addOrReplaceChild("right_arm", nothing, PartPose.offset(-5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("left_arm", nothing, PartPose.offset(5.0f, 2.0f, 0.0f));
        root.addOrReplaceChild("right_leg", nothing, PartPose.offset(-1.9f, 12.0f, 0.0f));
        root.addOrReplaceChild("left_leg", nothing, PartPose.offset(1.9f, 12.0f, 0.0f));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
