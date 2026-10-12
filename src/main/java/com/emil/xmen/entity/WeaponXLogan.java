package com.emil.xmen.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Logan as "Weapon X", the way Jean finds him in X-Men: Apocalypse: locked in a water tank with the Weapon X
 * helmet on his head and cables in his back. Use the tank console to set him free. He goes berserk and tears
 * through every soldier, scientist and mutant of the facility, but he leaves you alone (unless you hit him).
 */
public class WeaponXLogan extends WeaponXMob {
    private int rage;
    private int leapCooldown = 20;

    public WeaponXLogan(EntityType<? extends WeaponXLogan> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ATTACK_DAMAGE, 11);
    }

    @Override
    public boolean isStaff() {
        return false;
    }

    @Override
    protected boolean huntsPlayers() {
        return false;
    }

    @Override
    public int clawStyle() {
        return 1;
    }

    @Override
    protected void addBehaviorGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true) {
            @Override
            public boolean canUse() {
                return canAct() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7) {
            @Override
            public boolean canUse() {
                return canAct() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, WeaponXMob.class, 5, false, false,
                e -> canAct() && ((WeaponXMob) e).isStaff() && ((WeaponXMob) e).canAct()
                        && !(e instanceof WeaponXSoldier s && s.isIllusion())));
    }

    /** Called by the tank console. Returns false if he was already free. */
    public boolean release(ServerLevel level) {
        if (!contained) {
            return false;
        }
        breakOutOfTank(level, 2);
        rage = 20 * 180;
        narrate(level, "WEAPON X breaks free! He tears the cables out of his back and his claws come out",
                ChatFormatting.GOLD);
        sound(level, SoundEvents.RAVAGER_ROAR, 0.8f);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, rage, 1));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, rage, 0));
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (contained) {
            if (tickCount % 10 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE, getX(), getY() + 1, getZ(), 4, 0.3, 0.6, 0.3, 0.02);
            }
            return;
        }
        regenerate(2.0f);
        if (rage > 0 && --rage == 0) {
            narrate(level, "Logan calms down. The rage is gone from his eyes", ChatFormatting.GOLD);
        }
        LivingEntity target = getTarget();
        if (!powersOn()) {
            return;
        }
        if (leapCooldown > 0) {
            leapCooldown--;
        }
        if (target != null && leapCooldown == 0 && onGround()) {
            double distance = distanceTo(target);
            if (distance > 4 && distance < 14 && hasLineOfSight(target)) {
                leapCooldown = 50;
                leapAt(target, 1.7, 0.5);
                if (random.nextInt(3) == 0) {
                    sound(level, SoundEvents.RAVAGER_ROAR, 1.3f);
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0,
                    0, 0);
        }
        return hit;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_GROWL;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Rage", rage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        rage = tag.getInt("Rage");
    }
}
