package com.emil.xmen.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Mobs that fight for a player: summoned helpers (removed when their time is up) and controlled mobs
 * (set free when their time is up). Allies never attack their owner or each other and follow the owner.
 */
public final class Allies {
    public static final String OWNER_TAG = "xmen_ally_owner";

    private record Ally(UUID owner, ServerLevel level, long endTime, boolean summoned) {
    }

    private static final Map<UUID, Ally> ALLIES = new HashMap<>();

    private Allies() {
    }

    /** Spawns entity as a helper that disappears after seconds. */
    public static void summon(ServerPlayer owner, Entity entity, double seconds) {
        ServerLevel level = owner.serverLevel();
        level.addFreshEntity(entity);
        ALLIES.put(entity.getUUID(), new Ally(owner.getUUID(), level, level.getGameTime() + (long) (seconds * 20),
                true));
    }

    /** Makes an existing mob fight for owner for seconds (or forever if seconds is negative). */
    public static void control(ServerPlayer owner, Mob mob, double seconds) {
        ServerLevel level = owner.serverLevel();
        long end = seconds < 0 ? Long.MAX_VALUE : level.getGameTime() + (long) (seconds * 20);
        ALLIES.put(mob.getUUID(), new Ally(owner.getUUID(), level, end, false));
        if (mob.getTarget() == owner) {
            mob.setTarget(null);
        }
        if (seconds < 0) {
            mob.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
            mob.setPersistenceRequired();
        }
    }

    /** Re-adds permanent allies when they load from disk. */
    static void onEntityLoaded(Entity entity) {
        if (entity instanceof Mob mob && mob.getPersistentData().hasUUID(OWNER_TAG)
                && entity.level() instanceof ServerLevel level) {
            ALLIES.put(mob.getUUID(), new Ally(mob.getPersistentData().getUUID(OWNER_TAG), level, Long.MAX_VALUE,
                    false));
        }
    }

    public static UUID ownerOf(Entity entity) {
        Ally ally = ALLIES.get(entity.getUUID());
        return ally == null ? null : ally.owner();
    }

    public static boolean isAllyOf(Entity entity, UUID owner) {
        Ally ally = ALLIES.get(entity.getUUID());
        return ally != null && ally.owner().equals(owner);
    }

    public static int countOwned(UUID owner, boolean summoned) {
        int count = 0;
        for (Ally ally : ALLIES.values()) {
            if (ally.owner().equals(owner) && ally.summoned() == summoned && ally.endTime() == Long.MAX_VALUE) {
                count++;
            }
        }
        return count;
    }

    /** Should mob be stopped from targeting target? */
    static boolean blocksTarget(Entity mob, LivingEntity target) {
        Ally ally = ALLIES.get(mob.getUUID());
        if (ally == null || target == null) {
            return false;
        }
        return target.getUUID().equals(ally.owner()) || isAllyOf(target, ally.owner());
    }

    static void tick(long serverTick) {
        if (ALLIES.isEmpty() || serverTick % 10 != 0) {
            return;
        }
        List<UUID> done = new ArrayList<>();
        for (Map.Entry<UUID, Ally> entry : ALLIES.entrySet()) {
            Ally ally = entry.getValue();
            Entity entity = ally.level().getEntity(entry.getKey());
            boolean expired = ally.level().getGameTime() >= ally.endTime();
            if (entity == null || !entity.isAlive()) {
                if (expired || entity != null) {
                    done.add(entry.getKey());
                }
                continue;
            }
            if (expired) {
                done.add(entry.getKey());
                if (ally.summoned()) {
                    entity.discard();
                } else if (entity instanceof Mob mob) {
                    mob.setTarget(null);
                }
                continue;
            }
            if (entity instanceof Mob mob) {
                guide(mob, ally);
            }
        }
        done.forEach(ALLIES::remove);
    }

    private static void guide(Mob mob, Ally ally) {
        ServerPlayer owner = ally.level().getServer().getPlayerList().getPlayer(ally.owner());
        LivingEntity target = mob.getTarget();
        if (target != null && (blocksTarget(mob, target) || !target.isAlive())) {
            mob.setTarget(null);
            target = null;
        }
        if (owner == null || owner.level() != mob.level()) {
            return;
        }
        if (target == null) {
            LivingEntity wanted = owner.getLastHurtByMob();
            if (wanted == null || !wanted.isAlive() || blocksTarget(mob, wanted)) {
                wanted = owner.getLastHurtMob();
            }
            if (wanted == null || !wanted.isAlive() || blocksTarget(mob, wanted) || wanted.distanceTo(mob) > 24) {
                wanted = null;
                double best = 12 * 12;
                for (LivingEntity candidate : mob.level().getEntitiesOfClass(LivingEntity.class,
                        mob.getBoundingBox().inflate(12), e -> e instanceof Enemy && e.isAlive())) {
                    double dist = candidate.distanceToSqr(mob);
                    if (dist < best && !blocksTarget(mob, candidate) && ownerOf(candidate) == null) {
                        best = dist;
                        wanted = candidate;
                    }
                }
            }
            if (wanted != null) {
                mob.setTarget(wanted);
                target = wanted;
            }
        }
        double distance = mob.distanceTo(owner);
        if (distance > 40) {
            mob.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        } else if (target == null && distance > 8) {
            mob.getNavigation().moveTo(owner, 1.2);
        }
    }
}
