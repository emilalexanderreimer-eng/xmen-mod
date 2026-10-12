package com.emil.xmen.robot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A flying Danger Room drone: a round metal body with four rotors and a red eye. It circles above you and shoots
 * lasers, so you have to look up and hit it (or throw something at it).
 */
public class LaserDrone extends RobotMob {
    private int shootCooldown = 40;
    private double orbit;

    public LaserDrone(EntityType<? extends LaserDrone> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 6;
        orbit = random.nextDouble() * Math.PI * 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 16)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                RobotMob::huntable));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity target = getTarget();
        Vec3 goal;
        if (target != null) {
            // circle around the target a few blocks up
            orbit += 0.04;
            goal = target.position().add(Math.cos(orbit) * 5, 3.2 + Math.sin(tickCount * 0.05) * 0.6,
                    Math.sin(orbit) * 5);
            getLookControl().setLookAt(target, 30.0f, 30.0f);
            lookAt(target, 30.0f, 30.0f);
        } else {
            goal = position().add(0, Math.sin(tickCount * 0.08) * 0.1, 0);
        }
        BlockPos above = BlockPos.containing(goal);
        if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
            goal = goal.subtract(0, 1.5, 0);
        }
        Vec3 move = goal.subtract(position());
        double length = move.length();
        if (length > 0.05) {
            Vec3 want = move.scale(Math.min(0.35, length) / length);
            setDeltaMovement(getDeltaMovement().scale(0.8).add(want.scale(0.2)));
        } else {
            setDeltaMovement(getDeltaMovement().scale(0.8));
        }
        if (target != null && --shootCooldown <= 0) {
            if (distanceTo(target) < 20 && hasLineOfSight(target)) {
                Vec3 from = position().add(0, getBbHeight() / 2, 0);
                laser(level, from, target, RED_LASER, isTraining() ? 3 : 4, 0);
                shootCooldown = (isTraining() ? 50 : 40) + random.nextInt(20);
            } else {
                shootCooldown = 10;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        setNoGravity(true);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source,
            boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(Items.IRON_NUGGET, 2 + random.nextInt(3)));
        if (random.nextInt(2) == 0) {
            spawnAtLocation(new ItemStack(Items.REDSTONE, 1 + random.nextInt(2)));
        }
    }
}
