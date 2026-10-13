package com.emil.xmen.entity;

import org.joml.Vector3f;

import com.emil.xmen.ability.TempBlocks;
import com.emil.xmen.world.ModContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Magneto, Master of Magnetism and the boss of Asteroid M. He floats instead of walking, stops arrows in the air
 * and sends them back, rips metal loose and throws it, lifts people up by the iron in their blood and slams them
 * down, pulls metal weapons out of their hands and locks them in iron cages.
 */
public class MagnetoBoss extends Monster {
    private static final EntityDataAccessor<Boolean> CASTING =
            SynchedEntityData.defineId(MagnetoBoss.class, EntityDataSerializers.BOOLEAN);
    static final DustParticleOptions MAGNET = new DustParticleOptions(new Vector3f(0.75f, 0.2f, 0.85f), 1.3f);
    private static final String REFLECTED = "xmen.magneto_reflected";
    private static final BlockState[] METAL = {Blocks.IRON_BLOCK.defaultBlockState(),
            Blocks.ANVIL.defaultBlockState(), Blocks.RAW_IRON_BLOCK.defaultBlockState(),
            Blocks.CAULDRON.defaultBlockState(), Blocks.IRON_BLOCK.defaultBlockState(),
            Blocks.HOPPER.defaultBlockState()};

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Magneto").withStyle(ChatFormatting.DARK_PURPLE), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10);
    private int attackCooldown = 40;
    private int step;
    private int castTicks;
    private boolean greeted;
    private boolean enraged;
    private Vec3 home;
    private LivingEntity lifted;
    private int liftTicks;

    public MagnetoBoss(EntityType<? extends MagnetoBoss> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        setDropChance(EquipmentSlot.HEAD, 0.0f);
        xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ATTACK_DAMAGE, 6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CASTING, false);
    }

    /** True while he holds his hands out to use a power (the model raises its arms). */
    public boolean isCasting() {
        return entityData.get(CASTING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------------ every tick
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        bossBar.setProgress(Math.max(0, getHealth() / getMaxHealth()));
        if (getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModContent.MAGNETO_HELMET.get()));
        }
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target.level() != level
                || (target instanceof Player player && (player.isCreative() || player.isSpectator())))) {
            setTarget(null);
            target = null;
        }
        if (WeaponXItems.isSuppressed(this)) {
            powerless(target);
            return;
        }
        setNoGravity(true);
        hover(target);
        if (tickCount % 2 == 0) {
            level.sendParticles(MAGNET, getX(), getY() - 0.1, getZ(), 2, 0.25, 0.05, 0.25, 0);
        }
        shieldFromArrows(level);
        tickLift(level);
        if (castTicks > 0 && --castTicks == 0) {
            entityData.set(CASTING, false);
        }
        if (target == null) {
            return;
        }
        getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (!greeted) {
            greeted = true;
            attackCooldown = 40;
            shout(level, "Welcome to Asteroid M, my sanctuary. You should not have come here.");
            sound(level, SoundEvents.BEACON_ACTIVATE, 0.6f);
            return;
        }
        if (!enraged && getHealth() < getMaxHealth() / 2) {
            enraged = true;
            shout(level, "Enough! Let me show you what true power is!");
            sound(level, SoundEvents.WITHER_SPAWN, 1.4f);
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 60 * 10, 0, true, false));
            metalStorm(level, target, 8);
            attackCooldown = 90;
            return;
        }
        if (--attackCooldown > 0) {
            return;
        }
        attackCooldown = attack(level, target);
        if (enraged) {
            attackCooldown = attackCooldown * 2 / 3;
        }
    }

    /**
     * Wearing an inhibitor collar (like in his plastic prison in X2) he can't fly or bend metal: he drops to the
     * ground and can only walk up and punch you.
     */
    private void powerless(LivingEntity target) {
        setNoGravity(false);
        entityData.set(CASTING, false);
        lifted = null;
        if (target == null) {
            getNavigation().stop();
            return;
        }
        getLookControl().setLookAt(target, 30.0f, 30.0f);
        getNavigation().moveTo(target, 1.0);
        if (--attackCooldown <= 0 && distanceToSqr(target) < 6.25) {
            swing(InteractionHand.MAIN_HAND);
            doHurtTarget(target);
            attackCooldown = 20;
        }
    }

    /** He floats a few blocks above whoever he fights and slowly circles them. */
    private void hover(LivingEntity target) {
        if (home == null) {
            home = position();
        }
        Vec3 goal;
        if (target == null) {
            goal = home.add(0, Math.sin(tickCount * 0.05) * 0.4, 0);
        } else {
            Vec3 away = position().subtract(target.position());
            Vec3 flat = new Vec3(away.x, 0, away.z);
            flat = flat.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : flat.normalize();
            Vec3 around = flat.add(new Vec3(-flat.z, 0, flat.x).scale(0.3)).normalize();
            goal = target.position().add(around.scale(7)).add(0, 3 + Math.sin(tickCount * 0.08) * 0.5, 0);
        }
        Vec3 move = goal.subtract(position()).scale(0.08);
        if (move.length() > 0.35) {
            move = move.normalize().scale(0.35);
        }
        setDeltaMovement(getDeltaMovement().scale(0.5).add(move.scale(0.5)));
    }

    /** Arrows (and tridents) stop in the air and fly back at whoever shot them. */
    private void shieldFromArrows(ServerLevel level) {
        for (AbstractArrow arrow : level.getEntitiesOfClass(AbstractArrow.class, getBoundingBox().inflate(5),
                a -> a.getOwner() != this && !a.getTags().contains(REFLECTED)
                        && a.getDeltaMovement().lengthSqr() > 0.01)) {
            arrow.addTag(REFLECTED);
            LivingEntity shooter = arrow.getOwner() instanceof LivingEntity living ? living : null;
            arrow.setOwner(this);
            Vec3 back = shooter != null ? shooter.getEyePosition().subtract(arrow.position()).normalize()
                    : arrow.getDeltaMovement().reverse().normalize();
            arrow.shoot(back.x, back.y, back.z, 1.8f, 1.0f);
            level.sendParticles(MAGNET, arrow.getX(), arrow.getY(), arrow.getZ(), 8, 0.15, 0.15, 0.15, 0);
            sound(level, SoundEvents.CHAIN_BREAK, 1.6f);
        }
    }

    // ------------------------------------------------------------------ powers
    /** Uses one power. Returns how many ticks to wait before the next one. */
    private int attack(ServerLevel level, LivingEntity target) {
        double distance = distanceTo(target);
        if (distance < 4.5) {
            repel(level);
            return 50;
        }
        if (distance > 30) {
            return 20;
        }
        return switch (step++ % 5) {
            case 0, 3 -> metalStorm(level, target, 4);
            case 1 -> lift(level, target);
            case 2 -> disarm(level, target);
            default -> cage(level, target);
        };
    }

    private void cast(int ticks) {
        castTicks = ticks;
        entityData.set(CASTING, true);
    }

    /** Pulls chunks of metal out of the floor, spins them around himself and throws them one after another. */
    private int metalStorm(ServerLevel level, LivingEntity target, int count) {
        cast(30 + count * 7);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 3 + random.nextDouble() * 4;
            Vec3 from = new Vec3(getX() + Math.cos(angle) * radius, target.getY(), getZ() + Math.sin(angle) * radius);
            MagnetoDebris debris = new MagnetoDebris(MagnetoEntities.DEBRIS.get(), level);
            debris.setup(this, target, METAL[random.nextInt(METAL.length)], from, i, count, 30 + i * 7);
            level.addFreshEntity(debris);
            level.sendParticles(ParticleTypes.CRIT, from.x, from.y, from.z, 10, 0.3, 0.1, 0.3, 0.1);
        }
        if (random.nextBoolean()) {
            say(level, "Metal bends to my will.");
        }
        sound(level, SoundEvents.ANVIL_PLACE, 0.6f);
        return 70 + count * 7;
    }

    /** Lifts the target into the air by the iron in their blood, holds them there, then slams them down. */
    private int lift(ServerLevel level, LivingEntity target) {
        lifted = target;
        liftTicks = 40;
        cast(40);
        say(level, "Such a fragile thing, the human body... so full of iron.");
        sound(level, SoundEvents.BEACON_POWER_SELECT, 0.6f);
        return 80;
    }

    private void tickLift(ServerLevel level) {
        if (lifted == null) {
            return;
        }
        LivingEntity target = lifted;
        if (!target.isAlive() || target.level() != level || liftTicks <= 0) {
            lifted = null;
            return;
        }
        liftTicks--;
        if (liftTicks > 28) {
            target.setDeltaMovement(0, 0.55, 0);
        } else if (liftTicks > 10) {
            target.setDeltaMovement(0, 0.03, 0);
            target.resetFallDistance();
            if (liftTicks % 10 == 0) {
                target.hurt(damageSources().indirectMagic(this, this), 1.5f);
            }
        } else if (liftTicks == 10) {
            target.setDeltaMovement(0, -3.0, 0);
            target.hurt(damageSources().mobAttack(this), 5);
            sound(level, SoundEvents.ANVIL_LAND, 0.5f);
        }
        target.hurtMarked = true;
        if (liftTicks > 10) {
            MagnetoDebris.beam(level, getEyePosition(), target.position().add(0, target.getBbHeight() / 2, 0));
        }
    }

    /** Rips a metal weapon out of the target's hand. No metal in hand? Then their armor betrays them. */
    private int disarm(ServerLevel level, LivingEntity target) {
        cast(20);
        ItemStack held = target.getMainHandItem();
        Vec3 toMe = position().subtract(target.position()).normalize();
        MagnetoDebris.beam(level, getEyePosition(), target.getEyePosition());
        if (isMetal(held)) {
            target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            ItemEntity item = new ItemEntity(level, target.getX(), target.getEyeY(), target.getZ(), held);
            item.setDeltaMovement(toMe.scale(0.7).add(0, 0.35, 0));
            item.setPickUpDelay(40);
            level.addFreshEntity(item);
            say(level, "You trust in metal? It answers to me.");
            sound(level, SoundEvents.CHAIN_BREAK, 0.8f);
            return 60;
        }
        int metal = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            if (isMetal(target.getItemBySlot(slot))) {
                metal++;
            }
        }
        if (metal > 0) {
            target.hurt(damageSources().indirectMagic(this, this), 3 + metal * 2);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            target.setDeltaMovement(toMe.scale(1.2).add(0, 0.3, 0));
            say(level, "Your armor betrays you.");
        } else {
            target.hurt(damageSources().indirectMagic(this, this), 6);
            target.setDeltaMovement(toMe.scale(-2.0).add(0, 0.6, 0));
        }
        target.hurtMarked = true;
        sound(level, SoundEvents.IRON_GOLEM_DAMAGE, 0.8f);
        return 60;
    }

    /** Builds a cage of iron bars around the target. It melts away after a few seconds. */
    private int cage(ServerLevel level, LivingEntity target) {
        cast(25);
        BlockPos base = target.blockPosition();
        if (!level.getBlockState(base).canBeReplaced() || !level.getBlockState(base.above()).canBeReplaced()) {
            return metalStorm(level, target, 3);
        }
        target.teleportTo(base.getX() + 0.5, target.getY(), base.getZ() + 0.5);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    if (dy == 2) {
                        TempBlocks.place(level, pos, Blocks.IRON_BLOCK.defaultBlockState(), 100);
                    } else if (dx != 0 || dz != 0) {
                        TempBlocks.place(level, pos, Blocks.IRON_BARS.defaultBlockState(), 100);
                    }
                }
            }
        }
        MagnetoDebris.beam(level, getEyePosition(), target.getEyePosition());
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 30, 0.8, 0.8, 0.8,
                0.1);
        say(level, "Perhaps a cage will teach you some manners.");
        sound(level, SoundEvents.ANVIL_PLACE, 1.2f);
        return 70;
    }

    /** Anyone who gets too close is thrown away by a magnetic wave. */
    private void repel(ServerLevel level) {
        cast(15);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(6),
                e -> e != this && e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            Vec3 push = victim.position().subtract(position());
            push = new Vec3(push.x, 0, push.z);
            push = push.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : push.normalize();
            victim.setDeltaMovement(push.scale(2.2).add(0, 0.7, 0));
            victim.hurtMarked = true;
            victim.hurt(damageSources().mobAttack(this), 5);
        }
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            level.sendParticles(MAGNET, getX() + Math.cos(angle) * 2, getY() + 1, getZ() + Math.sin(angle) * 2, 3,
                    0.2, 0.3, 0.2, 0);
        }
        sound(level, SoundEvents.IRON_GOLEM_HURT, 0.6f);
    }

    /** Swords, tools and armor made of iron, gold, chain, netherite and so on. */
    private static boolean isMetal(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem()) {
            return false;
        }
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String metal : new String[] {"iron", "gold", "chain", "netherite", "adamantium", "copper", "magneto"}) {
            if (name.contains(metal)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ talking and sounds
    private void say(ServerLevel level, String line) {
        tell(level, Component.literal("Magneto: \"" + line + "\"").withStyle(ChatFormatting.ITALIC,
                ChatFormatting.LIGHT_PURPLE));
    }

    private void shout(ServerLevel level, String line) {
        tell(level, Component.literal("MAGNETO: \"" + line + "\"").withStyle(ChatFormatting.BOLD,
                ChatFormatting.DARK_PURPLE));
    }

    private void tell(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) < 48 * 48) {
                player.sendSystemMessage(message);
            }
        }
    }

    private void sound(ServerLevel level, SoundEvent sound, float pitch) {
        level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, 1.5f, pitch);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    // ------------------------------------------------------------------ damage, death, boss bar
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean powers = !WeaponXItems.isSuppressed(this);
        if ((powers && source.getDirectEntity() instanceof AbstractArrow)
                || source.getDirectEntity() instanceof MagnetoDebris || source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !isRemoved() && !dead;
        super.die(source);
        if (first && level() instanceof ServerLevel level) {
            spawnAtLocation(new ItemStack(ModContent.MAGNETO_HELMET.get()));
            spawnAtLocation(new ItemStack(Items.IRON_BLOCK, 6));
            spawnAtLocation(new ItemStack(Items.DIAMOND, 4));
            shout(level, "This isn't over... my old friend.");
            bossBar.removeAllPlayers();
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    // ------------------------------------------------------------------ saving
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Greeted", greeted);
        tag.putBoolean("Enraged", enraged);
        if (home != null) {
            tag.putDouble("HomeX", home.x);
            tag.putDouble("HomeY", home.y);
            tag.putDouble("HomeZ", home.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        greeted = tag.getBoolean("Greeted");
        enraged = tag.getBoolean("Enraged");
        if (tag.contains("HomeX")) {
            home = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
        }
        setNoGravity(true);
    }
}
