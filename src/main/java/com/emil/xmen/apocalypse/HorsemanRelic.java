package com.emil.xmen.apocalypse;

import java.util.List;

import com.emil.xmen.ability.Scheduler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** What the Four Horsemen drop: a piece of their power that you can use with right-click. */
public final class HorsemanRelic {
    private HorsemanRelic() {
    }

    /** Storm's Crystal: calls a lightning strike down where you look. */
    public static class StormCrystal extends Item {
        public StormCrystal(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel server) {
                HitResult hit = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition()
                        .add(player.getLookAngle().scale(40)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE,
                        player));
                Vec3 at = hit.getLocation();
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
                if (bolt != null) {
                    bolt.moveTo(at.x, at.y, at.z);
                    bolt.setVisualOnly(true);
                    server.addFreshEntity(bolt);
                }
                for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(3),
                        e -> e != player)) {
                    victim.hurt(player.damageSources().indirectMagic(player, player), 9);
                    victim.igniteForSeconds(3);
                }
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 1, at.z, 40, 1, 1, 1, 0.3);
                player.getCooldowns().addCooldown(this, 50);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                TooltipFlag flag) {
            tooltip.add(Component.literal("Right-click: call lightning where you look").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Psylocke's Psychic Blade: a purple energy sword. Right-click to dash forward and cut through everyone. */
    public static class PsychicBlade extends SwordItem {
        public PsychicBlade(Tier tier, Properties properties) {
            super(tier, properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel server) {
                Vec3 look = player.getLookAngle();
                Vec3 start = player.position();
                player.setDeltaMovement(look.x * 1.8, Math.max(0.15, look.y * 0.6), look.z * 1.8);
                player.hurtMarked = true;
                player.fallDistance = 0;
                AABB path = player.getBoundingBox().expandTowards(look.scale(7)).inflate(1);
                for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, path, e -> e != player)) {
                    victim.hurt(player.damageSources().playerAttack(player), 8);
                    server.sendParticles(PyramidFx.PURPLE, victim.getX(), victim.getY() + 1, victim.getZ(), 15, 0.3,
                            0.5, 0.3, 0);
                }
                PyramidFx.beam(server, start.add(0, 1, 0), start.add(look.scale(6)).add(0, 1, 0), PyramidFx.PURPLE);
                PyramidFx.sound(server, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 0.7f);
                player.getCooldowns().addCooldown(this, 40);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                TooltipFlag flag) {
            tooltip.add(Component.literal("Right-click: psychic dash that cuts through enemies")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** Archangel's Metal Wing: one beat of his wings throws you up and forward, then you glide down. */
    public static class MetalWing extends Item {
        public MetalWing(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel server) {
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(look.x * 1.4, 1.1, look.z * 1.4);
                player.hurtMarked = true;
                player.fallDistance = 0;
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 140, 0));
                server.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 25, 0.8,
                        0.3, 0.8, 0.05);
                server.sendParticles(PyramidFx.STEEL, player.getX(), player.getY() + 1, player.getZ(), 25, 1.0, 0.4,
                        1.0, 0);
                PyramidFx.sound(server, player, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.3f);
                player.getCooldowns().addCooldown(this, 60);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                TooltipFlag flag) {
            tooltip.add(Component.literal("Right-click: beat your metal wings and glide").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Magneto's Gauntlet: lifts everything in front of you into the air, then hurls it away. */
    public static class MagnetGauntlet extends Item {
        public MagnetGauntlet(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (level instanceof ServerLevel server) {
                Vec3 look = player.getLookAngle();
                List<LivingEntity> victims = server.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(10), e -> e != player && e.isAlive()
                                && e.position().subtract(player.position()).normalize().dot(look) > 0.5);
                for (LivingEntity victim : victims) {
                    victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1));
                    PyramidFx.beam(server, player, victim, PyramidFx.RED);
                }
                Scheduler.later(30, () -> {
                    for (LivingEntity victim : victims) {
                        if (victim.isAlive()) {
                            Vec3 push = victim.position().subtract(player.position()).normalize().scale(2.2);
                            victim.setDeltaMovement(push.x, 0.5, push.z);
                            victim.hurtMarked = true;
                            victim.hurt(player.damageSources().indirectMagic(player, player), 5);
                        }
                    }
                });
                PyramidFx.sound(server, player, SoundEvents.ANVIL_LAND, 0.6f, 0.6f);
                player.getCooldowns().addCooldown(this, 80);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                TooltipFlag flag) {
            tooltip.add(Component.literal("Right-click: lift what's in front of you and throw it")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
