package com.emil.xmen.entity;

import java.util.EnumSet;

import org.joml.Vector3f;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Everyone who lives in the Weapon X facility: soldiers, scientists, Colonel Stryker and his mutants. They work
 * together (when one of them spots you, the ones nearby come too), never fight each other, and all of them go
 * after Logan once he breaks out of his tank. Some of them start locked in a tank ("contained") and can't be hurt
 * until they break out.
 */
public abstract class WeaponXMob extends Monster {
    static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.6f, 0.0f, 0.0f), 1.0f);
    static final DustParticleOptions RED_BEAM = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.05f), 1.6f);

    protected boolean contained;
    private boolean greeted;
    private int alertCooldown;
    private ServerBossEvent bossBar;

    protected WeaponXMob(EntityType<? extends WeaponXMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    // ------------------------------------------------------------------ what kind of character this is
    /** False only for Logan: he isn't one of them. */
    public boolean isStaff() {
        return true;
    }

    /** Scientists run from players instead of fighting them. */
    protected boolean huntsPlayers() {
        return true;
    }

    /** Soldiers and Stryker hold their gun up to aim. */
    public boolean aimsGun() {
        return false;
    }

    /** Jason sits in his wheelchair. */
    public boolean sits() {
        return false;
    }

    /** Which claws the model shows: 0 none, 1 Wolverine's three, 2 Lady Deathstrike's five, 3 Weapon XI's blades. */
    public int clawStyle() {
        return 0;
    }

    /** The boss bar, or null for normal people. */
    protected ServerBossEvent createBossBar() {
        return null;
    }

    /** The first time this character sees someone to fight. */
    protected void greet(ServerLevel level, LivingEntity target) {
    }

    /** False while an inhibitor collar has shut this character's powers off. */
    protected boolean powersOn() {
        return !WeaponXItems.isSuppressed(this);
    }

    public boolean canAct() {
        return !contained;
    }

    public boolean isContained() {
        return contained;
    }

    // ------------------------------------------------------------------ goals
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return canAct() && super.canUse();
            }
        });
        addBehaviorGoals();
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return canAct() && super.canUse();
            }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                player -> canAct() && huntsPlayers()));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, WeaponXLogan.class, 10, true, false,
                logan -> canAct() && isStaff() && !((WeaponXLogan) logan).isContained()));
    }

    /** Movement and fighting goals for this kind of character. */
    protected abstract void addBehaviorGoals();

    @Override
    public void setTarget(LivingEntity target) {
        LivingEntity old = getTarget();
        super.setTarget(target);
        if (target instanceof Player && old == null && isStaff() && level() instanceof ServerLevel level) {
            if (!greeted) {
                greeted = true;
                greet(level, target);
            }
            if (alertCooldown <= 0) {
                alertCooldown = 100;
                alertNearby(target, 20);
            }
        }
    }

    /** Everyone from the facility close by comes after the target too. */
    protected void alertNearby(LivingEntity target, double range) {
        for (WeaponXMob other : level().getEntitiesOfClass(WeaponXMob.class, getBoundingBox().inflate(range),
                m -> m != this && m.isStaff() && m.canAct() && m.huntsPlayers() && m.getTarget() == null)) {
            other.setTarget(target);
        }
    }

    // ------------------------------------------------------------------ every tick
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (alertCooldown > 0) {
            alertCooldown--;
        }
        if (bossBar != null) {
            bossBar.setProgress(Math.max(0, getHealth() / getMaxHealth()));
        }
        LivingEntity target = getTarget();
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            setTarget(null);
        }
        if (contained) {
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
        }
    }

    /** Heals a little every second (healing factor). */
    protected void regenerate(float perSecond) {
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth() && isAlive() && powersOn()) {
            heal(perSecond);
        }
    }

    // ------------------------------------------------------------------ tanks
    /** Smashes the glass (and water) of the tank around this character and sets them free. */
    protected void breakOutOfTank(ServerLevel level, int radius) {
        contained = false;
        setNoGravity(false);
        BlockPos base = blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(base.offset(-radius, -1, -radius), base.offset(radius, 4, radius))) {
            BlockState state = level.getBlockState(pos);
            String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
            if (name.contains("glass")) {
                level.destroyBlock(pos, false);
            } else if (state.is(Blocks.WATER)) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
        level.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 80, 1.2, 1.0, 1.2, 0.2);
        sound(level, SoundEvents.GLASS_BREAK, 0.7f);
        sound(level, SoundEvents.GLASS_BREAK, 0.5f);
    }

    // ------------------------------------------------------------------ fighting helpers
    /**
     * Fires one gun shot at the target: a bullet trail, a bang, and damage if it hits. Walls stop bullets.
     * Returns true if it hit.
     */
    protected boolean shoot(ServerLevel level, LivingEntity target, float damage, float accuracy) {
        swing(InteractionHand.MAIN_HAND);
        Vec3 from = getEyePosition().add(getLookAngle().scale(0.6)).add(0, -0.15, 0);
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        boolean blocked = wall.getType() == HitResult.Type.BLOCK;
        boolean hit = !blocked && random.nextFloat() < accuracy;
        Vec3 end = blocked ? wall.getLocation() : hit ? to
                : to.add((random.nextDouble() - 0.5) * 2, (random.nextDouble() - 0.5) * 1.5,
                        (random.nextDouble() - 0.5) * 2);
        trail(level, from, end, ParticleTypes.CRIT, 0.9);
        level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 3, 0.05, 0.05, 0.05, 0.01);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.2f,
                1.7f + random.nextFloat() * 0.2f);
        if (blocked) {
            level.sendParticles(ParticleTypes.SMOKE, end.x, end.y, end.z, 4, 0.05, 0.05, 0.05, 0.02);
        }
        if (hit && damage > 0) {
            target.hurt(damageSources().mobAttack(this), damage);
        }
        return hit;
    }

    /** A line of particles from one point to another. */
    protected static void trail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double step) {
        Vec3 dir = to.subtract(from);
        double length = dir.length();
        if (length < 0.01) {
            return;
        }
        dir = dir.scale(1 / length);
        for (double d = 0; d < length; d += step) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** A claw or blade hit with blood and a slash sound. */
    protected void slash(ServerLevel level, LivingEntity target, float damage) {
        swing(InteractionHand.MAIN_HAND);
        if (target.hurt(damageSources().mobAttack(this), damage)) {
            level.sendParticles(BLOOD, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 10,
                    0.3, 0.3, 0.3, 0);
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0,
                0);
        sound(level, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8f + random.nextFloat() * 0.3f);
    }

    /** Jumps at the target. */
    protected void leapAt(LivingEntity target, double power, double up) {
        Vec3 dir = target.position().subtract(position());
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() < 0.01) {
            return;
        }
        double distance = flat.length();
        Vec3 push = flat.normalize().scale(Math.min(power, 0.25 + distance * 0.12));
        setDeltaMovement(push.x, up, push.z);
        hasImpulse = true;
    }

    // ------------------------------------------------------------------ talking and sounds
    protected void say(ServerLevel level, String name, String line, ChatFormatting color) {
        tell(level, Component.literal(name + ": \"" + line + "\"").withStyle(ChatFormatting.ITALIC, color));
    }

    /** Something happening, written like "* Lady Deathstrike extends her claws *". */
    protected void narrate(ServerLevel level, String text, ChatFormatting color) {
        tell(level, Component.literal("* " + text + " *").withStyle(ChatFormatting.ITALIC, color));
    }

    protected void tell(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) < 40 * 40) {
                player.sendSystemMessage(message);
            }
        }
    }

    protected void sound(ServerLevel level, SoundEvent sound, float pitch) {
        level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, 1.4f, pitch);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    // ------------------------------------------------------------------ damage, boss bar, saving
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (contained && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (bossBar != null) {
            bossBar.removeAllPlayers();
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (bossBar == null) {
            bossBar = createBossBar();
        }
        if (bossBar != null) {
            bossBar.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossBar != null) {
            bossBar.removePlayer(player);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Contained", contained);
        tag.putBoolean("Greeted", greeted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        contained = tag.getBoolean("Contained");
        greeted = tag.getBoolean("Greeted");
        setNoGravity(contained);
    }

    // ------------------------------------------------------------------ shared goals
    /**
     * Soldiers and Stryker: keep some distance, strafe from side to side and shoot. They close in when they can't
     * see their target, back off when it gets too close, and hit with the rifle when it's right next to them.
     */
    static class GunGoal extends Goal {
        private final WeaponXMob mob;
        private final double speed;
        private final double near;
        private final double far;
        private int strafeTicks;
        private boolean strafeLeft;
        private int meleeCooldown;

        GunGoal(WeaponXMob mob, double speed, double near, double far) {
            this.mob = mob;
            this.speed = speed;
            this.near = near;
            this.far = far;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = mob.getTarget();
            return mob.canAct() && target != null && target.isAlive();
        }

        @Override
        public void start() {
            mob.setAggressive(true);
        }

        @Override
        public void stop() {
            mob.setAggressive(false);
            mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = mob.getTarget();
            if (target == null || !(mob.level() instanceof ServerLevel level)) {
                return;
            }
            double distance = mob.distanceTo(target);
            boolean sees = mob.getSensing().hasLineOfSight(target);
            mob.getLookControl().setLookAt(target, 30.0f, 30.0f);
            if (!sees || distance > far) {
                mob.getNavigation().moveTo(target, speed);
            } else if (distance < near) {
                Vec3 away = DefaultRandomPos.getPosAway((PathfinderMob) mob, 8, 4, target.position());
                if (away != null) {
                    mob.getNavigation().moveTo(away.x, away.y, away.z, speed * 1.15);
                }
            } else {
                mob.getNavigation().stop();
                if (++strafeTicks >= 25) {
                    strafeTicks = 0;
                    if (mob.getRandom().nextFloat() < 0.4f) {
                        strafeLeft = !strafeLeft;
                    }
                }
                mob.getMoveControl().strafe(0.0f, strafeLeft ? 0.45f : -0.45f);
                mob.lookAt(target, 30.0f, 30.0f);
            }
            if (meleeCooldown > 0) {
                meleeCooldown--;
            }
            if (distance < 2.3 && meleeCooldown == 0) {
                meleeCooldown = 20;
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(target);
            } else if (sees && distance <= far + 6) {
                mob.gunTick(level, target, distance);
            }
        }
    }

    /** Called every tick while a gun goal has a target in sight. */
    protected void gunTick(ServerLevel level, LivingEntity target, double distance) {
    }
}
