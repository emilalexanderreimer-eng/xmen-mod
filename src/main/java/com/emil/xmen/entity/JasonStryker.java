package com.emil.xmen.entity;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Jason Stryker, "Mutant 143", the Colonel's son from X2. He sits in his wheelchair in the middle of the fake
 * Cerebro and can't walk, but he puts illusions into your head: the world spins and goes dark, fake soldiers
 * appear out of nowhere, and you find yourself somewhere else in the room without knowing how you got there.
 */
public class JasonStryker extends WeaponXMob {
    private static final String NAME = "Jason";
    private static final String[] LINES = {"Do you want to see something? Close your eyes...",
            "Father says you have to stay here. Forever.", "They're not really there. Or are they?",
            "You can't tell what's real anymore, can you?", "Shhh... don't wake up."};

    private int cooldown = 60;
    private int step;
    private boolean cried;

    public JasonStryker(EntityType<? extends JasonStryker> type, Level level) {
        super(type, level);
        xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 28)
                .add(Attributes.ATTACK_DAMAGE, 0);
    }

    @Override
    public boolean sits() {
        return true;
    }

    @Override
    protected ServerBossEvent createBossBar() {
        return new ServerBossEvent(Component.literal("Jason Stryker - Mutant 143")
                .withStyle(ChatFormatting.LIGHT_PURPLE), BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS);
    }

    @Override
    protected void addBehaviorGoals() {
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0f));
    }

    @Override
    protected void greet(ServerLevel level, LivingEntity target) {
        say(level, NAME, "Hello. I've been waiting for you.", ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        setDeltaMovement(0, getDeltaMovement().y, 0);
        LivingEntity target = getTarget();
        if (!(level() instanceof ServerLevel level) || !(target instanceof Player player)) {
            return;
        }
        getLookControl().setLookAt(target, 30.0f, 30.0f);
        if (tickCount % 3 == 0) {
            level.sendParticles(ParticleTypes.WITCH, getX(), getEyeY() + 0.3, getZ(), 1, 0.3, 0.2, 0.3, 0);
        }
        if (--cooldown > 0 || distanceTo(player) > 26) {
            return;
        }
        cooldown = 110 + random.nextInt(40);
        switch (step++ % 3) {
            case 0 -> nightmare(level);
            case 1 -> fakeSoldiers(level, player);
            default -> lostInTheMind(level, player);
        }
        if (random.nextInt(2) == 0) {
            say(level, NAME, LINES[random.nextInt(LINES.length)], ChatFormatting.LIGHT_PURPLE);
        }
    }

    /** Everyone close by gets dizzy and the world goes dark for a moment. */
    private void nightmare(ServerLevel level) {
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(22),
                p -> !p.isCreative() && !p.isSpectator())) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        sound(level, SoundEvents.ILLUSIONER_CAST_SPELL, 0.7f);
    }

    /** Soldiers who aren't really there. They shoot blanks and vanish when you hit them. */
    private void fakeSoldiers(ServerLevel level, Player player) {
        int alive = level.getEntitiesOfClass(WeaponXSoldier.class, getBoundingBox().inflate(40),
                WeaponXSoldier::isIllusion).size();
        for (int i = alive; i < Math.min(alive + 3, 5); i++) {
            BlockPos spot = freeSpotNear(level, player.blockPosition(), 5);
            if (spot == null) {
                continue;
            }
            WeaponXSoldier fake = WeaponXEntities.SOLDIER.get().create(level);
            if (fake == null) {
                continue;
            }
            fake.makeIllusion(300);
            fake.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360, 0);
            level.addFreshEntity(fake);
            fake.setTarget(player);
            level.sendParticles(ParticleTypes.WITCH, fake.getX(), fake.getY() + 1, fake.getZ(), 20, 0.3, 0.6, 0.3,
                    0.05);
        }
        sound(level, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8f);
    }

    /** You blink, and you're standing somewhere else in the room. */
    private void lostInTheMind(ServerLevel level, Player player) {
        BlockPos spot = freeSpotNear(level, blockPosition(), 9);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
        if (spot != null && player instanceof ServerPlayer serverPlayer) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 30,
                    0.3, 0.6, 0.3, 0.05);
            Vec3 look = position().subtract(Vec3.atBottomCenterOf(spot));
            float yaw = (float) (Math.atan2(look.z, look.x) * 180 / Math.PI) - 90;
            serverPlayer.teleportTo(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw,
                    player.getXRot());
            player.sendSystemMessage(Component.literal("* You blink... and you're somewhere else *")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.LIGHT_PURPLE));
        }
        sound(level, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 0.8f);
    }

    /** A random spot with floor under it and room for a person. */
    private BlockPos freeSpotNear(ServerLevel level, BlockPos center, int range) {
        for (int tries = 0; tries < 20; tries++) {
            BlockPos pos = center.offset(random.nextInt(range * 2 + 1) - range, random.nextInt(3) - 1,
                    random.nextInt(range * 2 + 1) - range);
            if (pos.closerThan(center, 2.5)) {
                continue;
            }
            if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                    && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()
                    && level.getFluidState(pos).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !cried && level() instanceof ServerLevel level) {
            cried = true;
            say(level, NAME, "Father! FATHER! Make it stop!", ChatFormatting.LIGHT_PURPLE);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !isRemoved() && !dead;
        super.die(source);
        if (first && level() instanceof ServerLevel level) {
            List<WeaponXSoldier> fakes = level.getEntitiesOfClass(WeaponXSoldier.class,
                    new AABB(blockPosition()).inflate(48), WeaponXSoldier::isIllusion);
            for (WeaponXSoldier fake : fakes) {
                fake.vanish(level);
            }
            narrate(level, "The illusions fade away", ChatFormatting.LIGHT_PURPLE);
            spawnAtLocation(new ItemStack(Items.ENDER_EYE, 2));
            spawnAtLocation(new ItemStack(Items.EXPERIENCE_BOTTLE, 4));
        }
    }
}
