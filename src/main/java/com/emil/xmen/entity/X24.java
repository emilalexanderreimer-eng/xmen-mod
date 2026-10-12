package com.emil.xmen.entity;

import com.emil.xmen.world.ModContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * X-24 from Logan: a clone of Wolverine grown in a tank, with his claws and an even stronger healing factor, but
 * none of his conscience. He waits in his growth tank until someone comes close, then smashes out. He pounces from
 * far away and goes berserk when he's badly hurt.
 */
public class X24 extends WeaponXMob {
    private int pounceCooldown = 40;
    private boolean berserk;

    public X24(EntityType<? extends X24> type, Level level) {
        super(type, level);
        xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ATTACK_DAMAGE, 10);
    }

    @Override
    public int clawStyle() {
        return 1;
    }

    @Override
    protected ServerBossEvent createBossBar() {
        return new ServerBossEvent(Component.literal("X-24").withStyle(ChatFormatting.DARK_RED),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    }

    @Override
    protected void addBehaviorGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
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
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
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
                Player near = level.getNearestPlayer(this, 7);
                if (near != null && !near.isCreative() && !near.isSpectator()) {
                    breakOut(level);
                    setTarget(near);
                }
            }
            return;
        }
        regenerate(berserk ? 3.0f : 2.0f);
        LivingEntity target = getTarget();
        if (target == null || !powersOn()) {
            return;
        }
        if (!berserk && getHealth() < getMaxHealth() * 0.35f) {
            berserk = true;
            narrate(level, "X-24 goes berserk!", ChatFormatting.DARK_RED);
            sound(level, SoundEvents.RAVAGER_ROAR, 1.1f);
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 60, 1));
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60, 1));
        }
        if (pounceCooldown > 0) {
            pounceCooldown--;
        }
        double distance = distanceTo(target);
        if (pounceCooldown == 0 && onGround() && distance > 5 && distance < 16 && hasLineOfSight(target)) {
            pounceCooldown = berserk ? 40 : 80;
            leapAt(target, 1.8, 0.55);
            sound(level, SoundEvents.WOLF_GROWL, 0.6f);
        }
    }

    private void breakOut(ServerLevel level) {
        breakOutOfTank(level, 2);
        narrate(level, "X-24 smashes out of his growth tank!", ChatFormatting.DARK_RED);
        sound(level, SoundEvents.RAVAGER_ROAR, 0.9f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (contained && source.getEntity() instanceof Player && level() instanceof ServerLevel level) {
            breakOut(level);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0,
                    0, 0);
            if (random.nextInt(4) == 0) {
                sound(level, SoundEvents.WOLF_GROWL, 0.7f);
            }
        }
        return hit;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_GROWL;
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !isRemoved() && !dead;
        super.die(source);
        if (first && level() instanceof ServerLevel level) {
            narrate(level, "X-24 falls. Even his healing factor couldn't save him", ChatFormatting.DARK_RED);
            spawnAtLocation(new ItemStack(ModContent.ADAMANTIUM_INGOT.get(), 5));
            spawnAtLocation(new ItemStack(Items.DIAMOND, 2));
            spawnAtLocation(new ItemStack(WeaponXEntities.HEALING_SERUM.get(), 2));
        }
    }
}
