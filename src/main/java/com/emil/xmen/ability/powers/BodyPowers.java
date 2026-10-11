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


/** Body & Strength. */
public final class BodyPowers {
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final AttributeModifier.Operation MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    private static final AttributeModifier.Operation TOTAL = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;

    private BodyPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private static boolean isMelee(net.minecraft.world.damagesource.DamageSource source) {
        return source.getEntity() != null && source.getDirectEntity() == source.getEntity()
                && !source.is(DamageTypeTags.IS_PROJECTILE) && !source.is(DamageTypeTags.IS_EXPLOSION);
    }

    public static void register() {
        reg("super_strength", b()
                .attribute(Attributes.ATTACK_DAMAGE, 5, ADD)
                .attribute(Attributes.ATTACK_KNOCKBACK, 1, ADD)
                .attribute(Attributes.BLOCK_BREAK_SPEED, 1, MUL)
                .attribute(Attributes.KNOCKBACK_RESISTANCE, 0.5, ADD));

        // Every kind of damage is cut to less than half.
        reg("invulnerability", b().onHurt((player, event) -> {
            if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                event.setAmount(event.getAmount() * 0.4f);
            }
        }));

        // R: grow twice as big (or back). Big means longer reach, harder hits and more health, but slower.
        reg("giant_growth", b().active(2, player -> {
            boolean on = state(player).toggle("giant");
            if (on) {
                setModifier(player, Attributes.SCALE, "giant_scale", 1, TOTAL);
                setModifier(player, Attributes.ENTITY_INTERACTION_RANGE, "giant_reach", 2, ADD);
                setModifier(player, Attributes.BLOCK_INTERACTION_RANGE, "giant_breach", 2, ADD);
                setModifier(player, Attributes.ATTACK_DAMAGE, "giant_attack", 4, ADD);
                setModifier(player, Attributes.MAX_HEALTH, "giant_health", 10, ADD);
                setModifier(player, Attributes.STEP_HEIGHT, "giant_step", 1, ADD);
                setModifier(player, Attributes.MOVEMENT_SPEED, "giant_slow", -0.2, MUL);
            } else {
                for (var attr : List.of(Attributes.SCALE, Attributes.ENTITY_INTERACTION_RANGE,
                        Attributes.BLOCK_INTERACTION_RANGE, Attributes.ATTACK_DAMAGE, Attributes.MAX_HEALTH,
                        Attributes.STEP_HEIGHT, Attributes.MOVEMENT_SPEED)) {
                    for (String name : List.of("giant_scale", "giant_reach", "giant_breach", "giant_attack",
                            "giant_health", "giant_step", "giant_slow")) {
                        removeModifier(player, attr, name);
                    }
                }
                player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
            }
            sound(player, SoundEvents.RAVAGER_ROAR, 0.6f, on ? 0.6f : 1.6f);
            return true;
        }));

        // R: become tiny (or back). Tiny means you fit through small gaps, hits often miss you,
        // and mobs farther than 8 blocks don't notice you.
        reg("shrinking", b()
                .onHurt((player, event) -> {
                    if (state(player).flag("tiny") && event.getSource().getEntity() != null
                            && player.getRandom().nextFloat() < 0.3f) {
                        event.setCanceled(true);
                        tell(player, "Missed you!");
                    }
                })
                .ignoredBy((player, mob) -> state(player).flag("tiny") && mob.distanceTo(player) > 8)
                .active(2, player -> {
                    boolean on = state(player).toggle("tiny");
                    if (on) {
                        setModifier(player, Attributes.SCALE, "tiny_scale", -0.75, TOTAL);
                    } else {
                        removeModifier(player, Attributes.SCALE, "tiny_scale");
                    }
                    sound(player, SoundEvents.PUFFER_FISH_BLOW_OUT, 1, on ? 2 : 0.8f);
                    return true;
                }));

        // Kept from 0.3.0: R pops claws out (or back in) for much stronger punches.
        reg("claws", b().active(0.5, player -> {
            if (state(player).toggle("claws")) {
                setModifier(player, Attributes.ATTACK_DAMAGE, "claws", 7, ADD);
                tell(player, "Claws out! Your punches hit much harder");
            } else {
                removeModifier(player, Attributes.ATTACK_DAMAGE, "claws");
                tell(player, "Claws in");
            }
            sound(player, SoundEvents.PLAYER_ATTACK_SWEEP, 1, 1.6f);
            return true;
        }));

        // Bone spikes stick out of your skin and hurt whoever hits you. R shoots a volley of spikes.
        reg("bone_spikes", b()
                .onHurt((player, event) -> {
                    if (isMelee(event.getSource()) && event.getSource().getEntity() instanceof LivingEntity attacker
                            && attacker != player) {
                        hurt(player, attacker, 3, player.damageSources().thorns(player));
                        particlesAround(player, attacker, ParticleTypes.CRIT, 6);
                    }
                })
                .active(6, player -> {
                    for (int i = 0; i < 5; i++) {
                        PowerProjectiles.shoot(player, "bone_spike", new ItemStack(Items.BONE), 2.0f, 6, true);
                    }
                    sound(player, SoundEvents.SKELETON_SHOOT, 1, 0.7f);
                    return true;
                }));
        PowerProjectiles.register("bone_spike", (owner, projectile, hit) -> {
            if (owner != null && hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity target) {
                hurt(owner, target, 3);
            }
        });

        // R switches between heavy (nothing moves you, extra armor, slow), light (you float) and normal.
        reg("density_control", b().active(1, player -> {
            PowerState st = state(player);
            int mode = ((int) st.number("density") + 1) % 3;
            st.put("density", (double) mode);
            for (String name : List.of("heavy_kb", "heavy_armor", "heavy_slow", "light_gravity", "light_fall")) {
                removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, name);
                removeModifier(player, Attributes.ARMOR, name);
                removeModifier(player, Attributes.MOVEMENT_SPEED, name);
                removeModifier(player, Attributes.GRAVITY, name);
                removeModifier(player, Attributes.SAFE_FALL_DISTANCE, name);
            }
            if (mode == 1) {
                setModifier(player, Attributes.KNOCKBACK_RESISTANCE, "heavy_kb", 1, ADD);
                setModifier(player, Attributes.ARMOR, "heavy_armor", 6, ADD);
                setModifier(player, Attributes.MOVEMENT_SPEED, "heavy_slow", -0.3, MUL);
                tell(player, "Heavy: nothing can push you");
            } else if (mode == 2) {
                setModifier(player, Attributes.GRAVITY, "light_gravity", -0.8, TOTAL);
                setModifier(player, Attributes.SAFE_FALL_DISTANCE, "light_fall", 20, ADD);
                tell(player, "Light as air");
            } else {
                tell(player, "Normal density");
            }
            sound(player, SoundEvents.ANVIL_PLACE, 0.4f, mode == 2 ? 2 : 0.6f);
            return true;
        }));

        // Stretchy body: falls don't hurt and you can reach much farther.
        reg("rubber_body", b()
                .attribute(Attributes.ENTITY_INTERACTION_RANGE, 3, ADD)
                .attribute(Attributes.BLOCK_INTERACTION_RANGE, 3, ADD)
                .onFall((player, distance) -> true));

        // More arms: attack and mine faster.
        reg("extra_arms", b()
                .attribute(Attributes.ATTACK_SPEED, 2, ADD)
                .attribute(Attributes.BLOCK_BREAK_SPEED, 0.5, MUL));

        // Sprinting doesn't make you hungry.
        reg("endless_stamina", b().tick(player -> {
            if (player.isSprinting()) {
                player.getFoodData().setExhaustion(0);
            }
        }));

        // Thick hide: arrows and other projectiles only do half damage.
        reg("thick_hide", b()
                .attribute(Attributes.ARMOR, 2, ADD)
                .onHurt((player, event) -> {
                    if (event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
                        event.setAmount(event.getAmount() * 0.5f);
                    }
                }));

        // Spit acid that eats away at armor.
        PowerProjectiles.register("acid", (owner, projectile, hit) -> {
            if (owner == null) {
                return;
            }
            if (hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity target) {
                hurt(owner, target, 4);
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.isArmor()) {
                        ItemStack armor = target.getItemBySlot(slot);
                        if (!armor.isEmpty() && armor.isDamageableItem()) {
                            armor.hurtAndBreak(25, target, slot);
                        }
                    }
                }
            }
            particles(owner, hit.getLocation(), ParticleTypes.ITEM_SLIME, 12, 0.3);
            soundAt(owner, hit.getLocation(), SoundEvents.LAVA_EXTINGUISH, 0.6f, 1.5f);
        });
        reg("acid_spit", b().active(2, player -> {
            PowerProjectiles.shoot(player, "acid", new ItemStack(Items.SLIME_BALL), 1.5f, 1, true);
            sound(player, SoundEvents.LLAMA_SPIT, 1, 1);
            return true;
        }));

        // Your hits poison.
        reg("venom_strike", b().onAttack((player, target, event) -> effect(target, MobEffects.POISON, 4, 0)));

        // A tail: a little more reach and you step up blocks easily.
        reg("tail", b()
                .attribute(Attributes.ENTITY_INTERACTION_RANGE, 1.5, ADD)
                .attribute(Attributes.STEP_HEIGHT, 0.6, ADD));

        // Wings: press R to flap (again and again) to fly up and forward. Sneak in the air to glide down.
        reg("wings", b()
                .attribute(Attributes.FALL_DAMAGE_MULTIPLIER, -0.5, TOTAL)
                .tick(player -> {
                    if (player.isShiftKeyDown() && !player.onGround() && player.getDeltaMovement().y < -0.1) {
                        setVelocity(player, new Vec3(player.getDeltaMovement().x * 1.02, -0.1,
                                player.getDeltaMovement().z * 1.02));
                        player.resetFallDistance();
                    }
                })
                .active(0.6, player -> {
                    Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
                    setVelocity(player, look.scale(0.6).add(0, 0.85, 0));
                    player.resetFallDistance();
                    player.causeFoodExhaustion(0.3f);
                    particles(player, player.position().add(0, 1.2, 0), ParticleTypes.CLOUD, 4, 0.4);
                    sound(player, SoundEvents.PHANTOM_FLAP, 1, 1.2f);
                    return true;
                }));

        // A one-in-four chance to dodge any attack.
        reg("quick_reflexes", b().onHurt((player, event) -> {
            if (event.getSource().getEntity() != null && player.getRandom().nextFloat() < 0.25f) {
                event.setCanceled(true);
                particlesAround(player, player, ParticleTypes.CLOUD, 5);
                tell(player, "Dodged!");
            }
        }));

        // 10 seconds of rage: huge strength and speed, but you take more damage.
        reg("berserker_rage", b()
                .onHurt((player, event) -> {
                    if (state(player).active("rage", now(player))) {
                        event.setAmount(event.getAmount() * 1.5f);
                    }
                })
                .active(30, player -> {
                    state(player).start("rage", now(player), 200);
                    effect(player, MobEffects.DAMAGE_BOOST, 10, 2);
                    effect(player, MobEffects.MOVEMENT_SPEED, 10, 1);
                    particlesAround(player, player, ParticleTypes.ANGRY_VILLAGER, 8);
                    sound(player, SoundEvents.RAVAGER_ROAR, 1, 1.2f);
                    return true;
                }));

        reg("tough_bones", b().attribute(Attributes.FALL_DAMAGE_MULTIPLIER, -0.6, TOTAL));

        // Unbreakable metal bones: nothing knocks you back and no single hit can take more than 6 hearts,
        // but you sink in water.
        reg("metal_skeleton", b()
                .attribute(Attributes.KNOCKBACK_RESISTANCE, 1, ADD)
                .attribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, 1, ADD)
                .onHurt((player, event) -> {
                    if (event.getAmount() > 12 && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                        event.setAmount(12);
                    }
                })
                .tick(player -> {
                    if (player.isInWater() && !player.onGround()) {
                        player.setDeltaMovement(player.getDeltaMovement().add(0, -0.03, 0));
                        player.hurtMarked = true;
                    }
                }));

        // Rotten flesh, spider eyes and raw meat don't make you sick.
        reg("iron_stomach", b().onEat((player, food) -> Scheduler.later(1, () -> {
            player.removeEffect(MobEffects.HUNGER);
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.CONFUSION);
        })));
    }
}
