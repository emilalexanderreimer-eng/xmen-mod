package com.emil.xmen.robot;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * The Danger Room's combat robot: a silver humanoid machine with a glowing blue visor. It runs at you, punches with
 * electric fists and, now and then, dashes forward with a shock that slows you down.
 */
public class TrainingRobot extends RobotMob {
    private int dashCooldown = 60;

    public TrainingRobot(EntityType<? extends TrainingRobot> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                RobotMob::huntable));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || --dashCooldown > 0) {
            return;
        }
        double distance = distanceTo(target);
        if (distance > 3 && distance < 10 && onGround() && hasLineOfSight(target)) {
            // a rocket-boosted dash at the target
            Vec3 dir = target.position().subtract(position()).normalize();
            setDeltaMovement(dir.x * 1.3, 0.35, dir.z * 1.3);
            hurtMarked = true;
            level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.2, getZ(), 12, 0.2, 0.1, 0.2, 0.02);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.HOSTILE,
                    1.0f, 1.2f);
            dashCooldown = 80 + random.nextInt(40);
        } else {
            dashCooldown = 10;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && level() instanceof ServerLevel level) {
            // electric fists
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 12, 0.3,
                    0.4, 0.3, 0.1);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BEE_STING,
                    SoundSource.HOSTILE, 0.8f, 0.5f);
        }
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source,
            boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(Items.IRON_NUGGET, 3 + random.nextInt(5)));
        if (random.nextInt(3) == 0) {
            spawnAtLocation(new ItemStack(Items.REDSTONE, 1 + random.nextInt(3)));
        }
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 0.4f, 1.6f);
    }
}
