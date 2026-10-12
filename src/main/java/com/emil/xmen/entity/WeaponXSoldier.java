package com.emil.xmen.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A soldier in black tactical gear with a rifle, like Stryker's men in X2. Fires short bursts. Jason Stryker can
 * also make fake ones ("illusions") that vanish when you hit them.
 */
public class WeaponXSoldier extends WeaponXMob {
    private static final String[] LINES = {"Contact! Weapon X has an intruder!", "Open fire!",
            "Lock down the level!", "Intruder on the lower decks!", "Take them down!"};

    private int cooldown = 30;
    private int burst;
    private boolean illusion;
    private int life;

    public WeaponXSoldier(EntityType<? extends WeaponXSoldier> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(WeaponXEntities.RIFLE.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.1f);
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }

    /** Turns this soldier into one of Jason's illusions: harmless, gone after a while or when hit. */
    public void makeIllusion(int ticks) {
        illusion = true;
        life = ticks;
        setDropChance(EquipmentSlot.MAINHAND, 0.0f);
        xpReward = 0;
    }

    public boolean isIllusion() {
        return illusion;
    }

    @Override
    public boolean aimsGun() {
        return true;
    }

    @Override
    protected void addBehaviorGoals() {
        goalSelector.addGoal(2, new GunGoal(this, 1.0, 5.0, 16.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void greet(ServerLevel level, LivingEntity target) {
        if (!illusion && random.nextInt(3) == 0) {
            say(level, "Soldier", LINES[random.nextInt(LINES.length)], ChatFormatting.GRAY);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (illusion && level() instanceof ServerLevel level) {
            if (tickCount % 4 == 0) {
                level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1, getZ(), 1, 0.3, 0.5, 0.3, 0);
            }
            if (--life <= 0) {
                vanish(level);
            }
        }
    }

    @Override
    protected void gunTick(ServerLevel level, LivingEntity target, double distance) {
        if (burst > 0) {
            if (tickCount % 4 == 0) {
                burst--;
                shoot(level, target, illusion ? 0.5f : 3.0f, distance < 8 ? 0.75f : 0.55f);
            }
            return;
        }
        if (--cooldown <= 0) {
            burst = 3;
            cooldown = 35 + random.nextInt(20);
        }
    }

    void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.WITCH, getX(), getY() + 1, getZ(), 25, 0.3, 0.6, 0.3, 0.05);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (illusion && !level().isClientSide() && source.getEntity() != null) {
            vanish((ServerLevel) level());
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        if (illusion) {
            return false;
        }
        return super.doHurtTarget(target);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Illusion")) {
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Illusion", illusion);
    }
}
