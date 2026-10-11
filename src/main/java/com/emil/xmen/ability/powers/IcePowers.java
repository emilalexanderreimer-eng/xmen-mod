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


/** Ice & Water. */
public final class IcePowers {
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;

    private IcePowers() {
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

    public static void register() {
        // Kept from 0.3.0: an ice blast that freezes a mob, or freezes water where it lands.
        reg("cryokinesis", b().active(1.5, player -> {
            ServerLevel level = level(player);
            LivingEntity target = lookedAtLiving(player, 30);
            Vec3 start = player.getEyePosition().add(player.getLookAngle());
            Vec3 end = target != null ? target.getBoundingBox().getCenter() : lookTarget(player, 30);
            particleLine(player, start, end, ParticleTypes.SNOWFLAKE, 0.4);
            if (target != null) {
                hurt(player, target, 4);
                freeze(target, 4);
            } else {
                BlockPos center = BlockPos.containing(end);
                for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
                    if (level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource()) {
                        level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                    }
                }
            }
            particles(player, end, ParticleTypes.SNOWFLAKE, 25, 0.5);
            sound(player, SoundEvents.PLAYER_HURT_FREEZE, 1, 1.4f);
            return true;
        }));

        // Always-on ice body: armor, frozen punches, no cold. R: shoot a ring of ice shards around you.
        PowerProjectiles.register("ice_shard", (owner, projectile, hit) -> {
            if (owner != null && hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity target) {
                hurt(owner, target, 5);
                freeze(target, 2);
            }
        });
        reg("ice_form", b()
                .attribute(Attributes.ARMOR, 6, ADD)
                .immuneTo((player, source) -> source.is(DamageTypeTags.IS_FREEZING))
                .onAttack((player, target, event) -> freeze(target, 2))
                .active(6, player -> {
                    float pitch = player.getXRot();
                    float yaw = player.getYRot();
                    for (int i = 0; i < 12; i++) {
                        player.setYRot(yaw + i * 30);
                        player.setXRot(0);
                        PowerProjectiles.shoot(player, "ice_shard", new ItemStack(Items.ICE), 1.6f, 0, false);
                    }
                    player.setYRot(yaw);
                    player.setXRot(pitch);
                    sound(player, SoundEvents.GLASS_BREAK, 1, 1.5f);
                    return true;
                }));

        // A slippery path of blue ice where you look, and a speed boost to ride it.
        reg("ice_slide", b().active(8, player -> {
            ServerLevel level = level(player);
            Vec3 look = flatLook(player);
            Vec3 start = player.position().add(0, -0.5, 0);
            int placed = 0;
            for (int i = 1; i <= 24; i++) {
                BlockPos pos = BlockPos.containing(start.add(look.scale(i)));
                BlockState current = level.getBlockState(pos);
                if (current.isAir() || current.canBeReplaced()) {
                    if (TempBlocks.place(level, pos, Blocks.BLUE_ICE.defaultBlockState(), 200)) {
                        placed++;
                    }
                } else if (!current.hasBlockEntity() && TempBlocks.replace(level, pos,
                        Blocks.BLUE_ICE.defaultBlockState(), 200)) {
                    placed++;
                }
            }
            if (placed == 0) {
                return false;
            }
            effect(player, MobEffects.MOVEMENT_SPEED, 8, 1);
            sound(player, SoundEvents.GLASS_PLACE, 1, 1.5f);
            return true;
        }));

        // Water freezes under your feet and melts again later.
        reg("frost_walker", b().every(2, player -> {
            if (!player.onGround() && !player.isInWater()) {
                return;
            }
            ServerLevel level = level(player);
            BlockPos feet = player.blockPosition().below();
            for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-2, 0, -2), feet.offset(2, 0, 2))) {
                if (pos.distSqr(feet) > 5) {
                    continue;
                }
                if (level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource()
                        && level.getBlockState(pos.above()).isAir()) {
                    level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                    level.scheduleTick(pos.immutable(), Blocks.FROSTED_ICE, 60 + player.getRandom().nextInt(60));
                }
            }
        }));

        // Powder snow never freezes you, and you're faster in cold places.
        reg("cold_proof", b()
                .immuneTo((player, source) -> source.is(DamageTypeTags.IS_FREEZING))
                .every(20, player -> {
                    player.setTicksFrozen(0);
                    BlockPos pos = player.blockPosition();
                    if (level(player).getBiome(pos).value().coldEnoughToSnow(pos)) {
                        buff(player, MobEffects.MOVEMENT_SPEED, 1);
                    }
                }));

        // A burst of eight hard-hitting snowballs.
        PowerProjectiles.register("hard_snowball", (owner, projectile, hit) -> {
            if (owner != null && hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity target) {
                hurt(owner, target, 3);
                target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze(), target.getTicksFrozen() + 40));
            }
        });
        reg("snowball_barrage", b().active(3, player -> {
            Scheduler.repeat(2, 8, () -> {
                PowerProjectiles.shoot(player, "hard_snowball", new ItemStack(Items.SNOWBALL), 1.8f, 4, true);
                sound(player, SoundEvents.SNOWBALL_THROW, 0.6f, 1.2f);
            });
            return true;
        }));

        // A water jet that pushes mobs and puts out fire. Sneak: make water where you look for a while.
        reg("hydrokinesis", b().active(2, player -> {
            ServerLevel level = level(player);
            if (player.isShiftKeyDown()) {
                BlockHitResult hit = lookedAtBlock(player, 16);
                if (hit.getType() == HitResult.Type.MISS) {
                    tell(player, "Look at a block");
                    return false;
                }
                BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
                if (level.dimensionType().ultraWarm()) {
                    particles(player, Vec3.atCenterOf(pos), ParticleTypes.LARGE_SMOKE, 10, 0.3);
                    tell(player, "The water boils away here");
                    return true;
                }
                if (!TempBlocks.place(level, pos, Blocks.WATER.defaultBlockState(), 400)) {
                    tell(player, "No room for water there");
                    return false;
                }
                sound(player, SoundEvents.BUCKET_EMPTY, 1, 1);
                return true;
            }
            Vec3 start = player.getEyePosition();
            Vec3 end = lookTarget(player, 20);
            particleLine(player, start.add(player.getLookAngle()), end, ParticleTypes.SPLASH, 0.25);
            particleLine(player, start.add(player.getLookAngle()), end, ParticleTypes.FALLING_WATER, 0.5);
            for (LivingEntity e : beam(player, 20, 0.6)) {
                push(player, e, 1.6, 0.3);
                e.clearFire();
                if (e instanceof net.minecraft.world.entity.monster.Blaze || e instanceof net.minecraft.world.entity.monster.EnderMan) {
                    hurt(player, e, 6, player.damageSources().drown());
                }
            }
            BlockPos at = BlockPos.containing(end);
            for (BlockPos pos : BlockPos.betweenClosed(at.offset(-2, -2, -2), at.offset(2, 2, 2))) {
                if (level.getBlockState(pos).is(BlockTags.FIRE)) {
                    level.removeBlock(pos, false);
                }
            }
            sound(player, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1, 1);
            return true;
        }));

        // Breathe underwater forever.
        reg("gills", b().effect(MobEffects.WATER_BREATHING, 0));

        // Underwater you see clearly, mine at full speed and hold your breath much longer.
        reg("aquatic_body", b()
                .attribute(Attributes.SUBMERGED_MINING_SPEED, 4, ADD)
                .attribute(Attributes.OXYGEN_BONUS, 3, ADD)
                .every(20, player -> {
                    if (player.isUnderWater()) {
                        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, true));
                    }
                }));

        // A wave rolls forward from you, carrying mobs away.
        reg("tidal_wave", b().active(15, player -> {
            Vec3 look = flatLook(player);
            Zones.Zone wave = Zones.add(player, player.position().add(look.scale(2)).add(0, 1, 0), 3, 3, 2,
                    ParticleTypes.SPLASH, (zone, inside) -> {
                        for (LivingEntity e : inside) {
                            if (!isFriend(player, e)) {
                                setVelocity(e, look.scale(1.2).add(0, 0.25, 0));
                                e.clearFire();
                                if (zone.age % 10 == 0) {
                                    hurt(player, e, 3, player.damageSources().drown());
                                }
                            }
                        }
                        level(player).sendParticles(ParticleTypes.BUBBLE_POP, zone.center.x, zone.center.y,
                                zone.center.z, 20, 2, 1, 2, 0.1);
                    });
            wave.velocity = look.scale(0.6);
            sound(player, SoundEvents.GENERIC_SPLASH, 2, 0.5f);
            return true;
        }));

        // R: coat yourself in ice for 12 seconds. Strong protection, and attackers freeze.
        reg("ice_armor", b()
                .onHurt((player, event) -> {
                    if (state(player).active("ice_armor", now(player))) {
                        event.setAmount(event.getAmount() * 0.4f);
                        if (event.getSource().getEntity() instanceof LivingEntity attacker && attacker != player) {
                            freeze(attacker, 2);
                        }
                    }
                })
                .every(5, player -> {
                    if (state(player).active("ice_armor", now(player))) {
                        particlesAround(player, player, ParticleTypes.SNOWFLAKE, 4);
                    }
                })
                .active(25, player -> {
                    state(player).start("ice_armor", now(player), 240);
                    sound(player, SoundEvents.GLASS_PLACE, 1, 0.6f);
                    tell(player, "Ice armor for 12 seconds");
                    return true;
                }));

        // A snowstorm that travels with you for 10 seconds, slowing and freezing mobs.
        reg("blizzard", b().active(25, player -> {
            Zones.following(player, 9, 10, 10, ParticleTypes.SNOWFLAKE, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (!isFriend(player, e)) {
                        effect(e, MobEffects.MOVEMENT_SLOWDOWN, 2, 2);
                        e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 40, e.getTicksFrozen() + 20));
                        hurt(player, e, 1, player.damageSources().freeze());
                    }
                }
                level(player).sendParticles(ParticleTypes.ITEM_SNOWBALL, zone.center.x, zone.center.y + 2,
                        zone.center.z, 20, 6, 1, 6, 0.05);
            });
            sound(player, SoundEvents.POWDER_SNOW_BREAK, 2, 0.5f);
            return true;
        }));

        // Water body: falls never hurt you. R: shrink into a small splash and slip through gaps for 6 seconds.
        reg("water_form", b()
                .onFall((player, distance) -> true)
                .active(12, player -> {
                    timedModifier(player, Attributes.SCALE, "water_form", -0.7,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, 6);
                    effect(player, MobEffects.DOLPHINS_GRACE, 6, 0);
                    particlesAround(player, player, ParticleTypes.SPLASH, 30);
                    sound(player, SoundEvents.PLAYER_SPLASH, 1, 1.4f);
                    return true;
                }));

        // Spikes of ice burst out of the ground in a line and throw mobs up.
        reg("ice_spikes", b().active(5, player -> {
            ServerLevel level = level(player);
            Vec3 look = flatLook(player);
            for (int i = 2; i <= 12; i++) {
                int step = i;
                Scheduler.later(i, () -> {
                    Vec3 at = player.position().add(look.scale(step));
                    BlockPos ground = safeSpotNear(level, BlockPos.containing(at));
                    if (ground == null) {
                        return;
                    }
                    TempBlocks.place(level, ground, Blocks.PACKED_ICE.defaultBlockState(), 60);
                    if (step % 2 == 0) {
                        TempBlocks.place(level, ground.above(), Blocks.PACKED_ICE.defaultBlockState(), 60);
                    }
                    level.sendParticles(ParticleTypes.SNOWFLAKE, ground.getX() + 0.5, ground.getY() + 1,
                            ground.getZ() + 0.5, 8, 0.3, 0.6, 0.3, 0.02);
                    for (LivingEntity e : livingNear(player, Vec3.atBottomCenterOf(ground), 1.5)) {
                        hurt(player, e, 5);
                        setVelocity(e, new Vec3(0, 0.9, 0));
                    }
                });
            }
            sound(player, SoundEvents.GLASS_BREAK, 1, 0.8f);
            return true;
        }));

        // Regenerate in water and rain.
        reg("rain_healing", b().every(20, player -> {
            if (player.isInWaterOrRain()) {
                buff(player, MobEffects.REGENERATION, 0);
            }
        }));

        // Freeze everything around you: water to ice, lava to obsidian, mobs frozen solid.
        reg("absolute_zero", b().active(45, player -> {
            ServerLevel level = level(player);
            BlockPos c = player.blockPosition();
            int r = 16;
            for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -6, -r), c.offset(r, 6, r))) {
                if (pos.distSqr(c) > r * r) {
                    continue;
                }
                BlockState s = level.getBlockState(pos);
                if (s.is(Blocks.WATER) && level.getFluidState(pos).isSource()) {
                    TempBlocks.replace(level, pos.immutable(), Blocks.ICE.defaultBlockState(), 1200);
                } else if (s.is(Blocks.LAVA) && level.getFluidState(pos).isSource()) {
                    TempBlocks.replace(level, pos.immutable(), Blocks.OBSIDIAN.defaultBlockState(), 1200);
                }
            }
            for (LivingEntity e : livingAround(player, r)) {
                if (!isFriend(player, e)) {
                    hurt(player, e, 8, player.damageSources().freeze());
                    freeze(e, 8);
                }
            }
            for (int i = 4; i <= r; i += 4) {
                ring(player, player.position().add(0, 0.5, 0), i, ParticleTypes.SNOWFLAKE);
            }
            sound(player, SoundEvents.GLASS_BREAK, 2, 0.4f);
            return true;
        }));

        // A thick fog where you stand. Inside it you are invisible and mobs lose you.
        reg("mist", b().active(20, player -> {
            Zones.add(player, player.position().add(0, 1, 0), 6, 12, 10, ParticleTypes.CLOUD, (zone, inside) -> {
                ServerPlayer owner = zone.ownerPlayer();
                if (owner != null && owner.position().distanceTo(zone.center) <= zone.radius) {
                    owner.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false, true));
                    for (LivingEntity e : livingAround(owner, 24)) {
                        forget(owner, e);
                    }
                }
                for (LivingEntity e : inside) {
                    if (!isFriend(player, e)) {
                        effect(e, MobEffects.BLINDNESS, 2, 0);
                    }
                }
            });
            sound(player, SoundEvents.FIRE_EXTINGUISH, 1, 0.5f);
            return true;
        }));

        // Spin up a whirlpool in the water you look at: mobs in it are dragged around and pulled under.
        reg("whirlpool", b().active(15, player -> {
            BlockHitResult hit = lookedAtBlockOrFluid(player, 32);
            Vec3 center = hit.getLocation();
            if (!level(player).getFluidState(hit.getBlockPos()).isSource()) {
                tell(player, "Look at water");
                return false;
            }
            Zones.add(player, center, 8, 8, 2, ParticleTypes.BUBBLE_POP, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (isFriend(player, e) || !e.isInWater()) {
                        continue;
                    }
                    Vec3 to = zone.center.subtract(e.position());
                    Vec3 flat = new Vec3(to.x, 0, to.z);
                    Vec3 spin = new Vec3(-flat.z, 0, flat.x).normalize().scale(0.35);
                    setVelocity(e, flat.normalize().scale(0.15).add(spin).add(0, -0.15, 0));
                    if (zone.age % 20 == 0) {
                        hurt(player, e, 2, player.damageSources().drown());
                    }
                }
                level(player).sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, zone.center.x, zone.center.y - 1,
                        zone.center.z, 15, 2, 0.5, 2, 0.1);
            });
            sound(player, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, 2, 1);
            return true;
        }));

        // A wall of ice blocks in front of you for 15 seconds.
        reg("ice_wall", b().active(12, player -> {
            ServerLevel level = level(player);
            Vec3 look = flatLook(player);
            Vec3 side = new Vec3(-look.z, 0, look.x);
            Vec3 center = player.position().add(look.scale(3));
            int placed = 0;
            for (int i = -2; i <= 2; i++) {
                for (int h = 0; h < 3; h++) {
                    BlockPos pos = BlockPos.containing(center.add(side.scale(i)).add(0, h, 0));
                    if (TempBlocks.place(level, pos, Blocks.PACKED_ICE.defaultBlockState(), 300)) {
                        placed++;
                    }
                }
            }
            if (placed == 0) {
                return false;
            }
            sound(player, SoundEvents.GLASS_PLACE, 1, 0.8f);
            return true;
        }));

        // The water around you boils for 6 seconds, hurting every mob swimming in it.
        reg("boiling_touch", b().active(10, player -> {
            Zones.following(player, 8, 6, 10, null, (zone, inside) -> {
                for (LivingEntity e : inside) {
                    if (!isFriend(player, e) && e.isInWater()) {
                        hurt(player, e, 3, player.damageSources().hotFloor());
                        particlesAround(player, e, ParticleTypes.BUBBLE_POP, 10);
                    }
                }
                ServerLevel level = level(player);
                BlockPos c = BlockPos.containing(zone.center);
                for (int i = 0; i < 20; i++) {
                    BlockPos p = c.offset(player.getRandom().nextInt(17) - 8, player.getRandom().nextInt(7) - 3,
                            player.getRandom().nextInt(17) - 8);
                    if (level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)) {
                        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, p.getX() + 0.5, p.getY() + 0.5,
                                p.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.05);
                    }
                }
            });
            sound(player, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1, 0.6f);
            return true;
        }));
    }
}
