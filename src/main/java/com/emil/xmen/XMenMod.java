package com.emil.xmen;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

@Mod(XMenMod.MOD_ID)
public class XMenMod {
    public static final String MOD_ID = "xmen";
    public static final Logger LOGGER = LogUtils.getLogger();

    public XMenMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogin);
        LOGGER.info("X-Men Mutants mod loaded");
    }

    // Shows a chat message on join so it's easy to see the mod is installed.
    private void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        event.getEntity().sendSystemMessage(
                Component.translatable("message.xmen.welcome").withStyle(ChatFormatting.GOLD));
    }
}
