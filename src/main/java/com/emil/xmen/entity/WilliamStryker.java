package com.emil.xmen.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import com.emil.xmen.world.ModContent;
import net.minecraft.nbt.CompoundTag;

/**
 * Colonel William Stryker, who runs Weapon X (X2, Origins, Apocalypse). Only a human, but a dangerous one: he
 * shoots with his pistol, injects people with his mutant control serum (slow, weak and dizzy), and calls in more
 * soldiers when things go badly.
 */
public class WilliamStryker extends WeaponXMob {
    private static final String NAME = "Colonel Stryker";
    private static final String[] LINES = {"Mutants are a disease. And I am the cure.",
            "You think you're the first one to break in here?", "I've spent my whole life on this program.",
            "Hold still. This won't hurt... much.", "You're making a mistake you won't live to regret."};

    private int cooldown = 30;
    private int shots;
    private int calls;
    private int callCooldown;

    public WilliamStryker(EntityType<? extends WilliamStryker> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(WeaponXEntities.RIFLE.get()));
        setDropChance(EquipmentSlot.MAINHAND, 1.0f);
        xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 90)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.ATTACK_DAMAGE, 5);
    }

    @Override
    public boolean aimsGun() {
        return true;
    }

    @Override
    protected ServerBossEvent createBossBar() {
        return new ServerBossEvent(Component.literal("Colonel William Stryker").withStyle(ChatFormatting.DARK_GREEN),
                BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    }

    @Override
    protected void addBehaviorGoals() {
        goalSelector.addGoal(2, new GunGoal(this, 0.9, 6.0, 15.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void greet(ServerLevel level, LivingEntity target) {
        say(level, NAME, "This is a restricted military facility. Nobody leaves Alkali Lake without my permission.",
                ChatFormatting.DARK_GREEN);
        alertNearby(target, 48);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (callCooldown > 0) {
            callCooldown--;
        }
        if (getTarget() != null && level() instanceof ServerLevel level && calls < 3 && callCooldown == 0
                && getHealth() < getMaxHealth() * (0.8f - calls * 0.25f)) {
            callReinforcements(level);
        }
    }

    @Override
    protected void gunTick(ServerLevel level, LivingEntity target, double distance) {
        if (--cooldown > 0) {
            return;
        }
        cooldown = 22 + random.nextInt(12);
        if (++shots % 5 == 0 && distance < 12 && powersOn()) {
            serumDart(level, target);
        } else {
            shoot(level, target, 5.0f, 0.75f);
        }
    }

    /** The mutant control serum from X2: a dart that leaves you slow, weak and dizzy. */
    private void serumDart(ServerLevel level, LivingEntity target) {
        Vec3 from = getEyePosition();
        Vec3 to = target.getEyePosition();
        trail(level, from, to, ParticleTypes.ENCHANTED_HIT, 0.6);
        sound(level, SoundEvents.CROSSBOW_SHOOT, 1.6f);
        if (hasLineOfSight(target) && target.hurt(damageSources().mobAttack(this), 2)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 2));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
            say(level, NAME, LINES[random.nextInt(LINES.length)], ChatFormatting.DARK_GREEN);
        }
    }

    /** "Get a team down here, now!" Two more soldiers show up next to him. */
    private void callReinforcements(ServerLevel level) {
        calls++;
        callCooldown = 200;
        say(level, NAME, "This is Stryker. I need a team down here, NOW!", ChatFormatting.DARK_GREEN);
        for (int i = 0; i < 2; i++) {
            for (int tries = 0; tries < 10; tries++) {
                BlockPos pos = blockPosition().offset(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                        && !level.getBlockState(pos.below()).isAir()) {
                    WeaponXSoldier soldier = WeaponXEntities.SOLDIER.get().create(level);
                    if (soldier != null) {
                        soldier.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360, 0);
                        soldier.setTarget(getTarget());
                        level.addFreshEntity(soldier);
                        level.sendParticles(ParticleTypes.CLOUD, soldier.getX(), soldier.getY() + 1, soldier.getZ(),
                                10, 0.3, 0.5, 0.3, 0.02);
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !isRemoved() && !dead;
        super.die(source);
        if (first && level() instanceof ServerLevel level) {
            say(level, NAME, "You have no idea what you've started...", ChatFormatting.DARK_GREEN);
            spawnAtLocation(new ItemStack(ModContent.ADAMANTIUM_INGOT.get(), 2));
            spawnAtLocation(new ItemStack(Items.EMERALD, 5));
            spawnAtLocation(new ItemStack(WeaponXEntities.COLLAR.get()));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Calls", calls);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        calls = tag.getInt("Calls");
    }
}
