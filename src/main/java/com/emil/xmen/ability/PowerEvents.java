package com.emil.xmen.ability;

import java.util.List;

import com.emil.xmen.entity.PowerClone;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Connects game events to the hooks that powers declare in their PowerBehavior. */
public final class PowerEvents {
    private static long serverTicks;

    private PowerEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PowerEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onServerStopping);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onLogout);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onRespawn);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onClone);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onStartTracking);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onDrops);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onExperienceDrop);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onProjectileImpact);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onBreak);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onFinishUsing);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onFall);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onXp);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(PowerEvents::onGameEvent);
    }

    private static List<PowerBehavior> behaviors(ServerPlayer player) {
        return PowerRuntime.behaviors(player);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        serverTicks++;
        Scheduler.tick();
        Zones.tick();
        TempBlocks.tick();
        Allies.tick(serverTicks);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        TempBlocks.restoreAll();
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerRuntime.tick(player);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerRuntime.onLogout(player);
        }
    }

    // A respawned player is a new entity, so its power bonuses have to be put back.
    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PowerState.clear(player);
            Morphs.set(player, null);
            PowerRuntime.refresh(player);
        }
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer player) {
            for (PowerBehavior behavior : behaviors(original)) {
                if (behavior.keepXp) {
                    player.experienceLevel = original.experienceLevel;
                    player.experienceProgress = original.experienceProgress;
                    player.totalExperience = original.totalExperience;
                }
            }
        }
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer target) {
            Morphs.onStartTracking(viewer, target);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();

        // Allies and clones never hurt their owner, and owners don't hurt their own helpers by accident.
        if (attacker != null && Allies.blocksTarget(attacker, victim)) {
            event.setCanceled(true);
            return;
        }
        if (attacker instanceof ServerPlayer owner && (Allies.isAllyOf(victim, owner.getUUID())
                || victim instanceof PowerClone clone && owner.getUUID().equals(clone.getOwnerId()))) {
            event.setCanceled(true);
            return;
        }

        if (victim instanceof ServerPlayer player) {
            if (source.is(DamageTypeTags.IS_EXPLOSION) && Abilities.isOwnBlast(player)) {
                event.setCanceled(true);
                return;
            }
            if (PowerRuntime.isImmune(player, source)) {
                event.setCanceled(true);
                return;
            }
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.HurtHook hook : behavior.hurtHooks) {
                    hook.onHurt(player, event);
                    if (event.isCanceled()) {
                        return;
                    }
                }
            }
        }

        if (attacker instanceof ServerPlayer player && attacker != victim && !Abilities.isPowerDamage()
                && source.getDirectEntity() == player) {
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.AttackHook hook : behavior.attackHooks) {
                    hook.onAttack(player, victim, event);
                }
            }
            PowerState.of(player).put("last_attack", player.level().getGameTime());
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.DeathHook hook : behavior.deathHooks) {
                    if (hook.preventDeath(player, event.getSource())) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
        DeathRecords.onDeath(event.getEntity());
    }

    private static void onDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.KillHook hook : behavior.killHooks) {
                    hook.onKill(player, event.getEntity(), event.getDrops());
                }
            }
        }
    }

    private static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (PowerBehavior behavior : behaviors(player)) {
                if (behavior.keepXp) {
                    event.setDroppedExperience(0);
                }
            }
        }
    }

    private static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target == null) {
            return;
        }
        if (Allies.blocksTarget(mob, target)) {
            event.setCanceled(true);
            return;
        }
        if (target instanceof ServerPlayer player && mob instanceof Mob attacker) {
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.TargetFilter filter : behavior.targetFilters) {
                    if (filter.ignores(player, attacker)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
            TargetWatchers.onTargeted(player, attacker);
        }
    }

    private static void onProjectileImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide()) {
            return;
        }
        HitResult hit = event.getRayTraceResult();
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof ServerPlayer player
                && projectile.getOwner() != player) {
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.ProjectileHook hook : behavior.projectileHooks) {
                    if (hook.stops(player, projectile)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        }
        PowerProjectiles.handle(projectile, hit);
    }

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.BreakHook hook : behavior.breakHooks) {
                    hook.onBreak(player, event.getPos(), event.getState());
                }
            }
        }
    }

    private static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getItem().has(net.minecraft.core.component.DataComponents.FOOD)) {
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.EatHook hook : behavior.eatHooks) {
                    hook.onEat(player, event.getItem());
                }
            }
        }
    }

    private static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.FallHook hook : behavior.fallHooks) {
                    if (hook.onFall(player, event.getDistance())) {
                        event.setDamageMultiplier(0);
                    }
                }
            }
        }
    }

    private static void onXp(PlayerXpEvent.PickupXp event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.XpHook hook : behavior.xpHooks) {
                    hook.onXp(player, event.getOrb().getValue());
                }
            }
        }
    }

    private static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        Allies.onEntityLoaded(entity);
        Highlights.onEntityLoaded(entity);
        // Summoned weapons can't be dropped and kept.
        if (entity instanceof net.minecraft.world.entity.item.ItemEntity item
                && com.emil.xmen.ability.powers.TechPowers.isSummoned(item.getItem())) {
            event.setCanceled(true);
            return;
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof ServerPlayer player) {
            for (PowerBehavior behavior : behaviors(player)) {
                for (PowerBehavior.SpawnHook hook : behavior.spawnHooks) {
                    hook.onSpawn(player, entity);
                }
            }
        }
    }

    private static void onGameEvent(VanillaGameEvent event) {
        if (event.getCause() instanceof ServerPlayer player) {
            for (PowerBehavior behavior : behaviors(player)) {
                if (behavior.silent) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }
}
