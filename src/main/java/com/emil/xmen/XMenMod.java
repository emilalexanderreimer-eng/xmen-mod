package com.emil.xmen;

import com.emil.xmen.ability.PowerBehaviors;
import com.emil.xmen.ability.PowerRuntime;
import com.emil.xmen.network.ModNetwork;
import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.power.PowerCommand;
import com.emil.xmen.power.Powers;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;

@Mod(XMenMod.MOD_ID)
public class XMenMod {
    public static final String MOD_ID = "xmen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public XMenMod(IEventBus modEventBus) {
        PlayerPowers.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(this::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::onIncomingDamage);
        LOGGER.info("X-Men Mutants loaded with {} powers, {} of them working", Powers.count(),
                PowerBehaviors.count());
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerPowers.onLogin(player);
        }
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerRuntime.onLogout(player);
        }
    }

    // A respawned player is a new entity, so its power bonuses have to be put back.
    private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerRuntime.onLogout(player);
            PowerRuntime.refresh(player);
        }
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerRuntime.tick(player);
        }
    }

    private void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PowerRuntime.isImmune(player, event.getSource())) {
            event.setCanceled(true);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PowerCommand.register(event.getDispatcher());
    }
}
