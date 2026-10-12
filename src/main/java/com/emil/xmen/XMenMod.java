package com.emil.xmen;

import com.emil.xmen.ability.PowerBehaviors;
import com.emil.xmen.ability.PowerEvents;
import com.emil.xmen.apocalypse.ApocalypseContent;
import com.emil.xmen.entity.MagnetoEntities;
import com.emil.xmen.entity.ModEntities;
import com.emil.xmen.entity.SinisterEntities;
import com.emil.xmen.entity.WeaponXEntities;
import com.emil.xmen.network.ModNetwork;
import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.power.PowerCommand;
import com.emil.xmen.power.Powers;
import com.emil.xmen.world.XMenWorld;
import com.mojang.logging.LogUtils;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

@Mod(XMenMod.MOD_ID)
public class XMenMod {
    public static final String MOD_ID = "xmen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public XMenMod(IEventBus modEventBus) {
        PlayerPowers.register(modEventBus);
        ModEntities.register(modEventBus);
        MagnetoEntities.register(modEventBus);
        SinisterEntities.register(modEventBus);
        ApocalypseContent.register(modEventBus);
        WeaponXEntities.register(modEventBus);
        XMenWorld.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
        PowerEvents.register();
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        LOGGER.info("X-Men Mutants loaded with {} powers, {} of them working", Powers.count(),
                PowerBehaviors.count());
        for (String id : PowerBehaviors.ids()) {
            if (Powers.get(id) == null) {
                LOGGER.error("Power code exists for unknown power id {}", id);
            }
        }
    }

    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerPowers.onLogin(player);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        PowerCommand.register(event.getDispatcher());
    }
}
