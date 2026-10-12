package com.emil.xmen.network;

import com.emil.xmen.client.ClientMorphs;
import com.emil.xmen.client.ClientPowerState;
import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.world.ElevatorBlock;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(SyncPowerPayload.TYPE, SyncPowerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPowerState.set(payload.powerId())));
        registrar.playToClient(CooldownPayload.TYPE, CooldownPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPowerState.setCooldown(payload.ticks())));
        registrar.playToClient(MorphPayload.TYPE, MorphPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientMorphs.set(payload.entityId(),
                        payload.entityType())));
        registrar.playToClient(ZoomPayload.TYPE, ZoomPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientPowerState.setZoomed(payload.zoomed())));
        registrar.playToServer(UsePowerPayload.TYPE, UsePowerPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        PlayerPowers.onUseKey(player);
                    }
                }));
        registrar.playToServer(ElevatorPayload.TYPE, ElevatorPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        ElevatorBlock.ride(player, payload.up());
                    }
                }));
    }
}
