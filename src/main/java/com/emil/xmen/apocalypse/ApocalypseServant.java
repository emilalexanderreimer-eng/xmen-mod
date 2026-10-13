package com.emil.xmen.apocalypse;

import com.emil.xmen.entity.WeaponXItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Apocalypse's Four Horsemen from the movie (Storm, Psylocke, Archangel, Magneto) and the Clan Akkaba cultists.
 * Each one has its own model, skin and powers.
 */
public class ApocalypseServant extends Monster {
    private final ServantKind kind;
    private final ServerBossEvent bar;
    private int cooldown = 40;
    private int step;
    private boolean armed;

    public ApocalypseServant(EntityType<? extends ApocalypseServant> type, Level level) {
        super(type, level);
        kind = ServantKind.of(type);
        bar = kind.horseman() ? new ServerBossEvent(getDisplayName(), kind.bar, BossEvent.BossBarOverlay.PROGRESS)
                : null;
        xpReward = kind.horseman() ? 40 : 6;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes(ServantKind kind) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, kind.health)
                .add(Attributes.ATTACK_DAMAGE, kind.damage)
                .add(Attributes.MOVEMENT_SPEED, kind.speed)
                .add(Attributes.FOLLOW_RANGE, kind.horseman() ? 32 : 24)
                .add(Attributes.ARMOR, kind.horseman() ? 6 : 2)
                .add(Attributes.KNOCKBACK_RESISTANCE, kind.horseman() ? 0.6 : 0.0);
    }

    public ServantKind kind() {
        return kind;
    }

    @Override
    protected void registerGoals() {
        ServantKind k = ServantKind.of(getType());
        goalSelector.addGoal(0, new FloatGoal(this));
        if (k != ServantKind.ARCHANGEL) {
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        }
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, ApocalypseBoss.class, ApocalypseServant.class)
                .setAlertOthers(ApocalypseServant.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return kind != ServantKind.ARCHANGEL && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && PyramidFx.ally(source.getEntity())) {
            return false;
        }
        if (kind == ServantKind.MAGNETO && source.getDirectEntity() instanceof Projectile projectile
                && !WeaponXItems.isSuppressed(this)) {
            // Magneto stops metal in the air
            if (level() instanceof ServerLevel level) {
                level.sendParticles(PyramidFx.RED, projectile.getX(), projectile.getY(), projectile.getZ(), 10, 0.2,
                        0.2, 0.2, 0);
            }
            amount *= 0.25f;
        }
        return super.hurt(source, amount);
    }

    // ------------------------------------------------------------------ ticking
    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!armed) {
            armed = true;
            if (kind == ServantKind.CULTIST && getMainHandItem().isEmpty()) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
                setDropChance(EquipmentSlot.MAINHAND, 0.0f);
            }
        }
        LivingEntity target = getTarget();
        boolean fighting = target != null && target.isAlive();
        boolean collared = WeaponXItems.isSuppressed(this);
        if (kind == ServantKind.ARCHANGEL) {
            fly(level, fighting && !collared ? target : null);
        }
        if (bar != null && tickCount % 10 == 0) {
            bar.setName(PyramidFx.barTitle(this, ChatFormatting.WHITE));
            PyramidFx.updateBar(level, this, bar, 32, fighting || getHealth() < getMaxHealth());
        }
        if (!fighting || collared || --cooldown > 0) {
            return;
        }
        cooldown = usePower(level, target);
        if (cooldown <= 0) {
            cooldown = 10;
        } else {
            step++;
        }
    }

    /** Archangel hovers above you on his metal wings. */
    private void fly(ServerLevel level, LivingEntity target) {
        if (target == null) {
            setNoGravity(false);
            return;
        }
        setNoGravity(true);
        fallDistance = 0;
        double angle = tickCount * 0.04;
        Vec3 goal = target.position().add(Math.cos(angle) * 5, 4.5, Math.sin(angle) * 5);
        if (step % 4 == 3 && cooldown > 20) {
            goal = target.position().add(0, 1, 0);  // diving at you
        }
        Vec3 move = goal.subtract(position());
        double length = move.length();
        if (length > 0.1) {
            setDeltaMovement(getDeltaMovement().scale(0.6).add(move.scale(Math.min(0.14, 0.5 / length))));
        }
        getLookControl().setLookAt(target, 30, 30);
        if (tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 1, 0.4, 0.2, 0.4, 0.0);
        }
        if (step % 4 == 3 && distanceTo(target) < 2.5 && cooldown > 20) {
            doHurtTarget(target);
            cooldown = 20;
        }
    }

    /** Does a special power. Returns ticks until the next one (0 = nothing happened). */
    private int usePower(ServerLevel level, LivingEntity target) {
        double distance = distanceTo(target);
        boolean sees = hasLineOfSight(target);
        switch (kind) {
            case STORM -> {
                if (step % 3 == 2 && distance < 9) {
                    // a wind blast that throws everyone back
                    for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class,
                            getBoundingBox().inflate(9), e -> e != this && !PyramidFx.ally(e))) {
                        Vec3 push = victim.position().subtract(position()).normalize().scale(2.0);
                        victim.setDeltaMovement(push.x, 0.6, push.z);
                        victim.hurtMarked = true;
                    }
                    level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 60, 3, 0.5, 3, 0.2);
                    PyramidFx.sound(level, this, SoundEvents.ELYTRA_FLYING, 1.0f, 1.6f);
                    return 60;
                }
                if (distance > 24 || !sees) {
                    return 0;
                }
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(target.getX(), target.getY(), target.getZ());
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                target.hurt(damageSources().indirectMagic(this, this), 7);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1, target.getZ(), 30,
                        0.4, 0.8, 0.4, 0.3);
                if (step % 3 == 0) {
                    PyramidFx.say(level, this, "Feel the storm!", ChatFormatting.WHITE);
                }
                return 70;
            }
            case PSYLOCKE -> {
                if (distance > 18) {
                    return 0;
                }
                if (step % 2 == 0) {
                    // psychic dash: she appears right next to you with her blade
                    Vec3 side = target.getLookAngle().yRot((float) Math.PI / 2).multiply(1.5, 0, 1.5);
                    level.sendParticles(PyramidFx.PURPLE, getX(), getY() + 1, getZ(), 25, 0.3, 0.8, 0.3, 0);
                    teleportTo(target.getX() + side.x, target.getY(), target.getZ() + side.z);
                    getLookControl().setLookAt(target);
                    target.hurt(damageSources().mobAttack(this), 9);
                    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    level.sendParticles(PyramidFx.PURPLE, target.getX(), target.getY() + 1, target.getZ(), 30, 0.4,
                            0.6, 0.4, 0);
                    PyramidFx.sound(level, this, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);
                    return 60;
                }
                if (!sees) {
                    return 0;
                }
                // psychic whip pulls you in
                PyramidFx.beam(level, this, target, PyramidFx.PURPLE);
                Vec3 pull = position().subtract(target.position()).normalize().scale(1.3);
                target.setDeltaMovement(pull.x, 0.35, pull.z);
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                PyramidFx.sound(level, this, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0f, 1.3f);
                return 60;
            }
            case ARCHANGEL -> {
                if (distance > 24 || !sees) {
                    return 0;
                }
                if (step % 4 == 3) {
                    PyramidFx.say(level, this, "Death from above!", ChatFormatting.BLUE);
                    return 50;  // the dive happens in fly()
                }
                // a volley of razor-sharp metal feathers
                for (int i = 0; i < 3; i++) {
                    Vec3 aim = target.position().add(random.nextGaussian() * 0.6, 1 + random.nextGaussian() * 0.4,
                            random.nextGaussian() * 0.6);
                    PyramidFx.beam(level, getEyePosition(), aim, PyramidFx.STEEL);
                }
                target.hurt(damageSources().mobAttack(this), 6);
                PyramidFx.sound(level, this, SoundEvents.ARROW_SHOOT, 1.0f, 1.4f);
                return 40;
            }
            case MAGNETO -> {
                if (distance > 24) {
                    return 0;
                }
                switch (step % 3) {
                    case 0 -> {
                        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 1));
                        PyramidFx.say(level, this, "You cannot fight what you cannot touch.",
                                ChatFormatting.DARK_RED);
                    }
                    case 1 -> {
                        Vec3 push = target.position().subtract(position()).normalize().scale(2.2);
                        target.setDeltaMovement(push.x, 0.8, push.z);
                        target.hurtMarked = true;
                        target.hurt(damageSources().indirectMagic(this, this), 7);
                    }
                    default -> {
                        if (target instanceof Player player && !player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
                            // pulls on your armor
                            Vec3 pull = position().subtract(target.position()).normalize().scale(1.6);
                            target.setDeltaMovement(pull.x, 0.3, pull.z);
                            target.hurtMarked = true;
                        }
                        target.hurt(damageSources().indirectMagic(this, this), 5);
                    }
                }
                PyramidFx.beam(level, this, target, PyramidFx.RED);
                PyramidFx.sound(level, this, SoundEvents.ANVIL_LAND, 0.6f, 0.7f);
                return 70;
            }
            default -> {
                return 0;
            }
        }
    }

    // ------------------------------------------------------------------ death and saving
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && kind.horseman()) {
            spawnAtLocation(new ItemStack(Items.GOLD_INGOT, 4));
            spawnAtLocation(new ItemStack(Items.DIAMOND, 1 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(switch (kind) {
                case STORM -> ApocalypseContent.STORM_CRYSTAL.get();
                case PSYLOCKE -> ApocalypseContent.PSYCHIC_BLADE.get();
                case ARCHANGEL -> ApocalypseContent.METAL_WING.get();
                default -> ApocalypseContent.MAGNET_GAUNTLET.get();
            }));
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(this) < 64 * 64) {
                    player.sendSystemMessage(Component.literal(getDisplayName().getString()
                            + ", Horseman of Apocalypse, has fallen!").withStyle(ChatFormatting.GOLD));
                }
            }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bar != null) {
            bar.removePlayer(player);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Armed", armed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        armed = tag.getBoolean("Armed");
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return PyramidFx.ally(other) || super.isAlliedTo(other);
    }
}
