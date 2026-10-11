package com.emil.xmen.ability.powers;

import static com.emil.xmen.ability.Abilities.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import com.emil.xmen.ability.Allies;
import com.emil.xmen.ability.Highlights;
import com.emil.xmen.ability.Morphs;
import com.emil.xmen.ability.PowerBehavior;
import com.emil.xmen.ability.PowerBehaviors;
import com.emil.xmen.ability.PowerProjectiles;
import com.emil.xmen.ability.PowerRuntime;
import com.emil.xmen.ability.PowerState;
import com.emil.xmen.ability.Scheduler;
import com.emil.xmen.ability.TempBlocks;
import com.emil.xmen.ability.Zones;
import com.emil.xmen.entity.ModEntities;
import com.emil.xmen.entity.PowerClone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Set;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.windcharge.WindCharge;

/**
 * What a player gets from looking like a mob (Total Transformation, Power Absorption): its size, health,
 * strength and speed, plus things like flight, fire immunity or water breathing, and a special move.
 */
public final class MobTraits {
    private static final Set<EntityType<?>> FLYERS = Set.of(EntityType.BAT, EntityType.PARROT, EntityType.PHANTOM,
            EntityType.BEE, EntityType.BLAZE, EntityType.GHAST, EntityType.ALLAY, EntityType.VEX,
            EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.BREEZE);
    private static final Set<EntityType<?>> FAST = Set.of(EntityType.HORSE, EntityType.WOLF, EntityType.OCELOT,
            EntityType.CAT, EntityType.FOX, EntityType.RABBIT);
    private static final String[] NAMES = { "trait_scale", "trait_health", "trait_attack", "trait_speed",
            "trait_armor", "trait_jump", "trait_fall" };

    private MobTraits() {
    }

    private static double base(EntityType<?> type, Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double fallback) {
        if (!DefaultAttributes.hasSupplier(type)) {
            return fallback;
        }
        @SuppressWarnings("unchecked")
        AttributeSupplier supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        return supplier.hasAttribute(attribute) ? supplier.getBaseValue(attribute) : fallback;
    }

    public static boolean canFly(EntityType<?> type) {
        return FLYERS.contains(type);
    }

    public static boolean climbs(EntityType<?> type) {
        return type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER;
    }

    /** Applies the traits of type. Call it again and again (every few ticks) while the player is that mob. */
    public static void apply(ServerPlayer player, EntityType<?> type) {
        double scale = Math.max(0.3, Math.min(2.5, type.getHeight() / 1.8));
        if (type == EntityType.ENDER_DRAGON || type == EntityType.WITHER || type == EntityType.GHAST) {
            scale = 2;
        }
        setModifier(player, Attributes.SCALE, NAMES[0], scale - 1, AttributeModifier.Operation.ADD_VALUE);
        double health = Math.max(4, Math.min(100, base(type, Attributes.MAX_HEALTH, 20)));
        setModifier(player, Attributes.MAX_HEALTH, NAMES[1], health - 20, AttributeModifier.Operation.ADD_VALUE);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        double attack = Math.min(20, base(type, Attributes.ATTACK_DAMAGE, 1));
        setModifier(player, Attributes.ATTACK_DAMAGE, NAMES[2], attack - 1, AttributeModifier.Operation.ADD_VALUE);
        double speed = base(type, Attributes.MOVEMENT_SPEED, 0.25) / 0.25 - 1;
        if (FAST.contains(type)) {
            speed = Math.max(speed, 0.4);
        }
        setModifier(player, Attributes.MOVEMENT_SPEED, NAMES[3], Math.max(-0.5, Math.min(1, speed)),
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        setModifier(player, Attributes.ARMOR, NAMES[4], Math.min(20, base(type, Attributes.ARMOR, 0)),
                AttributeModifier.Operation.ADD_VALUE);
        if (type == EntityType.RABBIT || type == EntityType.SLIME || type == EntityType.MAGMA_CUBE
                || type == EntityType.HORSE || type == EntityType.GOAT || type == EntityType.FROG) {
            setModifier(player, Attributes.JUMP_STRENGTH, NAMES[5], 0.3, AttributeModifier.Operation.ADD_VALUE);
        }
        if (type.is(EntityTypeTags.FALL_DAMAGE_IMMUNE) || canFly(type) || type == EntityType.CAT
                || type == EntityType.CHICKEN) {
            setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, NAMES[6], -1,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        if (type.fireImmune()) {
            buff(player, MobEffects.FIRE_RESISTANCE, 0);
        }
        if (type.is(EntityTypeTags.CAN_BREATHE_UNDER_WATER) || type.is(EntityTypeTags.AQUATIC)) {
            buff(player, MobEffects.WATER_BREATHING, 0);
            if (player.isInWater()) {
                buff(player, MobEffects.DOLPHINS_GRACE, 0);
            }
        }
        if (type == EntityType.CHICKEN) {
            buff(player, MobEffects.SLOW_FALLING, 0);
        }
        if (type == EntityType.CAT || type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER
                || type == EntityType.BAT || type == EntityType.ENDERMAN) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false, false));
        }
        if (type == EntityType.WARDEN) {
            buff(player, MobEffects.DAMAGE_RESISTANCE, 1);
        }
        if (canFly(type) != state(player).flag("flight")) {
            PowerRuntime.setTemporaryFlight(player, canFly(type));
        }
    }

    public static void remove(ServerPlayer player) {
        for (var attribute : List.of(Attributes.SCALE, Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE,
                Attributes.MOVEMENT_SPEED, Attributes.ARMOR, Attributes.JUMP_STRENGTH,
                Attributes.FALL_DAMAGE_MULTIPLIER)) {
            for (String name : NAMES) {
                removeModifier(player, attribute, name);
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        PowerRuntime.setTemporaryFlight(player, false);
    }

    /** Should a mob of type mobType leave a player who is disguised as disguise alone? */
    public static boolean sameKind(EntityType<?> disguise, Mob mob) {
        if (disguise == null) {
            return false;
        }
        return mob.getType() == disguise || disguise.is(EntityTypeTags.UNDEAD) && mob.getType().is(EntityTypeTags.UNDEAD);
    }

    /** The mob's special move. Returns false if it has none. */
    public static boolean special(ServerPlayer player, EntityType<?> type) {
        ServerLevel level = level(player);
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition().add(look.scale(1.5));
        if (type == EntityType.CREEPER) {
            explode(player, player.position(), 3, false);
            return true;
        }
        if (type == EntityType.BLAZE) {
            SmallFireball fireball = new SmallFireball(level, player, look);
            fireball.setPos(eye.x, eye.y, eye.z);
            level.addFreshEntity(fireball);
            sound(player, SoundEvents.BLAZE_SHOOT, 1, 1);
            return true;
        }
        if (type == EntityType.GHAST) {
            LargeFireball fireball = new LargeFireball(level, player, look, 1);
            fireball.setPos(eye.x, eye.y, eye.z);
            level.addFreshEntity(fireball);
            sound(player, SoundEvents.GHAST_SHOOT, 1, 1);
            return true;
        }
        if (type == EntityType.ENDER_DRAGON) {
            DragonFireball fireball = new DragonFireball(level, player, look);
            fireball.setPos(eye.x, eye.y, eye.z);
            level.addFreshEntity(fireball);
            sound(player, SoundEvents.ENDER_DRAGON_SHOOT, 1, 1);
            return true;
        }
        if (type == EntityType.WITHER) {
            WitherSkull skull = new WitherSkull(level, player, look);
            skull.setPos(eye.x, eye.y, eye.z);
            level.addFreshEntity(skull);
            sound(player, SoundEvents.WITHER_SHOOT, 1, 1);
            return true;
        }
        if (type == EntityType.ENDERMAN) {
            BlockPos spot = teleportSpot(player, 32);
            if (spot == null) {
                return false;
            }
            teleport(player, Vec3.atBottomCenterOf(spot));
            sound(player, SoundEvents.ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        if (type == EntityType.SNOW_GOLEM) {
            Snowball ball = new Snowball(level, player);
            ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f, 1);
            level.addFreshEntity(ball);
            return true;
        }
        if (type == EntityType.LLAMA || type == EntityType.TRADER_LLAMA) {
            PowerProjectiles.shoot(player, "acid", new ItemStack(Items.SLIME_BALL), 1.5f, 1, true);
            sound(player, SoundEvents.LLAMA_SPIT, 1, 1);
            return true;
        }
        if (type == EntityType.WARDEN) {
            for (LivingEntity e : beam(player, 20, 1)) {
                hurtMagic(player, e, 10);
            }
            for (int i = 1; i <= 10; i++) {
                Vec3 at = player.getEyePosition().add(look.scale(i * 2));
                level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
            sound(player, SoundEvents.WARDEN_SONIC_BOOM, 1, 1);
            return true;
        }
        if (type == EntityType.EVOKER) {
            for (int i = 1; i <= 8; i++) {
                Vec3 at = player.position().add(new Vec3(look.x, 0, look.z).normalize().scale(i * 1.25));
                level.addFreshEntity(new EvokerFangs(level, at.x, player.getY(), at.z, player.getYRot(), i, player));
            }
            return true;
        }
        if (type == EntityType.BREEZE) {
            Vec3 e = player.getEyePosition();
            WindCharge charge = new WindCharge(player, level, e.x, e.y - 0.1, e.z);
            charge.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.5f, 0);
            level.addFreshEntity(charge);
            return true;
        }
        if (type == EntityType.SHULKER) {
            LivingEntity target = target(player, 32);
            if (target == null) {
                return false;
            }
            effect(target, MobEffects.LEVITATION, 4, 0);
            hurt(player, target, 4);
            return true;
        }
        if (type == EntityType.IRON_GOLEM || type == EntityType.RAVAGER) {
            for (LivingEntity e : cone(player, 4, 45)) {
                hurt(player, e, 8);
                setVelocity(e, new Vec3(0, 1, 0));
            }
            return true;
        }
        if (type == EntityType.WITCH) {
            for (LivingEntity e : livingAround(player, 5)) {
                if (!isFriend(player, e)) {
                    effect(e, MobEffects.POISON, 5, 0);
                    effect(e, MobEffects.MOVEMENT_SLOWDOWN, 5, 1);
                }
            }
            particles(player, player.position().add(0, 1, 0), ParticleTypes.WITCH, 30, 2);
            return true;
        }
        if (type == EntityType.CHICKEN) {
            ItemEntity egg = new ItemEntity(level, player.getX(), player.getY() + 0.5, player.getZ(), new ItemStack(Items.EGG));
            level.addFreshEntity(egg);
            return true;
        }
        return false;
    }
}
