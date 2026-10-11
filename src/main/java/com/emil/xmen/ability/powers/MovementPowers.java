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
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Movement & Travel. */
public final class MovementPowers {
    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE;
    private static final Map<UUID, Long> PORTAL_COOLDOWN = new HashMap<>();

    private record Mark(ResourceKey<Level> dimension, Vec3 pos) {
    }

    private static final Map<UUID, Mark> TRAVEL_MARKS = new HashMap<>();

    private MovementPowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private static void puff(ServerPlayer player) {
        particles(player, player.position().add(0, 1, 0), ParticleTypes.PORTAL, 40, 0.4);
        sound(player, SoundEvents.ENDERMAN_TELEPORT, 1, 1);
    }

    /** Moves the player to another dimension (or the same one), onto solid ground. */
    private static void travelTo(ServerPlayer player, ServerLevel level, Vec3 to) {
        BlockPos spot = safeSpotNear(level, BlockPos.containing(to));
        if (spot == null) {
            spot = BlockPos.containing(to);
            for (BlockPos pos : BlockPos.betweenClosed(spot.offset(-1, -1, -1), spot.offset(1, -1, 1))) {
                if (level.getBlockState(pos).canBeReplaced()) {
                    level.setBlockAndUpdate(pos, Blocks.OBSIDIAN.defaultBlockState());
                }
            }
            for (BlockPos pos : BlockPos.betweenClosed(spot.offset(0, 0, 0), spot.offset(0, 1, 0))) {
                if (level.getBlockState(pos).getDestroySpeed(level, pos) >= 0) {
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
        puff(player);
        player.teleportTo(level, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        puff(player);
    }

    /** /power travel x y z, for Instant Travel. Returns a message for the player. */
    public static String travelCommand(ServerPlayer player, double x, double y, double z) {
        if (!PowerRuntime.has(player, "instant_travel")) {
            return "Only mutants with Instant Travel can do that";
        }
        long now = now(player);
        PowerState st = state(player);
        if (st.active("travel_cmd", now)) {
            return "Ready in " + (int) Math.ceil(st.remaining("travel_cmd", now) / 20.0) + "s";
        }
        st.start("travel_cmd", now, 600);
        travelTo(player, level(player), new Vec3(x, y, z));
        return "Travelled to " + (int) x + " " + (int) y + " " + (int) z;
    }

    public static void register() {
        // Kept from 0.3.0: teleport to the block you look at.
        reg("teleportation", b().active(1.5, player -> {
            BlockPos spot = teleportSpot(player, 48);
            if (spot == null) {
                tell(player, "No room to teleport there");
                return false;
            }
            particles(player, player.position().add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 30, 0.3);
            sound(player, SoundEvents.ENDERMAN_TELEPORT, 1, 0.8f);
            teleport(player, Vec3.atBottomCenterOf(spot));
            particles(player, player.position().add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 30, 0.3);
            sound(player, SoundEvents.ENDERMAN_TELEPORT, 1, 0.8f);
            return true;
        }));

        reg("super_speed", b()
                .attribute(Attributes.MOVEMENT_SPEED, 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .attribute(Attributes.STEP_HEIGHT, 0.5, ADD));

        // Climbing runs on the player's own computer (see XMenClient).
        reg("wall_crawling", b());

        reg("super_jump", b()
                .attribute(Attributes.JUMP_STRENGTH, 0.4, ADD)
                .attribute(Attributes.SAFE_FALL_DISTANCE, 10, ADD));

        // Kept from 0.3.0: walk through the wall in front of you.
        reg("phasing", b().active(2, player -> {
            Vec3 look = player.getLookAngle();
            Vec3 flat = new Vec3(look.x, 0, look.z);
            if (flat.lengthSqr() < 1.0E-4) {
                tell(player, "Look at a wall to walk through it");
                return false;
            }
            flat = flat.normalize();
            Vec3 origin = player.position();
            boolean passedWall = false;
            for (double d = 0.8; d <= 12; d += 0.5) {
                BlockPos feet = BlockPos.containing(origin.add(flat.scale(d)));
                if (!hasRoom(player, feet)) {
                    passedWall = true;
                } else if (passedWall) {
                    particles(player, player.position().add(0, 1, 0), ParticleTypes.PORTAL, 40, 0.4);
                    teleport(player, Vec3.atBottomCenterOf(feet));
                    sound(player, SoundEvents.CHORUS_FRUIT_TELEPORT, 1, 1.2f);
                    return true;
                }
            }
            tell(player, "No wall to walk through (or it's too thick)");
            return false;
        }));

        // Fly freely. R: fly three times faster for 5 seconds.
        reg("flight", b().flight().active(10, player -> {
            player.getAbilities().setFlyingSpeed(0.15f);
            player.onUpdateAbilities();
            state(player).onClear(() -> {
                player.getAbilities().setFlyingSpeed(0.05f);
                player.onUpdateAbilities();
            });
            Scheduler.later(100, () -> {
                player.getAbilities().setFlyingSpeed(0.05f);
                player.onUpdateAbilities();
            });
            sound(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1, 1);
            tell(player, "Turbo flight for 5 seconds");
            return true;
        }));

        // First R opens portal A where you stand, second R opens portal B. Walk into one to come out of the other.
        reg("portals", b().active(1, player -> {
            PowerState st = state(player);
            ServerLevel level = level(player);
            Vec3 here = player.position();
            Vec3 a = st.get("portal_a");
            if (a == null || !st.active("portal_wait", now(player))) {
                st.put("portal_a", here);
                st.start("portal_wait", now(player), 1200);
                ring(player, here.add(0, 1, 0), 1, ParticleTypes.PORTAL);
                tell(player, "Portal A opened. Go somewhere else and press R again");
                sound(player, SoundEvents.END_PORTAL_FRAME_FILL, 1, 1);
                return false;
            }
            if (a.distanceTo(here) < 4) {
                tell(player, "Too close to portal A");
                return false;
            }
            st.remove("portal_a");
            st.stop("portal_wait");
            Vec3 b = here;
            for (Vec3[] pair : new Vec3[][] { { a, b }, { b, a } }) {
                Vec3 from = pair[0];
                Vec3 to = pair[1];
                Zones.add(player, from.add(0, 1, 0), 1, 60, 2, ParticleTypes.REVERSE_PORTAL, (zone, inside) -> {
                    List<Entity> entities = new ArrayList<>(zone.entities());
                    ServerPlayer owner = zone.ownerPlayer();
                    if (owner != null && owner.level() == level && owner.position().add(0, 1, 0).distanceTo(zone.center) < 1.2) {
                        entities.add(owner);
                    }
                    long time = level.getGameTime();
                    for (Entity e : entities) {
                        Long last = PORTAL_COOLDOWN.get(e.getUUID());
                        if (last != null && time - last < 40) {
                            continue;
                        }
                        PORTAL_COOLDOWN.put(e.getUUID(), time);
                        e.teleportTo(to.x, to.y, to.z);
                        e.resetFallDistance();
                        level.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1, to.z, 30, 0.3, 0.6, 0.3, 0.5);
                    }
                    if (zone.age % 10 == 0) {
                        for (int i = 0; i < 16; i++) {
                            double ang = Math.PI * 2 * i / 16;
                            level.sendParticles(ParticleTypes.PORTAL, zone.center.x + Math.cos(ang) * 0.6,
                                    zone.center.y + Math.sin(ang) * 1.0, zone.center.z, 1, 0, 0, 0, 0);
                        }
                    }
                });
            }
            tell(player, "Portals open for 60 seconds");
            sound(player, SoundEvents.END_PORTAL_SPAWN, 0.6f, 1.5f);
            return true;
        }));

        // Jump between the Overworld and the Nether. Sneak: to the End (or home from the End).
        reg("dimension_hop", b().active(60, player -> {
            var server = player.server;
            ResourceKey<Level> here = player.level().dimension();
            if (player.isShiftKeyDown() || here == Level.END) {
                if (here == Level.END) {
                    ServerLevel overworld = server.overworld();
                    travelTo(player, overworld, Vec3.atBottomCenterOf(overworld.getSharedSpawnPos()));
                } else {
                    ServerLevel end = server.getLevel(Level.END);
                    if (end == null) {
                        return false;
                    }
                    travelTo(player, end, new Vec3(100.5, 50, 0.5));
                }
                return true;
            }
            if (here == Level.NETHER) {
                travelTo(player, server.overworld(), new Vec3(player.getX() * 8, 70, player.getZ() * 8));
            } else {
                ServerLevel nether = server.getLevel(Level.NETHER);
                if (nether == null) {
                    return false;
                }
                travelTo(player, nether, new Vec3(player.getX() / 8, 64, player.getZ() / 8));
            }
            return true;
        }));

        // A quick 6-block dash forward, through mobs.
        reg("blink_step", b().active(1, player -> {
            Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
            if (look.lengthSqr() < 1.0E-4) {
                return false;
            }
            look = look.normalize();
            Vec3 best = null;
            for (double d = 1; d <= 6; d += 0.5) {
                Vec3 at = player.position().add(look.scale(d));
                if (hasRoom(player, BlockPos.containing(at))) {
                    best = at;
                } else {
                    break;
                }
            }
            if (best == null) {
                return false;
            }
            particles(player, player.position().add(0, 1, 0), ParticleTypes.WITCH, 10, 0.3);
            teleport(player, best);
            sound(player, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1, 1.5f);
            return true;
        }));

        // Gliding runs on the player's own computer (see XMenClient).
        reg("glider", b().onFall((player, distance) -> true));

        // Running on water runs on the player's own computer (see XMenClient).
        reg("water_runner", b());

        reg("fast_swimmer", b().effect(MobEffects.DOLPHINS_GRACE, 0));

        // Stand still for 3 seconds to teleport to your bed (or the world spawn).
        reg("home_recall", b()
                .every(5, player -> {
                    PowerState st = state(player);
                    if (!st.flag("recalling")) {
                        return;
                    }
                    if (!isStill(player)) {
                        st.setFlag("recalling", false);
                        tell(player, "Recall cancelled, you moved");
                        return;
                    }
                    particlesAround(player, player, ParticleTypes.PORTAL, 10);
                    if (!st.active("recall", now(player))) {
                        st.setFlag("recalling", false);
                        BlockPos bed = player.getRespawnPosition();
                        ServerLevel level = player.server.getLevel(player.getRespawnDimension());
                        if (bed == null || level == null) {
                            level = player.server.overworld();
                            bed = level.getSharedSpawnPos();
                        }
                        travelTo(player, level, Vec3.atBottomCenterOf(bed));
                    }
                })
                .active(30, player -> {
                    state(player).setFlag("recalling", true);
                    state(player).start("recall", now(player), 60);
                    tell(player, "Stand still for 3 seconds...");
                    sound(player, SoundEvents.PORTAL_TRIGGER, 0.4f, 1.5f);
                    return true;
                }));

        // R: call every player and every one of your pets in this world to your side, wherever they are.
        reg("group_teleport", b().active(60, player -> {
            ServerLevel level = level(player);
            List<Entity> called = new ArrayList<>();
            for (Entity e : level.getAllEntities()) {
                if (e != player && e.isAlive() && (e instanceof ServerPlayer
                        || e instanceof net.minecraft.world.entity.TamableAnimal pet && pet.isTame()
                                && player.getUUID().equals(pet.getOwnerUUID()))) {
                    called.add(e);
                }
            }
            if (called.isEmpty()) {
                tell(player, "Nobody to call");
                return false;
            }
            for (Entity e : called) {
                level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 1, e.getZ(), 20, 0.3, 0.5, 0.3, 0.3);
                e.teleportTo(player.getX() + player.getRandom().triangle(0, 2), player.getY(),
                        player.getZ() + player.getRandom().triangle(0, 2));
                e.resetFallDistance();
                if (e instanceof ServerPlayer other) {
                    chat(other, player.getName().getString() + " called you to their side");
                }
            }
            puff(player);
            tell(player, "Called " + called.size() + " to you");
            return true;
        }));

        // R: blast off like a rocket. At the top of the flight you explode (without hurting yourself or blocks),
        // knocking back mobs, and you land safely.
        reg("rocket_launch", b()
                .onFall((player, distance) -> {
                    if (state(player).flag("rocket")) {
                        state(player).setFlag("rocket", false);
                        return true;
                    }
                    return false;
                })
                .every(2, player -> {
                    PowerState st = state(player);
                    if (st.flag("rocket") && !player.onGround()) {
                        particles(player, player.position(), ParticleTypes.FIREWORK, 3, 0.1);
                        if (st.flag("rocket_up") && player.getDeltaMovement().y <= 0) {
                            st.setFlag("rocket_up", false);
                            particles(player, player.position(), ParticleTypes.EXPLOSION_EMITTER, 1, 0);
                            soundAt(player, player.position(), SoundEvents.GENERIC_EXPLODE, 2, 1);
                            for (LivingEntity e : livingAround(player, 6)) {
                                if (!isFriend(player, e)) {
                                    hurt(player, e, 6);
                                    pushFrom(player.position(), e, 1.5, 0.3);
                                }
                            }
                        }
                    }
                })
                .active(5, player -> {
                    Vec3 look = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z);
                    setVelocity(player, look.scale(1.4).add(0, 1.6, 0));
                    state(player).setFlag("rocket", true);
                    state(player).setFlag("rocket_up", true);
                    particles(player, player.position(), ParticleTypes.EXPLOSION, 1, 0);
                    sound(player, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1, 0.6f);
                    return true;
                }));

        // You bounce when you land, and falls never hurt. Sneak to land without bouncing.
        reg("bounce", b().onFall((player, distance) -> {
            if (!player.isShiftKeyDown() && distance > 2) {
                double up = Math.min(1.6, 0.3 + distance * 0.08);
                Scheduler.later(1, () -> setVelocity(player, new Vec3(player.getDeltaMovement().x, up,
                        player.getDeltaMovement().z)));
                sound(player, SoundEvents.SLIME_JUMP, 1, 1);
            }
            return true;
        }));

        // Pull yourself to the block you look at.
        reg("grapple", b().active(2, player -> {
            BlockHitResult hit = lookedAtBlock(player, 32);
            if (hit.getType() == HitResult.Type.MISS) {
                tell(player, "Nothing to grab onto");
                return false;
            }
            Vec3 to = hit.getLocation();
            Vec3 dir = to.subtract(player.position());
            setVelocity(player, dir.normalize().scale(Math.min(2.6, 0.6 + dir.length() * 0.12)).add(0, 0.3, 0));
            player.resetFallDistance();
            particleLine(player, player.getEyePosition(), to,
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CHAIN.defaultBlockState()), 0.5);
            sound(player, SoundEvents.CHAIN_PLACE, 1, 1.2f);
            return true;
        }));

        // Running along walls runs on the player's own computer (see XMenClient).
        reg("wall_run", b());

        reg("low_gravity", b()
                .attribute(Attributes.GRAVITY, -0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .attribute(Attributes.SAFE_FALL_DISTANCE, 6, ADD));

        // Sneak + R saves a spot (any dimension). R takes you back to it. Or type /power travel x y z.
        reg("instant_travel", b().active(5, player -> {
            if (player.isShiftKeyDown()) {
                TRAVEL_MARKS.put(player.getUUID(), new Mark(player.level().dimension(), player.position()));
                tell(player, "Spot saved. Press R anywhere to come back");
                sound(player, SoundEvents.LODESTONE_COMPASS_LOCK, 1, 1);
                return false;
            }
            Mark mark = TRAVEL_MARKS.get(player.getUUID());
            if (mark == null) {
                tell(player, "Sneak + R to save a spot first (or type /power travel x y z)");
                return false;
            }
            ServerLevel level = player.server.getLevel(mark.dimension());
            if (level == null) {
                return false;
            }
            travelTo(player, level, mark.pos());
            return true;
        }));
    }
}
