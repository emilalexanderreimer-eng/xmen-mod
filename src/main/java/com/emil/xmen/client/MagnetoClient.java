package com.emil.xmen.client;

import com.emil.xmen.entity.MagnetoDebris;
import com.emil.xmen.entity.MagnetoEntities;
import com.emil.xmen.world.ModContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Client-only setup for Magneto: how he, his flying metal and his helmet are drawn. */
public final class MagnetoClient {
    private MagnetoClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MagnetoClient::registerRenderers);
        modEventBus.addListener(MagnetoClient::registerLayers);
        modEventBus.addListener(MagnetoClient::registerItemExtensions);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MagnetoEntities.MAGNETO.get(), MagnetoRenderer::new);
        event.registerEntityRenderer(MagnetoEntities.DEBRIS.get(), DebrisRenderer::new);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MagnetoHelmetModel.LAYER, MagnetoHelmetModel::create);
    }

    /** Worn helmets use the 3D helmet shape instead of a plain box. */
    private static void registerItemExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private HumanoidModel<LivingEntity> model;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot,
                    HumanoidModel<?> original) {
                if (model == null) {
                    model = new HumanoidModel<>(Minecraft.getInstance().getEntityModels()
                            .bakeLayer(MagnetoHelmetModel.LAYER));
                }
                return model;
            }
        }, ModContent.MAGNETO_HELMET.get());
    }

    /** A spinning block of metal. */
    private static class DebrisRenderer extends EntityRenderer<MagnetoDebris> {
        private final BlockRenderDispatcher blocks;

        DebrisRenderer(EntityRendererProvider.Context context) {
            super(context);
            blocks = context.getBlockRenderDispatcher();
            shadowRadius = 0.3f;
        }

        @Override
        public void render(MagnetoDebris debris, float yaw, float partialTick, PoseStack pose,
                MultiBufferSource buffers, int light) {
            float spin = (debris.tickCount + partialTick) * 9.0f;
            pose.pushPose();
            pose.translate(0.0, 0.45, 0.0);
            pose.mulPose(Axis.YP.rotationDegrees(spin));
            pose.mulPose(Axis.XP.rotationDegrees(spin * 0.6f));
            pose.scale(0.85f, 0.85f, 0.85f);
            pose.translate(-0.5, -0.5, -0.5);
            blocks.renderSingleBlock(debris.getBlock(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            super.render(debris, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public ResourceLocation getTextureLocation(MagnetoDebris debris) {
            return TextureAtlas.LOCATION_BLOCKS;
        }
    }
}
