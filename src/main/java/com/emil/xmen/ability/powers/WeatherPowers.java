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
import net.minecraft.world.entity.projectile.windcharge.WindCharge;

/** Weather & Air. */
public final class WeatherPowers {
    private WeatherPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private static Vec3 flatLook(ServerPlayer player) {
        Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
        return look.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : look.normalize();
    }

    private static boolean isLightning(net.minecraft.world.damagesource.DamageSource source) {
        return source.is(DamageTypeTags.IS_LIGHTNING);
    }

    public static void register() {
        // Lightning wherever you look. Sneak: cycle the weather between rain, thunder and clear.
        reg("weather_control", b()
                .immuneTo((player, source) -> isLightning(source))
                .active(1, player -> {
                    ServerLevel level = level(player);
                    if (player.isShiftKeyDown()) {
                        String now;
                        if (level.isThundering()) {
                            level.setWeatherParameters(12000, 0, false, false);
                            now = "Clear skies";
                        } else if (level.isRaining()) {
                            level.setWeatherParameters(0, 12000, true, true);
                            now = "Thunderstorm";
                        } else {
                            level.setWeatherParameters(0, 12000, true, false);
                            now = "Rain";
                        }
                        tell(player, "Weather: " + now);
                        return true;
                    }
                    lightning(player, lookTarget(player, 64));
                    return true;
                }));

        // Lightning finds the three closest monsters around you by itself.
        reg("lightning_strike", b()
                .immuneTo((player, source) -> isLightning(source))
                .active(6, player -> {
                    List<LivingEntity> enemies = enemiesAround(player, 24);
                    enemies.sort((a, c) -> Double.compare(a.distanceToSqr(player), c.distanceToSqr(player)));
                    if (enemies.isEmpty()) {
                        tell(player, "No monsters nearby to strike");
                        return false;
                    }
                    for (int i = 0; i < Math.min(3, enemies.size()); i++) {
                        LivingEntity target = enemies.get(i);
                        Scheduler.later(i * 6 + 1, () -> lightning(player, target.position()));
                    }
                    return true;
                }));

        // Throws a wind charge: it bursts where it lands and blows everything away.
        reg("wind_gust", b().active(1.5, player -> {
            Vec3 eye = player.getEyePosition();
            WindCharge charge = new WindCharge(player, player.level(), eye.x, eye.y - 0.1, eye.z);
            charge.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f, 0);
            player.level().addFreshEntity(charge);
            sound(player, SoundEvents.BREEZE_SHOOT, 1, 1);
            return true;
        }));

        // R: ride the wind for 10 seconds, carried wherever you look. You drift down gently afterwards.
        reg("wind_flight", b()
                .tick(player -> {
                    if (state(player).active("wind_ride", now(player))) {
                        setVelocity(player, player.getLookAngle().scale(0.9));
                        player.resetFallDistance();
                        if (player.tickCount % 3 == 0) {
                            particles(player, player.position(), ParticleTypes.CLOUD, 2, 0.3);
                        }
                    }
                })
                .active(14, player -> {
                    state(player).start("wind_ride", now(player), 200);
                    player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0));
                    sound(player, SoundEvents.BREEZE_IDLE_AIR, 1, 1);
                    return true;
                }));

        // A tornado moves forward from you, lifting and spinning everything it touches.
        reg("tornado", b().active(20, player -> {
            Vec3 look = flatLook(player);
            Zones.Zone zone = Zones.add(player, player.position().add(look.scale(3)), 3.5, 8, 2, ParticleTypes.CLOUD,
                    (z, inside) -> {
                        for (LivingEntity e : inside) {
                            if (isFriend(player, e)) {
                                continue;
                            }
                            Vec3 to = z.center.subtract(e.position());
                            Vec3 spin = new Vec3(-to.z, 0, to.x).normalize().scale(0.5);
                            setVelocity(e, spin.add(to.x * 0.1, 0.45, to.z * 0.1));
                            e.resetFallDistance();
                            if (z.age % 20 == 0) {
                                hurt(player, e, 2);
                            }
                        }
                        for (int h = 0; h < 8; h++) {
                            double a = z.age * 0.5 + h;
                            double r = 0.5 + h * 0.4;
                            level(player).sendParticles(ParticleTypes.CLOUD, z.center.x + Math.cos(a) * r,
                                    z.center.y + h, z.center.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                        }
                    });
            zone.velocity = look.scale(0.15);
            sound(player, SoundEvents.ELYTRA_FLYING, 2, 0.5f);
            return true;
        }));

        // Start rain. While it rains, the crops around you keep growing.
        reg("rain_maker", b()
                .every(40, player -> {
                    ServerLevel level = level(player);
                    if (level.isRaining() && level.canSeeSky(player.blockPosition())) {
                        BlockPos c = player.blockPosition();
                        for (int i = 0; i < 6; i++) {
                            BlockPos p = c.offset(player.getRandom().nextInt(17) - 8, player.getRandom().nextInt(5) - 2,
                                    player.getRandom().nextInt(17) - 8);
                            if (level.getBlockState(p).is(BlockTags.CROPS)) {
                                grow(level, p);
                            }
                        }
                    }
                })
                .active(30, player -> {
                    ServerLevel level = level(player);
                    if (level.isRaining()) {
                        tell(player, "It's already raining");
                        return false;
                    }
                    level.setWeatherParameters(0, 6000, true, false);
                    tell(player, "Rain is coming");
                    return true;
                }));

        // Stop rain and thunder. If it's daytime, undead nearby burn in the sudden sunlight.
        reg("clear_skies", b().active(30, player -> {
            ServerLevel level = level(player);
            level.setWeatherParameters(12000, 0, false, false);
            int burned = 0;
            if (level.isDay()) {
                for (LivingEntity e : livingAround(player, 32)) {
                    if (e.isInvertedHealAndHarm() && level.canSeeSky(e.blockPosition())) {
                        e.igniteForSeconds(8);
                        burned++;
                    }
                }
            }
            tell(player, "The sky clears" + (burned > 0 ? " and " + burned + " undead burn" : ""));
            sound(player, SoundEvents.BEACON_ACTIVATE, 1, 1.5f);
            return true;
        }));

        // A bubble of air for 20 seconds: you and friends inside can breathe underwater.
        reg("air_bubble", b().active(30, player -> {
            Zones.following(player, 4, 20, 10, ParticleTypes.BUBBLE, (zone, inside) -> {
                ServerPlayer owner = zone.ownerPlayer();
                if (owner != null) {
                    owner.setAirSupply(owner.getMaxAirSupply());
                }
                for (LivingEntity e : inside) {
                    if (owner != null && isFriend(owner, e)) {
                        e.setAirSupply(e.getMaxAirSupply());
                    }
                }
            });
            sound(player, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1, 1);
            return true;
        }));

        // Lightning can't hurt you.
        reg("lightning_proof", b().immuneTo((player, source) -> isLightning(source)));

        // Spin like a cyclone for 4 seconds: arrows bounce back and mobs next to you are flung away.
        reg("cyclone_spin", b()
                .stopsProjectiles((player, projectile) -> {
                    if (!state(player).active("spin", now(player))) {
                        return false;
                    }
                    setVelocity(projectile, projectile.getDeltaMovement().scale(-1));
                    projectile.setOwner(player);
                    return true;
                })
                .every(4, player -> {
                    if (state(player).active("spin", now(player))) {
                        player.setYRot(player.getYRot() + 90);
                        ring(player, player.position().add(0, 1, 0), 1.5, ParticleTypes.SWEEP_ATTACK);
                        for (LivingEntity e : livingAround(player, 2.5)) {
                            if (!isFriend(player, e)) {
                                hurt(player, e, 2);
                                pushFrom(player.position(), e, 1.2, 0.4);
                            }
                        }
                    }
                })
                .active(10, player -> {
                    state(player).start("spin", now(player), 80);
                    sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1, 0.5f);
                    return true;
                }));

        // Take the air away from the mobs in front of you: they choke for 5 seconds.
        reg("vacuum", b().active(12, player -> {
            List<LivingEntity> targets = cone(player, 12, 35);
            targets.removeIf(e -> isFriend(player, e));
            if (targets.isEmpty()) {
                tell(player, "No mobs in front of you");
                return false;
            }
            for (LivingEntity e : targets) {
                effect(e, MobEffects.MOVEMENT_SLOWDOWN, 5, 2);
                effect(e, MobEffects.WEAKNESS, 5, 1);
            }
            Scheduler.repeat(20, 5, () -> {
                for (LivingEntity e : targets) {
                    if (e.isAlive()) {
                        hurt(player, e, 3, player.damageSources().drown());
                        particlesAround(player, e, ParticleTypes.BUBBLE_POP, 6);
                    }
                }
            });
            sound(player, SoundEvents.BREEZE_INHALE, 1, 0.6f);
            return true;
        }));

        // A gust throws you straight up, then you float down.
        reg("wind_jump", b().active(4, player -> {
            setVelocity(player, new Vec3(player.getDeltaMovement().x, 1.6, player.getDeltaMovement().z));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
            particles(player, player.position(), ParticleTypes.GUST, 3, 0.3);
            sound(player, SoundEvents.BREEZE_JUMP, 1, 1);
            return true;
        }));

        // Rain makes you faster and stronger, thunderstorms even more.
        reg("storm_power", b().every(20, player -> {
            ServerLevel level = level(player);
            if (level.isThundering()) {
                buff(player, MobEffects.MOVEMENT_SPEED, 1);
                buff(player, MobEffects.DAMAGE_BOOST, 1);
            } else if (level.isRaining()) {
                buff(player, MobEffects.MOVEMENT_SPEED, 0);
                buff(player, MobEffects.DAMAGE_BOOST, 0);
            }
        }));

        // You float down like a feather instead of falling.
        reg("feather_fall", b()
                .attribute(Attributes.FALL_DAMAGE_MULTIPLIER, -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .tick(player -> {
                    if (!player.onGround() && !player.isInWater() && player.fallDistance > 3
                            && player.getDeltaMovement().y < -0.15) {
                        setVelocity(player, new Vec3(player.getDeltaMovement().x, -0.15, player.getDeltaMovement().z));
                    }
                }));

        // Hail rains down on the spot you look at for 6 seconds.
        reg("hailstorm", b().active(14, player -> {
            Vec3 target = lookTarget(player, 40);
            Zones.add(player, target.add(0, 1, 0), 5, 6, 10, null, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (!isFriend(player, e)) {
                        hurt(player, e, 2);
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 1, 1);
                    }
                }
                level(player).sendParticles(ParticleTypes.ITEM_SNOWBALL, zone.center.x, zone.center.y + 5,
                        zone.center.z, 40, 4, 1, 4, 0.0);
                level(player).sendParticles(ParticleTypes.SNOWFLAKE, zone.center.x, zone.center.y + 3, zone.center.z,
                        20, 4, 2, 4, 0.0);
            });
            soundAt(player, target, SoundEvents.GLASS_BREAK, 1, 1.5f);
            return true;
        }));

        // Crush a mob into the ground. Sneak: make it float away instead.
        reg("air_pressure", b().active(4, player -> {
            LivingEntity target = target(player, 32);
            if (target == null) {
                return false;
            }
            if (player.isShiftKeyDown()) {
                effect(target, MobEffects.LEVITATION, 4, 1);
                particlesAround(player, target, ParticleTypes.CLOUD, 15);
            } else {
                setVelocity(target, new Vec3(0, -2.5, 0));
                hurt(player, target, 6);
                effect(target, MobEffects.MOVEMENT_SLOWDOWN, 3, 3);
                particles(player, target.position(), ParticleTypes.EXPLOSION, 1, 0);
            }
            sound(player, SoundEvents.BREEZE_DEFLECT, 1, 0.6f);
            return true;
        }));

        // A thunderous clap stuns every mob around you.
        reg("thunderclap", b().active(10, player -> {
            for (LivingEntity e : livingAround(player, 7)) {
                if (!isFriend(player, e)) {
                    hurt(player, e, 2);
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, 3, 5);
                    effect(e, MobEffects.WEAKNESS, 3, 2);
                    if (e instanceof Mob mob) {
                        mob.getNavigation().stop();
                    }
                }
            }
            ring(player, player.position().add(0, 1, 0), 4, ParticleTypes.SONIC_BOOM);
            sound(player, SoundEvents.LIGHTNING_BOLT_THUNDER, 1, 1.6f);
            return true;
        }));

        // A wall of wind in front of you for 10 seconds that stops arrows and fireballs.
        reg("wind_wall", b().active(15, player -> {
            Vec3 look = flatLook(player);
            Vec3 side = new Vec3(-look.z, 0, look.x);
            Vec3 center = player.position().add(look.scale(3)).add(0, 1, 0);
            for (int i = -3; i <= 3; i++) {
                Zones.add(player, center.add(side.scale(i)), 1.5, 10, 1, null, (zone, inside) -> {
                    for (Entity e : zone.entities()) {
                        if (e instanceof Projectile p && p.getOwner() != zone.ownerPlayer()) {
                            p.discard();
                            level(player).sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 4, 0.1, 0.1,
                                    0.1, 0.02);
                        }
                    }
                    if (zone.age % 4 == 0) {
                        level(player).sendParticles(ParticleTypes.SMALL_GUST, zone.center.x, zone.center.y,
                                zone.center.z, 1, 0.3, 0.8, 0.3, 0);
                    }
                });
            }
            sound(player, SoundEvents.BREEZE_WIND_CHARGE_BURST, 1, 0.6f);
            return true;
        }));

        // Shoot forward extremely fast in a straight line for 3 seconds, ramming through mobs.
        reg("jet_stream", b()
                .tick(player -> {
                    if (state(player).active("jet", now(player))) {
                        Vec3 dir = state(player).get("jet_dir");
                        if (dir != null) {
                            setVelocity(player, dir.scale(2.2));
                            player.resetFallDistance();
                            particles(player, player.position(), ParticleTypes.CLOUD, 3, 0.2);
                            for (LivingEntity e : livingAround(player, 2)) {
                                if (!isFriend(player, e)) {
                                    hurt(player, e, 6);
                                    pushFrom(player.position(), e, 1.5, 0.5);
                                }
                            }
                        }
                    }
                })
                .active(12, player -> {
                    state(player).put("jet_dir", player.getLookAngle());
                    state(player).start("jet", now(player), 60);
                    sound(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1, 0.5f);
                    return true;
                }));

        // Mobs right next to you get zapped.
        reg("static_field", b().every(20, player -> {
            for (LivingEntity e : livingAround(player, 1.8)) {
                if (!isFriend(player, e)) {
                    hurt(player, e, 2);
                    particlesAround(player, e, ParticleTypes.ELECTRIC_SPARK, 8);
                }
            }
        }));
    }
}
