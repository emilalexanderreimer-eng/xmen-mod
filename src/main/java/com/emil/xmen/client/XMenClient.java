package com.emil.xmen.client;

import com.emil.xmen.XMenMod;
import com.emil.xmen.entity.ModEntities;
import com.emil.xmen.ability.PowerBehaviors;
import com.emil.xmen.network.ElevatorPayload;
import com.emil.xmen.network.UsePowerPayload;
import com.emil.xmen.world.ModContent;
import com.emil.xmen.power.Power;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

// Only loaded on the game client, never on a dedicated server.
@Mod(value = XMenMod.MOD_ID, dist = Dist.CLIENT)
public class XMenClient {
    private static boolean gliding;

    public static final KeyMapping USE_POWER = new KeyMapping("key.xmen.use_power",
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.categories.xmen");

    private int wallRunTicks;
    private boolean lastJump;
    private boolean lastSneak;
    private int elevatorCooldown;

    public XMenClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerKeys);
        modEventBus.addListener(this::registerGuiLayers);
        modEventBus.addListener(this::registerRenderers);
        MagnetoClient.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(this::onFov);
        NeoForge.EVENT_BUS.addListener(MorphRenderer::onRenderPlayer);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CLONE.get(), CloneRenderer::new);
    }

    // Eagle Eyes zooms in like a spyglass.
    private void onFov(ComputeFovModifierEvent event) {
        if (ClientPowerState.zoomed()) {
            event.setNewFovModifier(event.getFovModifier() * 0.2f);
        }
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(USE_POWER);
    }

    private void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "power_hud"),
                this::renderPowerHud);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        ClientPowerState.tick();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        while (USE_POWER.consumeClick()) {
            PacketDistributor.sendToServer(UsePowerPayload.INSTANCE);
        }
        elevatorTick(player);
        MorphRenderer.tick();
        Power power = ClientPowerState.get();
        if (power == null || player.getAbilities().flying || player.isSpectator()) {
            return;
        }
        // Player movement is decided on the client, so climbing, gliding and running on water happen here.
        String id = power.id();
        String morph = ClientMorphs.get(player.getId());
        boolean climber = id.equals("wall_crawling") || id.equals("beast_form") || id.equals("spider_powers")
                || "minecraft:spider".equals(morph) || "minecraft:cave_spider".equals(morph);
        Vec3 motion = player.getDeltaMovement();
        if (climber && player.horizontalCollision) {
            player.setDeltaMovement(motion.x, player.isShiftKeyDown() ? 0 : 0.2, motion.z);
            player.resetFallDistance();
        }
        if (id.equals("metal_wings") && !player.onGround() && !player.isInWater()
                && player.input.jumping && motion.y < 0) {
            Vec3 look = player.getLookAngle();
            player.setDeltaMovement(motion.x * 0.85 + look.x * 0.75 * 0.15, Math.max(motion.y, -0.08),
                    motion.z * 0.85 + look.z * 0.75 * 0.15);
            player.resetFallDistance();
        }
        // Glider works like a light elytra: look down to dive and pick up speed, look up to trade speed for height.
        if (id.equals("glider")) {
            boolean airborne = !player.onGround() && !player.isInWater() && player.input.jumping;
            if (airborne && (gliding || motion.y < 0)) {
                gliding = true;
                Vec3 look = player.getLookAngle();
                double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                double pitch = look.y;
                if (pitch < 0) {
                    speed = Math.min(1.4, speed - pitch * 0.06);
                } else {
                    speed = Math.max(0.15, speed - pitch * 0.04);
                }
                double flat = Math.sqrt(look.x * look.x + look.z * look.z);
                double dx = flat < 1.0E-3 ? 0 : look.x / flat;
                double dz = flat < 1.0E-3 ? 0 : look.z / flat;
                double vy = pitch < 0 ? pitch * speed * 0.9 : pitch * speed * 0.6 - 0.04;
                player.setDeltaMovement(dx * speed, Math.max(vy, -1.2), dz * speed);
                player.resetFallDistance();
            } else {
                gliding = false;
            }
        }
        if (id.equals("water_runner") && player.isSprinting()) {
            BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.1, player.getZ());
            boolean onWater = player.level().getFluidState(below).is(FluidTags.WATER);
            if (player.isInWater() && !player.isUnderWater()) {
                player.setDeltaMovement(motion.x, 0.25, motion.z);
            } else if (onWater && motion.y < 0) {
                player.setDeltaMovement(motion.x, 0, motion.z);
                player.setOnGround(true);
                player.resetFallDistance();
            }
        }
        if (id.equals("wall_run")) {
            if (player.onGround()) {
                wallRunTicks = 0;
            } else if (player.horizontalCollision && player.input.forwardImpulse > 0 && wallRunTicks < 30) {
                wallRunTicks++;
                player.setDeltaMovement(motion.x * 1.1, Math.max(0, motion.y), motion.z * 1.1);
                player.resetFallDistance();
            }
        }
    }

    // Elevator blocks: jump to ride up, sneak to ride down. The keys are only known on the client.
    private void elevatorTick(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean jump = minecraft.options.keyJump.isDown();
        boolean sneak = minecraft.options.keyShift.isDown();
        if (minecraft.screen == null && player.onGround() && elevatorCooldown <= 0) {
            BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
            if (player.level().getBlockState(below).is(ModContent.ELEVATOR.get())) {
                if (jump && !lastJump) {
                    PacketDistributor.sendToServer(new ElevatorPayload(true));
                    elevatorCooldown = 8;
                } else if (sneak && !lastSneak) {
                    PacketDistributor.sendToServer(new ElevatorPayload(false));
                    elevatorCooldown = 8;
                }
            }
        }
        if (elevatorCooldown > 0) {
            elevatorCooldown--;
        }
        lastJump = jump;
        lastSneak = sneak;
    }

    private void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientPowerState.set("");
        ClientMorphs.clear();
        MorphRenderer.clear();
    }

    // Shows "Optic Blasts [Beta]" in the top-left corner.
    private void renderPowerHud(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Power power = ClientPowerState.get();
        if (power == null || minecraft.options.hideGui || minecraft.getDebugOverlay().showDebugScreen()) {
            return;
        }
        graphics.drawString(minecraft.font, power.title(), 4, 4, 0xFFFFFF, true);
        Component status;
        if (!PowerBehaviors.isImplemented(power.id())) {
            status = Component.literal("coming in a later update").withStyle(ChatFormatting.DARK_GRAY);
        } else if (!power.type().usesKey()) {
            status = Component.literal("always on").withStyle(ChatFormatting.GRAY);
        } else if (ClientPowerState.cooldownTicks() > 0) {
            status = Component.literal("[").append(USE_POWER.getTranslatedKeyMessage())
                    .append("] " + (int) Math.ceil(ClientPowerState.cooldownTicks() / 20.0) + "s")
                    .withStyle(ChatFormatting.GRAY);
        } else {
            status = Component.literal("[").append(USE_POWER.getTranslatedKeyMessage()).append("] ready")
                    .withStyle(ChatFormatting.GREEN);
        }
        graphics.drawString(minecraft.font, status, 4, 15, 0xFFFFFF, true);
    }
}
