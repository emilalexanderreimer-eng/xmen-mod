package com.emil.xmen.robot;

import com.emil.xmen.entity.MutantNpc;
import com.emil.xmen.world.ModContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A Sentinel from Days of Future Past: a giant purple mutant-hunting robot. It stomps after mutants, punches,
 * charges up a laser in its eyes and, when you get too close, slams the ground. Set "Boss" to make it the huge
 * Sentinel Mark I with a boss bar, "Training" for the Danger Room's simulation.
 */
public class Sentinel extends RobotMob {
    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(Sentinel.class, EntityDataSerializers.BOOLEAN);
    private static final double BOSS_SCALE = 2.0;

    private boolean boss;
    private int cooldown = 40;
    private int charge;
    private int step;
    private ServerBossEvent bossBar;

    public Sentinel(EntityType<? extends Sentinel> type, Level level) {
        super(type, level);
        xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.STEP_HEIGHT, 1.5)
                .add(Attributes.MAX_HEALTH, 150)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING, false);
    }

    /** True while the eyes charge up a laser (the renderer makes them flash). */
    public boolean isCharging() {
        return entityData.get(CHARGING);
    }

    public boolean isBoss() {
        return boss;
    }

    /** The huge Sentinel Mark I: twice as big, much stronger, with a boss bar. */
    public void setBoss(boolean value) {
        boss = value;
        if (value) {
            getAttribute(Attributes.SCALE).setBaseValue(BOSS_SCALE);
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(20);
            xpReward = 200;
        }
    }

    @Override
    public void setTraining(boolean value) {
        super.setTraining(value);
        if (value) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(90);
            getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8);
        }
    }

    @Override
    protected Component getTypeName() {
        if (boss) {
            return Component.literal("Sentinel Mark I");
        }
        return isTraining() ? Component.literal("Sentinel Simulation") : super.getTypeName();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return charge == 0 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return charge == 0 && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                RobotMob::huntable));
        // Sentinels were built to hunt mutants
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, MutantNpc.class, 10, true, false,
                target -> !((MutantNpc) target).isHostile()));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (bossBar != null) {
            bossBar.setProgress(Math.max(0, getHealth() / getMaxHealth()));
        }
        LivingEntity target = getTarget();
        if (charge > 0) {
            getNavigation().stop();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0f, 30.0f);
            }
            Vec3 eyes = eyes();
            level.sendParticles(RED_LASER, eyes.x, eyes.y, eyes.z, 2, 0.15, 0.05, 0.15, 0);
            if (--charge == 0) {
                entityData.set(CHARGING, false);
                if (target != null && target.isAlive()) {
                    laser(level, eyes, target, RED_LASER, boss ? 14 : isTraining() ? 6 : 9, isTraining() ? 0 : 4);
                }
            }
            return;
        }
        if (target == null || --cooldown > 0) {
            return;
        }
        double distance = distanceTo(target);
        if (distance > 32) {
            cooldown = 20;
            return;
        }
        if (distance < 4.5 * getScale() && step++ % 2 == 1) {
            slam(level);
        } else if (hasLineOfSight(target)) {
            charge = isTraining() ? 24 : 18;
            entityData.set(CHARGING, true);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5f, 1.6f);
        }
        cooldown = (boss ? 50 : isTraining() ? 90 : 70) + random.nextInt(20);
    }

    /** Where the laser comes out: between the eyes. */
    private Vec3 eyes() {
        Vec3 look = Vec3.directionFromRotation(getXRot(), getYHeadRot());
        return getEyePosition().add(look.scale(0.6 * getScale()));
    }

    /** Stomps the ground: everyone close is hurt and thrown away. */
    private void slam(ServerLevel level) {
        double radius = 5 * getScale();
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.5, getZ(), 6, radius / 3, 0.2, radius / 3, 0);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f,
                0.7f);
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 2,
                radius), e -> e != this && !(e instanceof RobotMob) && huntable(e))) {
            near.hurt(damageSources().mobAttack(this), boss ? 14 : isTraining() ? 5 : 8);
            Vec3 push = near.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.4);
            near.setDeltaMovement(push.x, 0.6, push.z);
            near.hurtMarked = true;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.4, 0));
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0f, 0.7f);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // lasers and fire barely scratch the metal; it can't drown
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.0f, 0.6f);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (boss) {
            spawnAtLocation(new ItemStack(ModContent.SENTINEL_PART.get(), 8));
            spawnAtLocation(new ItemStack(Items.IRON_BLOCK, 3));
            spawnAtLocation(new ItemStack(Items.DIAMOND, 2));
        } else {
            spawnAtLocation(new ItemStack(ModContent.SENTINEL_PART.get(), 1 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 2 + random.nextInt(4)));
        }
    }

    // ------------------------------------------------------------------ boss bar and saving
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (boss && bossBar == null) {
            bossBar = new ServerBossEvent(Component.literal("Sentinel Mark I").withStyle(ChatFormatting.LIGHT_PURPLE),
                    BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_10);
        }
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (bossBar != null) {
            bossBar.removeAllPlayers();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Boss", boss);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBoss(tag.getBoolean("Boss"));
        if (!tag.contains("Health")) {
            // placed by a structure: start with full (boss) health
            setHealth(getMaxHealth());
        }
    }
}
