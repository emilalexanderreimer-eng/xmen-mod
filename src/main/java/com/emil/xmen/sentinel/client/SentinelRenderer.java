package com.emil.xmen.sentinel.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.sentinel.SentinelMarkX;
import com.emil.xmen.sentinel.SentinelContent;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Draws the Sentinel Mark X: black plates glowing orange, or blue with a yellow X when it's your own. */
public class SentinelRenderer extends HumanoidMobRenderer<SentinelMarkX, SentinelModel> {
    private static final ResourceLocation MARK_X = texture("mark_x");
    private static final ResourceLocation MARK_X_GLOW = texture("mark_x_glow");
    private static final ResourceLocation ALLY = texture("ally");
    private static final ResourceLocation ALLY_GLOW = texture("ally_glow");
    /** The model is player-sized; the boss is 3.45 times bigger (your own one is shrunk by its SCALE attribute). */
    private static final float SIZE = 3.45f;

    public SentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new SentinelModel(context.bakeLayer(SentinelModel.LAYER)), 1.4f);
        addLayer(new Glow(this));
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SentinelRenderer::registerRenderers);
        modEventBus.addListener(SentinelRenderer::registerLayers);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SentinelModel.LAYER, SentinelModel::create);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SentinelContent.MARK_X.get(), SentinelRenderer::new);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "textures/entity/sentinel/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(SentinelMarkX sentinel) {
        return sentinel.isAlly() ? ALLY : MARK_X;
    }

    @Override
    protected void scale(SentinelMarkX sentinel, PoseStack pose, float partialTick) {
        pose.scale(SIZE, SIZE, SIZE);
    }

    /** Eyes, chest core and (on the Mark X) the seams between its plates glow in the dark. */
    private static class Glow extends RenderLayer<SentinelMarkX, SentinelModel> {
        Glow(RenderLayerParent<SentinelMarkX, SentinelModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, SentinelMarkX sentinel,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (sentinel.isInvisible() || !sentinel.isAwake()) {
                return;
            }
            ResourceLocation glow = sentinel.isAlly() ? ALLY_GLOW : MARK_X_GLOW;
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(glow)), 15728640,
                    OverlayTexture.NO_OVERLAY);
        }
    }
}
