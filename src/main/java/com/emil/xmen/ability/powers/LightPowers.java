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


/** Light, Shadow & Stealth. */
public final class LightPowers {
    private static final List<Holder<MobEffect>> RAINBOW = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.WEAKNESS,
            MobEffects.POISON, MobEffects.LEVITATION, MobEffects.GLOWING, MobEffects.CONFUSION, MobEffects.BLINDNESS,
            MobEffects.WITHER);
    private static final float[][] COLORS = { { 1, 0, 0 }, { 1, 0.5f, 0 }, { 1, 1, 0 }, { 0, 1, 0 }, { 0, 0.6f, 1 },
            { 0.3f, 0, 1 }, { 0.8f, 0, 1 } };
    private static final DustParticleOptions LASER = new DustParticleOptions(new Vector3f(0.2f, 1.0f, 0.3f), 0.6f);
    private static final DustParticleOptions SHADOW = new DustParticleOptions(new Vector3f(0.05f, 0.0f, 0.1f), 2.0f);

    private LightPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private static void loseAll(ServerPlayer player, double radius) {
        for (LivingEntity e : livingAround(player, radius)) {
            forget(player, e);
        }
    }

    public static void register() {
        // R: turn invisible (or visible again). It lasts until you attack something, but it makes you hungry.
        reg("invisibility", b()
                .every(10, player -> {
                    if (state(player).flag("invisible")) {
                        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
                        player.causeFoodExhaustion(0.15f);
                    }
                })
                .onAttack((player, target, event) -> {
                    if (state(player).flag("invisible")) {
                        state(player).setFlag("invisible", false);
                        player.removeEffect(MobEffects.INVISIBILITY);
                        tell(player, "You attacked and became visible");
                    }
                })
                .active(1, player -> {
                    boolean on = state(player).toggle("invisible");
                    if (on) {
                        for (LivingEntity e : livingAround(player, 32)) {
                            forget(player, e);
                        }
                    } else {
                        player.removeEffect(MobEffects.INVISIBILITY);
                    }
                    particles(player, player.position().add(0, 1, 0), ParticleTypes.CLOUD, 20, 0.4);
                    sound(player, SoundEvents.ILLUSIONER_CAST_SPELL, 1, on ? 1.2f : 0.8f);
                    tell(player, on ? "Invisible until you attack" : "Visible again");
                    return true;
                }));

        // A dazzling light show where you look, for 8 seconds: mobs inside are blinded, dizzy, and attack each other.
        reg("light_bending", b().active(20, player -> {
            Vec3 at = lookTarget(player, 32);
            Zones.add(player, at.add(0, 1, 0), 5, 8, 20, null, (zone, inside) -> {
                List<Mob> mobs = new ArrayList<>();
                for (LivingEntity e : inside) {
                    if (e instanceof Mob mob && !isFriend(player, e)) {
                        effect(mob, MobEffects.BLINDNESS, 2, 0);
                        effect(mob, MobEffects.CONFUSION, 3, 0);
                        mobs.add(mob);
                    }
                }
                if (mobs.size() > 1) {
                    for (int i = 0; i < mobs.size(); i++) {
                        mobs.get(i).setTarget(mobs.get((i + 1) % mobs.size()));
                    }
                }
                for (float[] c : COLORS) {
                    level(player).sendParticles(new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 1.5f),
                            zone.center.x, zone.center.y, zone.center.z, 6, 3, 1.5, 3, 0);
                }
            });
            soundAt(player, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 1, 2);
            return true;
        }));

        // Darkness fills the area around you for 12 seconds. Mobs inside are blind; you see perfectly.
        reg("darkness_control", b().active(25, player -> {
            Zones.add(player, player.position().add(0, 1, 0), 8, 12, 10, ParticleTypes.SQUID_INK, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (!isFriend(player, e)) {
                        effect(e, MobEffects.DARKNESS, 2, 0);
                        effect(e, MobEffects.BLINDNESS, 2, 0);
                        if (e instanceof Mob mob && mob.getTarget() == zone.ownerPlayer()) {
                            mob.setTarget(null);
                        }
                    }
                }
                ServerPlayer owner = zone.ownerPlayer();
                if (owner != null && owner.position().distanceTo(zone.center) < zone.radius) {
                    owner.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
                }
            });
            sound(player, SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.5f, 0.6f);
            return true;
        }));

        // Step into one shadow and out of another: teleport to a dark spot you look at.
        reg("shadow_walk", b().active(3, player -> {
            BlockPos spot = teleportSpot(player, 64);
            if (spot == null) {
                tell(player, "No room there");
                return false;
            }
            int light = level(player).getMaxLocalRawBrightness(spot);
            if (light > 7 || light(player) > 7) {
                tell(player, "You need darkness here and there (light " + light + ")");
                return false;
            }
            particles(player, player.position().add(0, 1, 0), SHADOW, 30, 0.4);
            teleport(player, Vec3.atBottomCenterOf(spot));
            particles(player, player.position().add(0, 1, 0), SHADOW, 30, 0.4);
            sound(player, SoundEvents.SCULK_CLICKING, 1, 0.6f);
            return true;
        }));

        // A blinding flash: every mob around you can't see for 5 seconds.
        reg("flash", b().active(8, player -> {
            int blinded = 0;
            for (LivingEntity e : livingAround(player, 10)) {
                if (!isFriend(player, e)) {
                    effect(e, MobEffects.BLINDNESS, 5, 0);
                    if (e instanceof Mob mob) {
                        mob.setTarget(null);
                    }
                    blinded++;
                }
            }
            particles(player, player.getEyePosition(), ParticleTypes.END_ROD, 60, 1.5);
            sound(player, SoundEvents.FIREWORK_ROCKET_BLAST, 1, 2);
            return blinded > 0 || true;
        }));

        // R: glow like a torch (or stop). The light moves with you.
        reg("glow", b()
                .every(5, player -> {
                    if (state(player).flag("glowing")) {
                        BlockPos head = BlockPos.containing(player.getEyePosition());
                        if (!TempBlocks.place(level(player), head, Blocks.LIGHT.defaultBlockState(), 12)) {
                            TempBlocks.place(level(player), player.blockPosition(), Blocks.LIGHT.defaultBlockState(), 12);
                        }
                    }
                })
                .active(1, player -> {
                    tell(player, state(player).toggle("glowing") ? "You glow" : "Light off");
                    return true;
                }));

        // Stand still for a moment and you blend into the background: invisible, and mobs lose you.
        reg("camouflage", b().every(10, player -> {
            PowerState st = state(player);
            if (isStill(player)) {
                double still = st.add("still", 10);
                if (still >= 40) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 15, 0, false, false, true));
                    if (still == 40) {
                        loseAll(player, 24);
                    }
                }
            } else {
                st.put("still", 0.0);
            }
        }));

        // You make no sound: sculk sensors, shriekers and wardens never notice you.
        reg("silent_steps", b().silent()
                .ignoredBy((player, mob) -> mob instanceof net.minecraft.world.entity.monster.warden.Warden));

        // Shadow copies of you rise behind the nearest monsters and fight, but only in the dark: light makes
        // them fade. R again while they're out swaps your place with one of them.
        reg("shadow_clones", b()
                .every(20, player -> {
                    for (PowerClone clone : level(player).getEntitiesOfClass(PowerClone.class,
                            player.getBoundingBox().inflate(48), c -> player.getUUID().equals(c.getOwnerId())
                                    && c.getVariant() == PowerClone.SHADOW)) {
                        if (light(clone) > 7) {
                            particlesAround(player, clone, ParticleTypes.SMOKE, 15);
                            clone.discard();
                        }
                    }
                })
                .active(2, player -> {
                    List<PowerClone> mine = level(player).getEntitiesOfClass(PowerClone.class,
                            player.getBoundingBox().inflate(48), c -> player.getUUID().equals(c.getOwnerId())
                                    && c.getVariant() == PowerClone.SHADOW);
                    if (!mine.isEmpty()) {
                        PowerClone other = mine.get(player.getRandom().nextInt(mine.size()));
                        Vec3 me = player.position();
                        teleport(player, other.position());
                        other.teleportTo(me.x, me.y, me.z);
                        particlesAround(player, player, SHADOW, 15);
                        return false;
                    }
                    if (state(player).active("shadow_rest", now(player))) {
                        tell(player, "Ready in " + state(player).remaining("shadow_rest", now(player)) / 20 + "s");
                        return false;
                    }
                    if (light(player) > 7) {
                        tell(player, "Too bright. Shadow clones need darkness");
                        return false;
                    }
                    List<LivingEntity> enemies = enemiesAround(player, 20);
                    enemies.sort((a, c) -> Double.compare(a.distanceToSqr(player), c.distanceToSqr(player)));
                    for (int i = 0; i < 3; i++) {
                        LivingEntity target = i < enemies.size() ? enemies.get(i) : null;
                        Vec3 at = target != null
                                ? target.position().subtract(target.getLookAngle().multiply(1.5, 0, 1.5))
                                : player.position().add(player.getRandom().triangle(0, 2), 0, player.getRandom().triangle(0, 2));
                        PowerClone clone = spawnClone(player, at, PowerClone.SHADOW, 30);
                        if (clone != null) {
                            if (target != null) {
                                clone.setTarget(target);
                            }
                            particlesAround(player, clone, SHADOW, 15);
                        }
                    }
                    state(player).start("shadow_rest", now(player), 600);
                    sound(player, SoundEvents.WITHER_AMBIENT, 0.4f, 1.6f);
                    return false;
                }));

        // A glowing image of you stands where you look for 15 seconds. Mobs go after it instead of you.
        reg("hologram", b().active(20, player -> {
            BlockPos spot = teleportSpot(player, 24);
            Vec3 at = spot == null ? player.position().add(player.getLookAngle().scale(3)) : Vec3.atBottomCenterOf(spot);
            PowerClone image = spawnClone(player, at, PowerClone.HOLOGRAM, 15);
            if (image == null) {
                return false;
            }
            for (LivingEntity e : livingAround(player, 24)) {
                if (e instanceof Mob mob && (mob.getTarget() == player || isEnemy(player, mob))) {
                    mob.setTarget(image);
                }
            }
            sound(player, SoundEvents.BEACON_POWER_SELECT, 1, 1.8f);
            return true;
        }));

        // A pool of living shadow where you look: mobs are dragged in, blinded and hurt for 6 seconds.
        reg("darkforce", b().active(20, player -> {
            Vec3 at = lookTarget(player, 32);
            Zones.add(player, at.add(0, 0.5, 0), 4, 6, 5, SHADOW, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (!isFriend(player, e)) {
                        pull(zone.center, e, 0.15);
                        effect(e, MobEffects.BLINDNESS, 1, 0);
                        if (zone.age % 20 == 0) {
                            hurtMagic(player, e, 3);
                            effect(e, MobEffects.WITHER, 2, 0);
                        }
                    }
                }
            });
            soundAt(player, at, SoundEvents.SCULK_SHRIEKER_SHRIEK, 1, 0.4f);
            return true;
        }));

        // A thin, precise laser: hits only the first mob, from very far away, very hard.
        reg("laser", b().active(1.5, player -> {
            LivingEntity target = lookedAtLiving(player, 96);
            Vec3 end = target != null ? target.getBoundingBox().getCenter() : lookTarget(player, 96);
            particleLine(player, player.getEyePosition().add(player.getLookAngle()), end, LASER, 0.25);
            if (target != null) {
                hurt(player, target, 9);
            }
            sound(player, SoundEvents.BEACON_DEACTIVATE, 0.6f, 2);
            return true;
        }));

        // Under the night sky you're tough and you heal.
        reg("starlight_power", b().every(20, player -> {
            if (isNightOutside(player)) {
                buff(player, MobEffects.DAMAGE_RESISTANCE, 0);
                buff(player, MobEffects.REGENERATION, 0);
            }
        }));

        // Wrap yourself in shadow for 10 seconds: hostile mobs can't see you, and your first hit from the
        // shadows does triple damage (and ends the cloak).
        reg("shadow_cloak", b()
                .ignoredBy((player, mob) -> state(player).active("cloak", now(player)) && isEnemy(player, mob))
                .onAttack((player, target, event) -> {
                    if (state(player).active("cloak", now(player))) {
                        state(player).stop("cloak");
                        event.setAmount(event.getAmount() * 3);
                        particlesAround(player, target, SHADOW, 15);
                        tell(player, "Strike from the shadows!");
                    }
                })
                .every(5, player -> {
                    if (state(player).active("cloak", now(player))) {
                        particlesAround(player, player, SHADOW, 2);
                    }
                })
                .active(20, player -> {
                    state(player).start("cloak", now(player), 200);
                    loseAll(player, 32);
                    particlesAround(player, player, SHADOW, 20);
                    sound(player, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1, 0.8f);
                    return true;
                }));

        // A rainbow beam: every mob it touches gets a different random effect.
        reg("rainbow_beam", b().active(3, player -> {
            Vec3 start = player.getEyePosition().add(player.getLookAngle());
            Vec3 end = lookTarget(player, 32);
            for (int i = 0; i < COLORS.length; i++) {
                float[] c = COLORS[i];
                Vec3 offset = new Vec3(0, (i - 3) * 0.08, 0);
                particleLine(player, start.add(offset), end.add(offset),
                        new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), 0.8f), 0.4);
            }
            for (LivingEntity e : beam(player, 32, 0.6)) {
                hurt(player, e, 3);
                effect(e, RAINBOW.get(player.getRandom().nextInt(RAINBOW.size())), 6, 1);
            }
            sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1, 1.2f);
            return true;
        }));

        // Turn into light: zip up to 64 blocks in a straight line, burning through every mob on the way.
        reg("light_speed", b().active(6, player -> {
            Vec3 start = player.position();
            Vec3 look = player.getLookAngle();
            Vec3 end = start;
            for (double d = 1; d <= 64; d += 0.5) {
                Vec3 at = start.add(look.scale(d));
                if (!hasRoom(player, BlockPos.containing(at))) {
                    break;
                }
                end = at;
            }
            if (end.distanceTo(start) < 2) {
                tell(player, "No room to move");
                return false;
            }
            for (LivingEntity e : beam(player, end.distanceTo(start) + 1, 0.8)) {
                hurt(player, e, 6);
                e.igniteForSeconds(2);
            }
            particleLine(player, start.add(0, 1, 0), end.add(0, 1, 0), ParticleTypes.END_ROD, 0.4);
            teleport(player, end);
            sound(player, SoundEvents.BEACON_POWER_SELECT, 1, 2);
            return true;
        }));

        // Touch a mob to blind it for 8 seconds. It forgets where you are.
        reg("blinding_touch", b().active(4, player -> {
            LivingEntity target = target(player, 4);
            if (target == null) {
                return false;
            }
            effect(target, MobEffects.BLINDNESS, 8, 0);
            effect(target, MobEffects.DARKNESS, 8, 0);
            forget(player, target);
            particlesAround(player, target, SHADOW, 10);
            sound(player, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1, 1);
            return true;
        }));

        // Turn day into night, or night into day.
        reg("eclipse", b().active(60, player -> {
            ServerLevel level = player.server.overworld();
            long time = level.getDayTime();
            long dayStart = time - time % 24000L;
            boolean night = time % 24000L >= 13000 && time % 24000L < 23000;
            level.setDayTime(night ? dayStart + 24000L + 1000 : dayStart + 14000);
            for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                chat(p, player.getName().getString() + (night ? " brought back the sun" : " covered the sun"));
            }
            sound(player, SoundEvents.END_PORTAL_SPAWN, 0.5f, 0.5f);
            return true;
        }));

        // You feed on light: bright light slowly heals you. R swallows the torches and lanterns around you,
        // healing you more for each one and leaving the area dark.
        reg("light_eater", b()
                .every(40, player -> {
                    if (light(player) >= 13) {
                        player.heal(1);
                        particles(player, player.position().add(0, 2.1, 0), ParticleTypes.WAX_ON, 2, 0.2);
                    }
                })
                .active(20, player -> {
                    ServerLevel level = level(player);
                    BlockPos c = player.blockPosition();
                    int eaten = 0;
                    for (BlockPos pos : BlockPos.betweenClosed(c.offset(-8, -4, -8), c.offset(8, 4, 8))) {
                        BlockState s = level.getBlockState(pos);
                        if (s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.SOUL_TORCH)
                                || s.is(Blocks.SOUL_WALL_TORCH) || s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN)) {
                            particleLine(player, Vec3.atCenterOf(pos), player.getBoundingBox().getCenter(),
                                    ParticleTypes.WAX_ON, 0.5);
                            TempBlocks.replace(level, pos.immutable(), Blocks.AIR.defaultBlockState(), 600);
                            eaten++;
                        }
                    }
                    if (eaten == 0) {
                        tell(player, "No torches or lanterns nearby");
                        return false;
                    }
                    player.heal(eaten * 2);
                    sound(player, SoundEvents.GENERIC_EAT, 1, 0.6f);
                    tell(player, "You ate " + eaten + " lights");
                    return true;
                }));

        // Mobs don't notice you at all until you attack them.
        reg("unnoticed", b().ignoredBy((player, mob) -> {
            if (mob.getLastHurtByMob() == player) {
                return false;
            }
            Object last = state(player).get("last_attack");
            return !(last instanceof Long time && now(player) - time < 200);
        }));
    }
}
