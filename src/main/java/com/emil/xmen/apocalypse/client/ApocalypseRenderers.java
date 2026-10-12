package com.emil.xmen.apocalypse.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.apocalypse.ApocalypseBoss;
import com.emil.xmen.apocalypse.ApocalypseContent;
import com.emil.xmen.apocalypse.ApocalypseServant;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only setup: how Apocalypse, his Horsemen and the cultists are drawn. */
public final class ApocalypseRenderers {
    private ApocalypseRenderers() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ApocalypseRenderers::registerRenderers);
        modEventBus.addListener(ApocalypseRenderers::registerLayers);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ApocalypseModel.LAYER, ApocalypseModel::create);
        event.registerLayerDefinition(ServantModel.LAYER, ServantModel::create);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ApocalypseContent.APOCALYPSE.get(), Boss::new);
        event.registerEntityRenderer(ApocalypseContent.STORM.get(), Servant::new);
        event.registerEntityRenderer(ApocalypseContent.PSYLOCKE.get(), Servant::new);
        event.registerEntityRenderer(ApocalypseContent.ARCHANGEL.get(), Servant::new);
        event.registerEntityRenderer(ApocalypseContent.HORSEMAN_MAGNETO.get(), Servant::new);
        event.registerEntityRenderer(ApocalypseContent.CULTIST.get(), Servant::new);
    }

    static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "textures/entity/apocalypse/" + name + ".png");
    }

    static class Boss extends HumanoidMobRenderer<ApocalypseBoss, ApocalypseModel> {
        private static final ResourceLocation SKIN = texture("apocalypse");
        private static final ResourceLocation GLOW = texture("apocalypse_glow");

        Boss(EntityRendererProvider.Context context) {
            super(context, new ApocalypseModel(context.bakeLayer(ApocalypseModel.LAYER)), 0.7f);
            addLayer(new GlowLayer<>(this, boss -> boss.isAwake() ? GLOW : null));
        }

        @Override
        public ResourceLocation getTextureLocation(ApocalypseBoss boss) {
            return SKIN;
        }
    }

    static class Servant extends HumanoidMobRenderer<ApocalypseServant, ServantModel> {
        Servant(EntityRendererProvider.Context context) {
            super(context, new ServantModel(context.bakeLayer(ServantModel.LAYER)), 0.5f);
            addLayer(new GlowLayer<>(this, servant -> switch (servant.kind()) {
                case STORM -> texture("storm_glow");
                case PSYLOCKE -> texture("psylocke_glow");
                case ARCHANGEL -> texture("archangel_glow");
                default -> null;
            }));
        }

        @Override
        public ResourceLocation getTextureLocation(ApocalypseServant servant) {
            return texture(servant.kind().id);
        }
    }
}
