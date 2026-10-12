package com.emil.xmen.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.entity.MutantNpc;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws the X-Men characters like players, each with its own skin. Professor X sits in his wheelchair. */
public class MutantRenderer extends HumanoidMobRenderer<MutantNpc, PlayerModel<MutantNpc>> {
    /** Characters drawn with thin arms (like Alex). Must match SLIM in tools/skins.py. */
    private static final java.util.Set<String> SLIM = java.util.Set.of("storm", "jean_grey", "rogue", "kitty_pryde",
            "jubilee", "moira", "mystique", "white_queen", "krakoa_mutant");
    private static final ResourceLocation FALLBACK =
            ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private final PlayerModel<MutantNpc> wide;
    private final PlayerModel<MutantNpc> slim;

    public MutantRenderer(EntityRendererProvider.Context context) {
        super(context, new SeatedModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        wide = model;
        slim = new SeatedModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        addLayer(new WheelchairLayer(this, context.bakeLayer(WheelchairLayer.LAYER)));
    }

    @Override
    public void render(MutantNpc entity, float yaw, float partialTicks, PoseStack poseStack,
            MultiBufferSource buffers, int light) {
        model = SLIM.contains(entity.getCharacter()) ? slim : wide;
        poseStack.pushPose();
        if (entity.isSitting()) {
            poseStack.translate(0, -WheelchairLayer.SINK, 0);
        }
        super.render(entity, yaw, partialTicks, poseStack, buffers, light);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MutantNpc entity) {
        String character = entity.getCharacter();
        if (character.isEmpty() || !character.matches("[a-z0-9_]+")) {
            return FALLBACK;
        }
        return ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "textures/entity/mutant/" + character + ".png");
    }

    /** Player model that sits down (legs forward) when the character is sitting. */
    private static class SeatedModel extends PlayerModel<MutantNpc> {
        SeatedModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setupAnim(MutantNpc entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                float netHeadYaw, float headPitch) {
            riding = riding || entity.isSitting();
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            if (entity.isSitting()) {
                // hands resting on the armrests
                rightArm.xRot = -0.35f;
                leftArm.xRot = -0.35f;
                rightArm.zRot = 0.05f;
                leftArm.zRot = -0.05f;
                rightSleeve.copyFrom(rightArm);
                leftSleeve.copyFrom(leftArm);
            }
        }
    }
}
