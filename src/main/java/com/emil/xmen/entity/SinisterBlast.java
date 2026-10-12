package com.emil.xmen.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * One of Mister Sinister's red energy blasts. It flies straight, can be dodged, and bursts on whatever it hits,
 * throwing people back. It never breaks blocks. Never saved.
 */
public class SinisterBlast extends Entity {
    private LivingEntity owner;
    private float damage = 7;

    public SinisterBlast(EntityType<? extends SinisterBlast> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Fires from the owner's hand towards aim. */
    public void setup(LivingEntity owner, Vec3 from, Vec3 aim, double speed, float damage) {
        this.owner = owner;
        this.damage = damage;
        setPos(from);
        setDeltaMovement(aim.subtract(from).normalize().scale(speed));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;  // the server moves it, the client only sees the particles
        }
        if (tickCount > 80) {
            discard();
            return;
        }
        Vec3 now = position();
        Vec3 next = now.add(getDeltaMovement());
        AABB path = getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.3);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, path,
                e -> e != owner && e.isAlive() && !e.isSpectator() && !(e instanceof SinisterClone)
                        && !(e instanceof SinisterBoss))) {
            if (owner != null) {
                victim.hurt(damageSources().indirectMagic(this, owner), damage);
            } else {
                victim.hurt(damageSources().magic(), damage);
            }
            Vec3 push = getDeltaMovement().normalize();
            victim.setDeltaMovement(victim.getDeltaMovement().add(push.x * 1.2, 0.45, push.z * 1.2));
            victim.hurtMarked = true;
            burst(level);
            return;
        }
        BlockHitResult hit = level.clip(new ClipContext(now, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                this));
        if (hit.getType() != HitResult.Type.MISS) {
            setPos(hit.getLocation());
            burst(level);
            return;
        }
        setPos(next);
        // a bright red core with a darker trail
        level.sendParticles(SinisterBoss.RED_ENERGY, getX(), getY(), getZ(), 4, 0.08, 0.08, 0.08, 0);
        level.sendParticles(SinisterBoss.DARK_ENERGY, getX() - getDeltaMovement().x * 0.5,
                getY() - getDeltaMovement().y * 0.5, getZ() - getDeltaMovement().z * 0.5, 2, 0.1, 0.1, 0.1, 0);
    }

    private void burst(ServerLevel level) {
        level.sendParticles(SinisterBoss.RED_ENERGY, getX(), getY(), getZ(), 30, 0.5, 0.5, 0.5, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
                0.6f, 1.6f);
        discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
