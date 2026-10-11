package com.emil.xmen.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.network.UsePowerPayload;
import com.emil.xmen.power.Power;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

// Only loaded on the game client, never on a dedicated server.
@Mod(value = XMenMod.MOD_ID, dist = Dist.CLIENT)
public class XMenClient {
    public static final KeyMapping USE_POWER = new KeyMapping("key.xmen.use_power",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.xmen");

    public XMenClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerKeys);
        modEventBus.addListener(this::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(USE_POWER);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "power_hud"),
                this::renderPowerHud);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        while (USE_POWER.consumeClick()) {
            if (Minecraft.getInstance().player != null) {
                PacketDistributor.sendToServer(UsePowerPayload.INSTANCE);
            }
        }
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPowerState.set("");
    }

    // Shows "Optic Blasts [Beta]" in the top-left corner.
    private void renderPowerHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Power power = ClientPowerState.get();
        if (power == null || minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        graphics.drawString(minecraft.font, power.title(), 4, 4, 0xFFFFFF, true);
    }
}
