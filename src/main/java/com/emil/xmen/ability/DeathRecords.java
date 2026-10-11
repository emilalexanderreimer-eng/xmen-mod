package com.emil.xmen.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Remembers recent deaths of players and pets, for Resurrection. */
public final class DeathRecords {
    public record PlayerDeath(UUID player, ResourceKey<Level> dimension, Vec3 pos, long time) {
    }

    public record PetDeath(CompoundTag data, ResourceKey<Level> dimension, Vec3 pos, long time) {
    }

    private static PlayerDeath lastPlayerDeath;
    private static final Map<UUID, PetDeath> LAST_PET_DEATH = new HashMap<>();

    private DeathRecords() {
    }

    static void onDeath(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            lastPlayerDeath = new PlayerDeath(player.getUUID(), level.dimension(), player.position(),
                    level.getGameTime());
        } else if (entity instanceof TamableAnimal pet && pet.isTame() && pet.getOwnerUUID() != null) {
            CompoundTag tag = new CompoundTag();
            if (pet.save(tag)) {
                LAST_PET_DEATH.put(pet.getOwnerUUID(), new PetDeath(tag, level.dimension(), pet.position(),
                        level.getGameTime()));
            }
        }
    }

    public static PlayerDeath lastPlayerDeath() {
        return lastPlayerDeath;
    }

    public static void clearPlayerDeath() {
        lastPlayerDeath = null;
    }

    public static PetDeath takePetDeath(UUID owner) {
        return LAST_PET_DEATH.remove(owner);
    }
}
