package com.emil.xmen.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.emil.xmen.network.MorphPayload;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.network.PacketDistributor;

/** Makes a player look like a mob to everyone (the body stays a player body for hits and movement). */
public final class Morphs {
    private static final Map<UUID, ResourceLocation> MORPHS = new HashMap<>();

    private Morphs() {
    }

    public static void set(ServerPlayer player, EntityType<?> type) {
        if (type == null) {
            if (MORPHS.remove(player.getUUID()) == null) {
                return;
            }
        } else {
            MORPHS.put(player.getUUID(), BuiltInRegistries.ENTITY_TYPE.getKey(type));
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, payload(player));
    }

    public static EntityType<?> get(ServerPlayer player) {
        ResourceLocation id = MORPHS.get(player.getUUID());
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.get(id);
    }

    public static boolean isMorphed(ServerPlayer player) {
        return MORPHS.containsKey(player.getUUID());
    }

    static MorphPayload payload(ServerPlayer player) {
        ResourceLocation id = MORPHS.get(player.getUUID());
        return new MorphPayload(player.getId(), id == null ? "" : id.toString());
    }

    /** Tells a player who just started seeing target what target looks like. */
    static void onStartTracking(ServerPlayer viewer, ServerPlayer target) {
        if (MORPHS.containsKey(target.getUUID())) {
            PacketDistributor.sendToPlayer(viewer, payload(target));
        }
    }
}
