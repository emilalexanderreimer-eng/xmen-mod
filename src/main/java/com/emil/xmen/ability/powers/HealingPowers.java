package com.emil.xmen.ability.powers;

import static com.emil.xmen.ability.Abilities.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import com.emil.xmen.ability.Allies;
import com.emil.xmen.ability.Highlights;
import com.emil.xmen.ability.Morphs;
import com.emil.xmen.ability.PowerBehavior;
import com.emil.xmen.ability.PowerBehaviors;
import com.emil.xmen.ability.PowerProjectiles;
import com.emil.xmen.ability.PowerRuntime;
import com.emil.xmen.ability.PowerState;
import com.emil.xmen.ability.Scheduler;
import com.emil.xmen.ability.TempBlocks;
import com.emil.xmen.ability.Zones;
import com.emil.xmen.entity.ModEntities;
import com.emil.xmen.entity.PowerClone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.HashSet;
import java.util.Set;
import com.emil.xmen.ability.DeathRecords;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.ZombieVillager;

/** Healing & Life. */
public final class HealingPowers {
    private HealingPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private static void removeBadEffects(LivingEntity entity) {
        List<Holder<MobEffect>> bad = new ArrayList<>();
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (instance.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) {
                bad.add(instance.getEffect());
            }
        }
        bad.forEach(entity::removeEffect);
    }

    /** Infects target and, a moment later, the mobs standing next to it. */
    private static void infect(ServerPlayer player, LivingEntity target, Set<UUID> infected, int generation) {
        if (!infected.add(target.getUUID())) {
            return;
        }
        effect(target, MobEffects.POISON, 8, 0);
        effect(target, MobEffects.WEAKNESS, 12, 1);
        effect(target, MobEffects.MOVEMENT_SLOWDOWN, 8, 0);
        particlesAround(player, target, ParticleTypes.SNEEZE, 10);
        if (generation >= 4) {
            return;
        }
        Scheduler.later(40, () -> {
            if (!target.isAlive()) {
                return;
            }
            for (LivingEntity next : livingNear(player, target.position(), 3.5)) {
                if (!isFriend(player, next)) {
                    infect(player, next, infected, generation + 1);
                }
            }
        });
    }

    public static void register() {
        // Heal fast from any wound.
        reg("healing_factor", b().effect(MobEffects.REGENERATION, 1));

        // You keep healing on your own, poison and wither barely work, and when you're nearly dead
        // your body snaps back to full health (once a minute).
        reg("extreme_regeneration", b()
                .every(40, player -> player.heal(1))
                .onHurt((player, event) -> {
                    var source = event.getSource();
                    if (source.is(net.minecraft.world.damagesource.DamageTypes.WITHER)
                            || source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                                    && player.hasEffect(MobEffects.POISON)) {
                        event.setAmount(event.getAmount() * 0.2f);
                    }
                })
                .every(5, player -> {
                    if (player.getHealth() < player.getMaxHealth() * 0.3f && !state(player).active("snap", now(player))) {
                        state(player).start("snap", now(player), 1200);
                        player.setHealth(player.getMaxHealth());
                        particlesAround(player, player, ParticleTypes.HEART, 10);
                        tell(player, "Your body snaps back together!");
                        sound(player, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.6f, 1.5f);
                    }
                }));

        // Heal the friend, pet or animal you look at (or yourself if you look at nothing).
        reg("healing_touch", b().active(4, player -> {
            LivingEntity target = lookedAtLiving(player, 6);
            if (target == null || isEnemy(player, target)) {
                target = player;
            }
            target.heal(8);
            removeBadEffects(target);
            particlesAround(player, target, ParticleTypes.HEART, 8);
            sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1, 1.8f);
            return true;
        }));

        // Steal health from every hostile mob around you.
        reg("life_drain", b().active(6, player -> {
            float stolen = 0;
            for (LivingEntity e : livingAround(player, 6)) {
                if (!isFriend(player, e)) {
                    hurtMagic(player, e, 3);
                    stolen += 2;
                    particleLine(player, e.getBoundingBox().getCenter(), player.getBoundingBox().getCenter(),
                            ParticleTypes.DAMAGE_INDICATOR, 0.6);
                }
            }
            if (stolen == 0) {
                tell(player, "No mobs to drain");
                return false;
            }
            player.heal(stolen);
            sound(player, SoundEvents.WITHER_SHOOT, 0.5f, 1.8f);
            return true;
        }));

        reg("poison_proof", b()
                .immuneTo((player, source) -> source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)
                        && player.hasEffect(MobEffects.POISON))
                .every(10, player -> player.removeEffect(MobEffects.POISON)));

        reg("sickness_proof", b()
                .immuneTo((player, source) -> source.is(net.minecraft.world.damagesource.DamageTypes.WITHER))
                .every(10, player -> {
                    player.removeEffect(MobEffects.HUNGER);
                    player.removeEffect(MobEffects.CONFUSION);
                    player.removeEffect(MobEffects.WITHER);
                }));

        // Hunger goes down three times slower.
        reg("slow_metabolism", b().tick(player -> {
            var food = player.getFoodData();
            float now = food.getExhaustionLevel();
            float last = (float) state(player).number("exhaustion");
            if (now > last) {
                now = last + (now - last) * 0.3f;
                food.setExhaustion(now);
            }
            state(player).put("exhaustion", (double) now);
        }));

        // Bring back the player who died last (to where they fell), or your last pet that died.
        reg("resurrection", b().active(120, player -> {
            DeathRecords.PlayerDeath death = DeathRecords.lastPlayerDeath();
            if (death != null && !death.player().equals(player.getUUID())
                    && now(player) - death.time() < 20 * 60 * 10) {
                ServerPlayer friend = player.server.getPlayerList().getPlayer(death.player());
                ServerLevel level = player.server.getLevel(death.dimension());
                if (friend != null && friend.isAlive() && level != null) {
                    DeathRecords.clearPlayerDeath();
                    friend.teleportTo(level, death.pos().x, death.pos().y, death.pos().z, friend.getYRot(),
                            friend.getXRot());
                    friend.setHealth(friend.getMaxHealth());
                    friend.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
                    friend.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
                    level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, death.pos().x, death.pos().y + 1,
                            death.pos().z, 60, 0.5, 1, 0.5, 0.3);
                    chat(friend, player.getName().getString() + " brought you back to where you fell!");
                    chat(player, "You brought " + friend.getName().getString() + " back to life");
                    sound(player, SoundEvents.TOTEM_USE, 1, 1);
                    return true;
                }
            }
            DeathRecords.PetDeath pet = DeathRecords.takePetDeath(player.getUUID());
            if (pet != null) {
                ServerLevel level = player.server.getLevel(pet.dimension());
                if (level != null) {
                    Entity revived = EntityType.loadEntityRecursive(pet.data(), level, e -> {
                        e.moveTo(player.getX(), player.getY(), player.getZ(), e.getYRot(), e.getXRot());
                        return e;
                    });
                    if (revived instanceof LivingEntity living) {
                        living.setHealth(living.getMaxHealth());
                        living.clearFire();
                        living.setUUID(UUID.randomUUID());
                        if (revived.level() != player.level()) {
                            revived.moveTo(player.getX(), player.getY(), player.getZ());
                        }
                        player.serverLevel().addFreshEntity(revived);
                        particlesAround(player, living, ParticleTypes.TOTEM_OF_UNDYING, 40);
                        chat(player, "Your " + living.getName().getString() + " is back!");
                        sound(player, SoundEvents.TOTEM_USE, 1, 1.3f);
                        return true;
                    }
                }
            }
            tell(player, "Nobody has died recently");
            return false;
        }));

        // Touch a living thing: fully heal a friend, or badly hurt an enemy.
        reg("biokinesis", b().active(8, player -> {
            LivingEntity target = target(player, 5);
            if (target == null) {
                return false;
            }
            if (isFriend(player, target) || target instanceof net.minecraft.world.entity.animal.Animal) {
                target.setHealth(target.getMaxHealth());
                removeBadEffects(target);
                particlesAround(player, target, ParticleTypes.HEART, 12);
            } else {
                hurtMagic(player, target, 10);
                effect(target, MobEffects.WITHER, 5, 1);
                particlesAround(player, target, ParticleTypes.DAMAGE_INDICATOR, 12);
            }
            sound(player, SoundEvents.EVOKER_CAST_SPELL, 1, 1.5f);
            return true;
        }));

        // Remove bad effects from yourself or the one you look at. Cures zombie villagers on the spot.
        reg("cure_touch", b().active(6, player -> {
            LivingEntity target = lookedAtLiving(player, 5);
            if (target instanceof ZombieVillager zombie) {
                zombie.convertTo(EntityType.VILLAGER, true);
                particles(player, zombie.position().add(0, 1, 0), ParticleTypes.HAPPY_VILLAGER, 30, 0.5);
                sound(player, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 1, 1);
                chat(player, "You cured a zombie villager!");
                return true;
            }
            if (target == null || isEnemy(player, target)) {
                target = player;
            }
            removeBadEffects(target);
            target.clearFire();
            target.setTicksFrozen(0);
            particlesAround(player, target, ParticleTypes.HAPPY_VILLAGER, 15);
            sound(player, SoundEvents.BREWING_STAND_BREW, 1, 1.5f);
            return true;
        }));

        // Once a day, a killing blow sends you safely to the world spawn instead.
        reg("second_life", b().onDeath((player, source) -> {
            if (!onceADay(player, "second_life")) {
                return false;
            }
            player.setHealth(6);
            player.clearFire();
            ServerLevel overworld = player.server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            BlockPos safe = safeSpotNear(overworld, spawn);
            BlockPos to = safe == null ? spawn : safe;
            player.teleportTo(overworld, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, player.getYRot(), player.getXRot());
            player.resetFallDistance();
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
            chat(player, "Second life! You escaped death (once per day)");
            sound(player, SoundEvents.TOTEM_USE, 1, 1.5f);
            return true;
        }));

        // Friends near you regenerate.
        reg("healing_aura", b().every(20, player -> {
            for (LivingEntity e : livingAround(player, 8)) {
                if (isFriend(player, e)) {
                    buff(e, MobEffects.REGENERATION, 0);
                }
            }
            if (player.tickCount % 60 == 0) {
                ring(player, player.position().add(0, 0.2, 0), 8, ParticleTypes.HAPPY_VILLAGER);
            }
        }));

        // Standing on grass or moss heals you.
        reg("grass_healing", b().every(40, player -> {
            BlockState ground = below(player);
            if (ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.MOSS_BLOCK) || ground.is(Blocks.MYCELIUM)) {
                player.heal(1);
            }
        }));

        // When your health is low you get fast and strong.
        reg("adrenaline", b().every(10, player -> {
            if (player.getHealth() <= 6) {
                buff(player, MobEffects.MOVEMENT_SPEED, 1);
                buff(player, MobEffects.DAMAGE_BOOST, 0);
            }
        }));

        // Breathe out a poison cloud that drifts slowly where you look and keeps growing.
        reg("toxic_cloud", b().active(15, player -> {
            Vec3 look = player.getLookAngle();
            Zones.Zone cloud = Zones.add(player, player.getEyePosition().add(look.scale(2)), 2, 10, 10, null,
                    (zone, inside) -> {
                        zone.radius = Math.min(7, zone.radius + 0.25);
                        for (LivingEntity e : inside) {
                            if (!isFriend(player, e)) {
                                effect(e, MobEffects.POISON, 3, 1);
                            }
                        }
                        level(player).sendParticles(new DustParticleOptions(new Vector3f(0.4f, 0.8f, 0.1f), 2),
                                zone.center.x, zone.center.y, zone.center.z, (int) (zone.radius * 6), zone.radius * 0.7,
                                zone.radius * 0.4, zone.radius * 0.7, 0);
                    });
            cloud.velocity = new Vec3(look.x, 0, look.z).normalize().scale(0.05);
            sound(player, SoundEvents.PUFFER_FISH_BLOW_UP, 1, 0.6f);
            return true;
        }));

        // Make a mob sick. The sickness jumps to mobs standing next to it, and on from them.
        reg("plague", b().active(10, player -> {
            LivingEntity target = target(player, 16);
            if (target == null) {
                return false;
            }
            infect(player, target, new HashSet<>(), 0);
            sound(player, SoundEvents.ZOMBIE_INFECT, 1, 0.6f);
            return true;
        }));

        // See the health of the mob you look at.
        reg("life_sense", b().every(5, player -> {
            LivingEntity target = lookedAtLiving(player, 24);
            if (target != null) {
                tell(player, target.getName().getString() + ": " + (int) Math.ceil(target.getHealth()) + " / "
                        + (int) target.getMaxHealth() + " health");
            }
        }));

        // Getting hurt fills your hunger.
        reg("pain_eater", b().onHurt((player, event) -> {
            int food = Math.max(1, (int) (event.getAmount() / 2));
            player.getFoodData().eat(food, 0.3f);
        }));

        // While you have food, damage eats your hunger instead of your health.
        reg("undying", b().onHurt((player, event) -> {
            var food = player.getFoodData();
            if (food.getFoodLevel() > 0 && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                int cost = (int) Math.ceil(event.getAmount());
                food.setFoodLevel(Math.max(0, food.getFoodLevel() - cost));
                food.setSaturation(0);
                event.setCanceled(true);
                particlesAround(player, player, ParticleTypes.SMOKE, 4);
            }
        }));

        // Your punches heal you a little.
        reg("vampire_bite", b().onAttack((player, target, event) -> {
            player.heal(Math.max(1, event.getAmount() * 0.25f));
            particlesAround(player, target, ParticleTypes.DAMAGE_INDICATOR, 3);
        }));
    }
}
