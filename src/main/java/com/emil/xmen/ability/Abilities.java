package com.emil.xmen.ability;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared building blocks that many powers use: aiming, beams, particles, safe teleporting. */
public final class Abilities {
    private Abilities() {
    }

    public static ServerLevel level(ServerPlayer player) {
        return player.serverLevel();
    }

    /** The block the player is looking at, up to range blocks away (type MISS if nothing). */
    public static BlockHitResult lookedAtBlock(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(range));
        return player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                player));
    }

    /** Where the player's view ends: the block hit, or range blocks away. */
    public static Vec3 lookTarget(ServerPlayer player, double range) {
        return lookedAtBlock(player, range).getLocation();
    }

    /** The first living thing in the player's view, not hidden behind blocks. */
    public static LivingEntity lookedAtLiving(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double max = lookTarget(player, range).distanceTo(start);
        Vec3 end = start.add(look.scale(max));
        AABB box = player.getBoundingBox().expandTowards(look.scale(max)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, box,
                e -> e instanceof LivingEntity && e != player && !e.isSpectator() && e.isPickable(), max * max);
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    /** Every living thing along a straight beam from the player's eyes until it hits a block. */
    public static List<LivingEntity> beam(ServerPlayer player, double range, double width) {
        Vec3 start = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double length = lookTarget(player, range).distanceTo(start);
        Set<LivingEntity> hits = new LinkedHashSet<>();
        AABB area = player.getBoundingBox().expandTowards(look.scale(length)).inflate(width + 1);
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator());
        for (double d = 0; d <= length; d += 0.5) {
            Vec3 point = start.add(look.scale(d));
            for (LivingEntity candidate : candidates) {
                if (candidate.getBoundingBox().inflate(width).contains(point)) {
                    hits.add(candidate);
                }
            }
        }
        return new ArrayList<>(hits);
    }

    public static List<LivingEntity> livingAround(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive() && !e.isSpectator());
    }

    public static List<Entity> entitiesAround(ServerPlayer player, double radius) {
        return player.level().getEntities(player, player.getBoundingBox().inflate(radius),
                e -> e.isAlive() && !e.isSpectator());
    }

    public static void particleLine(ServerPlayer player, Vec3 from, Vec3 to, ParticleOptions particle, double spacing) {
        ServerLevel level = level(player);
        double length = from.distanceTo(to);
        Vec3 step = to.subtract(from).normalize();
        for (double d = 0; d <= length; d += spacing) {
            Vec3 p = from.add(step.scale(d));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    public static void particles(ServerPlayer player, Vec3 at, ParticleOptions particle, int count, double spread) {
        level(player).sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, 0.02);
    }

    public static void sound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS,
                volume, pitch);
    }

    public static void hurt(ServerPlayer player, LivingEntity target, float damage) {
        target.hurt(player.damageSources().playerAttack(player), damage);
    }

    /** Pushes an entity in the player's look direction. */
    public static void push(ServerPlayer player, Entity target, double strength, double lift) {
        Vec3 look = player.getLookAngle();
        target.setDeltaMovement(look.x * strength, Math.max(look.y * strength, 0) + lift, look.z * strength);
        target.hurtMarked = true;
    }

    /** Throws the player in the direction they look. */
    public static void launchSelf(ServerPlayer player, double strength) {
        player.setDeltaMovement(player.getLookAngle().scale(strength));
        player.hurtMarked = true;
        player.resetFallDistance();
    }

    public static boolean hasRoom(ServerPlayer player, BlockPos feet) {
        ServerLevel level = level(player);
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    public static void teleport(ServerPlayer player, Vec3 to) {
        player.teleportTo(to.x, to.y, to.z);
        player.resetFallDistance();
    }

    /** The open spot right in front of the block the player looks at, or null. */
    public static BlockPos teleportSpot(ServerPlayer player, double range) {
        BlockHitResult hit = lookedAtBlock(player, range);
        BlockPos spot;
        if (hit.getType() == HitResult.Type.MISS) {
            spot = BlockPos.containing(hit.getLocation());
        } else {
            spot = hit.getBlockPos().relative(hit.getDirection());
            if (!hasRoom(player, spot) && hasRoom(player, spot.below())) {
                spot = spot.below();
            }
        }
        return hasRoom(player, spot) ? spot : null;
    }

    public static void tell(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.GRAY), true);
    }
}
