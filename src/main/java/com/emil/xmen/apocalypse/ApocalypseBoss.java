package com.emil.xmen.apocalypse;

import com.emil.xmen.ability.Scheduler;
import com.emil.xmen.ability.TempBlocks;
import com.emil.xmen.world.ModContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * En Sabah Nur, the first mutant, as in X-Men: Apocalypse. He sleeps on the altar in the heart of his pyramid until
 * someone comes close; then the sun shines down through the crystal capstone, the gold veins light up and he wakes.
 * He fights with the powers he shows in the movie and has a health bar with numbers on it.
 */
public class ApocalypseBoss extends Monster {
    private static final EntityDataAccessor<Boolean> AWAKE =
            SynchedEntityData.defineId(ApocalypseBoss.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CASTING =
            SynchedEntityData.defineId(ApocalypseBoss.class, EntityDataSerializers.BOOLEAN);
    private static final double SCALE = 1.35;
    private static final double SCALE_REBORN = 1.75;
    private static final int WAKE_TIME = 70;

    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Apocalypse"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private BlockPos home;
    private int beamHeight = 20;
    private boolean reborn;
    private int wakeTicks;
    private int cooldown = 40;
    private int step;
    private int castTicks;
    private int stormTicks;
    private int sinceSpoke;

    public ApocalypseBoss(EntityType<? extends ApocalypseBoss> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 250;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600)
                .add(Attributes.ATTACK_DAMAGE, 14)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.SCALE, SCALE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AWAKE, false);
        builder.define(CASTING, false);
    }

    public boolean isAwake() {
        return entityData.get(AWAKE);
    }

    /** True while he raises his hands to use a power (the model lifts its arms). */
    public boolean isCasting() {
        return entityData.get(CASTING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 20.0f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, ApocalypseServant.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ------------------------------------------------------------------ every tick
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (home == null) {
            home = blockPosition();
        }
        if (tickCount % 5 == 0) {
            bar.setName(PyramidFx.barTitle(this, ChatFormatting.BLUE));
            PyramidFx.updateBar(level, this, bar, 56, isAwake() || wakeTicks > 0);
        }
        if (castTicks > 0 && --castTicks == 0) {
            entityData.set(CASTING, false);
        }
        if (!isAwake()) {
            sleep(level);
            return;
        }
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target.level() != level
                || (target instanceof Player player && (player.isCreative() || player.isSpectator())))) {
            setTarget(null);
            target = null;
        }
        if (!reborn && getHealth() <= getMaxHealth() / 2) {
            rebirth(level);
        }
        if (stormTicks > 0) {
            dustStorm(level, --stormTicks);
        }
        if (reborn && tickCount % 3 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    getX(), getY() + 0.3, getZ(), 3, 1.2, 0.2, 1.2, 0.05);
        }
        sinceSpoke++;
        if (target == null || --cooldown > 0) {
            return;
        }
        int wait = usePower(level, target);
        cooldown = wait <= 0 ? 10 : (int) (wait * (reborn ? 0.7 : 1.0));
        if (wait > 0) {
            step++;
        }
    }

    /** Asleep on his altar. Wakes up when someone comes close (or hits him). */
    private void sleep(ServerLevel level) {
        setTarget(null);
        getNavigation().stop();
        if (wakeTicks > 0) {
            waking(level, --wakeTicks);
            return;
        }
        if (tickCount % 40 == 0) {
            level.sendParticles(PyramidFx.BLUE, getX(), getY() + 1.5, getZ(), 4, 0.4, 0.8, 0.4, 0);
        }
        if (tickCount % 10 == 0) {
            Player near = level.getNearestPlayer(this, 12);
            if (near != null && !near.isCreative() && !near.isSpectator()) {
                startWaking(level);
            }
        }
    }

    private void startWaking(ServerLevel level) {
        if (wakeTicks > 0 || isAwake()) {
            return;
        }
        wakeTicks = WAKE_TIME;
        narrate(level, "The sun strikes the crystal capstone... light pours down into the pyramid!");
        PyramidFx.sound(level, this, SoundEvents.BEACON_ACTIVATE, 3.0f, 0.6f);
    }

    /** The movie moment: a beam of sunlight from the capstone, the gold veins flare, and he opens his eyes. */
    private void waking(ServerLevel level, int left) {
        Vec3 top = new Vec3(home.getX() + 0.5, home.getY() + beamHeight, home.getZ() + 0.5);
        if (left % 2 == 0) {
            PyramidFx.beam(level, top, position().add(0, getBbHeight(), 0), ParticleTypes.END_ROD);
            PyramidFx.beam(level, top, position().add(0, getBbHeight(), 0), PyramidFx.GOLD);
        }
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1, getZ(), 6, 0.5, 1.0, 0.5, 0.05);
        if (left == WAKE_TIME - 25) {
            VeinBlock.setLit(level, home, 34, 26, beamHeight + 2, true);
            PyramidFx.sound(level, this, SoundEvents.CONDUIT_ACTIVATE, 3.0f, 0.7f);
        }
        if (left == 0) {
            entityData.set(AWAKE, true);
            PyramidFx.say(level, this, "Everything they've built will fall!", ChatFormatting.BLUE);
            PyramidFx.sound(level, this, SoundEvents.WITHER_SPAWN, 2.0f, 0.6f);
            shockwave(level, 10, 4);
            cooldown = 40;
        }
    }

    // ------------------------------------------------------------------ powers
    private int usePower(ServerLevel level, LivingEntity target) {
        double distance = distanceTo(target);
        boolean sees = hasLineOfSight(target);
        int pattern = step % (reborn ? 7 : 6);
        switch (pattern) {
            case 0, 4 -> {
                return energyBlast(level, target, distance, sees);
            }
            case 1 -> {
                return sinkIntoSand(level, target, distance);
            }
            case 2 -> {
                if (distance > 9) {
                    return energyBlast(level, target, distance, sees);
                }
                cast();
                shockwave(level, 9, 9);
                return 70;
            }
            case 3 -> {
                return teleportStrike(level, target, distance);
            }
            case 5 -> {
                return servants(level, target);
            }
            default -> {
                cast();
                stormTicks = 80;
                PyramidFx.say(level, this, "Kneel before me!", ChatFormatting.BLUE);
                PyramidFx.sound(level, this, SoundEvents.ELYTRA_FLYING, 2.0f, 0.5f);
                return 110;
            }
        }
    }

    private void cast() {
        castTicks = 20;
        entityData.set(CASTING, true);
    }

    /** A blue energy beam from his hands. */
    private int energyBlast(ServerLevel level, LivingEntity target, double distance, boolean sees) {
        if (distance > 32 || !sees) {
            return 0;
        }
        cast();
        PyramidFx.beam(level, this, target, PyramidFx.BLUE);
        PyramidFx.beam(level, this, target, ParticleTypes.ELECTRIC_SPARK);
        target.hurt(damageSources().indirectMagic(this, this), reborn ? 12 : 9);
        Vec3 push = target.position().subtract(position()).normalize().scale(0.8);
        target.push(push.x, 0.25, push.z);
        target.hurtMarked = true;
        PyramidFx.sound(level, this, SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 1.3f);
        return 50;
    }

    /** Like the guards in the movie: he turns the floor to sand and sinks you into it. */
    private int sinkIntoSand(ServerLevel level, LivingEntity target, double distance) {
        if (distance > 22) {
            return 0;
        }
        cast();
        BlockPos feet = target.blockPosition();
        BlockState sand = Blocks.SAND.defaultBlockState();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-1, 0, -1), feet.offset(1, 1, 1))) {
            if (pos.getX() != feet.getX() || pos.getZ() != feet.getZ()) {
                TempBlocks.place(level, pos.immutable(), sand, 70);
            }
        }
        target.teleportTo(feet.getX() + 0.5, target.getY() - 0.4, feet.getZ() + 0.5);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 5));
        target.hurt(damageSources().indirectMagic(this, this), 6);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, sand), target.getX(), target.getY() + 0.5,
                target.getZ(), 60, 0.6, 0.6, 0.6, 0.1);
        PyramidFx.beam(level, this, target, PyramidFx.GOLD);
        PyramidFx.sound(level, target, SoundEvents.SAND_BREAK, 2.0f, 0.6f);
        if (sinceSpoke > 200) {
            PyramidFx.say(level, this, "Dust to dust.", ChatFormatting.BLUE);
            sinceSpoke = 0;
        }
        return 70;
    }

    /** Throws everyone around him away. */
    private void shockwave(ServerLevel level, double radius, float damage) {
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
                e -> e != this && !PyramidFx.ally(e))) {
            if (victim instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            victim.hurt(damageSources().mobAttack(this), damage);
            Vec3 push = victim.position().subtract(position()).normalize().scale(2.0);
            victim.setDeltaMovement(push.x, 0.8, push.z);
            victim.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()),
                getX(), getY() + 0.2, getZ(), 80, radius / 3, 0.2, radius / 3, 0.2);
        PyramidFx.sound(level, this, SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.7f);
    }

    /** He vanishes in a dark ring and appears right behind you. */
    private int teleportStrike(ServerLevel level, LivingEntity target, double distance) {
        if (distance < 5 || distance > 30) {
            return 0;
        }
        Vec3 behind = target.position().subtract(target.getLookAngle().multiply(2, 0, 2));
        BlockPos spot = BlockPos.containing(behind);
        if (!level.getBlockState(spot).isAir() || !level.getBlockState(spot.above()).isAir()
                || !level.getBlockState(spot.above(2)).isAir()) {
            return 0;
        }
        portal(level);
        teleportTo(behind.x, target.getY(), behind.z);
        portal(level);
        getLookControl().setLookAt(target);
        doHurtTarget(target);
        return 60;
    }

    private void portal(ServerLevel level) {
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI * 2 / 24;
            level.sendParticles(ParticleTypes.SQUID_INK, getX() + Math.cos(angle) * 1.2, getY() + 1.4,
                    getZ() + Math.sin(angle) * 1.2, 1, 0, 0.6, 0, 0.01);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 40, 0.5, 1, 0.5, 0.1);
        PyramidFx.sound(level, this, SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.5f);
    }

    /** Powers up his horsemen if any are close, otherwise calls Clan Akkaba out of the sand. */
    private int servants(ServerLevel level, LivingEntity target) {
        var horsemen = level.getEntitiesOfClass(ApocalypseServant.class, getBoundingBox().inflate(48),
                s -> s.kind().horseman());
        cast();
        if (!horsemen.isEmpty()) {
            for (ApocalypseServant horseman : horsemen) {
                horseman.heal(30);
                horseman.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));
                horseman.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 300, 0));
                PyramidFx.beam(level, this, horseman, PyramidFx.GOLD);
            }
            PyramidFx.say(level, this, "My horsemen, I give you more power!", ChatFormatting.BLUE);
            PyramidFx.sound(level, this, SoundEvents.EVOKER_PREPARE_SUMMON, 2.0f, 0.7f);
            return 80;
        }
        int cultists = level.getEntitiesOfClass(ApocalypseServant.class, getBoundingBox().inflate(32),
                s -> s.kind() == ServantKind.CULTIST).size();
        if (cultists >= 4) {
            return 0;
        }
        for (int i = 0; i < 2; i++) {
            ApocalypseServant cultist = ApocalypseContent.CULTIST.get().create(level);
            if (cultist == null) {
                break;
            }
            double angle = random.nextDouble() * Math.PI * 2;
            cultist.moveTo(getX() + Math.cos(angle) * 3, getY(), getZ() + Math.sin(angle) * 3, getYRot(), 0);
            cultist.addTag("xmen.no_loot");
            level.addFreshEntity(cultist);
            cultist.setTarget(target);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    cultist.getX(), cultist.getY() + 0.5, cultist.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        }
        PyramidFx.say(level, this, "Rise, my children!", ChatFormatting.BLUE);
        PyramidFx.sound(level, this, SoundEvents.EVOKER_PREPARE_SUMMON, 2.0f, 0.7f);
        return 80;
    }

    /** Reborn form only: a sandstorm that drags everyone toward him. */
    private void dustStorm(ServerLevel level, int left) {
        double radius = 3 + (80 - left) * 0.12;
        for (int i = 0; i < 6; i++) {
            double angle = (left * 0.35) + i * Math.PI / 3;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                    getX() + Math.cos(angle) * radius, getY() + 0.5 + (i % 3), getZ() + Math.sin(angle) * radius, 4,
                    0.3, 0.3, 0.3, 0.05);
        }
        if (left % 10 != 0) {
            return;
        }
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16),
                e -> e != this && !PyramidFx.ally(e))) {
            if (victim instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            Vec3 pull = position().subtract(victim.position()).normalize().scale(0.7);
            victim.push(pull.x, 0.1, pull.z);
            victim.hurtMarked = true;
            victim.hurt(damageSources().indirectMagic(this, this), 2);
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
        }
    }

    /** At half health he grows and gets stronger, like his giant form at the end of the movie. */
    private void rebirth(ServerLevel level) {
        reborn = true;
        AttributeInstance scale = getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.setBaseValue(SCALE_REBORN);
        }
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.32);
        }
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
        bar.setColor(BossEvent.BossBarColor.PURPLE);
        cast();
        PyramidFx.say(level, this, "You are all just children! I have lived a thousand lives!",
                ChatFormatting.BLUE);
        narrate(level, "Apocalypse grows to his true size. Sand starts swirling around him!");
        PyramidFx.sound(level, this, SoundEvents.ENDER_DRAGON_GROWL, 2.0f, 0.6f);
        shockwave(level, 8, 6);
        stormTicks = 80;
    }

    private void narrate(ServerLevel level, String text) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(this) < 64 * 64) {
                player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GOLD,
                        ChatFormatting.ITALIC));
            }
        }
    }

    // ------------------------------------------------------------------ damage and death
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.LIGHTNING_BOLT)
                || (source.getEntity() != null && PyramidFx.ally(source.getEntity()))) {
            return false;
        }
        if (!isAwake() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (level() instanceof ServerLevel level && source.getEntity() instanceof Player) {
                startWaking(level);
            }
            return false;  // you can't hurt him before he's awake
        }
        if (reborn && source.getDirectEntity() instanceof Projectile projectile) {
            // he turns arrows to dust in the air
            if (level() instanceof ServerLevel level) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()),
                        projectile.getX(), projectile.getY(), projectile.getZ(), 12, 0.2, 0.2, 0.2, 0.05);
            }
            amount *= 0.4f;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return PyramidFx.ally(other) || super.isAlliedTo(other);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !isRemoved() && !dead;
        super.die(source);
        if (!first || !(level() instanceof ServerLevel level)) {
            return;
        }
        bar.removeAllPlayers();
        spawnAtLocation(new ItemStack(ModContent.CELESTIAL_ORB.get()));
        spawnAtLocation(new ItemStack(Items.DIAMOND, 8));
        spawnAtLocation(new ItemStack(Items.GOLD_BLOCK, 4));
        PyramidFx.say(level, this, "This... is not... the end...", ChatFormatting.BLUE);
        narrate(level, "Apocalypse has fallen! He left behind a Celestial Orb...");
        BlockPos center = home != null ? home : blockPosition();
        if (level.getBlockState(center.below()).getBlock() instanceof VeinBlock) {
            Scheduler.later(60, () -> {
                for (ServerPlayer player : level.players()) {
                    if (player.distanceToSqr(center.getX(), center.getY(), center.getZ()) < 64 * 64) {
                        player.sendSystemMessage(Component.literal("The altar is cracking! Jump down into the vault "
                                + "below, the water will catch you...").withStyle(ChatFormatting.GOLD,
                                        ChatFormatting.ITALIC));
                    }
                }
                level.playSound(null, center, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, net.minecraft.sounds.SoundSource
                        .BLOCKS, 2.0f, 0.5f);
            });
            Scheduler.later(120, () -> collapse(level, center));
        }
    }

    /** Like the end of the movie's opening: the altar floor gives way and drops into the hidden vault. */
    static void collapse(ServerLevel level, BlockPos center) {
        for (int y = 1; y <= 4; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = center.offset(dx, -y, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0 && !state.hasBlockEntity()) {
                        level.removeBlock(pos, false);
                    }
                }
            }
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()),
                center.getX() + 0.5, center.getY() - 1, center.getZ() + 0.5, 250, 2, 1.5, 2, 0.2);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, center.getX() + 0.5, center.getY() - 2,
                center.getZ() + 0.5, 30, 2, 1, 2, 0.02);
        level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.BLOCKS,
                2.0f, 0.5f);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bar.removeAllPlayers();
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bar.removePlayer(player);
    }

    // ------------------------------------------------------------------ saving
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awake", isAwake());
        tag.putBoolean("Reborn", reborn);
        tag.putInt("BeamHeight", beamHeight);
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(AWAKE, tag.getBoolean("Awake"));
        reborn = tag.getBoolean("Reborn");
        if (tag.contains("BeamHeight")) {
            beamHeight = tag.getInt("BeamHeight");
        }
        if (tag.contains("HomeX")) {
            home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
        }
    }
}
