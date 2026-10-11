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
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.BlockItem;

/** Fire & Heat. */
public final class FirePowers {
    private static final DustParticleOptions BLUE_FIRE = new DustParticleOptions(new Vector3f(0.2f, 0.6f, 1.0f), 1.3f);

    private FirePowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private static boolean isFire(net.minecraft.world.damagesource.DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE);
    }

    public static void register() {
        // Three fireballs. Sneak: put out all fire around you.
        reg("pyrokinesis", b().active(1.5, player -> {
            ServerLevel level = level(player);
            if (player.isShiftKeyDown()) {
                int out = 0;
                BlockPos c = player.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(c.offset(-6, -3, -6), c.offset(6, 3, 6))) {
                    if (level.getBlockState(pos).is(BlockTags.FIRE)) {
                        level.removeBlock(pos, false);
                        out++;
                    }
                }
                for (LivingEntity e : livingAround(player, 6)) {
                    if (isFriend(player, e)) {
                        e.clearFire();
                    }
                }
                player.clearFire();
                tell(player, out > 0 ? "Put out " + out + " fires" : "No fire nearby");
                sound(player, SoundEvents.FIRE_EXTINGUISH, 1, 1);
                return true;
            }
            Vec3 look = player.getLookAngle();
            for (int i = 0; i < 3; i++) {
                Vec3 movement = look.add(player.getRandom().triangle(0, 0.08), player.getRandom().triangle(0, 0.08),
                        player.getRandom().triangle(0, 0.08));
                SmallFireball fireball = new SmallFireball(level, player, movement);
                Vec3 at = player.getEyePosition().add(look.scale(1.2));
                fireball.setPos(at.x, at.y, at.z);
                level.addFreshEntity(fireball);
            }
            sound(player, SoundEvents.BLAZE_SHOOT, 1, 1);
            return true;
        }));

        // Fire and lava can't hurt you at all.
        reg("fireproof", b().effect(MobEffects.FIRE_RESISTANCE, 0));

        // R: become living fire for 8 seconds. Punches can't hurt you and everything close catches fire.
        reg("living_flame", b()
                .immuneTo((player, source) -> isFire(source))
                .onHurt((player, event) -> {
                    if (state(player).active("flame", now(player)) && !event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                            && event.getSource().getEntity() instanceof LivingEntity attacker
                            && event.getSource().getDirectEntity() == attacker) {
                        attacker.igniteForSeconds(5);
                        event.setCanceled(true);
                    }
                })
                .every(5, player -> {
                    if (state(player).active("flame", now(player))) {
                        particlesAround(player, player, ParticleTypes.FLAME, 12);
                        for (LivingEntity e : livingAround(player, 2.5)) {
                            if (!isFriend(player, e)) {
                                e.igniteForSeconds(4);
                                hurt(player, e, 1, player.damageSources().inFire());
                            }
                        }
                    }
                })
                .active(20, player -> {
                    state(player).start("flame", now(player), 160);
                    player.igniteForSeconds(0.1f);
                    sound(player, SoundEvents.BLAZE_AMBIENT, 1, 0.8f);
                    tell(player, "You are living flame for 8 seconds");
                    return true;
                }));

        // Snow and ice melt around you, and cold never freezes you.
        reg("body_heat", b()
                .immuneTo((player, source) -> source.is(DamageTypeTags.IS_FREEZING))
                .every(10, player -> {
                    player.setTicksFrozen(0);
                    ServerLevel level = level(player);
                    BlockPos c = player.blockPosition();
                    for (BlockPos pos : BlockPos.betweenClosed(c.offset(-2, -1, -2), c.offset(2, 2, 2))) {
                        BlockState s = level.getBlockState(pos);
                        if (s.is(Blocks.SNOW) || s.is(Blocks.POWDER_SNOW)) {
                            level.removeBlock(pos, false);
                        } else if (s.is(Blocks.SNOW_BLOCK)) {
                            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        } else if (s.is(Blocks.ICE) || s.is(Blocks.FROSTED_ICE)) {
                            level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                        }
                    }
                }));

        // Lava you walk over cools into basalt for a while.
        reg("lava_walker", b()
                .immuneTo((player, source) -> source.is(net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR))
                .every(2, player -> {
                    ServerLevel level = level(player);
                    BlockPos feet = player.blockPosition().below();
                    for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-2, 0, -2), feet.offset(2, 0, 2))) {
                        if (level.getBlockState(pos).is(Blocks.LAVA) && level.getFluidState(pos).isSource()
                                && level.getBlockState(pos.above()).isAir()) {
                            TempBlocks.replace(level, pos, Blocks.BASALT.defaultBlockState(), 200);
                        }
                    }
                }));

        // Breathe fire for three seconds.
        reg("fire_breath", b().active(6, player -> {
            Scheduler.repeat(3, 20, () -> {
                if (!player.isAlive()) {
                    return;
                }
                Vec3 look = player.getLookAngle();
                Vec3 mouth = player.getEyePosition().add(look.scale(0.6)).add(0, -0.2, 0);
                for (int i = 0; i < 6; i++) {
                    Vec3 v = look.add(player.getRandom().triangle(0, 0.15), player.getRandom().triangle(0, 0.15),
                            player.getRandom().triangle(0, 0.15)).scale(0.6);
                    level(player).sendParticles(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 0, v.x, v.y, v.z, 1);
                }
                for (LivingEntity e : cone(player, 7, 25)) {
                    e.igniteForSeconds(4);
                    hurt(player, e, 1.5f);
                }
            });
            sound(player, SoundEvents.BLAZE_SHOOT, 1, 0.6f);
            return true;
        }));

        // Vanish in a burst of smoke: mobs nearby are blinded, and you're invisible and fast for 6 seconds.
        reg("smoke_form", b().active(20, player -> {
            for (LivingEntity e : livingAround(player, 6)) {
                if (!isFriend(player, e)) {
                    effect(e, MobEffects.BLINDNESS, 4, 0);
                }
                forget(player, e);
            }
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0, false, false, true));
            effect(player, MobEffects.MOVEMENT_SPEED, 6, 1);
            particles(player, player.position().add(0, 1, 0), ParticleTypes.CAMPFIRE_COSY_SMOKE, 60, 1.2);
            particles(player, player.position().add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 40, 1.5);
            sound(player, SoundEvents.FIRE_EXTINGUISH, 1, 0.6f);
            return true;
        }));

        // Sprinting leaves little patches of embers that set mobs on fire.
        reg("ember_trail", b().every(4, player -> {
            if (player.isSprinting() && player.onGround()) {
                Zones.add(player, player.position().add(0, 0.3, 0), 0.8, 3, 10, ParticleTypes.SMALL_FLAME,
                        (zone, inside) -> {
                            for (LivingEntity e : inside) {
                                if (!isFriend(player, e)) {
                                    e.igniteForSeconds(3);
                                }
                            }
                        });
            }
        }));

        // R charges your fist: the next punch explodes.
        reg("blast_punch", b()
                .onAttack((player, target, event) -> {
                    if (state(player).flag("blast")) {
                        state(player).setFlag("blast", false);
                        event.setAmount(event.getAmount() + 6);
                        explode(player, target.position().add(0, 0.5, 0), 1.2f, false);
                    }
                })
                .active(4, player -> {
                    state(player).setFlag("blast", true);
                    particlesAround(player, player, ParticleTypes.FLAME, 10);
                    tell(player, "Fist charged! Punch something");
                    sound(player, SoundEvents.FIRECHARGE_USE, 1, 1.2f);
                    return true;
                }));

        // Mobs that hit you catch fire.
        reg("molten_skin", b().onHurt((player, event) -> {
            if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != player) {
                attacker.igniteForSeconds(4);
                particlesAround(player, attacker, ParticleTypes.LAVA, 4);
            }
        }));

        // Stare at a block to smelt it: sand into glass, ore into ingots, logs into charcoal.
        reg("heat_vision", b().active(1, player -> {
            ServerLevel level = level(player);
            BlockPos pos = lookedAtPos(player, 16);
            LivingEntity mob = lookedAtLiving(player, 16);
            if (mob != null) {
                mob.igniteForSeconds(5);
                particleLine(player, player.getEyePosition().add(player.getLookAngle()), mob.getBoundingBox().getCenter(),
                        ParticleTypes.SMALL_FLAME, 0.5);
                return true;
            }
            if (pos == null) {
                tell(player, "Look at a block");
                return false;
            }
            BlockState state = level.getBlockState(pos);
            ItemStack result = smeltResult(level, new ItemStack(state.getBlock().asItem()));
            if (result.isEmpty() || state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0) {
                tell(player, "That doesn't smelt");
                return false;
            }
            particleLine(player, player.getEyePosition().add(player.getLookAngle()), Vec3.atCenterOf(pos),
                    ParticleTypes.SMALL_FLAME, 0.5);
            if (result.getItem() instanceof BlockItem blockItem && result.getCount() == 1) {
                level.setBlockAndUpdate(pos, blockItem.getBlock().defaultBlockState());
            } else {
                level.removeBlock(pos, false);
                Block.popResource(level, pos, result);
            }
            particles(player, Vec3.atCenterOf(pos), ParticleTypes.FLAME, 10, 0.4);
            sound(player, SoundEvents.FURNACE_FIRE_CRACKLE, 1, 1);
            return true;
        }));

        // A wall of flames in front of you for 10 seconds that burns anything walking through.
        reg("wall_of_fire", b().active(15, player -> {
            Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
            if (look.lengthSqr() < 1.0E-4) {
                look = new Vec3(0, 0, 1);
            }
            look = look.normalize();
            Vec3 side = new Vec3(-look.z, 0, look.x);
            Vec3 center = player.position().add(look.scale(4));
            for (int i = -4; i <= 4; i++) {
                for (int h = 0; h < 3; h++) {
                    Vec3 at = center.add(side.scale(i)).add(0, h + 0.5, 0);
                    Zones.add(player, at, 0.9, 10, 5, h == 0 ? ParticleTypes.FLAME : null, (zone, inside) -> {
                        for (LivingEntity e : inside) {
                            if (!isFriend(player, e)) {
                                e.igniteForSeconds(5);
                                hurt(player, e, 2, player.damageSources().inFire());
                            }
                        }
                        level(player).sendParticles(ParticleTypes.FLAME, zone.center.x, zone.center.y, zone.center.z, 2,
                                0.3, 0.5, 0.3, 0.01);
                    });
                }
            }
            sound(player, SoundEvents.FIRECHARGE_USE, 1, 0.5f);
            return true;
        }));

        // Once a day, death turns into a burst of flames and you rise again.
        reg("phoenix_rebirth", b().onDeath((player, source) -> {
            if (!onceADay(player, "phoenix_rebirth")) {
                return false;
            }
            player.setHealth(player.getMaxHealth());
            player.removeAllEffects();
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            for (LivingEntity e : livingAround(player, 8)) {
                if (!isFriend(player, e)) {
                    hurt(player, e, 10, player.damageSources().inFire());
                    e.igniteForSeconds(8);
                    pushFrom(player.position(), e, 1.5, 0.6);
                }
            }
            ring(player, player.position().add(0, 0.5, 0), 3, ParticleTypes.FLAME);
            ring(player, player.position().add(0, 1.5, 0), 5, ParticleTypes.FLAME);
            particles(player, player.position().add(0, 1, 0), ParticleTypes.TOTEM_OF_UNDYING, 60, 0.6);
            sound(player, SoundEvents.TOTEM_USE, 1, 0.7f);
            chat(player, "You rose from the flames! (once per day)");
            return true;
        }));

        // Fire heals you instead of hurting (lava still hurts).
        reg("fire_healing", b()
                .immuneTo((player, source) -> source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE)
                        || source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE))
                .every(20, player -> {
                    if (player.isOnFire() || level(player).getBlockState(player.blockPosition()).is(BlockTags.FIRE)) {
                        player.heal(2);
                        particlesAround(player, player, ParticleTypes.HEART, 1);
                    }
                }));

        // A burning meteor falls from the sky where you look.
        reg("meteor_call", b().active(12, player -> {
            Vec3 target = lookTarget(player, 80);
            ServerLevel level = level(player);
            Vec3 from = target.add(player.getRandom().triangle(0, 4), 30, player.getRandom().triangle(0, 4));
            LargeFireball meteor = new LargeFireball(level, player, target.subtract(from).normalize(), 3);
            meteor.setPos(from.x, from.y, from.z);
            level.addFreshEntity(meteor);
            particles(player, target, ParticleTypes.LAVA, 20, 1);
            soundAt(player, target, SoundEvents.FIRECHARGE_USE, 2, 0.4f);
            return true;
        }));

        // Warm-blooded mobs close to you glow, even behind walls. Undead are cold and stay hidden.
        reg("heat_sense", b().every(20, player -> {
            for (LivingEntity e : livingAround(player, 10)) {
                if (!e.isInvertedHealAndHarm()) {
                    glow(e, 1.5);
                }
            }
        }));

        // Sunlight makes you strong and heals you.
        reg("sun_battery", b().every(20, player -> {
            if (inSunlight(player)) {
                buff(player, MobEffects.DAMAGE_BOOST, 0);
                buff(player, MobEffects.REGENERATION, 0);
            }
        }));

        // Rocket forward, leaving a line of fire behind you.
        reg("flame_dash", b().active(5, player -> {
            launchSelf(player, 2.0);
            Scheduler.repeat(2, 8, () -> {
                Zones.add(player, player.position().add(0, 0.5, 0), 1.2, 4, 10, ParticleTypes.FLAME,
                        (zone, inside) -> {
                            for (LivingEntity e : inside) {
                                if (!isFriend(player, e)) {
                                    e.igniteForSeconds(4);
                                    hurt(player, e, 1, player.damageSources().inFire());
                                }
                            }
                        });
            });
            sound(player, SoundEvents.FIRECHARGE_USE, 1, 1.4f);
            return true;
        }));

        // The ground around you erupts: magma floor, lava sprays and burning mobs thrown into the air.
        reg("volcano", b().active(30, player -> {
            ServerLevel level = level(player);
            BlockPos c = player.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-6, -1, -6), c.offset(6, -1, 6))) {
                double d = Math.sqrt(pos.distSqr(c.below()));
                if (d > 1.5 && d <= 6 && level.getBlockState(pos.above()).canBeReplaced()
                        && !level.getBlockState(pos).isAir() && player.getRandom().nextFloat() < 0.6f) {
                    TempBlocks.replace(level, pos.immutable(), Blocks.MAGMA_BLOCK.defaultBlockState(), 200);
                }
            }
            Zones.following(player, 6, 10, 10, ParticleTypes.LAVA, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (!isFriend(player, e)) {
                        e.igniteForSeconds(4);
                        hurt(player, e, 3, player.damageSources().inFire());
                        if (player.getRandom().nextFloat() < 0.3f) {
                            setVelocity(e, new Vec3(0, 1.0, 0));
                        }
                    }
                }
            });
            explode(player, player.position(), 2.5f, false);
            sound(player, SoundEvents.LAVA_POP, 2, 0.5f);
            return true;
        }));

        // Blue fire burns through armor and fire resistance, and burns for a long time.
        reg("blue_flame", b().active(4, player -> {
            Vec3 look = player.getLookAngle();
            for (int i = 1; i <= 8; i++) {
                Vec3 at = player.getEyePosition().add(look.scale(i));
                level(player).sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0.02);
            }
            for (LivingEntity e : cone(player, 8, 30)) {
                hurtMagic(player, e, 6);
                e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 240));
                particlesAround(player, e, BLUE_FIRE, 8);
            }
            sound(player, SoundEvents.SOUL_ESCAPE, 1, 1.2f);
            return true;
        }));
    }
}
