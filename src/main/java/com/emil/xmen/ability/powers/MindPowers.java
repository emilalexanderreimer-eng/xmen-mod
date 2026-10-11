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
import com.emil.xmen.power.PlayerPowers;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.GameType;

/** Mind & Psychic. */
public final class MindPowers {
    private static final DustParticleOptions PSY = new DustParticleOptions(new Vector3f(0.8f, 0.3f, 1.0f), 1.2f);
    private static final DustParticleOptions PINK = new DustParticleOptions(new Vector3f(1.0f, 0.4f, 0.8f), 1.6f);

    private record Held(int entityId, long since) {
    }

    private MindPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private static void flee(Mob mob, Vec3 from) {
        mob.setTarget(null);
        if (mob instanceof PathfinderMob pathfinder) {
            Vec3 away = DefaultRandomPos.getPosAway(pathfinder, 16, 7, from);
            if (away != null) {
                pathfinder.getNavigation().moveTo(away.x, away.y, away.z, 1.6);
            }
        }
    }

    private static void endAstral(ServerPlayer player) {
        PowerState st = state(player);
        Vec3 origin = st.get("astral_origin");
        GameType mode = st.get("astral_mode");
        if (origin == null || mode == null) {
            return;
        }
        st.remove("astral_origin");
        st.remove("astral_mode");
        player.teleportTo(origin.x, origin.y, origin.z);
        player.setGameMode(mode);
        player.getPersistentData().remove("xmen_astral");
        PowerRuntime.refresh(player);
        tell(player, "Back in your body");
    }

    private static void endPossession(ServerPlayer player) {
        PowerState st = state(player);
        Mob host = st.get("host");
        Vec3 origin = st.get("host_origin");
        st.remove("host");
        st.remove("host_origin");
        player.setCamera(player);
        if (host != null) {
            host.setNoAi(false);
        }
        if (origin != null) {
            player.teleportTo(origin.x, origin.y, origin.z);
        }
        player.removeEffect(MobEffects.INVISIBILITY);
    }

    /** A player whose astral trip was cut off (crash, quit) is put back when they log in. */
    public static void onLogin(ServerPlayer player) {
        var tag = player.getPersistentData();
        if (tag.contains("xmen_astral")) {
            var data = tag.getCompound("xmen_astral");
            player.teleportTo(data.getDouble("x"), data.getDouble("y"), data.getDouble("z"));
            player.setGameMode(GameType.byId(data.getInt("mode")));
            tag.remove("xmen_astral");
        }
    }

    public static void register() {
        // Read the minds of the mobs around you: see who each of them is hunting.
        reg("telepathy", b().active(5, player -> {
            int found = 0;
            for (LivingEntity e : livingAround(player, 24)) {
                if (e instanceof Mob mob && mob.getTarget() != null) {
                    String target = mob.getTarget() == player ? "you" : mob.getTarget().getName().getString();
                    chat(player, mob.getName().getString() + " wants to attack " + target + " ("
                            + direction(player, mob.position()) + ")");
                    particlesAround(player, mob, PSY, 6);
                    found++;
                }
            }
            if (found == 0) {
                chat(player, "No mob around you is thinking about a fight");
            }
            sound(player, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 0.6f, 1.5f);
            return true;
        }));

        // Kept from 0.3.0: first R lifts the mob you look at, second R throws it.
        reg("telekinesis", b()
                .tick(player -> {
                    Held held = state(player).get("held");
                    if (held == null) {
                        return;
                    }
                    Entity entity = player.level().getEntity(held.entityId());
                    long heldFor = now(player) - held.since();
                    if (entity == null || !entity.isAlive() || entity.distanceTo(player) > 32 || heldFor > 200) {
                        state(player).remove("held");
                        return;
                    }
                    Vec3 target = player.getEyePosition().add(player.getLookAngle().scale(4))
                            .subtract(0, entity.getBbHeight() / 2, 0);
                    setVelocity(entity, target.subtract(entity.position()).scale(0.4));
                    entity.resetFallDistance();
                    if (player.tickCount % 4 == 0) {
                        particles(player, entity.getBoundingBox().getCenter(), ParticleTypes.WITCH, 3, 0.3);
                    }
                })
                .active(2, player -> {
                    Held held = state(player).get("held");
                    if (held != null) {
                        state(player).remove("held");
                        Entity entity = player.level().getEntity(held.entityId());
                        if (entity != null) {
                            setVelocity(entity, player.getLookAngle().scale(2.5));
                            sound(player, SoundEvents.EVOKER_CAST_SPELL, 1, 0.8f);
                        }
                        return true;
                    }
                    LivingEntity target = lookedAtLiving(player, 24);
                    if (target == null) {
                        tell(player, "Look at a mob to lift it");
                        return false;
                    }
                    state(player).put("held", new Held(target.getId(), now(player)));
                    sound(player, SoundEvents.EVOKER_CAST_SPELL, 1, 1.4f);
                    tell(player, "Press R again to throw it");
                    return false;
                }));

        // Make the mob you look at fight for you for a minute.
        reg("mind_control", b().active(20, player -> {
            LivingEntity target = target(player, 24);
            if (!(target instanceof Mob mob) || isFriend(player, target)) {
                if (target != null) {
                    tell(player, "You can't control that");
                }
                return false;
            }
            Allies.control(player, mob, 60);
            particlesAround(player, mob, PSY, 20);
            tell(player, mob.getName().getString() + " fights for you for 60 seconds");
            sound(player, SoundEvents.EVOKER_PREPARE_WOLOLO, 1, 1);
            return true;
        }));

        // A mental blast: hurts, makes the mob dizzy and confused.
        reg("psychic_blast", b().active(3, player -> {
            LivingEntity target = target(player, 24);
            if (target == null) {
                return false;
            }
            particleLine(player, player.getEyePosition(), target.getEyePosition(), PSY, 0.4);
            hurtMagic(player, target, 6);
            effect(target, MobEffects.CONFUSION, 6, 0);
            effect(target, MobEffects.WEAKNESS, 6, 0);
            if (target instanceof Mob mob) {
                flee(mob, target.position().add(player.getRandom().triangle(0, 3), 0, player.getRandom().triangle(0, 3)));
            }
            sound(player, SoundEvents.ELDER_GUARDIAN_CURSE, 0.4f, 2);
            return true;
        }));

        // Three fake copies of you appear and draw the mobs' attention.
        reg("illusions", b().active(20, player -> {
            List<PowerClone> clones = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                double a = Math.PI * 2 * i / 3;
                PowerClone clone = spawnClone(player, player.position().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5),
                        PowerClone.ILLUSION, 15);
                if (clone != null) {
                    clones.add(clone);
                }
            }
            if (clones.isEmpty()) {
                return false;
            }
            int i = 0;
            for (LivingEntity e : livingAround(player, 24)) {
                if (e instanceof Mob mob && mob.getTarget() == player) {
                    mob.setTarget(clones.get(i++ % clones.size()));
                }
            }
            sound(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1, 1);
            return true;
        }));

        // Your mind is shielded: no nausea, blindness or darkness.
        reg("psychic_shield", b().every(5, player -> {
            player.removeEffect(MobEffects.CONFUSION);
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);
        }));

        // The mob you look at forgets you and won't come after you for 30 seconds.
        reg("memory_wipe", b()
                .ignoredBy((player, mob) -> {
                    Map<UUID, Long> wiped = state(player).get("wiped");
                    Long until = wiped == null ? null : wiped.get(mob.getUUID());
                    return until != null && now(player) < until;
                })
                .active(8, player -> {
                    LivingEntity target = target(player, 24);
                    if (target == null) {
                        return false;
                    }
                    Map<UUID, Long> wiped = state(player).get("wiped");
                    if (wiped == null) {
                        wiped = new HashMap<>();
                        state(player).put("wiped", wiped);
                    }
                    wiped.put(target.getUUID(), now(player) + 600);
                    forget(player, target);
                    particlesAround(player, target, ParticleTypes.POOF, 10);
                    sound(player, SoundEvents.ILLUSIONER_CAST_SPELL, 1, 1.5f);
                    return true;
                }));

        // Mobs around you panic and run away for 6 seconds.
        reg("fear", b().active(15, player -> {
            List<Mob> scared = new ArrayList<>();
            for (LivingEntity e : livingAround(player, 10)) {
                if (e instanceof Mob mob && !isFriend(player, e)) {
                    scared.add(mob);
                }
            }
            if (scared.isEmpty()) {
                return false;
            }
            Scheduler.repeat(10, 12, () -> {
                for (Mob mob : scared) {
                    if (mob.isAlive()) {
                        flee(mob, player.position());
                    }
                }
            });
            for (Mob mob : scared) {
                flee(mob, player.position());
                particlesAround(player, mob, ParticleTypes.SMOKE, 6);
            }
            sound(player, SoundEvents.WARDEN_ROAR, 0.6f, 1.4f);
            return true;
        }));

        // Hostile mobs around you calm down and won't attack for 20 seconds, unless you hit them.
        reg("calm", b()
                .ignoredBy((player, mob) -> {
                    if (!state(player).active("calm", now(player))) {
                        return false;
                    }
                    return mob.getLastHurtByMob() != player;
                })
                .active(30, player -> {
                    state(player).start("calm", now(player), 400);
                    for (LivingEntity e : livingAround(player, 12)) {
                        forget(player, e);
                        particlesAround(player, e, ParticleTypes.HEART, 2);
                    }
                    sound(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1, 0.8f);
                    tell(player, "Mobs are calm for 20 seconds");
                    return true;
                }));

        // R: form a psychic blade (or let it go). Its hits cut straight through armor.
        reg("psychic_blade", b()
                .onAttack((player, target, event) -> {
                    if (state(player).flag("blade")) {
                        Scheduler.later(1, () -> {
                            target.invulnerableTime = 0;
                            hurtMagic(player, target, 6);
                        });
                        particlesAround(player, target, PINK, 8);
                    }
                })
                .every(10, player -> {
                    if (state(player).flag("blade")) {
                        Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.8)).add(0, -0.4, 0);
                        particles(player, hand, PINK, 2, 0.05);
                    }
                })
                .active(1, player -> {
                    tell(player, state(player).toggle("blade") ? "Psychic blade ready" : "Blade gone");
                    sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1, 0.6f);
                    return true;
                }));

        // Leave your body for 15 seconds and fly through anything as a ghost. Then you snap back.
        reg("astral_projection", b()
                .every(5, player -> {
                    if (state(player).get("astral_origin") != null && !state(player).active("astral", now(player))) {
                        endAstral(player);
                    }
                })
                .active(60, player -> {
                    PowerState st = state(player);
                    st.put("astral_origin", player.position());
                    st.put("astral_mode", player.gameMode.getGameModeForPlayer());
                    st.start("astral", now(player), 300);
                    st.onClear(() -> endAstral(player));
                    var data = new net.minecraft.nbt.CompoundTag();
                    data.putDouble("x", player.getX());
                    data.putDouble("y", player.getY());
                    data.putDouble("z", player.getZ());
                    data.putInt("mode", player.gameMode.getGameModeForPlayer().getId());
                    player.getPersistentData().put("xmen_astral", data);
                    player.setGameMode(GameType.SPECTATOR);
                    tell(player, "Astral form for 15 seconds");
                    sound(player, SoundEvents.SOUL_ESCAPE, 1, 0.6f);
                    return true;
                }));

        // The Phoenix Force: fly, never burn, leave a burning trail, and R unleashes a huge fire nova.
        reg("phoenix_force", b()
                .flight()
                .effect(MobEffects.FIRE_RESISTANCE, 0)
                .effect(MobEffects.DAMAGE_RESISTANCE, 0)
                .every(5, player -> {
                    if (player.getAbilities().flying) {
                        particles(player, player.position(), ParticleTypes.FLAME, 4, 0.3);
                        for (LivingEntity e : livingAround(player, 3)) {
                            if (!isFriend(player, e)) {
                                e.igniteForSeconds(4);
                            }
                        }
                    }
                })
                .active(10, player -> {
                    for (LivingEntity e : livingAround(player, 12)) {
                        if (!isFriend(player, e)) {
                            hurt(player, e, 12, player.damageSources().inFire());
                            e.igniteForSeconds(8);
                            pushFrom(player.position(), e, 1.5, 0.6);
                        }
                    }
                    for (int r = 2; r <= 12; r += 2) {
                        ring(player, player.position().add(0, 1, 0), r, ParticleTypes.FLAME);
                    }
                    sound(player, SoundEvents.ENDER_DRAGON_GROWL, 1, 1.5f);
                    return true;
                }));

        // Tell every other player your health and where you are, and see theirs.
        reg("mind_link", b().active(10, player -> {
            String me = player.getName().getString() + " (" + (int) player.getHealth() + " health) is at "
                    + player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ();
            int linked = 0;
            for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
                if (other == player) {
                    continue;
                }
                chat(other, "[Mind link] " + me);
                String where = other.level() == player.level() ? direction(player, other.position())
                        : "in another dimension";
                chat(player, "[Mind link] " + other.getName().getString() + ": " + (int) other.getHealth()
                        + " health, " + where);
                linked++;
            }
            if (linked == 0) {
                tell(player, "Nobody else is online");
                return false;
            }
            return true;
        }));

        // Put the mob you look at into a deep sleep for 10 seconds.
        reg("hypnosis", b().active(12, player -> {
            LivingEntity target = target(player, 16);
            if (!(target instanceof Mob mob) || mob.isNoAi()) {
                return false;
            }
            mob.setNoAi(true);
            mob.setTarget(null);
            Scheduler.repeat(20, 10, () -> {
                if (mob.isAlive()) {
                    level(player).sendParticles(ParticleTypes.NOTE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3,
                            mob.getZ(), 1, 0.2, 0.1, 0.2, 0);
                }
            });
            Scheduler.later(200, () -> mob.setNoAi(false));
            sound(player, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1, 1);
            tell(player, mob.getName().getString() + " is asleep");
            return true;
        }));

        // R: float with your mind (or land). You drift where you look; sneak to hover in place.
        reg("telekinetic_flight", b()
                .tick(player -> {
                    if (!state(player).flag("tk_fly")) {
                        return;
                    }
                    if (player.onGround() && player.getLookAngle().y < -0.3) {
                        return;
                    }
                    Vec3 v = player.isShiftKeyDown() ? Vec3.ZERO : player.getLookAngle().scale(0.45);
                    setVelocity(player, v);
                    player.resetFallDistance();
                    if (player.tickCount % 6 == 0) {
                        particles(player, player.position(), PSY, 2, 0.2);
                    }
                })
                .active(1, player -> {
                    tell(player, state(player).toggle("tk_fly") ? "Floating. Look where you want to go"
                            : "Landing");
                    return true;
                }));

        // A psychic barrier for 6 seconds that pushes every mob and projectile away from you.
        reg("telekinetic_barrier", b()
                .tick(player -> {
                    if (!state(player).active("barrier", now(player))) {
                        return;
                    }
                    for (Entity e : entitiesAround(player, 4)) {
                        if (e instanceof Projectile || e instanceof LivingEntity living && !isFriend(player, living)) {
                            pushFrom(player.position(), e, 0.8, 0.2);
                        }
                    }
                    if (player.tickCount % 5 == 0) {
                        ring(player, player.position().add(0, 1, 0), 3.5, PSY);
                    }
                })
                .active(15, player -> {
                    state(player).start("barrier", now(player), 120);
                    sound(player, SoundEvents.BEACON_ACTIVATE, 1, 1.2f);
                    return true;
                }));

        // Take over the mob you look at for 20 seconds: see through its eyes and steer it by looking.
        // Press R to make it attack what you look at, sneak to leave it.
        reg("possession", b()
                .tick(player -> {
                    PowerState st = state(player);
                    Mob host = st.get("host");
                    if (host == null) {
                        return;
                    }
                    if (!host.isAlive() || player.isShiftKeyDown() || !st.active("possess", now(player))) {
                        endPossession(player);
                        return;
                    }
                    Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
                    host.setYRot(player.getYRot());
                    host.setYHeadRot(player.getYRot());
                    Vec3 to = host.position().add(look.scale(3));
                    host.getNavigation().moveTo(to.x, to.y, to.z, 1.2);
                    if (player.distanceToSqr(host) > 4) {
                        player.teleportTo(host.getX(), host.getY(), host.getZ());
                    }
                })
                .onHurt((player, event) -> {
                    if (state(player).get("host") != null) {
                        event.setCanceled(true);
                    }
                })
                .active(1, player -> {
                    PowerState st = state(player);
                    Mob host = st.get("host");
                    if (host != null) {
                        LivingEntity victim = null;
                        for (LivingEntity e : livingNear(player, host.position(), 6)) {
                            if (e != host && !isFriend(player, e)) {
                                victim = e;
                                break;
                            }
                        }
                        if (victim != null) {
                            host.doHurtTarget(victim);
                            host.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                        }
                        return false;
                    }
                    if (st.active("possess_cd", now(player))) {
                        tell(player, "Ready in " + st.remaining("possess_cd", now(player)) / 20 + "s");
                        return false;
                    }
                    LivingEntity target = target(player, 16);
                    if (!(target instanceof Mob mob)) {
                        return false;
                    }
                    st.put("host", mob);
                    st.put("host_origin", player.position());
                    st.start("possess", now(player), 400);
                    st.start("possess_cd", now(player), 1200);
                    st.onClear(() -> endPossession(player));
                    mob.setNoAi(false);
                    mob.setTarget(null);
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 420, 0, false, false));
                    player.setCamera(mob);
                    tell(player, "Possessing " + mob.getName().getString() + ". R: attack, sneak: leave");
                    sound(player, SoundEvents.SOUL_ESCAPE, 1, 1);
                    return false;
                }));

        // Throw a glowing psychic spear.
        PowerProjectiles.register("psychic_spear", (owner, projectile, hit) -> {
            if (owner != null && hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity target) {
                hurtMagic(owner, target, 10);
                particlesAround(owner, target, PINK, 15);
            }
        });
        reg("psychic_weapons", b().active(2, player -> {
            PowerProjectiles.shoot(player, "psychic_spear", new ItemStack(Items.AMETHYST_SHARD), 2.5f, 0, false);
            sound(player, SoundEvents.TRIDENT_THROW, 1, 1.5f);
            return true;
        }));

        // Every hostile mob near you joins your mind for 30 seconds: they fight for you and glow.
        reg("hive_mind", b().active(60, player -> {
            int joined = 0;
            for (LivingEntity e : livingAround(player, 16)) {
                if (e instanceof Mob mob && isEnemy(player, e)) {
                    Allies.control(player, mob, 30);
                    glow(mob, 30);
                    particlesAround(player, mob, PSY, 10);
                    joined++;
                }
            }
            if (joined == 0) {
                tell(player, "No minds to take");
                return false;
            }
            tell(player, joined + " mobs joined your hive mind for 30 seconds");
            sound(player, SoundEvents.EVOKER_PREPARE_SUMMON, 1, 0.6f);
            return true;
        }));

        // Mobs that are angry at you glow.
        reg("emotion_sense", b().every(20, player -> {
            for (LivingEntity e : livingAround(player, 32)) {
                if (e instanceof Mob mob && mob.getTarget() == player) {
                    glow(mob, 1.5);
                }
            }
        }));
    }
}
