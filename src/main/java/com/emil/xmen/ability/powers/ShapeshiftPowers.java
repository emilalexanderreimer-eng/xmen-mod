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
import net.minecraft.world.entity.monster.Slime;

/** Shapeshifting. */
public final class ShapeshiftPowers {
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final AttributeModifier.Operation MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    private static final AttributeModifier.Operation TOTAL = AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;

    private ShapeshiftPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private record Mod(Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double amount,
            AttributeModifier.Operation operation) {
    }

    /** A form you switch on and off with R: a look, attribute changes and maybe flight. */
    private static PowerBehavior.Builder form(String key, EntityType<?> look, boolean flies, List<Mod> mods,
            PowerBehavior.Builder builder, double cooldown) {
        return builder
                .every(10, player -> {
                    if (state(player).flag(key)) {
                        for (int i = 0; i < mods.size(); i++) {
                            Mod mod = mods.get(i);
                            setModifier(player, mod.attribute(), key + "_" + i, mod.amount(), mod.operation());
                        }
                    }
                })
                .active(cooldown, player -> {
                    boolean on = state(player).toggle(key);
                    if (on) {
                        for (int i = 0; i < mods.size(); i++) {
                            Mod mod = mods.get(i);
                            setModifier(player, mod.attribute(), key + "_" + i, mod.amount(), mod.operation());
                        }
                        Morphs.set(player, look);
                        state(player).onClear(() -> Morphs.set(player, null));
                    } else {
                        for (int i = 0; i < mods.size(); i++) {
                            removeModifier(player, mods.get(i).attribute(), key + "_" + i);
                        }
                        Morphs.set(player, null);
                        if (player.getHealth() > player.getMaxHealth()) {
                            player.setHealth(player.getMaxHealth());
                        }
                    }
                    if (flies) {
                        PowerRuntime.setTemporaryFlight(player, on);
                    }
                    particlesAround(player, player, ParticleTypes.POOF, 20);
                    sound(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1, on ? 0.8f : 1.2f);
                    return true;
                });
    }

    private static boolean inForm(ServerPlayer player, String key) {
        return state(player).flag(key);
    }

    /** Morph + traits used by Total Transformation and absorbed mob powers. */
    private static void becomeMob(ServerPlayer player, EntityType<?> type, String key) {
        state(player).put(key, type);
        Morphs.set(player, type);
        MobTraits.apply(player, type);
        state(player).onClear(() -> {
            Morphs.set(player, null);
            MobTraits.remove(player);
        });
        particlesAround(player, player, ParticleTypes.POOF, 30);
        sound(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1, 0.7f);
    }

    private static void stopMob(ServerPlayer player, String key) {
        state(player).remove(key);
        Morphs.set(player, null);
        MobTraits.remove(player);
        particlesAround(player, player, ParticleTypes.POOF, 30);
    }

    public static void register() {
        // Look like the last kind of mob you hit (R), or yourself again (sneak + R). Mobs of that kind leave you alone.
        reg("shapeshifting", b()
                .onAttack((player, target, event) -> state(player).put("last_hit_type", target.getType()))
                .ignoredBy((player, mob) -> MobTraits.sameKind(Morphs.get(player), mob))
                .active(3, player -> {
                    if (player.isShiftKeyDown()) {
                        Morphs.set(player, null);
                        tell(player, "Back to yourself");
                        return true;
                    }
                    EntityType<?> type = state(player).get("last_hit_type");
                    LivingEntity looked = lookedAtLiving(player, 16);
                    if (looked != null && !(looked instanceof Player)) {
                        type = looked.getType();
                    }
                    if (type == null) {
                        tell(player, "Hit a mob (or look at one) to copy how it looks");
                        return false;
                    }
                    Morphs.set(player, type);
                    state(player).onClear(() -> Morphs.set(player, null));
                    for (LivingEntity e : livingAround(player, 32)) {
                        if (e instanceof Mob mob && MobTraits.sameKind(type, mob)) {
                            forget(player, mob);
                        }
                    }
                    particlesAround(player, player, ParticleTypes.POOF, 20);
                    tell(player, "You look like a " + type.getDescription().getString());
                    return true;
                }));

        // R: become a wolf (or back): fast, with a bite that slows.
        reg("wolf_form", form("wolf", EntityType.WOLF, false, List.of(
                new Mod(Attributes.SCALE, -0.45, TOTAL),
                new Mod(Attributes.MOVEMENT_SPEED, 0.35, MUL),
                new Mod(Attributes.ATTACK_DAMAGE, 3, ADD)), b()
                        .onAttack((player, target, event) -> {
                            if (inForm(player, "wolf")) {
                                effect(target, MobEffects.MOVEMENT_SLOWDOWN, 2, 1);
                            }
                        }), 2));

        // R: become a parrot (or back) and fly. You're small and fragile.
        reg("bird_form", form("bird", EntityType.PARROT, true, List.of(
                new Mod(Attributes.SCALE, -0.6, TOTAL),
                new Mod(Attributes.MAX_HEALTH, -8, ADD)), b(), 2));

        // R: turn into a small tropical fish (or back). Super fast and breathes underwater, but slow on land.
        reg("fish_form", form("fish", EntityType.TROPICAL_FISH, false, List.of(
                new Mod(Attributes.SCALE, -0.6, TOTAL)), b()
                        .every(10, player -> {
                            if (inForm(player, "fish")) {
                                if (player.isInWater()) {
                                    buff(player, MobEffects.DOLPHINS_GRACE, 1);
                                    buff(player, MobEffects.WATER_BREATHING, 0);
                                    buff(player, MobEffects.NIGHT_VISION, 0);
                                } else {
                                    buff(player, MobEffects.MOVEMENT_SLOWDOWN, 1);
                                }
                            }
                        }), 2));

        // Blue furry body: strong, jumps high, fast, and climbs walls (climbing in XMenClient).
        reg("beast_form", b()
                .attribute(Attributes.ATTACK_DAMAGE, 3, ADD)
                .attribute(Attributes.JUMP_STRENGTH, 0.2, ADD)
                .attribute(Attributes.MOVEMENT_SPEED, 0.2, MUL)
                .attribute(Attributes.SAFE_FALL_DISTANCE, 4, ADD));

        // R: become a ghost for 8 seconds: nothing can touch you and nothing hunts you, but you can't hit either.
        reg("ghost_form", b()
                .onHurt((player, event) -> {
                    if (state(player).active("ghost", now(player))
                            && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                        event.setCanceled(true);
                    }
                })
                .onAttack((player, target, event) -> {
                    if (state(player).active("ghost", now(player))) {
                        event.setCanceled(true);
                    }
                })
                .ignoredBy((player, mob) -> state(player).active("ghost", now(player)))
                .every(4, player -> {
                    if (state(player).active("ghost", now(player))) {
                        particlesAround(player, player, ParticleTypes.SOUL, 2);
                        player.resetFallDistance();
                    }
                })
                .active(20, player -> {
                    state(player).start("ghost", now(player), 160);
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0, false, false, true));
                    for (LivingEntity e : livingAround(player, 32)) {
                        forget(player, e);
                    }
                    sound(player, SoundEvents.SOUL_ESCAPE, 1, 0.8f);
                    return true;
                }));

        // R: turn to stone (or back). As a statue you can't move, but nothing hurts you and mobs ignore you.
        reg("statue_form", b()
                .onHurt((player, event) -> {
                    if (state(player).flag("statue")
                            && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                        event.setCanceled(true);
                    }
                })
                .ignoredBy((player, mob) -> state(player).flag("statue"))
                .every(10, player -> {
                    if (state(player).flag("statue")) {
                        level(player).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK,
                                Blocks.STONE.defaultBlockState()), player.getX(), player.getY() + 1, player.getZ(), 2,
                                0.3, 0.5, 0.3, 0);
                    }
                })
                .active(2, player -> {
                    boolean on = state(player).toggle("statue");
                    if (on) {
                        setModifier(player, Attributes.MOVEMENT_SPEED, "statue_still", -1, TOTAL);
                        setModifier(player, Attributes.JUMP_STRENGTH, "statue_jump", -1, TOTAL);
                        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, "statue_kb", 1, ADD);
                        for (LivingEntity e : livingAround(player, 32)) {
                            forget(player, e);
                        }
                    } else {
                        removeModifier(player, Attributes.MOVEMENT_SPEED, "statue_still");
                        removeModifier(player, Attributes.JUMP_STRENGTH, "statue_jump");
                        removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, "statue_kb");
                    }
                    tell(player, on ? "You are a statue" : "You can move again");
                    sound(player, SoundEvents.STONE_PLACE, 1, on ? 0.5f : 1.2f);
                    return true;
                }));

        // You copy the power of the nearest other mutant player for 60 seconds.
        reg("power_mimic", b().every(100, player -> {
            ServerPlayer nearest = null;
            double best = 16 * 16;
            for (ServerPlayer other : level(player).players()) {
                double d = other.distanceToSqr(player);
                if (other != player && d < best && com.emil.xmen.power.PlayerPowers.get(other) != null
                        && !"power_mimic".equals(PowerRuntime.activePowerId(other))) {
                    best = d;
                    nearest = other;
                }
            }
            if (nearest != null) {
                String id = PowerRuntime.activePowerId(nearest);
                if (id != null && PowerBehaviors.get(id) != null) {
                    PowerRuntime.borrow(player, id, 1200);
                    chat(player, "You copied " + nearest.getName().getString() + "'s power for 60 seconds: "
                            + com.emil.xmen.power.Powers.get(id).name());
                }
            }
        }));

        // Touch a player to steal their power for 60 seconds (they lose it meanwhile).
        // Touch a mob to drain it: you take part of its life and its special move (R) for 60 seconds,
        // and the mob is left weak and slow.
        reg("power_absorption", b().active(3, player -> {
            PowerState st = state(player);
            EntityType<?> stolen = st.get("stolen_move");
            if (stolen != null && st.active("stolen", now(player)) && !player.isShiftKeyDown()) {
                if (!MobTraits.special(player, stolen)) {
                    tell(player, "That mob had no special move. Sneak + R to absorb something else");
                    return false;
                }
                return true;
            }
            if (st.active("absorb_rest", now(player))) {
                tell(player, "Ready in " + st.remaining("absorb_rest", now(player)) / 20 + "s");
                return false;
            }
            LivingEntity target = target(player, 4);
            if (target == null) {
                return false;
            }
            if (target instanceof ServerPlayer other) {
                String id = PowerRuntime.activePowerId(other);
                if (id == null || PowerBehaviors.get(id) == null) {
                    tell(player, other.getName().getString() + " has nothing to steal");
                    return false;
                }
                PowerRuntime.suppress(other, 1200);
                PowerRuntime.borrow(player, id, 1200);
                chat(other, player.getName().getString() + " absorbed your power for 60 seconds!");
                chat(player, "You absorbed " + com.emil.xmen.power.Powers.get(id).name() + " for 60 seconds");
            } else {
                st.put("stolen_move", target.getType());
                st.start("stolen", now(player), 1200);
                int hearts = (int) Math.min(8, target.getMaxHealth() / 4);
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 1200, Math.max(0, hearts / 2 - 1)));
                effect(target, MobEffects.WEAKNESS, 60, 2);
                effect(target, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
                hurtMagic(player, target, 4);
                chat(player, "You drained the " + target.getName().getString() + ". R uses its special move for 60 seconds");
            }
            st.start("absorb_rest", now(player), 1200);
            particleLine(player, target.getBoundingBox().getCenter(), player.getBoundingBox().getCenter(),
                    ParticleTypes.ENCHANT, 0.3);
            return false;
        }));

        // R: become small like a baby mob (or back): quick, but a bit weaker.
        reg("baby_form", form("baby", null, false, List.of(
                new Mod(Attributes.SCALE, -0.5, TOTAL),
                new Mod(Attributes.MOVEMENT_SPEED, 0.4, MUL),
                new Mod(Attributes.MAX_HEALTH, -4, ADD)), b(), 2));

        // Slime body: no fall damage, getting hit splits off little slimes that fight for you,
        // and R makes a huge slimy leap.
        reg("slime_body", b()
                .onFall((player, distance) -> true)
                .onHurt((player, event) -> {
                    if (event.getSource().getEntity() instanceof LivingEntity && event.getAmount() >= 2
                            && !state(player).active("split", now(player))) {
                        state(player).start("split", now(player), 60);
                        Slime slime = EntityType.SLIME.create(player.level());
                        if (slime != null) {
                            slime.setSize(1, true);
                            slime.moveTo(player.getX(), player.getY() + 0.5, player.getZ());
                            Allies.summon(player, slime, 20);
                        }
                    }
                })
                .active(3, player -> {
                    Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
                    setVelocity(player, look.scale(0.8).add(0, 1.3, 0));
                    particles(player, player.position(), ParticleTypes.ITEM_SLIME, 20, 0.5);
                    sound(player, SoundEvents.SLIME_JUMP, 1, 0.6f);
                    return true;
                }));

        // R: become a swarm of bees for 10 seconds: fly, punches pass through you, and you sting mobs nearby.
        reg("bee_swarm_form", b()
                .onHurt((player, event) -> {
                    if (state(player).active("swarm", now(player)) && event.getSource().getDirectEntity() instanceof LivingEntity) {
                        event.setCanceled(true);
                    }
                })
                .every(10, player -> {
                    PowerState st = state(player);
                    if (st.flag("swarming") && !st.active("swarm", now(player))) {
                        st.setFlag("swarming", false);
                        Morphs.set(player, null);
                        removeModifier(player, Attributes.SCALE, "swarm_scale");
                        PowerRuntime.setTemporaryFlight(player, false);
                        return;
                    }
                    if (st.active("swarm", now(player))) {
                        for (LivingEntity e : livingAround(player, 2.5)) {
                            if (!isFriend(player, e)) {
                                hurt(player, e, 1.5f);
                                effect(e, MobEffects.POISON, 3, 0);
                            }
                        }
                        particles(player, player.position().add(0, 0.5, 0), ParticleTypes.FALLING_NECTAR, 4, 0.8);
                    }
                })
                .active(25, player -> {
                    state(player).start("swarm", now(player), 200);
                    state(player).setFlag("swarming", true);
                    Morphs.set(player, EntityType.BEE);
                    setModifier(player, Attributes.SCALE, "swarm_scale", -0.65, TOTAL);
                    PowerRuntime.setTemporaryFlight(player, true);
                    state(player).onClear(() -> Morphs.set(player, null));
                    sound(player, SoundEvents.BEE_LOOP_AGGRESSIVE, 1, 1);
                    return true;
                }));

        // R: become a dragon for 30 seconds: big, flying and tough. R again breathes dragon fire.
        // Sneak + R turns you back early.
        reg("dragon_form", b()
                .every(10, player -> {
                    PowerState st = state(player);
                    if (!st.flag("dragon")) {
                        return;
                    }
                    if (!st.active("dragon_time", now(player))) {
                        st.setFlag("dragon", false);
                        stopMob(player, "dragon_type");
                        tell(player, "You turn back into a human");
                    } else {
                        MobTraits.apply(player, EntityType.ENDER_DRAGON);
                    }
                })
                .active(1.5, player -> {
                    PowerState st = state(player);
                    if (st.flag("dragon")) {
                        if (player.isShiftKeyDown()) {
                            st.setFlag("dragon", false);
                            stopMob(player, "dragon_type");
                            return true;
                        }
                        return MobTraits.special(player, EntityType.ENDER_DRAGON);
                    }
                    if (st.active("dragon_rest", now(player))) {
                        tell(player, "Ready in " + st.remaining("dragon_rest", now(player)) / 20 + "s");
                        return false;
                    }
                    st.setFlag("dragon", true);
                    st.start("dragon_time", now(player), 600);
                    st.start("dragon_rest", now(player), 1800);
                    becomeMob(player, EntityType.ENDER_DRAGON, "dragon_type");
                    sound(player, SoundEvents.ENDER_DRAGON_GROWL, 1, 1);
                    return true;
                }));

        // Stronger and faster at night.
        reg("werewolf", b().every(20, player -> {
            if (player.level().isNight()) {
                buff(player, MobEffects.DAMAGE_BOOST, 1);
                buff(player, MobEffects.MOVEMENT_SPEED, 0);
                buff(player, MobEffects.JUMP, 0);
            }
        }));

        // R cycles through four bodies: fire, water, earth and air.
        reg("elemental_form", b()
                .immuneTo((player, source) -> (int) state(player).number("element") == 1
                        && source.is(DamageTypeTags.IS_FIRE))
                .onAttack((player, target, event) -> {
                    if ((int) state(player).number("element") == 1) {
                        target.igniteForSeconds(4);
                    }
                })
                .every(10, player -> {
                    int element = (int) state(player).number("element");
                    switch (element) {
                        case 1 -> particlesAround(player, player, ParticleTypes.FLAME, 3);
                        case 2 -> {
                            buff(player, MobEffects.WATER_BREATHING, 0);
                            buff(player, MobEffects.DOLPHINS_GRACE, 0);
                            player.resetFallDistance();
                            particlesAround(player, player, ParticleTypes.SPLASH, 4);
                        }
                        case 3 -> {
                            buff(player, MobEffects.DAMAGE_RESISTANCE, 1);
                            buff(player, MobEffects.MOVEMENT_SLOWDOWN, 0);
                            setModifier(player, Attributes.KNOCKBACK_RESISTANCE, "earth_kb", 1, ADD);
                        }
                        case 4 -> {
                            buff(player, MobEffects.MOVEMENT_SPEED, 1);
                            buff(player, MobEffects.JUMP, 1);
                            buff(player, MobEffects.SLOW_FALLING, 0);
                            particlesAround(player, player, ParticleTypes.CLOUD, 2);
                        }
                        default -> {
                        }
                    }
                })
                .active(3, player -> {
                    int element = ((int) state(player).number("element") + 1) % 5;
                    state(player).put("element", (double) element);
                    removeModifier(player, Attributes.KNOCKBACK_RESISTANCE, "earth_kb");
                    String[] names = { "Normal body", "Fire body", "Water body", "Earth body", "Air body" };
                    tell(player, names[element]);
                    sound(player, SoundEvents.EVOKER_CAST_SPELL, 1, 0.8f + element * 0.2f);
                    return true;
                }));

        // You teleport away when hit and water hurts you, like an enderman. R picks up the block you look at.
        reg("enderman_form", b()
                .onHurt((player, event) -> {
                    if (event.getSource().getEntity() != null && player.getRandom().nextFloat() < 0.5f) {
                        for (int i = 0; i < 16; i++) {
                            BlockPos spot = safeSpotNear(level(player), player.blockPosition().offset(
                                    player.getRandom().nextInt(17) - 8, 0, player.getRandom().nextInt(17) - 8));
                            if (spot != null) {
                                particlesAround(player, player, ParticleTypes.PORTAL, 20);
                                teleport(player, Vec3.atBottomCenterOf(spot));
                                sound(player, SoundEvents.ENDERMAN_TELEPORT, 1, 1);
                                event.setCanceled(true);
                                return;
                            }
                        }
                    }
                })
                .every(20, player -> {
                    if (player.isInWaterOrRain()) {
                        player.hurt(player.damageSources().drown(), 1);
                    }
                })
                .active(1, player -> {
                    ServerLevel level = level(player);
                    BlockPos pos = lookedAtPos(player, 8);
                    if (pos == null) {
                        return false;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.hasBlockEntity() || state.getDestroySpeed(level, pos) < 0
                            || state.getDestroySpeed(level, pos) > 20 || state.getBlock().asItem() == Items.AIR) {
                        tell(player, "You can't pick that up");
                        return false;
                    }
                    level.removeBlock(pos, false);
                    ItemStack item = new ItemStack(state.getBlock().asItem());
                    if (!player.getInventory().add(item)) {
                        player.drop(item, false);
                    }
                    particles(player, Vec3.atCenterOf(pos), ParticleTypes.PORTAL, 15, 0.3);
                    sound(player, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 1.6f);
                    return true;
                }));

        // Zombies, skeletons and other undead leave you alone, but sunlight makes you weak.
        reg("undead_body", b()
                .ignoredBy((player, mob) -> mob.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD))
                .every(20, player -> {
                    if (inSunlight(player)) {
                        buff(player, MobEffects.WEAKNESS, 0);
                    }
                }));

        // R: become an iron golem (or back): huge, strong, lots of health, slow. Hits throw mobs up.
        reg("golem_form", form("golem", EntityType.IRON_GOLEM, false, List.of(
                new Mod(Attributes.SCALE, 0.4, TOTAL),
                new Mod(Attributes.MAX_HEALTH, 20, ADD),
                new Mod(Attributes.ATTACK_DAMAGE, 10, ADD),
                new Mod(Attributes.KNOCKBACK_RESISTANCE, 1, ADD),
                new Mod(Attributes.MOVEMENT_SPEED, -0.3, MUL)), b()
                        .onAttack((player, target, event) -> {
                            if (inForm(player, "golem")) {
                                Scheduler.later(1, () -> setVelocity(target, new Vec3(0, 0.9, 0)));
                            }
                        }), 3));

        // Arrows fly right through your liquid body, and wounds close again a second later.
        reg("liquid_metal_body", b()
                .stopsProjectiles((player, projectile) -> {
                    particles(player, projectile.position(), ParticleTypes.DRIPPING_OBSIDIAN_TEAR, 4, 0.2);
                    return true;
                })
                .onHurt((player, event) -> {
                    float amount = event.getAmount();
                    Scheduler.later(20, () -> {
                        if (player.isAlive()) {
                            player.heal(amount * 0.6f);
                        }
                    });
                }));

        // Become the mob you look at (or the last one you hit), with its size, health, strength and abilities.
        // R again uses its special move (fireballs, teleporting, explosions...). Sneak + R turns you back.
        reg("total_transformation", b()
                .onAttack((player, target, event) -> state(player).put("last_hit_type", target.getType()))
                .ignoredBy((player, mob) -> MobTraits.sameKind(state(player).get("mob_type"), mob))
                .every(10, player -> {
                    EntityType<?> type = state(player).get("mob_type");
                    if (type != null) {
                        MobTraits.apply(player, type);
                    }
                })
                .active(1, player -> {
                    EntityType<?> current = state(player).get("mob_type");
                    if (current != null) {
                        if (player.isShiftKeyDown()) {
                            stopMob(player, "mob_type");
                            tell(player, "Back to yourself");
                            return true;
                        }
                        if (!MobTraits.special(player, current)) {
                            tell(player, "This mob has no special move. Sneak + R to turn back");
                            return false;
                        }
                        return true;
                    }
                    EntityType<?> type = state(player).get("last_hit_type");
                    LivingEntity looked = lookedAtLiving(player, 24);
                    if (looked != null && !(looked instanceof Player)) {
                        type = looked.getType();
                    }
                    if (type == null) {
                        tell(player, "Look at a mob (or hit one) to become it");
                        return false;
                    }
                    becomeMob(player, type, "mob_type");
                    tell(player, "You are a " + type.getDescription().getString() + "! R: special move, sneak + R: back");
                    return true;
                }));
    }
}
