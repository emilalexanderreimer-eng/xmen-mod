package com.emil.xmen.entity;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A copy of a player made by a power. Fighters attack monsters, illusions wander around to distract them,
 * holograms stand still and glow. Clones are never saved and vanish when their time runs out.
 */
public class PowerClone extends PathfinderMob {
    public static final int FIGHTER = 0;
    public static final int SHADOW = 1;
    public static final int ILLUSION = 2;
    public static final int HOLOGRAM = 3;

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(PowerClone.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(PowerClone.class, EntityDataSerializers.INT);

    private int lifetime = 600;

    public PowerClone(EntityType<? extends PowerClone> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    public void setup(Player owner, int variant, int lifetimeTicks) {
        entityData.set(OWNER, Optional.of(owner.getUUID()));
        entityData.set(VARIANT, variant);
        lifetime = lifetimeTicks;
        if (variant == ILLUSION || variant == HOLOGRAM) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(1);
            setHealth(1);
        }
        if (variant == HOLOGRAM) {
            setNoAi(true);
            setGlowingTag(true);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER, Optional.empty());
        builder.define(VARIANT, FIGHTER);
    }

    public UUID getOwnerId() {
        return entityData.get(OWNER).orElse(null);
    }

    public int getVariant() {
        return entityData.get(VARIANT);
    }

    private boolean fights() {
        int variant = getVariant();
        return variant == FIGHTER || variant == SHADOW;
    }

    private Player owner() {
        UUID id = getOwnerId();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() {
                return fights() && super.canUse();
            }
        });
        goalSelector.addGoal(2, new FollowOwnerGoal());
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true) {
            @Override
            public boolean canUse() {
                return fights() && super.canUse();
            }
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            if (tickCount > lifetime || getOwnerId() == null) {
                server.sendParticles(ParticleTypes.POOF, getX(), getY() + 1, getZ(), 10, 0.3, 0.5, 0.3, 0.02);
                discard();
                return;
            }
            if (getVariant() == SHADOW && tickCount % 5 == 0) {
                server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1, getZ(), 3, 0.3, 0.6, 0.3, 0.01);
            }
            LivingEntity target = getTarget();
            if (target != null && target.getUUID().equals(getOwnerId())) {
                setTarget(null);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && source.getEntity().getUUID().equals(getOwnerId())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    private class FollowOwnerGoal extends Goal {
        @Override
        public boolean canUse() {
            Player owner = owner();
            return owner != null && getTarget() == null && distanceToSqr(owner) > 36;
        }

        @Override
        public void tick() {
            Player owner = owner();
            if (owner == null) {
                return;
            }
            if (distanceToSqr(owner) > 1600) {
                teleportTo(owner.getX(), owner.getY(), owner.getZ());
            } else {
                getNavigation().moveTo(owner, 1.2);
            }
        }
    }
}
