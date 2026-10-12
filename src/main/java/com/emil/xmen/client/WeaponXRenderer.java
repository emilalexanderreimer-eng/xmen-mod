package com.emil.xmen.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.entity.WeaponXI;
import com.emil.xmen.entity.WeaponXMob;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/**
 * Draws the people of Weapon X: a player-shaped body with their own skin, plus their claws or blades, Jason sitting
 * in his chair and soldiers holding their rifles up.
 */
public class WeaponXRenderer extends HumanoidMobRenderer<WeaponXMob, WeaponXRenderer.Model> {
    public static final ModelLayerLocation CLAWS = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "weapon_x_claws"), "main");
    private static final ResourceLocation CLAW_TEXTURE = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID,
            "textures/entity/weapon_x/claws.png");

    private final ResourceLocation skin;

    public WeaponXRenderer(EntityRendererProvider.Context context, String skinName) {
        super(context, new Model(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        skin = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "textures/entity/weapon_x/" + skinName + ".png");
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(
                ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidArmorModel<>(context.bakeLayer(
                        ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        addLayer(new Claws(this, context.bakeLayer(CLAWS)));
    }

    public static EntityRendererProvider<WeaponXMob> skin(String skinName) {
        return context -> new WeaponXRenderer(context, skinName);
    }

    @Override
    public ResourceLocation getTextureLocation(WeaponXMob mob) {
        return skin;
    }

    @Override
    public void render(WeaponXMob mob, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        pose.pushPose();
        if (mob.sits()) {
            pose.translate(0.0, -0.55, 0.0);
        }
        super.render(mob, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }

    /** Weapon XI's eyes glow red while he charges an optic blast. */
    @Override
    protected int getBlockLightLevel(WeaponXMob mob, net.minecraft.core.BlockPos pos) {
        return mob instanceof WeaponXI xi && xi.isCharging() ? 15 : super.getBlockLightLevel(mob, pos);
    }

    public static class Model extends PlayerModel<WeaponXMob> {
        public Model(ModelPart root) {
            super(root, false);
        }

        @Override
        public void prepareMobModel(WeaponXMob mob, float limbSwing, float limbSwingAmount, float partialTick) {
            if (mob.aimsGun() && mob.isAggressive()) {
                rightArmPose = HumanoidModel.ArmPose.CROSSBOW_HOLD;
            } else {
                rightArmPose = mob.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY
                        : HumanoidModel.ArmPose.ITEM;
            }
            leftArmPose = HumanoidModel.ArmPose.EMPTY;
            super.prepareMobModel(mob, limbSwing, limbSwingAmount, partialTick);
        }

        @Override
        public void setupAnim(WeaponXMob mob, float limbSwing, float limbSwingAmount, float ageInTicks,
                float netHeadYaw, float headPitch) {
            if (mob.sits()) {
                riding = true;
                limbSwing = 0;
                limbSwingAmount = 0;
            }
            if (mob.isContained()) {
                limbSwing = 0;
                limbSwingAmount = 0;
            }
            super.setupAnim(mob, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (mob.isContained()) {
                // floating in the tank: arms a little out, head hanging
                rightArm.xRot = 0.1f;
                rightArm.zRot = 0.35f;
                leftArm.xRot = 0.1f;
                leftArm.zRot = -0.35f;
                head.xRot = 0.45f;
                hat.copyFrom(head);
                rightSleeve.copyFrom(rightArm);
                leftSleeve.copyFrom(leftArm);
            }
            if (mob.sits()) {
                rightArm.xRot = -0.55f;
                leftArm.xRot = -0.55f;
                rightSleeve.copyFrom(rightArm);
                leftSleeve.copyFrom(leftArm);
            }
        }
    }

    // ------------------------------------------------------------------ claws and blades
    /** The claw shapes. Each arm gets three Wolverine claws, five finger claws, or a long blade. */
    public static LayerDefinition createClaws() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (String side : new String[] {"right", "left"}) {
            float c = side.equals("right") ? -1.0f : 1.0f;   // middle of the hand
            CubeListBuilder three = CubeListBuilder.create();
            for (int i = -1; i <= 1; i++) {
                three.texOffs(0, 0).addBox(c + i * 1.1f - 0.25f, 9.5f, -1.6f, 0.5f, 7.0f, 0.5f);
            }
            CubeListBuilder five = CubeListBuilder.create();
            for (int i = 0; i < 5; i++) {
                five.texOffs(0, 0).addBox(c - 1.75f + i * 0.85f, 10.5f, -1.4f + (i % 2) * 0.3f, 0.35f, 6.5f, 0.35f);
            }
            CubeListBuilder blade = CubeListBuilder.create()
                    .texOffs(0, 0).addBox(c - 0.2f, 4.0f, -0.8f, 0.4f, 16.0f, 1.6f)
                    .texOffs(0, 0).addBox(c - 0.2f, 19.5f, -0.4f, 0.4f, 1.5f, 0.8f);
            root.addOrReplaceChild(side + "_three", three, PartPose.ZERO);
            root.addOrReplaceChild(side + "_five", five, PartPose.ZERO);
            root.addOrReplaceChild(side + "_blade", blade, PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static class Claws extends RenderLayer<WeaponXMob, Model> {
        private final ModelPart[][] parts = new ModelPart[4][];

        Claws(RenderLayerParent<WeaponXMob, Model> parent, ModelPart root) {
            super(parent);
            parts[1] = new ModelPart[] {root.getChild("right_three"), root.getChild("left_three")};
            parts[2] = new ModelPart[] {root.getChild("right_five"), root.getChild("left_five")};
            parts[3] = new ModelPart[] {root.getChild("right_blade"), root.getChild("left_blade")};
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, WeaponXMob mob, float limbSwing,
                float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            int style = mob.clawStyle();
            if (style <= 0 || style >= parts.length || mob.isContained()) {
                return;
            }
            Model model = getParentModel();
            ModelPart right = parts[style][0];
            ModelPart left = parts[style][1];
            right.copyFrom(model.rightArm);
            left.copyFrom(model.leftArm);
            var buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(CLAW_TEXTURE));
            int color = FastColor.ARGB32.color(255, 255, 255, 255);
            right.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
            left.render(pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
        }
    }
}
