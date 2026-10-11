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

/** Energy & Blasts. */
public final class EnergyPowers {
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final DustParticleOptions RED_BEAM = new DustParticleOptions(new Vector3f(1.0f, 0.1f, 0.1f), 1.6f);
    private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.3f, 1.0f, 0.2f), 1.2f);
    private static final DustParticleOptions PLASMA = new DustParticleOptions(new Vector3f(1.0f, 0.4f, 0.9f), 1.4f);
    private static final DustParticleOptions LIGHT_BLUE = new DustParticleOptions(new Vector3f(0.4f, 0.8f, 1.0f), 1.0f);

    private EnergyPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    public static void register() {
        // Omega: one huge beam, or sneak for a cosmic barrier of absorption hearts.
        reg("cosmic_energy_control", b()
                .effect(MobEffects.DAMAGE_RESISTANCE, 0)
                .effect(MobEffects.FIRE_RESISTANCE, 0)
                .active(3, player -> {
                    if (player.isShiftKeyDown()) {
                        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 4));
                        ring(player, player.position().add(0, 1, 0), 1.5, ParticleTypes.END_ROD);
                        sound(player, SoundEvents.BEACON_ACTIVATE, 1, 1.5f);
                        tell(player, "Cosmic barrier up");
                        return true;
                    }
                    Vec3 start = player.getEyePosition();
                    particleLine(player, start.add(player.getLookAngle()), lookTarget(player, 48), ParticleTypes.END_ROD,
                            0.3);
                    for (LivingEntity target : beam(player, 48, 1.2)) {
                        hurt(player, target, 14);
                        target.knockback(1.5, -player.getLookAngle().x, -player.getLookAngle().z);
                    }
                    sound(player, SoundEvents.BEACON_POWER_SELECT, 1, 0.5f);
                    return true;
                }));

        // Stores part of every hit and throws it back as a shockwave once enough has built up.
        reg("energy_absorption", b().onHurt((player, event) -> {
            float absorbed = event.getAmount() * 0.4f;
            event.setAmount(event.getAmount() - absorbed);
            PowerState state = state(player);
            double stored = state.add("absorbed", absorbed);
            tell(player, "Stored energy: " + (int) stored + "/20");
            if (stored >= 20) {
                state.put("absorbed", 0.0);
                for (LivingEntity target : livingAround(player, 6)) {
                    hurt(player, target, (float) stored);
                    pushFrom(player.position(), target, 1.5, 0.5);
                }
                ring(player, player.position().add(0, 1, 0), 3, ParticleTypes.ELECTRIC_SPARK);
                ring(player, player.position().add(0, 1, 0), 5, ParticleTypes.ELECTRIC_SPARK);
                sound(player, SoundEvents.GENERIC_EXPLODE, 1, 1.4f);
            }
        }));

        // Builds a glowing bridge or staircase toward where you look.
        reg("hard_light_constructs", b().active(4, player -> {
            Vec3 look = player.getLookAngle();
            Vec3 start = player.position().add(0, -0.5, 0);
            ServerLevel level = level(player);
            int placed = 0;
            for (int i = 1; i <= 14; i++) {
                BlockPos pos = BlockPos.containing(start.add(look.scale(i)));
                if (TempBlocks.place(level, pos, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(), 300)) {
                    placed++;
                }
            }
            if (placed == 0) {
                tell(player, "No room to build there");
                return false;
            }
            sound(player, SoundEvents.AMETHYST_BLOCK_CHIME, 1, 1.5f);
            return true;
        }));

        // Toggle: a poison aura around you, but it makes you hungry.
        reg("radiation_aura", b()
                .every(20, player -> {
                    if (!state(player).flag("aura")) {
                        return;
                    }
                    for (LivingEntity target : livingAround(player, 6)) {
                        if (!isFriend(player, target)) {
                            effect(target, MobEffects.POISON, 2, 0);
                            effect(target, MobEffects.WEAKNESS, 2, 0);
                        }
                    }
                    buff(player, MobEffects.HUNGER, 0);
                    ring(player, player.position().add(0, 0.5, 0), 6, GREEN);
                })
                .active(1, player -> {
                    tell(player, state(player).toggle("aura") ? "Radiation aura on" : "Radiation aura off");
                    return true;
                }));

        // Deletes the blocks in a line (no drops).
        reg("disintegration_ray", b().active(4, player -> {
            ServerLevel level = level(player);
            Vec3 start = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            for (double d = 1; d <= 24; d += 0.5) {
                BlockPos pos = BlockPos.containing(start.add(look.scale(d)));
                BlockState state = level.getBlockState(pos);
                if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0 && state.getDestroySpeed(level, pos) < 50
                        && !state.hasBlockEntity()) {
                    level.destroyBlock(pos, false, player);
                }
            }
            particleLine(player, start.add(look), start.add(look.scale(24)), ParticleTypes.REVERSE_PORTAL, 0.3);
            for (LivingEntity target : beam(player, 24, 0.4)) {
                hurt(player, target, 6);
            }
            sound(player, SoundEvents.BEACON_DEACTIVATE, 1, 2);
            return true;
        }));

        // Throws a glowing copy of what you hold (a card if empty) that explodes on impact.
        PowerProjectiles.register("kinetic_card", (owner, projectile, hit) -> {
            if (owner != null) {
                explode(owner, hit.getLocation(), 1.6f, false);
            }
        });
        reg("kinetic_charging", b().active(1.5, player -> {
            ItemStack held = player.getMainHandItem();
            ItemStack look = held.isEmpty() ? new ItemStack(Items.PAPER) : held.copyWithCount(1);
            PowerProjectiles.shoot(player, "kinetic_card", look, 2.0f, 0.5f, false);
            particles(player, player.getEyePosition().add(player.getLookAngle()), PLASMA, 8, 0.1);
            sound(player, SoundEvents.SNOWBALL_THROW, 1, 0.6f);
            return true;
        }));

        // Long beam that makes everything it touches glow and blinds it for a moment.
        reg("photon_beam", b().active(2, player -> {
            particleLine(player, player.getEyePosition().add(player.getLookAngle()), lookTarget(player, 64),
                    ParticleTypes.GLOW, 0.5);
            for (LivingEntity target : beam(player, 64, 0.5)) {
                hurt(player, target, 5);
                glow(target, 10);
                effect(target, MobEffects.BLINDNESS, 2, 0);
            }
            sound(player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1, 2);
            return true;
        }));

        // Touch a mob or block, and it blows up three seconds later.
        reg("explosive_touch", b().active(5, player -> {
            LivingEntity target = lookedAtLiving(player, 4);
            if (target != null) {
                glow(target, 3);
                particlesAround(player, target, ParticleTypes.FLAME, 15);
                Scheduler.later(60, () -> {
                    if (target.isAlive()) {
                        explode(player, target.position(), 2.0f, false);
                    }
                });
                tell(player, "Marked! It explodes in 3 seconds");
                return true;
            }
            BlockPos pos = lookedAtPos(player, 4);
            if (pos == null) {
                tell(player, "Touch a mob or a block");
                return false;
            }
            particles(player, Vec3.atCenterOf(pos), ParticleTypes.FLAME, 15, 0.4);
            Scheduler.later(60, () -> explode(player, Vec3.atCenterOf(pos), 2.0f, true));
            tell(player, "Marked! It explodes in 3 seconds");
            return true;
        }));

        // Weak at night, very strong in the sun. Sets things on fire.
        reg("solar_beams", b().active(2, player -> {
            boolean sunny = inSunlight(player);
            float damage = sunny ? 10 : 4;
            particleLine(player, player.getEyePosition().add(player.getLookAngle()), lookTarget(player, 32),
                    ParticleTypes.FLAME, 0.4);
            for (LivingEntity target : beam(player, 32, 0.5)) {
                hurt(player, target, damage);
                target.igniteForSeconds(sunny ? 6 : 2);
            }
            if (!sunny) {
                tell(player, "Weak without sunlight");
            }
            sound(player, SoundEvents.FIRECHARGE_USE, 1, 1.5f);
            return true;
        }));

        // Slow, floating orbs of plasma.
        PowerProjectiles.register("plasma", (owner, projectile, hit) -> {
            if (owner != null && hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity target) {
                hurt(owner, target, 7);
                target.igniteForSeconds(2);
            }
            if (owner != null) {
                particles(owner, hit.getLocation(), PLASMA, 20, 0.4);
            }
        });
        reg("plasma_bolts", b().active(1, player -> {
            PowerProjectiles.shoot(player, "plasma", new ItemStack(Items.MAGMA_CREAM), 0.8f, 0, false);
            sound(player, SoundEvents.SHULKER_SHOOT, 1, 1.2f);
            return true;
        }));

        // A scream in front of you that hurts, stuns and makes mobs dizzy.
        reg("sonic_scream", b().active(4, player -> {
            for (LivingEntity target : cone(player, 10, 30)) {
                hurt(player, target, 4);
                effect(target, MobEffects.MOVEMENT_SLOWDOWN, 2, 3);
                effect(target, MobEffects.CONFUSION, 4, 0);
            }
            Vec3 look = player.getLookAngle();
            for (int i = 1; i <= 5; i++) {
                Vec3 at = player.getEyePosition().add(look.scale(i * 2));
                level(player).sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
            sound(player, SoundEvents.WARDEN_SONIC_BOOM, 1, 1.5f);
            return true;
        }));

        // Hits one mob and jumps to up to four more nearby.
        reg("chain_lightning", b().active(3, player -> {
            LivingEntity first = target(player, 20);
            if (first == null) {
                return false;
            }
            List<LivingEntity> hit = new ArrayList<>();
            LivingEntity current = first;
            Vec3 from = player.getEyePosition();
            for (int jump = 0; jump < 5 && current != null; jump++) {
                hit.add(current);
                particleLine(player, from, current.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK, 0.3);
                hurt(player, current, jump == 0 ? 6 : 4);
                from = current.getBoundingBox().getCenter();
                LivingEntity next = null;
                double best = 25;
                for (LivingEntity candidate : livingNear(player, current.position(), 5)) {
                    double d = candidate.distanceToSqr(current);
                    if (!hit.contains(candidate) && !isFriend(player, candidate) && d < best) {
                        best = d;
                        next = candidate;
                    }
                }
                current = next;
            }
            sound(player, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.6f);
            return true;
        }));

        // For five seconds, every arrow and fireball that reaches you is destroyed.
        reg("energy_shield", b()
                .stopsProjectiles((player, projectile) -> {
                    if (!state(player).active("shield", now(player))) {
                        return false;
                    }
                    particles(player, projectile.position(), ParticleTypes.ELECTRIC_SPARK, 8, 0.2);
                    projectile.discard();
                    return true;
                })
                .every(4, player -> {
                    if (state(player).active("shield", now(player))) {
                        ring(player, player.position().add(0, 1, 0), 1.2, LIGHT_BLUE);
                    }
                })
                .active(12, player -> {
                    state(player).start("shield", now(player), 100);
                    sound(player, SoundEvents.BEACON_ACTIVATE, 1, 2);
                    return true;
                }));

        // Getting hit charges your next punch to double damage.
        reg("reactive_charge", b()
                .onHurt((player, event) -> {
                    if (!state(player).flag("charged")) {
                        state(player).setFlag("charged", true);
                        tell(player, "Charged! Your next punch does double damage");
                    }
                })
                .onAttack((player, target, event) -> {
                    if (state(player).flag("charged")) {
                        state(player).setFlag("charged", false);
                        event.setAmount(event.getAmount() * 2);
                        particlesAround(player, target, ParticleTypes.ELECTRIC_SPARK, 15);
                    }
                }));

        // Punch the air: a narrow blast that throws mobs far away.
        reg("concussive_punch", b().active(2, player -> {
            for (LivingEntity target : cone(player, 6, 20)) {
                hurt(player, target, 1);
                push(player, target, 3, 0.6);
            }
            particles(player, player.getEyePosition().add(player.getLookAngle().scale(2)), ParticleTypes.EXPLOSION, 1,
                    0);
            sound(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1, 0.6f);
            return true;
        }));

        // Cooks the raw food in your inventory and heats up mobs around you.
        reg("microwave_pulse", b().active(6, player -> {
            ServerLevel level = level(player);
            int cooked = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.isEmpty() || !stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                    continue;
                }
                var input = new net.minecraft.world.item.crafting.SingleRecipeInput(stack);
                var recipe = level.getRecipeManager().getRecipeFor(
                        net.minecraft.world.item.crafting.RecipeType.SMELTING, input, level);
                if (recipe.isPresent()) {
                    ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
                    result.setCount(stack.getCount() * result.getCount());
                    player.getInventory().setItem(slot, result);
                    cooked += stack.getCount();
                }
            }
            for (LivingEntity target : livingAround(player, 5)) {
                if (!isFriend(player, target)) {
                    hurt(player, target, 2);
                    target.igniteForSeconds(2);
                }
            }
            ring(player, player.position().add(0, 1, 0), 3, ParticleTypes.SMALL_FLAME);
            tell(player, cooked > 0 ? "Cooked " + cooked + " food" : "Nothing to cook");
            sound(player, SoundEvents.FURNACE_FIRE_CRACKLE, 1, 1.5f);
            return true;
        }));

        // Two quick short-range energy bolts.
        PowerProjectiles.register("palm_blast", (owner, projectile, hit) -> {
            if (owner != null && hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity target) {
                hurt(owner, target, 4);
            }
            if (owner != null) {
                particles(owner, hit.getLocation(), ParticleTypes.CRIT, 8, 0.2);
            }
        });
        reg("palm_blasts", b().active(1.2, player -> {
            PowerProjectiles.shoot(player, "palm_blast", new ItemStack(Items.GLOWSTONE_DUST), 2.2f, 2, true);
            Scheduler.later(4, () -> PowerProjectiles.shoot(player, "palm_blast", new ItemStack(Items.GLOWSTONE_DUST),
                    2.2f, 2, true));
            sound(player, SoundEvents.SHULKER_SHOOT, 0.6f, 2);
            return true;
        }));

        // Punches deal a little extra shock damage.
        reg("bio_electric_touch", b().onAttack((player, target, event) -> {
            event.setAmount(event.getAmount() + 2);
            particlesAround(player, target, ParticleTypes.ELECTRIC_SPARK, 6);
        }));

        // Mobs you punch are slowed for a moment.
        reg("static_shock", b().onAttack((player, target, event) -> {
            effect(target, MobEffects.MOVEMENT_SLOWDOWN, 1.5, 1);
            particlesAround(player, target, ParticleTypes.WAX_OFF, 6);
        }));

        // Kept from 0.3.0.
        reg("optic_blasts", b().active(2, EnergyPowers::opticBlast));
    }

    private static boolean opticBlast(ServerPlayer player) {
        Vec3 start = player.getEyePosition().add(0, -0.1, 0);
        Vec3 end = lookTarget(player, 32);
        particleLine(player, start.add(player.getLookAngle()), end, RED_BEAM, 0.4);
        for (LivingEntity target : beam(player, 32, 0.4)) {
            hurt(player, target, 8);
            Vec3 look = player.getLookAngle();
            target.knockback(1.2, -look.x, -look.z);
        }
        sound(player, SoundEvents.BLAZE_SHOOT, 1, 1.6f);
        return true;
    }
}
