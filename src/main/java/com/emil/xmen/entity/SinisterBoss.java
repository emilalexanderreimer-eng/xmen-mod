package com.emil.xmen.entity;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.power.Power;
import com.emil.xmen.world.ModContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Mister Sinister (Nathaniel Essex), the boss of his underground lab. He fires red energy blasts from his hands,
 * freezes people in place with telepathy, throws them back with telekinesis, takes on the look of whoever he
 * fights, wakes the clones sleeping in his tanks, heals from almost anything, and steals a sample of your
 * mutant genes when he strikes you.
 */
public class SinisterBoss extends Monster implements SinisterLook {
    private static final EntityDataAccessor<Boolean> CASTING =
            SynchedEntityData.defineId(SinisterBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> DISGUISE =
            SynchedEntityData.defineId(SinisterBoss.class, EntityDataSerializers.OPTIONAL_UUID);
    static final DustParticleOptions RED_ENERGY = new DustParticleOptions(new Vector3f(1.0f, 0.08f, 0.12f), 1.4f);
    static final DustParticleOptions DARK_ENERGY = new DustParticleOptions(new Vector3f(0.35f, 0.0f, 0.05f), 1.8f);
    static final DustParticleOptions MIND = new DustParticleOptions(new Vector3f(0.8f, 0.2f, 0.6f), 1.0f);

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("Mister Sinister").withStyle(ChatFormatting.DARK_RED), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10);
    private final Set<UUID> sampled = new HashSet<>();
    private int attackCooldown = 40;
    private int step;
    private int castTicks;
    private int disguiseTicks;
    private boolean greeted;
    private boolean enraged;
    private boolean healed;
    private LivingEntity frozen;
    private Vec3 frozenAt;
    private int frozenTicks;

    public SinisterBoss(EntityType<? extends SinisterBoss> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 150;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ATTACK_DAMAGE, 9);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CASTING, false);
        builder.define(DISGUISE, Optional.empty());
    }

    @Override
    public boolean isCasting() {
        return entityData.get(CASTING);
    }

    @Override
    public boolean isDormant() {
        return false;
    }

    @Override
    public UUID getDisguise() {
        return entityData.get(DISGUISE).orElse(null);
    }

    @Override
    public boolean hasCape() {
        return getDisguise() == null;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, SinisterClone.class));
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
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target.level() != level
                || (target instanceof Player player && (player.isCreative() || player.isSpectator())))) {
            setTarget(null);
            target = null;
        }
        // healing factor: slowly closes his wounds all the time
        if (tickCount % 30 == 0 && getHealth() < getMaxHealth()) {
            heal(1.0f);
        }
        if (tickCount % 3 == 0 && getDisguise() == null) {
            level.sendParticles(DARK_ENERGY, getX(), getY() + 0.2, getZ(), 1, 0.3, 0.1, 0.3, 0);
        }
        tickFreeze(level);
        tickDisguise(level);
        if (castTicks > 0 && --castTicks == 0) {
            entityData.set(CASTING, false);
        }
        if (target == null) {
            return;
        }
        if (!greeted) {
            greeted = true;
            attackCooldown = 40;
            shout(level, "Ahh, visitors. How kind of you to bring me such fine genetic material.");
            sound(level, SoundEvents.EVOKER_PREPARE_SUMMON, 0.6f);
            return;
        }
        if (!healed && getHealth() < getMaxHealth() * 0.3f) {
            healed = true;
            regenerate(level);
            return;
        }
        if (!enraged && getHealth() < getMaxHealth() / 2) {
            enraged = true;
            shout(level, "Enough. Children, wake up. Your father needs you.");
            sound(level, SoundEvents.WITHER_SPAWN, 1.3f);
            wakeClones(level, target, 8);
            attackCooldown = 60;
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

    // ------------------------------------------------------------------ powers
    /** Uses one power. Returns how many ticks to wait before the next one. */
    private int attack(ServerLevel level, LivingEntity target) {
        double distance = distanceTo(target);
        if (distance < 3.0 && step % 2 == 0) {
            step++;
            return shove(level);
        }
        if (distance > 32) {
            return 20;
        }
        return switch (step++ % 6) {
            case 0, 3 -> blast(level, target);
            case 1 -> freeze(level, target);
            case 2 -> wakeClones(level, target, 2);
            case 4 -> shapeshift(level, target);
            default -> blast(level, target);
        };
    }

    private void cast(int ticks) {
        castTicks = ticks;
        entityData.set(CASTING, true);
    }

    /** Red energy blasts from his hands. When he's angry he fires three at once. */
    private int blast(ServerLevel level, LivingEntity target) {
        cast(20);
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.6, 0);
        Vec3 from = getEyePosition().add(getLookAngle().scale(0.8)).add(0, -0.4, 0);
        int count = enraged ? 3 : 1;
        Vec3 side = new Vec3(-(aim.z - from.z), 0, aim.x - from.x).normalize();
        for (int i = 0; i < count; i++) {
            SinisterBlast blast = new SinisterBlast(SinisterEntities.BLAST.get(), level);
            Vec3 spread = side.scale((i - (count - 1) / 2.0) * 1.6);
            blast.setup(this, from, aim.add(spread), 0.9, 7);
            level.addFreshEntity(blast);
        }
        if (random.nextInt(3) == 0) {
            say(level, "Hold still. This will only hurt a great deal.");
        }
        sound(level, SoundEvents.BLAZE_SHOOT, 0.6f);
        return 50;
    }

    /** Takes over the target's mind and holds them frozen in place, then fires a blast while they can't move. */
    private int freeze(ServerLevel level, LivingEntity target) {
        cast(30);
        frozen = target;
        frozenAt = target.position();
        frozenTicks = 60;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 9, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false));
        line(level, getEyePosition(), target.getEyePosition(), MIND);
        say(level, "Your mind is mine. Your body will follow.");
        sound(level, SoundEvents.ILLUSIONER_CAST_SPELL, 0.5f);
        return 25;
    }

    private void tickFreeze(ServerLevel level) {
        if (frozen == null) {
            return;
        }
        if (!frozen.isAlive() || frozen.level() != level || frozenTicks-- <= 0) {
            frozen = null;
            return;
        }
        if (frozen.position().distanceToSqr(frozenAt) > 0.04) {
            frozen.teleportTo(frozenAt.x, frozenAt.y, frozenAt.z);
        }
        frozen.setDeltaMovement(0, Math.min(0, frozen.getDeltaMovement().y), 0);
        frozen.hurtMarked = true;
        if (frozenTicks % 4 == 0) {
            level.sendParticles(MIND, frozen.getX(), frozen.getEyeY() + 0.4, frozen.getZ(), 4, 0.3, 0.1, 0.3, 0);
        }
    }

    /** A telekinetic push that throws everyone close to him across the room. */
    private int shove(ServerLevel level) {
        cast(15);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5),
                e -> e != this && e.isAlive() && !(e instanceof SinisterClone)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            Vec3 push = victim.position().subtract(position());
            push = new Vec3(push.x, 0, push.z);
            push = push.lengthSqr() < 0.01 ? new Vec3(1, 0, 0) : push.normalize();
            victim.setDeltaMovement(push.scale(1.8).add(0, 0.6, 0));
            victim.hurtMarked = true;
            victim.hurt(damageSources().mobAttack(this), 4);
        }
        level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY() + 1, getZ(), 1, 0, 0, 0, 0);
        for (int i = 0; i < 20; i++) {
            double angle = Math.PI * 2 * i / 20;
            level.sendParticles(RED_ENERGY, getX() + Math.cos(angle) * 2, getY() + 1, getZ() + Math.sin(angle) * 2,
                    2, 0.1, 0.3, 0.1, 0);
        }
        sound(level, SoundEvents.WARDEN_SONIC_BOOM, 1.4f);
        return 40;
    }

    /**
     * Wakes clones sleeping in the tanks near him (they smash out of the glass). If none are left, he grows new
     * ones in a flash of red light, but never more than four at a time.
     */
    private int wakeClones(ServerLevel level, LivingEntity target, int count) {
        cast(30);
        List<SinisterClone> clones = level.getEntitiesOfClass(SinisterClone.class, getBoundingBox().inflate(32));
        int awake = (int) clones.stream().filter(c -> !c.isDormant()).count();
        int woken = 0;
        for (SinisterClone clone : clones) {
            if (woken >= count || awake + woken >= (enraged ? 6 : 4)) {
                break;
            }
            if (clone.isDormant()) {
                clone.wake(target);
                woken++;
            }
        }
        if (woken > 0) {
            say(level, woken == 1 ? "Wake, my child." : "Wake, my children.");
            sound(level, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.8f);
            return 70;
        }
        if (awake >= 4) {
            return blast(level, target);
        }
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            SinisterClone clone = new SinisterClone(SinisterEntities.CLONE.get(), level);
            clone.moveTo(getX() + Math.cos(angle) * 2, getY(), getZ() + Math.sin(angle) * 2, getYRot(), 0);
            level.addFreshEntity(clone);
            clone.wake(target);
            level.sendParticles(RED_ENERGY, clone.getX(), clone.getY() + 1, clone.getZ(), 40, 0.3, 0.9, 0.3, 0);
        }
        say(level, "Meet my children. Each one is a perfect copy of me.");
        sound(level, SoundEvents.EVOKER_CAST_SPELL, 0.7f);
        return 90;
    }

    /** Shapeshifts into the target's exact look, blinks behind them and strikes. */
    private int shapeshift(ServerLevel level, LivingEntity target) {
        if (!(target instanceof Player player)) {
            return blast(level, target);
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.3, 0.8, 0.3, 0.02);
        entityData.set(DISGUISE, Optional.of(player.getUUID()));
        setCustomName(player.getName().copy());
        disguiseTicks = 160;
        Vec3 behind = player.position().subtract(player.getLookAngle().multiply(1, 0, 1).normalize().scale(2));
        if (level.noCollision(this, getBoundingBox().move(behind.subtract(position())))) {
            teleportTo(behind.x, behind.y, behind.z);
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 30, 0.3, 0.8, 0.3, 0.02);
        say(level, "Look closer. Which of us is real?");
        sound(level, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8f);
        return 60;
    }

    private void tickDisguise(ServerLevel level) {
        if (disguiseTicks > 0 && --disguiseTicks == 0) {
            dropDisguise(level);
        }
    }

    private void dropDisguise(ServerLevel level) {
        if (getDisguise() == null) {
            return;
        }
        entityData.set(DISGUISE, Optional.empty());
        setCustomName(null);
        disguiseTicks = 0;
        level.sendParticles(RED_ENERGY, getX(), getY() + 1, getZ(), 30, 0.3, 0.8, 0.3, 0);
        sound(level, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.5f);
    }

    /** Once, when nearly beaten, his body knits itself back together. */
    private void regenerate(ServerLevel level) {
        cast(40);
        heal(getMaxHealth() * 0.25f);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 8, 1, true, false));
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.2, getZ(), 12, 0.5, 0.6, 0.5, 0);
        level.sendParticles(RED_ENERGY, getX(), getY() + 1, getZ(), 60, 0.5, 1.0, 0.5, 0);
        shout(level, "Did you really think you could kill me? I have cheated death for over a century.");
        sound(level, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.6f);
        attackCooldown = 40;
    }

    /** When he hits a player, he takes a sample of their mutant genes and heals from it. */
    @Override
    public boolean doHurtTarget(Entity victim) {
        boolean hit = super.doHurtTarget(victim);
        if (hit && victim instanceof ServerPlayer player && level() instanceof ServerLevel level) {
            dropDisguise(level);
            heal(6);
            line(level, getEyePosition(), player.getEyePosition(), RED_ENERGY);
            if (sampled.add(player.getUUID())) {
                Power power = PlayerPowers.get(player);
                say(level, power == null ? "Such ordinary genes. Disappointing."
                        : power.name() + "... Fascinating. Your genes will make a fine addition to my collection.");
                sound(level, SoundEvents.BOTTLE_FILL, 0.6f);
            }
        }
        return hit;
    }

    static void line(ServerLevel level, Vec3 a, Vec3 b, DustParticleOptions particle) {
        int steps = (int) (a.distanceTo(b) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) Math.max(steps, 1));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    // ------------------------------------------------------------------ talking and sounds
    private void say(ServerLevel level, String line) {
        tell(level, Component.literal("Mister Sinister: \"" + line + "\"").withStyle(ChatFormatting.ITALIC,
                ChatFormatting.RED));
    }

    private void shout(ServerLevel level, String line) {
        tell(level, Component.literal("MISTER SINISTER: \"" + line + "\"").withStyle(ChatFormatting.BOLD,
                ChatFormatting.DARK_RED));
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
    protected SoundEvent getAmbientSound() {
        return null;
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
        if (source.getDirectEntity() instanceof SinisterBlast || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && level() instanceof ServerLevel level) {
            dropDisguise(level);
        }
        return hurt;
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
            spawnAtLocation(new ItemStack(ModContent.MUTANT_SERUM.get()));
            spawnAtLocation(new ItemStack(Items.EMERALD, 12));
            spawnAtLocation(new ItemStack(Items.DIAMOND, 4));
            shout(level, "You haven't killed me. You've merely... inconvenienced me. Essex always returns.");
            // without their maker, the clones fall apart
            for (SinisterClone clone : level.getEntitiesOfClass(SinisterClone.class, getBoundingBox().inflate(40),
                    c -> !c.isDormant())) {
                clone.dissolve(level);
            }
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
        tag.putBoolean("Healed", healed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        greeted = tag.getBoolean("Greeted");
        enraged = tag.getBoolean("Enraged");
        healed = tag.getBoolean("Healed");
        // a saved disguise name would outlive the disguise itself
        if (getDisguise() == null) {
            setCustomName(null);
        }
    }
}
