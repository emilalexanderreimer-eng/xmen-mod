package com.emil.xmen.entity;

import com.emil.xmen.world.ModContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Weapon XI, "the Deadpool" from X-Men Origins: Wolverine. Stryker stitched the powers of many mutants into one
 * body: he teleports, fires optic blasts from his eyes, has katana blades that slide out of his arms, heals like
 * Wolverine and cuts arrows out of the air. His mouth is sewn shut, so he never speaks. He waits in his pod at the
 * very bottom of the facility.
 */
public class WeaponXI extends WeaponXMob {
    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(WeaponXI.class, EntityDataSerializers.BOOLEAN);

    private int cooldown = 40;
    private int step;
    private int charge;
    private boolean frenzy;

    public WeaponXI(EntityType<? extends WeaponXI> type, Level level) {
        super(type, level);
        xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400)
                .add(Attributes.MOVEMENT_SPEED, 0.33)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_DAMAGE, 11);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING, false);
    }

    /** True while his eyes glow before an optic blast. */
    public boolean isCharging() {
        return entityData.get(CHARGING);
    }

    @Override
    public int clawStyle() {
        return 3;
    }

    @Override
    protected ServerBossEvent createBossBar() {
        return new ServerBossEvent(Component.literal("Weapon XI").withStyle(ChatFormatting.RED),
                BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);
    }

    @Override
    protected void addBehaviorGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true) {
            @Override
            public boolean canUse() {
                return canAct() && charge == 0 && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
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
                Player near = level.getNearestPlayer(this, 11);
                if (near != null && !near.isCreative() && !near.isSpectator()) {
                    awaken(level);
                    setTarget(near);
                }
            }
            return;
        }
        regenerate(frenzy ? 2.0f : 1.0f);
        if (!powersOn()) {
            if (charge > 0) {
                charge = 0;
                entityData.set(CHARGING, false);
            }
            return;
        }
        cutArrows(level);
        LivingEntity target = getTarget();
        if (charge > 0) {
            getNavigation().stop();
            if (target != null) {
                getLookControl().setLookAt(target, 60.0f, 60.0f);
            }
            if (tickCount % 2 == 0) {
                level.sendParticles(RED_BEAM, getX(), getEyeY(), getZ(), 2, 0.15, 0.05, 0.15, 0);
            }
            if (--charge == 0) {
                entityData.set(CHARGING, false);
                if (target != null) {
                    opticBlast(level, target);
                }
            }
            return;
        }
        if (target == null) {
            return;
        }
        if (!frenzy && getHealth() < getMaxHealth() / 2) {
            frenzy = true;
            narrate(level, "Weapon XI's wounds close by themselves. His eyes burn red", ChatFormatting.RED);
            sound(level, SoundEvents.WITHER_SPAWN, 1.6f);
            cooldown = 20;
        }
        if (--cooldown > 0) {
            return;
        }
        double distance = distanceTo(target);
        if (distance > 32) {
            cooldown = 20;
            return;
        }
        switch (step++ % 3) {
            case 0 -> teleportStrike(level, target);
            case 1 -> startOpticBlast(level);
            default -> {
                if (distance < 6) {
                    bladeSpin(level);
                } else {
                    teleportStrike(level, target);
                }
            }
        }
        cooldown = frenzy ? 35 + random.nextInt(15) : 55 + random.nextInt(20);
    }

    private void awaken(ServerLevel level) {
        breakOutOfTank(level, 2);
        narrate(level, "The pod opens. Weapon XI's blades slide out of his arms", ChatFormatting.RED);
        sound(level, SoundEvents.WARDEN_EMERGE, 1.2f);
    }

    /** Blinks behind the target and cuts. */
    private void teleportStrike(ServerLevel level, LivingEntity target) {
        Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(1.6));
        BlockPos pos = BlockPos.containing(behind);
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                || !level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
            behind = target.position();
        }
        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.3);
        sound(level, SoundEvents.ENDERMAN_TELEPORT, 0.8f);
        teleportTo(behind.x, behind.y, behind.z);
        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 30, 0.3, 0.6, 0.3, 0.3);
        lookAt(target, 360, 360);
        slash(level, target, 9);
    }

    private void startOpticBlast(ServerLevel level) {
        charge = 18;
        entityData.set(CHARGING, true);
        sound(level, SoundEvents.BEACON_ACTIVATE, 1.8f);
    }

    /** A red beam from his eyes, like Cyclops. It burns and goes through other people in its way. */
    private void opticBlast(ServerLevel level, LivingEntity target) {
        Vec3 from = getEyePosition();
        Vec3 to = target.getEyePosition().subtract(0, 0.3, 0);
        Vec3 dir = to.subtract(from).normalize();
        Vec3 end = from.add(dir.scale(30));
        BlockHitResult wall = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        if (wall.getType() == HitResult.Type.BLOCK) {
            end = wall.getLocation();
        }
        trail(level, from, end, RED_BEAM, 0.25);
        level.sendParticles(ParticleTypes.LAVA, end.x, end.y, end.z, 6, 0.2, 0.2, 0.2, 0);
        sound(level, SoundEvents.WARDEN_SONIC_BOOM, 1.5f);
        double length = end.distanceTo(from);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                getBoundingBox().expandTowards(dir.scale(length)).inflate(1),
                e -> e != this && e.isAlive() && !(e instanceof WeaponXMob mob && mob.isStaff()))) {
            Vec3 c = victim.position().add(0, victim.getBbHeight() / 2, 0);
            double along = c.subtract(from).dot(dir);
            if (along < 0 || along > length) {
                continue;
            }
            if (from.add(dir.scale(along)).distanceTo(c) < 1.0) {
                victim.hurt(damageSources().indirectMagic(this, this), 10);
                victim.igniteForSeconds(4);
            }
        }
    }

    /** Spins with both blades out, cutting everyone around him. */
    private void bladeSpin(ServerLevel level) {
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.5),
                e -> e != this && e.isAlive() && !(e instanceof WeaponXMob mob && mob.isStaff()))) {
            slash(level, victim, 8);
            Vec3 push = victim.position().subtract(position()).multiply(1, 0, 1);
            if (push.lengthSqr() > 0.01) {
                victim.setDeltaMovement(push.normalize().scale(1.1).add(0, 0.35, 0));
                victim.hurtMarked = true;
            }
        }
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2 * i / 16;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(angle) * 2.5, getY() + 1,
                    getZ() + Math.sin(angle) * 2.5, 1, 0, 0, 0, 0);
        }
        sound(level, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6f);
    }

    /** Arrows that fly at him get cut in half by his blades most of the time. */
    private void cutArrows(ServerLevel level) {
        for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class, getBoundingBox().inflate(3),
                a -> a.getOwner() != this && a.getDeltaMovement().lengthSqr() > 0.05)) {
            if (random.nextFloat() < 0.6f) {
                level.sendParticles(ParticleTypes.CRIT, arrow.getX(), arrow.getY(), arrow.getZ(), 8, 0.1, 0.1, 0.1,
                        0.2);
                sound(level, SoundEvents.ANVIL_LAND, 2.0f);
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                arrow.discard();
            } else {
                arrow.setDeltaMovement(arrow.getDeltaMovement().scale(0.999));
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (contained && source.getEntity() instanceof Player && level() instanceof ServerLevel level) {
            awaken(level);
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !isRemoved() && !dead;
        super.die(source);
        if (first && level() instanceof ServerLevel level) {
            narrate(level, "Weapon XI is finally down. The Weapon X program is over", ChatFormatting.RED);
            spawnAtLocation(new ItemStack(ModContent.ADAMANTIUM_INGOT.get(), 8));
            spawnAtLocation(new ItemStack(Items.DIAMOND, 5));
            spawnAtLocation(new ItemStack(Items.NETHERITE_SCRAP, 2));
            spawnAtLocation(new ItemStack(WeaponXEntities.CLAWS.get()));
        }
    }
}
