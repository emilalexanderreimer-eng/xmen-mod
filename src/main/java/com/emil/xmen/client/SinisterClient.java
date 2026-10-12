package com.emil.xmen.client;

import com.emil.xmen.entity.SinisterEntities;

import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only setup for Mister Sinister: how he, his clones and his blasts are drawn. */
public final class SinisterClient {
    private SinisterClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SinisterClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SinisterEntities.SINISTER.get(),
                context -> new SinisterRenderer<>(context, SinisterRenderer.SINISTER));
        event.registerEntityRenderer(SinisterEntities.CLONE.get(),
                context -> new SinisterRenderer<>(context, SinisterRenderer.CLONE));
        // the blast is all particles, sent by the server
        event.registerEntityRenderer(SinisterEntities.BLAST.get(), NoopRenderer::new);
    }
}
