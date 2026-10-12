package com.emil.xmen.client;

import com.emil.xmen.entity.WeaponXEntities;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Client-only setup for the Weapon X facility's people. */
public final class WeaponXClient {
    private WeaponXClient() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(WeaponXClient::registerRenderers);
        modEventBus.addListener(WeaponXClient::registerLayers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WeaponXEntities.SOLDIER.get(), WeaponXRenderer.skin("soldier"));
        event.registerEntityRenderer(WeaponXEntities.SCIENTIST.get(), WeaponXRenderer.skin("scientist"));
        event.registerEntityRenderer(WeaponXEntities.STRYKER.get(), WeaponXRenderer.skin("stryker"));
        event.registerEntityRenderer(WeaponXEntities.DEATHSTRIKE.get(), WeaponXRenderer.skin("deathstrike"));
        event.registerEntityRenderer(WeaponXEntities.JASON.get(), WeaponXRenderer.skin("jason"));
        event.registerEntityRenderer(WeaponXEntities.X_24.get(), WeaponXRenderer.skin("x24"));
        event.registerEntityRenderer(WeaponXEntities.WEAPON_XI.get(), WeaponXRenderer.skin("weapon_xi"));
        event.registerEntityRenderer(WeaponXEntities.LOGAN.get(), WeaponXRenderer.skin("logan"));
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WeaponXRenderer.CLAWS, WeaponXRenderer::createClaws);
    }
}
