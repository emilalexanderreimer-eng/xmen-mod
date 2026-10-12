package com.emil.xmen.robot;

import org.joml.Vector3f;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Everything made of metal that fights mutants: the Danger Room's training robots and drones and the Sentinels.
 * A "training" robot is a Danger Room simulation: it gives no loot and no XP and bursts into sparks when beaten.
 */
public abstract class RobotMob extends Monster {
    public static final DustParticleOptions RED_LASER = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 1.4f);
    public static final DustParticleOptions BLUE_LASER = new DustParticleOptions(new Vector3f(0.3f, 0.7f, 1.0f), 1.2f);

    private boolean training;

    protected RobotMob(EntityType<? extends RobotMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public boolean isTraining() {
        return training;
    }

    public void setTraining(boolean value) {
        training = value;
        if (value) {
            xpReward = 0;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Training", training);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setTraining(tag.getBoolean("Training"));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDropLoot() {
        return !training && !getTags().contains("xmen.no_loot") && super.shouldDropLoot();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight() / 2, getZ(), 40,
                    getBbWidth() / 2, getBbHeight() / 3, getBbWidth() / 2, 0.2);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() / 2, getZ(), 10,
                    getBbWidth() / 3, getBbHeight() / 3, getBbWidth() / 3, 0.02);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (getTarget() instanceof Player player && (player.isCreative() || player.isSpectator())) {
            setTarget(null);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    /** Players and (for Sentinels) mutants they can hunt. Creative players are left alone. */
    protected static boolean huntable(LivingEntity target) {
        return !(target instanceof Player player) || !player.isCreative() && !player.isSpectator();
    }

    /**
     * Fires a laser from {@code from} at the target. It stops at walls; returns true if it hit.
     */
    protected boolean laser(ServerLevel level, Vec3 from, LivingEntity target, DustParticleOptions color, float damage,
            int fireSeconds) {
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                this));
        boolean hit = wall.getType() == HitResult.Type.MISS;
        Vec3 end = hit ? to : wall.getLocation();
        int steps = (int) (from.distanceTo(end) * 4);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(end, i / (double) Math.max(steps, 1));
            level.sendParticles(color, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.0f, 1.8f);
        if (hit) {
            target.hurt(damageSources().mobAttack(this), damage);
            if (fireSeconds > 0) {
                target.igniteForSeconds(fireSeconds);
            }
            level.sendParticles(ParticleTypes.SMALL_FLAME, end.x, end.y, end.z, 6, 0.2, 0.2, 0.2, 0.01);
        }
        return hit;
    }
}
