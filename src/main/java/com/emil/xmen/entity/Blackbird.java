package com.emil.xmen.entity;

import com.emil.xmen.world.ModContent;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The X-Jet. Right-click to get in (up to 4 people), the first one flies it: look where you want to go and hold
 * forward to speed up, back to slow down. With nobody flying it, it sinks slowly to the ground.
 * Like a boat, the pilot's own game moves it and tells the server where it is.
 */
public class Blackbird extends Entity {
    private static final double MAX_SPEED = 1.6;
    private static final int SEATS = 4;

    private double speed;
    private int damageTaken;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    public Blackbird(EntityType<? extends Blackbird> type, Level level) {
        super(type, level);
        setNoGravity(true);  // we do our own gravity; this also keeps servers from kicking the pilot for "flying"
        blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < SEATS;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || isPassengerOfSameVehicle(player)) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    /** Punch it a few times (or once in creative) to pick it up again. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved() || isInvulnerableTo(source)) {
            return false;
        }
        if (!(source.getEntity() instanceof Player player)) {
            return false;  // explosions and monsters can't wreck it
        }
        damageTaken += 1;
        playSound(SoundEvents.ANVIL_LAND, 0.4f, 1.6f);
        if (player.getAbilities().instabuild || damageTaken >= 8) {
            if (!player.getAbilities().instabuild) {
                spawnAtLocation(new ItemStack(ModContent.BLACKBIRD.get()));
            }
            ejectPassengers();
            discard();
        }
        return true;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        int seat = getPassengers().indexOf(passenger);
        double forward = seat == 0 ? 2.2 : seat == 1 ? 1.0 : -0.2;
        double side = seat >= 2 ? (seat == 2 ? -0.6 : 0.6) : 0;
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        double x = -Mth.sin(yaw) * forward + Mth.cos(yaw) * side;
        double z = Mth.cos(yaw) * forward + Mth.sin(yaw) * side;
        callback.accept(passenger, getX() + x, getY() + 0.55, getZ() + z);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        // jumping out high up in the air: glide down instead of falling
        if (!level().isClientSide && passenger instanceof LivingEntity living && !onGround()) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(getX() + Mth.cos(yaw) * 3.2, getY() + 0.2, getZ() + Mth.sin(yaw) * 3.2);
    }

    // ------------------------------------------------------------------ flying
    @Override
    public void tick() {
        super.tick();
        tickLerp();
        if (isControlledByLocalInstance()) {
            LivingEntity pilot = getControllingPassenger();
            if (pilot != null) {
                fly(pilot);
            } else {
                drift();
            }
            move(MoverType.SELF, getDeltaMovement());
            if (horizontalCollision && speed > 0.3) {
                speed *= 0.3;
            }
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        if (level().isClientSide && speed > 0.15) {
            float yaw = getYRot() * Mth.DEG_TO_RAD;
            for (int side = -1; side <= 1; side += 2) {
                double x = getX() + Mth.sin(yaw) * 4.2 + Mth.cos(yaw) * 0.9 * side;
                double z = getZ() - Mth.cos(yaw) * 4.2 + Mth.sin(yaw) * 0.9 * side;
                level().addParticle(ParticleTypes.FLAME, x, getY() + 0.8, z, 0, 0, 0);
                level().addParticle(ParticleTypes.SMOKE, x, getY() + 0.8, z, 0, 0, 0);
            }
        }
        if (!level().isClientSide && getControllingPassenger() != null && tickCount % 30 == 0 && speed > 0.1) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL,
                    0.6f, 0.6f);
        }
        checkInsideBlocks();
    }

    private void fly(LivingEntity pilot) {
        float forward = pilot.zza;
        if (forward > 0) {
            speed = Math.min(MAX_SPEED, speed + 0.03);
        } else if (forward < 0) {
            speed = Math.max(0, speed - 0.06);
        } else {
            speed = Math.max(0, speed - 0.004);
        }
        // turn towards where the pilot looks
        float targetYaw = pilot.getYRot();
        float turn = Mth.clamp(Mth.wrapDegrees(targetYaw - getYRot()), -7f, 7f);
        setYRot(getYRot() + turn);
        setXRot(Mth.approach(getXRot(), Mth.clamp(pilot.getXRot(), -80f, 80f), 4f));
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float pitch = getXRot() * Mth.DEG_TO_RAD;
        Vec3 dir = new Vec3(-Mth.sin(yaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
        // low speed: hovers like a VTOL; looking straight up or down moves it up or down
        Vec3 motion = dir.scale(speed);
        if (speed < 0.05 && forward == 0) {
            motion = Vec3.ZERO;
        }
        setDeltaMovement(motion);
        resetFallDistance();
        pilot.resetFallDistance();
    }

    private void drift() {
        speed = Math.max(0, speed - 0.05);
        setXRot(Mth.approach(getXRot(), 0, 3f));
        Vec3 motion = getDeltaMovement();
        double y = onGround() ? 0 : Math.max(-0.4, motion.y - 0.02);
        double friction = onGround() ? 0.5 : 0.95;
        setDeltaMovement(motion.x * friction, y, motion.z * friction);
    }

    public double getSpeed() {
        return speed;
    }

    // ------------------------------------------------------------------ smooth movement for everyone else
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpSteps = 10;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return lerpSteps > 0 ? (float) lerpXRot : getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return lerpSteps > 0 ? (float) lerpYRot : getYRot();
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, lerpXRot);
            lerpSteps--;
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModContent.BLACKBIRD.get());
    }
}
