package com.emil.xmen.power;

import java.util.function.Supplier;

import com.emil.xmen.XMenMod;
import com.emil.xmen.ability.PowerBehaviors;
import com.emil.xmen.ability.PowerRuntime;
import com.emil.xmen.network.SyncPowerPayload;
import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Stores each player's power on the player, so it is saved with the world and kept after death. */
public final class PlayerPowers {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, XMenMod.MOD_ID);

    /** The power id, or "" if the player hasn't been given one yet. */
    public static final Supplier<AttachmentType<String>> POWER = ATTACHMENT_TYPES.register("power",
            () -> AttachmentType.builder(() -> "").serialize(Codec.STRING).copyOnDeath().build());

    private PlayerPowers() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    public static Power get(ServerPlayer player) {
        return Powers.get(player.getData(POWER));
    }

    public static void set(ServerPlayer player, Power power) {
        Power old = get(player);
        player.setData(POWER, power.id());
        sync(player);
        PowerRuntime.onPowerChanged(player, old);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncPowerPayload(player.getData(POWER)));
    }

    /** Gives a new player their random power, or reminds a returning player of theirs. */
    public static void onLogin(ServerPlayer player) {
        com.emil.xmen.ability.powers.MindPowers.onLogin(player);
        Power power = get(player);
        if (power == null) {
            power = Powers.roll(player.getRandom());
            set(player, power);
            player.sendSystemMessage(Component.literal("You are a mutant! Your power is ")
                    .withStyle(ChatFormatting.YELLOW).append(power.title()));
            player.sendSystemMessage(Component.literal(power.description()).withStyle(ChatFormatting.GRAY));
        } else {
            sync(player);
            PowerRuntime.refresh(player);
            player.sendSystemMessage(Component.literal("Your power: ").withStyle(ChatFormatting.YELLOW)
                    .append(power.title()));
        }
    }

    /** Called when the player presses the power key. */
    public static void onUseKey(ServerPlayer player) {
        Power power = get(player);
        if (power == null || PowerRuntime.use(player)) {
            return;
        }
        String message;
        if (!PowerBehaviors.isImplemented(power.id())) {
            message = power.name() + " doesn't work yet. It comes in a later update.";
        } else {
            message = power.name() + " is always on, no key needed.";
        }
        player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.GRAY), true);
    }
}
