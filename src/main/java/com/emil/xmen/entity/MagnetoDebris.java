package com.emil.xmen.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A chunk of metal Magneto rips loose: it flies up to him, circles around him, then gets thrown at his target.
 * Never saved; it breaks apart when it hits something or when Magneto is gone.
 */
public class MagnetoDebris extends Entity {
    private static final EntityDataAccessor<BlockState> BLOCK =
            SynchedEntityData.defineId(MagnetoDebris.class, EntityDataSerializers.BLOCK_STATE);

    private MagnetoBoss owner;
    private LivingEntity target;
    private Vec3 start;
    private int slot;
    private int slots = 1;
    private int launchAt = 40;
    private boolean launched;

    public MagnetoDebris(EntityType<? extends MagnetoDebris> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    public void setup(MagnetoBoss owner, LivingEntity target, BlockState block, Vec3 from, int slot, int slots,
            int launchAt) {
        this.owner = owner;
        this.target = target;
        this.start = from;
        this.slot = slot;
        this.slots = slots;
        this.launchAt = launchAt;
        entityData.set(BLOCK, block);
        setPos(from);
    }

    public BlockState getBlock() {
        return entityData.get(BLOCK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(BLOCK, Blocks.IRON_BLOCK.defaultBlockState());
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level)) {
            return;  // the server moves it, the client just draws it
        }
        if (owner == null || !owner.isAlive() || owner.level() != level || tickCount > 200) {
            shatter(level);
            return;
        }
        if (!launched) {
            double angle = Math.PI * 2 * slot / slots + tickCount * 0.12;
            Vec3 orbit = owner.position().add(Math.cos(angle) * 2.6, 2.4 + Math.sin(tickCount * 0.2 + slot) * 0.3,
                    Math.sin(angle) * 2.6);
            setPos(tickCount < 15 ? start.lerp(orbit, tickCount / 15.0) : orbit);
            if (tickCount >= launchAt) {
                launched = true;
                Vec3 aim = target != null && target.isAlive() && target.level() == level
                        ? target.position().add(0, target.getBbHeight() * 0.6, 0) : position().add(
                                owner.getLookAngle().scale(10));
                setDeltaMovement(aim.subtract(position()).normalize().scale(1.15));
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE,
                        1.0f, 0.6f);
            }
            return;
        }
        Vec3 now = position();
        Vec3 next = now.add(getDeltaMovement());
        AABB path = getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.4);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, path,
                e -> e != owner && e.isAlive() && !e.isSpectator())) {
            victim.hurt(damageSources().mobAttack(owner), 7);
            Vec3 push = getDeltaMovement().normalize();
            victim.setDeltaMovement(victim.getDeltaMovement().add(push.x, 0.4, push.z));
            victim.hurtMarked = true;
            shatter(level);
            return;
        }
        BlockHitResult hit = level.clip(new ClipContext(now, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                this));
        if (hit.getType() != HitResult.Type.MISS) {
            setPos(hit.getLocation());
            shatter(level);
            return;
        }
        setPos(next);
        level.sendParticles(MagnetoBoss.MAGNET, getX(), getY() + 0.4, getZ(), 1, 0.1, 0.1, 0.1, 0);
    }

    private void shatter(ServerLevel level) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlock()), getX(), getY() + 0.4, getZ(), 25,
                0.3, 0.3, 0.3, 0.1);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.7f, 0.8f);
        discard();
    }

    /** A purple line of magnetic particles from a to b. */
    static void beam(ServerLevel level, Vec3 a, Vec3 b) {
        int steps = (int) (a.distanceTo(b) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) Math.max(steps, 1));
            level.sendParticles(MagnetoBoss.MAGNET, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
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
