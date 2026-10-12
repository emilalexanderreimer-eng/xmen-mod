package com.emil.xmen.world;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Hooks up everything that comes with the structures. */
public final class XMenWorld {
    private XMenWorld() {
    }

    public static void register(IEventBus modEventBus) {
        ModContent.register(modEventBus);
        MutantMobs.register();
        NeoForge.EVENT_BUS.addListener(XMenWorld::onServerTick);
        NeoForge.EVENT_BUS.addListener(XMenWorld::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(XMenWorld::onServerStopping);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        DangerRoom.tick();
        CerebroBlock.tickSessions();
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 40 == 0) {
            StructureEffects.tick(player);
            XMenSuit.tick(player);
        }
        if (player.tickCount % 4 == 0) {
            XDoorBlock.openNear(player);
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        DangerRoom.clearAll();
        CerebroBlock.clearSessions();
        MutantMobs.clearBars();
    }
}
