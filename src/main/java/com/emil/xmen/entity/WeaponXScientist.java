package com.emil.xmen.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A scientist in a white lab coat. Doesn't fight: calls security when it sees you, runs away and throws sedatives
 * (slowness and weakness potions) when you get close.
 */
public class WeaponXScientist extends WeaponXMob {
    private static final String[] ALARM = {"Security! We have an intruder in the lab!",
            "Somebody call Colonel Stryker!", "Don't let it near the subjects!", "Guards! GUARDS!"};

    private int alarmCooldown;
    private int throwCooldown = 40;

    public WeaponXScientist(EntityType<? extends WeaponXScientist> type, Level level) {
        super(type, level);
        xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 16)
                .add(Attributes.ATTACK_DAMAGE, 1);
    }

    @Override
    protected boolean huntsPlayers() {
        return false;
    }

    @Override
    protected void addBehaviorGoals() {
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 8.0f, 1.0, 1.3,
                e -> canAct() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, WeaponXLogan.class, 12.0f, 1.1, 1.45,
                e -> !((WeaponXLogan) e).isContained()));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level) || !canAct()) {
            return;
        }
        if (alarmCooldown > 0) {
            alarmCooldown--;
        }
        if (throwCooldown > 0) {
            throwCooldown--;
        }
        if (tickCount % 10 != 0) {
            return;
        }
        Player player = level.getNearestPlayer(this, 12);
        if (player == null || player.isCreative() || player.isSpectator() || !hasLineOfSight(player)) {
            return;
        }
        if (alarmCooldown == 0) {
            alarmCooldown = 600;
            say(level, "Scientist", ALARM[random.nextInt(ALARM.length)], ChatFormatting.WHITE);
            alertNearby(player, 32);
        }
        if (throwCooldown == 0 && distanceTo(player) < 9) {
            throwCooldown = 70 + random.nextInt(40);
            ThrownPotion potion = new ThrownPotion(level, this);
            potion.setItem(PotionContents.createItemStack(Items.SPLASH_POTION,
                    random.nextBoolean() ? Potions.SLOWNESS : Potions.WEAKNESS));
            Vec3 to = player.getEyePosition().subtract(getEyePosition());
            potion.setXRot(potion.getXRot() - 20.0f);
            potion.shoot(to.x, to.y + to.horizontalDistance() * 0.2, to.z, 0.75f, 6.0f);
            level.addFreshEntity(potion);
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }
}
