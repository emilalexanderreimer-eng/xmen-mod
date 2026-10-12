package com.emil.xmen.sentinel;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import org.joml.Vector3f;

import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.power.Power;
import com.emil.xmen.robot.RobotEntities;
import com.emil.xmen.robot.Sentinel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Sentinel Mark X ({@code xmen:sentinel_mark_x}): the black future Sentinel from Days of Future Past and the
 * boss of the Sentinel factory. It sleeps in its assembly cradle until you come close, then uses copied mutant
 * powers (ice and fire), fires its eye laser, slams the ground, grabs and throws you, and its plating ADAPTS to
 * whatever hurts it most, so you have to keep switching how you fight. Halfway down it calls in two Mark I
 * Sentinels ({@link Sentinel}, the same ones as in the factory and the Danger Room).
 * <p>
 * Its core builds a Sentinel Control Chip, which makes a smaller, reprogrammed Mark X ("Ally") that protects you.
 */
public class SentinelMarkX extends Monster {
    private static final EntityDataAccessor<Boolean> AWAKE =
            SynchedEntityData.defineId(SentinelMarkX.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ALLY =
            SynchedEntityData.defineId(SentinelMarkX.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTION =
            SynchedEntityData.defineId(SentinelMarkX.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ADAPTED =
            SynchedEntityData.defineId(SentinelMarkX.class, EntityDataSerializers.INT);

    /** What the arms are doing, for the model. */
    public static final int IDLE = 0;
    public static final int AIM = 1;
    public static final int RAISE = 2;
    public static final int GRAB = 3;

    static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1.0f, 0.12f, 0.1f), 1.4f);
    static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.25f, 0.6f, 1.0f), 1.4f);
    static final DustParticleOptions ORANGE = new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.1f), 1.6f);

    private enum Move { LASER, SLAM, GRAB, ICE, FIRE }

    private ServerBossEvent bossBar;
    private final int[] hits = new int[Threat.values().length];
    private final Set<UUID> scanned = new HashSet<>();
    private UUID owner;
    private boolean staying;
    private boolean enraged;
    private int attackCooldown = 60;
    private int step;
    private Move pending;
    private int windup;
    private LivingEntity grabbed;
    private int grabTicks;
    private int actionTicks;

    public SentinelMarkX(EntityType<? extends SentinelMarkX> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 300;
        if (!level.isClientSide()) {
            bossBar = new ServerBossEvent(Component.literal("Sentinel Mark X").withStyle(ChatFormatting.DARK_RED),
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_12);
            bossBar.setVisible(false);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 14)
                .add(Attributes.ARMOR_TOUGHNESS, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 16)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0)
                .add(Attributes.STEP_HEIGHT, 2.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AWAKE, false);
        builder.define(ALLY, false);
        builder.define(ACTION, IDLE);
        builder.define(ADAPTED, -1);
    }

    // ------------------------------------------------------------------ what kind of Sentinel this is
    /** The factory boss. Your own reprogrammed one is not. */
    public boolean isBoss() {
        return !isAlly();
    }

    /** The Mark X sleeps until somebody comes close; everybody else is always awake. */
    public boolean isAwake() {
        return !isBoss() || entityData.get(AWAKE);
    }

    public boolean isAlly() {
        return entityData.get(ALLY);
    }

    public int action() {
        return entityData.get(ACTION);
    }

    /** What the Mark X's plating has adapted to, or null. */
    public Threat adapted() {
        int index = entityData.get(ADAPTED);
        return index < 0 ? null : Threat.values()[index];
    }

    /** Reprograms this Sentinel to protect the player instead of hunting mutants. */
    public void makeAlly(Player player) {
        entityData.set(ALLY, true);
        owner = player.getUUID();
        setTarget(null);
        allyStats();
        setHealth(getMaxHealth());
        setCustomName(Component.literal(player.getGameProfile().getName() + "'s Sentinel")
                .withStyle(ChatFormatting.AQUA));
        xpReward = 0;
    }

    /** Half the size of the boss (still 3 blocks tall) and not as strong. */
    private void allyStats() {
        getAttribute(Attributes.SCALE).setBaseValue(0.5);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(150);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);
        getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.1);
        refreshDimensions();
        xpReward = 0;
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAllPlayers();
        }
    }

    public Player getOwner() {
        return owner == null ? null : level().getPlayerByUUID(owner);
    }

    private boolean isOwner(Entity entity) {
        return owner != null && entity != null && owner.equals(entity.getUUID());
    }

    /** Who this Sentinel is allowed to hurt with lasers and shockwaves. */
    private boolean isFoe(LivingEntity entity) {
        if (entity == this || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player player) {
            return !isAlly() && !player.isCreative() && !player.isSpectator();
        }
        if (entity instanceof SentinelMarkX other) {
            return other.isAlly() != isAlly();
        }
        if (entity instanceof Sentinel) {
            return isAlly();
        }
        return isAlly() ? entity instanceof Enemy : entity == getTarget();
    }

    // ------------------------------------------------------------------ AI
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, when(this::canFight, new MeleeAttackGoal(this, 1.0, true)));
        goalSelector.addGoal(3, new FollowOwnerGoal());
        goalSelector.addGoal(5, when(() -> isAwake() && !isAlly() && pending == null,
                new WaterAvoidingRandomStrollGoal(this, 0.6)));
        goalSelector.addGoal(6, when(this::isAwake, new LookAtPlayerGoal(this, Player.class, 16.0f)));
        goalSelector.addGoal(7, when(this::isAwake, new RandomLookAroundGoal(this)));
        targetSelector.addGoal(1, when(this::isAwake, new HurtByTargetGoal(this, SentinelMarkX.class) {
            @Override
            public boolean canUse() {
                return !isOwner(getLastHurtByMob()) && super.canUse();
            }
        }));
        targetSelector.addGoal(2, new DefendOwnerGoal());
        targetSelector.addGoal(3, when(() -> isAwake() && !isAlly(), new NearestAttackableTargetGoal<>(this,
                Player.class, true)));
        targetSelector.addGoal(3, when(() -> isAwake() && !isAlly(), new NearestAttackableTargetGoal<>(this,
                SentinelMarkX.class, 10, true, false, e -> ((SentinelMarkX) e).isAlly())));
        targetSelector.addGoal(4, when(this::isAlly, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true,
                false, e -> e instanceof Enemy && !(e instanceof SentinelMarkX s && s.isAlly()))));
    }

    private boolean canFight() {
        return isAwake() && pending == null && grabbed == null;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (bossBar != null) {
            bossBar.setProgress(Math.max(0, getHealth() / getMaxHealth()));
        }
        if (actionTicks > 0 && --actionTicks == 0 && pending == null && grabbed == null) {
            entityData.set(ACTION, IDLE);
        }
        if (!isAwake()) {
            sleep(level);
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target.level() != level || isOwner(target)
                || (target instanceof Player player && (player.isCreative() || player.isSpectator()
                        || isAlly())))) {
            setTarget(null);
            target = null;
        }
        if (isAlly() && getHealth() < getMaxHealth() && tickCount % 40 == 0) {
            heal(1);
        }
        tickGrab(level);
        if (pending != null) {
            getNavigation().stop();
            if (target != null) {
                getLookControl().setLookAt(target, 30.0f, 30.0f);
            }
            if (--windup <= 0) {
                Move move = pending;
                pending = null;
                if (target != null) {
                    use(level, move, target);
                }
            }
            return;
        }
        if (target == null) {
            return;
        }
        scan(level, target);
        if (isBoss() && !enraged && getHealth() < getMaxHealth() / 2) {
            enrage(level);
            return;
        }
        if (--attackCooldown > 0 || grabbed != null) {
            return;
        }
        double distance = distanceTo(target);
        if (!isBoss()) {
            // your own Sentinel: punches up close (the melee goal does that), eye laser from further away
            if (distance > 4 && distance < 28 && hasLineOfSight(target)) {
                prepare(Move.LASER, 15, AIM);
                attackCooldown = 70 + random.nextInt(30);
            } else {
                attackCooldown = 20;
            }
            return;
        }
        Move move;
        if (distance < 7) {
            move = step++ % 2 == 0 ? Move.SLAM : Move.GRAB;
        } else if (distance < 32) {
            move = switch (step++ % 4) {
                case 0, 2 -> Move.LASER;
                case 1 -> Move.ICE;
                default -> Move.FIRE;
            };
        } else {
            attackCooldown = 20;
            return;
        }
        switch (move) {
            case LASER -> prepare(move, 20, AIM);
            case SLAM -> {
                prepare(move, 22, RAISE);
                say(level, "TARGET IN RANGE. ENGAGING.");
            }
            case GRAB -> prepare(move, 12, GRAB);
            case ICE -> {
                prepare(move, 20, AIM);
                say(level, "ADAPTING MUTANT ABILITY: CRYOKINESIS.");
            }
            default -> {
                prepare(move, 20, AIM);
                say(level, "ADAPTING MUTANT ABILITY: PYROKINESIS.");
            }
        }
        attackCooldown = enraged ? 35 : 55;
    }

    private void prepare(Move move, int ticks, int action) {
        pending = move;
        windup = ticks;
        entityData.set(ACTION, action);
        actionTicks = ticks + 12;
        if (level() instanceof ServerLevel level) {
            sound(level, SoundEvents.BEACON_POWER_SELECT, move == Move.LASER ? 1.8f : 0.7f);
        }
    }

    /** The sleeping Mark X waits in its cradle. A player coming close (or hitting it) switches it on. */
    private void sleep(ServerLevel level) {
        getNavigation().stop();
        setTarget(null);
        if (tickCount % 20 == 0) {
            Player near = level.getNearestPlayer(this, 14);
            if (near != null && !near.isCreative() && !near.isSpectator()) {
                wake(level);
            }
        }
    }

    private void wake(ServerLevel level) {
        if (isAwake()) {
            return;
        }
        entityData.set(AWAKE, true);
        attackCooldown = 50;
        if (bossBar != null) {
            bossBar.setVisible(true);
        }
        tell(level, Component.literal("Bolivar Trask: \"Activate the prototype. Let's see what it can do.\"")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        shout(level, "SENTINEL MARK X ONLINE. SCANNING FOR MUTANTS.");
        sound(level, SoundEvents.BEACON_ACTIVATE, 0.5f);
        sound(level, SoundEvents.IRON_GOLEM_REPAIR, 0.5f);
        level.sendParticles(ORANGE, getX(), getY() + getBbHeight() * 0.8, getZ(), 40, 0.6, 0.6, 0.6, 0);
    }

    /** The first time a Sentinel locks onto a player, it reads their mutant gene out loud. */
    private void scan(ServerLevel level, LivingEntity target) {
        if (isAlly() || !(target instanceof ServerPlayer player) || !scanned.add(player.getUUID())) {
            return;
        }
        Power power = PlayerPowers.get(player);
        MutableComponent line = Component.literal((isBoss() ? "SENTINEL MARK X" : "SENTINEL")
                + ": MUTANT DETECTED. GENE: ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        line.append(power != null ? power.title() : Component.literal("UNKNOWN"));
        line.append(Component.literal(". TERMINATE.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        player.sendSystemMessage(line);
        sound(level, SoundEvents.BEACON_AMBIENT, 1.6f);
    }

    private void enrage(ServerLevel level) {
        enraged = true;
        shout(level, "DAMAGE CRITICAL. ADAPTIVE PROTOCOL OMEGA. DEPLOYING REINFORCEMENTS.");
        sound(level, SoundEvents.WITHER_SPAWN, 1.6f);
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60 * 20, 0, true, false));
        for (int i = 0; i < 2; i++) {
            Sentinel helper = new Sentinel(RobotEntities.SENTINEL.get(), level);
            double angle = random.nextDouble() * Math.PI * 2;
            helper.moveTo(getX() + Math.cos(angle) * 5, getY(), getZ() + Math.sin(angle) * 5, getYRot(), 0);
            helper.setTarget(getTarget());
            level.addFreshEntity(helper);
            level.sendParticles(ParticleTypes.EXPLOSION, helper.getX(), helper.getY() + 1, helper.getZ(), 3, 0.5,
                    0.5, 0.5, 0);
        }
        attackCooldown = 60;
    }

    // ------------------------------------------------------------------ attacks
    private void use(ServerLevel level, Move move, LivingEntity target) {
        switch (move) {
            case LASER -> laser(level, target);
            case SLAM -> slam(level);
            case GRAB -> grab(level, target);
            case ICE -> blast(level, target, false);
            case FIRE -> blast(level, target, true);
        }
    }

    private float laserDamage() {
        return isBoss() ? 10 : 7;
    }

    /** A beam from the eyes. Walls block it, so hiding behind something works. */
    private void laser(ServerLevel level, LivingEntity target) {
        Vec3 from = getEyePosition();
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.6, 0);
        BlockHitResult wall = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                this));
        boolean blocked = wall.getType() == HitResult.Type.BLOCK;
        if (blocked) {
            to = wall.getLocation();
        }
        beam(level, from, to, isAlly() ? BLUE : ORANGE, isBoss() ? 0.25 : 0.4);
        sound(level, SoundEvents.GUARDIAN_ATTACK, 1.6f);
        if (!blocked) {
            target.hurt(damageSources().mobAttack(this), laserDamage());
            if (isBoss()) {
                target.igniteForSeconds(3);
            }
        }
        level.sendParticles(ParticleTypes.LAVA, to.x, to.y, to.z, 3, 0.1, 0.1, 0.1, 0);
    }

    /** Both fists hit the floor: a shockwave throws everyone nearby into the air. */
    private void slam(ServerLevel level) {
        double radius = 7;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 2,
                radius), this::isFoe)) {
            Vec3 push = victim.position().subtract(position());
            push = new Vec3(push.x, 0, push.z);
            double distance = push.length();
            if (distance > radius) {
                continue;
            }
            push = distance < 0.01 ? new Vec3(1, 0, 0) : push.normalize();
            victim.hurt(damageSources().mobAttack(this), 12);
            victim.setDeltaMovement(push.scale(1.6).add(0, 0.9, 0));
            victim.hurtMarked = true;
        }
        for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2 * i / 32;
            for (double r = 2; r <= radius; r += 2.5) {
                double x = getX() + Math.cos(angle) * r;
                double z = getZ() + Math.sin(angle) * r;
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, getY() + 0.2, z, 1, 0.1, 0.05, 0.1, 0.01);
                BlockState floor = level.getBlockState(BlockPos.containing(x, getY() - 0.5, z));
                if (!floor.isAir() && i % 4 == 0) {
                    level.sendParticles(ParticleTypes.EXPLOSION, x, getY() + 0.3, z, 1, 0, 0, 0, 0);
                }
            }
        }
        sound(level, SoundEvents.GENERIC_EXPLODE.value(), 0.6f);
        sound(level, SoundEvents.ANVIL_LAND, 0.5f);
    }

    /** Picks the target up in one giant hand, squeezes, then throws them across the room. */
    private void grab(ServerLevel level, LivingEntity target) {
        if (distanceTo(target) > 8) {
            laser(level, target);
            return;
        }
        grabbed = target;
        grabTicks = 45;
        entityData.set(ACTION, GRAB);
        say(level, "MUTANT CAPTURED.");
        sound(level, SoundEvents.IRON_GOLEM_ATTACK, 0.6f);
    }

    private void tickGrab(ServerLevel level) {
        if (grabbed == null) {
            return;
        }
        LivingEntity target = grabbed;
        if (!target.isAlive() || target.level() != level || grabTicks <= 0
                || (target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            grabbed = null;
            entityData.set(ACTION, IDLE);
            return;
        }
        grabTicks--;
        getNavigation().stop();
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        Vec3 hand = position().add(look.scale(getBbWidth() * 0.9)).add(0, getBbHeight() * 0.55, 0);
        if (grabTicks > 0) {
            target.teleportTo(hand.x, hand.y, hand.z);
            target.setDeltaMovement(Vec3.ZERO);
            target.resetFallDistance();
            if (grabTicks % 10 == 0) {
                target.hurt(damageSources().mobAttack(this), 2);
                sound(level, SoundEvents.IRON_GOLEM_DAMAGE, 1.2f);
            }
        } else {
            target.setDeltaMovement(look.scale(2.4).add(0, 0.9, 0));
            target.hurt(damageSources().mobAttack(this), 6);
            sound(level, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.6f);
            grabbed = null;
            entityData.set(ACTION, IDLE);
        }
        target.hurtMarked = true;
    }

    /** A cone of ice (copied from Iceman) or fire (copied from Sunspot) out of the open face. */
    private void blast(ServerLevel level, LivingEntity target, boolean fire) {
        Vec3 from = getEyePosition();
        Vec3 aim = target.getEyePosition().subtract(from).normalize();
        ParticleOptions particle = fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE;
        for (double d = 1; d < 16; d += 0.6) {
            double spread = d * 0.18;
            Vec3 at = from.add(aim.scale(d));
            level.sendParticles(particle, at.x, at.y, at.z, 4, spread, spread, spread, 0.02);
        }
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16),
                this::isFoe)) {
            Vec3 to = victim.getEyePosition().subtract(from);
            if (to.length() > 16 || to.normalize().dot(aim) < 0.88 || !hasLineOfSight(victim)) {
                continue;
            }
            victim.hurt(fire ? damageSources().mobAttack(this) : damageSources().freeze(), 7);
            if (fire) {
                victim.igniteForSeconds(6);
            } else {
                victim.setTicksFrozen(Math.max(victim.getTicksFrozen(), 260));
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
            }
        }
        sound(level, fire ? SoundEvents.BLAZE_SHOOT : SoundEvents.GLASS_BREAK, 0.6f);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.35, 0));
            entityData.set(ACTION, RAISE);
            actionTicks = 8;
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0f, isBoss() ? 0.5f : 0.8f);
        }
        return hit;
    }

    // ------------------------------------------------------------------ getting hurt and adapting
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN)
                || (isAlly() && isOwner(source.getEntity()))
                || (source.getEntity() instanceof SentinelMarkX other && other.isAlly() == isAlly())) {
            return false;
        }
        if (level() instanceof ServerLevel level && !isAwake() && source.getEntity() != null) {
            wake(level);
        }
        Threat threat = Threat.of(source);
        boolean resisted = isBoss() && threat != null && threat == adapted();
        if (resisted) {
            amount *= 0.2f;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && level() instanceof ServerLevel level && isBoss() && threat != null) {
            if (resisted) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + getBbHeight() * 0.6, getZ(), 12,
                        0.6, 0.8, 0.6, 0.2);
            } else if (++hits[threat.ordinal()] >= 5) {
                adapt(level, threat);
            }
        }
        return hurt;
    }

    private void adapt(ServerLevel level, Threat threat) {
        java.util.Arrays.fill(hits, 0);
        entityData.set(ADAPTED, threat.ordinal());
        shout(level, "ADAPTING... PLATING NOW RESISTS " + threat.description.toUpperCase() + ".");
        tell(level, Component.literal("(It barely feels " + threat.description + " now. Hit it with something else!)")
                .withStyle(ChatFormatting.GRAY));
        if (bossBar != null) {
            bossBar.setName(Component.literal("Sentinel Mark X").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.literal("  (adapted to " + threat.description + ")")
                            .withStyle(ChatFormatting.GRAY)));
        }
        sound(level, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 0.6f);
        level.sendParticles(ORANGE, getX(), getY() + getBbHeight() * 0.5, getZ(), 50, 0.8, 1.5, 0.8, 0);
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
        if (!first || !(level() instanceof ServerLevel level)) {
            return;
        }
        if (bossBar != null) {
            bossBar.removeAllPlayers();
        }
        grabbed = null;
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + getBbHeight() / 2, getZ(), isBoss() ? 12 : 4,
                getBbWidth() / 2, getBbHeight() / 3, getBbWidth() / 2, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() / 2, getZ(), 30,
                getBbWidth() / 2, getBbHeight() / 3, getBbWidth() / 2, 0.05);
        sound(level, SoundEvents.GENERIC_EXPLODE.value(), 0.8f);
        if (isBoss()) {
            shout(level, "SYSTEM FAILURE...");
            spawnAtLocation(new ItemStack(SentinelContent.SENTINEL_CORE.get()));
            spawnAtLocation(new ItemStack(com.emil.xmen.world.ModContent.SENTINEL_PART.get(), 10));
            spawnAtLocation(new ItemStack(Items.IRON_BLOCK, 4));
            spawnAtLocation(new ItemStack(Items.DIAMOND, 3));
        } else {
            // the core survives, so the player can build a new chip
            spawnAtLocation(new ItemStack(SentinelContent.SENTINEL_CORE.get()));
            Player player = getOwner();
            if (player != null) {
                player.sendSystemMessage(Component.literal("Your Sentinel was destroyed. Its core survived.")
                        .withStyle(ChatFormatting.AQUA));
            }
        }
    }

    // ------------------------------------------------------------------ your own Sentinel
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlly() || !isOwner(player) || hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(com.emil.xmen.world.ModContent.SENTINEL_PART.get())) {
            if (getHealth() >= getMaxHealth()) {
                player.displayClientMessage(Component.literal("Your Sentinel is already fully repaired."), true);
                return InteractionResult.CONSUME;
            }
            heal(30);
            held.consume(1, player);
            playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0f, 1.0f);
            player.displayClientMessage(Component.literal("Sentinel repaired: " + (int) getHealth() + " / "
                    + (int) getMaxHealth()).withStyle(ChatFormatting.AQUA), true);
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown() && held.isEmpty()) {
            // pack it back into its control chip
            player.getInventory().add(new ItemStack(SentinelContent.CONTROL_CHIP.get()));
            if (level() instanceof ServerLevel level) {
                level.sendParticles(BLUE, getX(), getY() + 1.5, getZ(), 40, 0.6, 1.2, 0.6, 0);
            }
            playSound(SoundEvents.BEACON_DEACTIVATE, 1.0f, 1.2f);
            discard();
            return InteractionResult.CONSUME;
        }
        staying = !staying;
        getNavigation().stop();
        player.displayClientMessage(Component.literal(staying ? "Sentinel: GUARDING THIS AREA."
                : "Sentinel: FOLLOWING YOU.").withStyle(ChatFormatting.AQUA), true);
        playSound(SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.6f);
        return InteractionResult.CONSUME;
    }

    /** Your Sentinel walks after you, and teleports to you when you get too far away. */
    private class FollowOwnerGoal extends Goal {
        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Player player = getOwner();
            return isAlly() && !staying && player != null && !player.isSpectator()
                    && (getTarget() == null || distanceToSqr(player) > 20 * 20) && distanceToSqr(player) > 7 * 7;
        }

        @Override
        public boolean canContinueToUse() {
            Player player = getOwner();
            return isAlly() && !staying && player != null && distanceToSqr(player) > 4 * 4;
        }

        @Override
        public void tick() {
            Player player = getOwner();
            if (player == null) {
                return;
            }
            getLookControl().setLookAt(player, 10.0f, getMaxHeadXRot());
            if (distanceToSqr(player) > 28 * 28 && player.level() == level()) {
                Vec3 behind = player.position().subtract(player.getLookAngle().multiply(3, 0, 3));
                teleportTo(behind.x, player.getY(), behind.z);
                getNavigation().stop();
            } else if (tickCount % 10 == 0) {
                getNavigation().moveTo(player, 1.15);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    /** Your Sentinel attacks whatever hurts you and whatever you attack. */
    private class DefendOwnerGoal extends TargetGoal {
        private LivingEntity enemy;
        private int lastHurtBy;
        private int lastHurt;

        DefendOwnerGoal() {
            super(SentinelMarkX.this, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            Player player = getOwner();
            if (!isAlly() || player == null) {
                return false;
            }
            if (player.getLastHurtByMobTimestamp() != lastHurtBy && usable(player.getLastHurtByMob())) {
                enemy = player.getLastHurtByMob();
                return true;
            }
            if (player.getLastHurtMobTimestamp() != lastHurt && usable(player.getLastHurtMob())) {
                enemy = player.getLastHurtMob();
                return true;
            }
            return false;
        }

        private boolean usable(LivingEntity entity) {
            return entity != null && entity != SentinelMarkX.this && !(entity instanceof Player)
                    && !(entity instanceof SentinelMarkX s && s.isAlly())
                    && canAttack(entity, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            Player player = getOwner();
            if (player != null) {
                lastHurtBy = player.getLastHurtByMobTimestamp();
                lastHurt = player.getLastHurtMobTimestamp();
            }
            mob.setTarget(enemy);
            super.start();
        }
    }

    /** Runs a goal only while a condition holds (asleep Sentinels don't walk, allies don't hunt players...). */
    private static Goal when(BooleanSupplier condition, Goal goal) {
        Goal gated = new Goal() {
            @Override
            public boolean canUse() {
                return condition.getAsBoolean() && goal.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return condition.getAsBoolean() && goal.canContinueToUse();
            }

            @Override
            public boolean isInterruptable() {
                return goal.isInterruptable();
            }

            @Override
            public void start() {
                goal.start();
            }

            @Override
            public void stop() {
                goal.stop();
            }

            @Override
            public boolean requiresUpdateEveryTick() {
                return goal.requiresUpdateEveryTick();
            }

            @Override
            public void tick() {
                goal.tick();
            }
        };
        gated.setFlags(goal.getFlags());
        return gated;
    }

    // ------------------------------------------------------------------ effects, talking and sounds
    /** A line of particles, used for the eye lasers. */
    static void beam(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double step) {
        Vec3 line = to.subtract(from);
        double length = line.length();
        Vec3 dir = line.normalize();
        for (double d = 0; d < length; d += step) {
            Vec3 at = from.add(dir.scale(d));
            level.sendParticles(particle, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    private void say(ServerLevel level, String line) {
        tell(level, Component.literal(name() + ": " + line).withStyle(ChatFormatting.RED));
    }

    private void shout(ServerLevel level, String line) {
        tell(level, Component.literal(name() + ": " + line).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
    }

    private String name() {
        return isBoss() ? "SENTINEL MARK X" : "SENTINEL";
    }

    private void tell(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) < 48 * 48) {
                player.sendSystemMessage(message);
            }
        }
    }

    private void sound(ServerLevel level, SoundEvent sound, float pitch) {
        level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.HOSTILE, isBoss() ? 2.5f : 1.5f, pitch);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        playSound(SoundEvents.IRON_GOLEM_STEP, isBoss() ? 2.0f : 1.2f, isBoss() ? 0.5f : 0.75f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return isBoss() ? 0.5f : 0.75f;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
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
    public void tick() {
        super.tick();
        if (level().isClientSide() && isAwake() && action() == AIM && tickCount % 2 == 0) {
            Vec3 eyes = getEyePosition().add(Vec3.directionFromRotation(0, getYHeadRot()).scale(getBbWidth() * 0.4));
            level().addParticle(isAlly() ? BLUE : ORANGE, eyes.x, eyes.y, eyes.z,
                    0, 0, 0);
        }
        if (!level().isClientSide() && isBoss() && !isAwake() && tickCount % 40 == 0
                && level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX() + Mth.nextDouble(random, -1, 1),
                    getY() + getBbHeight() * 0.7, getZ() + Mth.nextDouble(random, -1, 1), 2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    // ------------------------------------------------------------------ saving
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awake", entityData.get(AWAKE));
        tag.putBoolean("Enraged", enraged);
        tag.putInt("Adapted", entityData.get(ADAPTED));
        tag.putBoolean("Staying", staying);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(AWAKE, tag.getBoolean("Awake"));
        enraged = tag.getBoolean("Enraged");
        if (tag.contains("Adapted")) {
            entityData.set(ADAPTED, tag.getInt("Adapted"));
        }
        staying = tag.getBoolean("Staying");
        if (tag.hasUUID("Owner")) {
            owner = tag.getUUID("Owner");
            entityData.set(ALLY, true);
            allyStats();
        }
        if (bossBar != null && !isAlly()) {
            bossBar.setVisible(isAwake());
            Threat threat = adapted();
            if (threat != null) {
                bossBar.setName(Component.literal("Sentinel Mark X").withStyle(ChatFormatting.DARK_RED)
                        .append(Component.literal("  (adapted to " + threat.description + ")")
                                .withStyle(ChatFormatting.GRAY)));
            }
        }
    }
}
