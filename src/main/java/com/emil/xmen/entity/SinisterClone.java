package com.emil.xmen.entity;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A clone Mister Sinister grows in his tanks. It sleeps in its tank until he wakes it (or someone hits it), then
 * smashes out of the glass and fights with claws and weaker copies of his energy blasts.
 */
public class SinisterClone extends Monster implements SinisterLook {
    private static final EntityDataAccessor<Boolean> DORMANT =
            SynchedEntityData.defineId(SinisterClone.class, EntityDataSerializers.BOOLEAN);
    private int blastCooldown = 60;

    public SinisterClone(EntityType<? extends SinisterClone> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.ATTACK_DAMAGE, 5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DORMANT, false);
    }

    @Override
    public boolean isCasting() {
        return blastCooldown > 50 && !isDormant() && getTarget() != null;
    }

    @Override
    public boolean isDormant() {
        return entityData.get(DORMANT);
    }

    @Override
    public UUID getDisguise() {
        return null;
    }

    @Override
    public boolean hasCape() {
        return false;
    }

    /** Puts it to sleep in a tank (the lab places them like this). */
    public void setDormant(boolean dormant) {
        entityData.set(DORMANT, dormant);
        setNoAi(dormant);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, SinisterBoss.class, SinisterClone.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** Wakes up and smashes out of its tank. */
    public void wake(LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (isDormant()) {
            setDormant(false);
            BlockPos base = blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(1, 3, 1))) {
                if (level.getBlockState(pos).is(BlockTags.IMPERMEABLE)) {
                    level.destroyBlock(pos, false);
                }
            }
            level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 40, 0.5, 0.8, 0.5, 0.1);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 0.8f);
        }
        if (target != null && !(target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            setTarget(target);
        }
    }

    /** Falls apart into red dust (when Sinister dies). */
    public void dissolve(ServerLevel level) {
        level.sendParticles(SinisterBoss.RED_ENERGY, getX(), getY() + 1, getZ(), 30, 0.3, 0.8, 0.3, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 15, 0.3, 0.8, 0.3, 0.02);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 0.5f,
                1.6f);
        discard();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level) || isDormant()) {
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && target instanceof Player p && (p.isCreative() || p.isSpectator())) {
            setTarget(null);
            target = null;
        }
        if (--blastCooldown > 0 || target == null || WeaponXItems.isSuppressed(this)) {
            return;
        }
        blastCooldown = 100 + random.nextInt(40);
        double distance = distanceTo(target);
        if (distance < 5 || distance > 20 || !hasLineOfSight(target)) {
            return;
        }
        SinisterBlast blast = new SinisterBlast(SinisterEntities.BLAST.get(), level);
        Vec3 from = getEyePosition().add(getLookAngle().scale(0.8)).add(0, -0.4, 0);
        blast.setup(this, from, target.position().add(0, target.getBbHeight() * 0.6, 0), 0.75, 4);
        level.addFreshEntity(blast);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8f, 0.9f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof SinisterBlast) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && isDormant() && source.getEntity() instanceof LivingEntity attacker) {
            wake(attacker);
        }
        return hurt;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dormant", isDormant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Dormant")) {
            setDormant(true);
        }
    }
}
