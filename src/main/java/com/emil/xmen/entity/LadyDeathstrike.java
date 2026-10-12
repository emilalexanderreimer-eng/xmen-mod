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
import net.minecraft.world.level.Level;

/**
 * Lady Deathstrike (Yuriko Oyama) from X2: adamantium bones, ten long adamantium claws on her fingers, a healing
 * factor and Stryker's serum in her neck (her eyes glow blue). She never says a word. She lunges from far away,
 * cuts so deep you keep bleeding, and attacks in fast flurries up close.
 */
public class LadyDeathstrike extends WeaponXMob {
    private int lungeCooldown = 40;
    private int flurryCooldown = 60;
    private int flurry;
    private boolean lunging;

    public LadyDeathstrike(EntityType<? extends LadyDeathstrike> type, Level level) {
        super(type, level);
        xpReward = 80;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.ATTACK_DAMAGE, 8);
    }

    @Override
    public int clawStyle() {
        return 2;
    }

    @Override
    protected ServerBossEvent createBossBar() {
        return new ServerBossEvent(Component.literal("Lady Deathstrike").withStyle(ChatFormatting.AQUA),
                BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_6);
    }

    @Override
    protected void addBehaviorGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true) {
            @Override
            public boolean canUse() {
                return canAct() && super.canUse();
            }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void greet(ServerLevel level, LivingEntity target) {
        narrate(level, "Lady Deathstrike's eyes glow blue. Ten adamantium claws slide out of her fingers",
                ChatFormatting.AQUA);
        sound(level, SoundEvents.ANVIL_PLACE, 1.9f);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        regenerate(1.0f);
        LivingEntity target = getTarget();
        if (!(level() instanceof ServerLevel level) || target == null || !canAct()) {
            return;
        }
        if (lungeCooldown > 0) {
            lungeCooldown--;
        }
        if (flurryCooldown > 0) {
            flurryCooldown--;
        }
        double distance = distanceTo(target);
        if (lunging) {
            if (distance < 2.4) {
                lunging = false;
                slash(level, target, 10);
                bleed(target);
            } else if (onGround() && getDeltaMovement().y <= 0) {
                lunging = false;
            }
        }
        if (lungeCooldown == 0 && onGround() && distance > 4 && distance < 14 && hasLineOfSight(target)) {
            lungeCooldown = 70 + random.nextInt(30);
            lunging = true;
            leapAt(target, 1.6, 0.45);
            sound(level, SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f);
            level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.1, getZ(), 6, 0.3, 0.0, 0.3, 0.02);
        }
        if (flurryCooldown == 0 && distance < 3) {
            flurryCooldown = 90 + random.nextInt(40);
            flurry = 4;
        }
        if (flurry > 0 && tickCount % 4 == 0) {
            flurry--;
            if (distance < 3.2) {
                slash(level, target, 4);
            }
        }
    }

    /** Adamantium claws cut deep: the wound keeps hurting for a few seconds. */
    private void bleed(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living && level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD, living.getX(), living.getY() + 1, living.getZ(), 8, 0.3, 0.3, 0.3, 0);
            if (random.nextInt(3) == 0) {
                bleed(living);
            }
        }
        return hit;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !isRemoved() && !dead;
        super.die(source);
        if (first && level() instanceof ServerLevel level) {
            narrate(level, "The blue light fades from Lady Deathstrike's eyes", ChatFormatting.AQUA);
            spawnAtLocation(new ItemStack(ModContent.ADAMANTIUM_INGOT.get(), 4));
        }
    }
}
