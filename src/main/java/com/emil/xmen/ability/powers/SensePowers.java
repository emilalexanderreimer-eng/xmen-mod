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
import java.util.ArrayDeque;
import java.util.Deque;
import com.emil.xmen.network.ZoomPayload;
import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.power.Power;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Senses & Detection. */
public final class SensePowers {
    private record Seen(ResourceKey<Level> dimension, Vec3 pos, long time) {
    }

    /** Where every player was over the last few minutes, for Postcognition. */
    private static final Map<UUID, Deque<Seen>> HISTORY = new HashMap<>();
    private static long ticks;

    private static final List<TagKey<Structure>> STRUCTURES = List.of(StructureTags.VILLAGE,
            StructureTags.EYE_OF_ENDER_LOCATED, StructureTags.MINESHAFT, StructureTags.RUINED_PORTAL,
            StructureTags.SHIPWRECK);
    private static final List<String> STRUCTURE_NAMES = List.of("village", "stronghold", "mineshaft",
            "ruined portal", "shipwreck");

    private SensePowers() {
    }

    private static PowerBehavior.Builder b() {
        return PowerBehavior.builder();
    }

    private static void reg(String id, PowerBehavior.Builder builder) {
        PowerBehaviors.register(id, builder);
    }

    private static void recordPlayers(ServerTickEvent.Post event) {
        if (++ticks % 200 != 0) {
            return;
        }
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            Deque<Seen> history = HISTORY.computeIfAbsent(p.getUUID(), id -> new ArrayDeque<>());
            history.addLast(new Seen(p.level().dimension(), p.position(), p.level().getGameTime()));
            while (history.size() > 18) {
                history.removeFirst();
            }
        }
    }

    private static boolean isContainer(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof ShulkerBoxBlock
                || block instanceof SpawnerBlock;
    }

    /** Outlines ores and/or containers within radius. */
    private static int highlight(ServerPlayer player, int radius, boolean ores, boolean containers, double seconds,
            int limit) {
        ServerLevel level = level(player);
        BlockPos c = player.blockPosition();
        int shown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-radius, -radius, -radius), c.offset(radius, radius, radius))) {
            if (shown >= limit) {
                break;
            }
            BlockState state = level.getBlockState(pos);
            if (ores && state.is(Tags.Blocks.ORES)) {
                Highlights.block(level, pos.immutable(), 0xFFD700, seconds);
                shown++;
            } else if (containers && isContainer(state)) {
                Highlights.block(level, pos.immutable(), 0x55FFFF, seconds);
                shown++;
            }
        }
        return shown;
    }

    private static void endRemoteView(ServerPlayer player) {
        PowerState st = state(player);
        ArmorStand eye = st.get("view_eye");
        Vec3 origin = st.get("view_origin");
        ResourceKey<Level> dim = st.get("view_dim");
        st.remove("view_eye");
        if (eye != null) {
            player.setCamera(player);
            eye.discard();
        }
        if (origin != null) {
            st.remove("view_origin");
            ServerLevel level = dim == null ? null : player.server.getLevel(dim);
            if (level != null) {
                player.teleportTo(level, origin.x, origin.y, origin.z, player.getYRot(), player.getXRot());
            }
            player.removeEffect(MobEffects.INVISIBILITY);
        }
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SensePowers::recordPlayers);

        reg("night_vision", b().effect(MobEffects.NIGHT_VISION, 0));

        // For 8 seconds you see through stone: ores, chests and spawners nearby light up, and mobs glow.
        reg("x_ray_vision", b().active(20, player -> {
            int found = highlight(player, 10, true, true, 8, 60);
            for (LivingEntity e : livingAround(player, 24)) {
                glow(e, 8);
            }
            effect(player, MobEffects.NIGHT_VISION, 8, 0);
            tell(player, "X-ray: " + found + " ores and containers nearby");
            sound(player, SoundEvents.BEACON_AMBIENT, 1, 2);
            return true;
        }));

        // You hear hidden monsters: you're told where the nearest one behind a wall is.
        reg("super_hearing", b().every(40, player -> {
            LivingEntity nearest = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : enemiesAround(player, 20)) {
                double d = e.distanceToSqr(player);
                if (!player.hasLineOfSight(e) && d < best) {
                    best = d;
                    nearest = e;
                }
            }
            if (nearest != null) {
                tell(player, "You hear a " + nearest.getName().getString() + ": " + direction(player, nearest.position()));
            }
        }));

        // Warns you the moment a mob starts hunting you (see TargetWatchers).
        reg("danger_sense", b());

        // R: zoom in like a spyglass (R again to zoom out).
        reg("eagle_eyes", b().active(0.3, player -> {
            boolean on = state(player).toggle("zoom");
            PacketDistributor.sendToPlayer(player, new ZoomPayload(on));
            state(player).onClear(() -> PacketDistributor.sendToPlayer(player, new ZoomPayload(false)));
            sound(player, SoundEvents.SPYGLASS_USE, 1, 1);
            return true;
        }));

        // Lock onto the mob or player you look at (or the nearest player) and follow its trail for 60 seconds.
        reg("tracker", b()
                .every(10, player -> {
                    PowerState st = state(player);
                    UUID id = st.get("track");
                    if (id == null || !st.active("tracking", now(player))) {
                        return;
                    }
                    Entity target = level(player).getEntity(id);
                    if (target == null || !target.isAlive()) {
                        tell(player, "The trail went cold");
                        st.remove("track");
                        return;
                    }
                    tell(player, target.getName().getString() + ": " + direction(player, target.position()));
                    Vec3 from = player.position().add(0, 0.2, 0);
                    Vec3 dir = target.position().subtract(from).normalize();
                    for (int i = 1; i <= 6; i++) {
                        privateParticles(player, from.add(dir.scale(i)), ParticleTypes.HAPPY_VILLAGER, 1, 0);
                    }
                })
                .active(5, player -> {
                    Entity target = lookedAtLiving(player, 64);
                    if (target == null) {
                        double best = Double.MAX_VALUE;
                        for (ServerPlayer other : level(player).players()) {
                            double d = other.distanceToSqr(player);
                            if (other != player && d < best) {
                                best = d;
                                target = other;
                            }
                        }
                    }
                    if (target == null) {
                        tell(player, "Nothing to track. Look at a mob");
                        return false;
                    }
                    state(player).put("track", target.getUUID());
                    state(player).start("tracking", now(player), 1200);
                    tell(player, "Tracking " + target.getName().getString());
                    return true;
                }));

        // Lists every player's power and where they are.
        reg("mutant_detection", b().active(10, player -> {
            chat(player, "Mutants online:");
            for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
                if (other == player) {
                    continue;
                }
                Power power = PlayerPowers.get(other);
                String where = other.level() == player.level() ? direction(player, other.position())
                        : "in another dimension";
                player.sendSystemMessage(Component.literal(" " + other.getName().getString() + ": ")
                        .append(power == null ? Component.literal("no power") : power.title())
                        .append(Component.literal(" (" + where + ")")));
            }
            if (player.server.getPlayerList().getPlayerCount() == 1) {
                chat(player, " Only you right now");
            }
            return true;
        }));

        // Animals and dropped items near you glow.
        reg("super_smell", b().every(40, player -> {
            for (Entity e : entitiesAround(player, 16)) {
                if (e instanceof Animal animal) {
                    glow(animal, 2.5);
                } else if (e instanceof ItemEntity item && !item.isCurrentlyGlowing()) {
                    item.setGlowingTag(true);
                    Scheduler.later(50, () -> item.setGlowingTag(false));
                }
            }
        }));

        // You see the next attack coming and dodge it. Takes 8 seconds to sense the next one.
        reg("precognition", b().onHurt((player, event) -> {
            if (event.getSource().getEntity() != null && !state(player).active("precog", now(player))) {
                state(player).start("precog", now(player), 160);
                event.setCanceled(true);
                particlesAround(player, player, ParticleTypes.ENCHANT, 15);
                tell(player, "You saw that coming");
            }
        }));

        // A radar of the mobs around you, even when blinded.
        reg("radar_sense", b().every(20, player -> {
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(MobEffects.DARKNESS);
            int hostile = 0;
            int other = 0;
            LivingEntity nearest = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : livingAround(player, 32)) {
                if (isEnemy(player, e)) {
                    hostile++;
                    double d = e.distanceToSqr(player);
                    if (d < best) {
                        best = d;
                        nearest = e;
                    }
                } else {
                    other++;
                }
            }
            String text = "Radar: " + hostile + " hostile, " + other + " other";
            if (nearest != null) {
                text += " | nearest " + nearest.getName().getString() + " " + direction(player, nearest.position());
            }
            tell(player, text);
        }));

        // Points to the nearest village. Sneak + R picks another kind of structure first.
        reg("structure_sense", b().active(5, player -> {
            PowerState st = state(player);
            int index = (int) st.number("structure");
            if (player.isShiftKeyDown()) {
                index = (index + 1) % STRUCTURES.size();
                st.put("structure", (double) index);
                tell(player, "Now sensing: " + STRUCTURE_NAMES.get(index));
                return false;
            }
            BlockPos found = level(player).findNearestMapStructure(STRUCTURES.get(index), player.blockPosition(), 64,
                    false);
            if (found == null) {
                tell(player, "No " + STRUCTURE_NAMES.get(index) + " nearby");
                return true;
            }
            Vec3 at = new Vec3(found.getX(), player.getY(), found.getZ());
            chat(player, "Nearest " + STRUCTURE_NAMES.get(index) + ": " + direction(player, at) + " (at "
                    + found.getX() + ", " + found.getZ() + ")");
            Vec3 dir = at.subtract(player.position()).normalize();
            for (int i = 1; i <= 8; i++) {
                privateParticles(player, player.getEyePosition().add(dir.scale(i)), ParticleTypes.END_ROD, 1, 0);
            }
            return true;
        }));

        // Chests, barrels and spawners nearby glow through walls for 15 seconds.
        reg("treasure_sense", b().active(20, player -> {
            int found = highlight(player, 24, false, true, 15, 40);
            tell(player, found == 0 ? "No treasure nearby" : "Found " + found + " containers");
            return true;
        }));

        // A ping that outlines all mobs and shows the caves around you.
        reg("echolocation", b().active(8, player -> {
            for (LivingEntity e : livingAround(player, 24)) {
                glow(e, 6);
            }
            ServerLevel level = level(player);
            BlockPos c = player.blockPosition();
            for (int i = 0; i < 400; i++) {
                BlockPos p = c.offset(player.getRandom().nextInt(25) - 12, player.getRandom().nextInt(25) - 12,
                        player.getRandom().nextInt(25) - 12);
                if (level.getBlockState(p).is(Blocks.CAVE_AIR)) {
                    privateParticles(player, Vec3.atCenterOf(p), ParticleTypes.SCULK_SOUL, 1, 0);
                }
            }
            ring(player, player.position().add(0, 1, 0), 3, ParticleTypes.NOTE);
            sound(player, SoundEvents.WARDEN_SONIC_CHARGE, 0.5f, 2);
            return true;
        }));

        // Your critical hits (jumping hits) do extra damage.
        reg("weak_spot_sight", b().onAttack((player, target, event) -> {
            if (player.fallDistance > 0 && !player.onGround() && !player.isInWater()) {
                event.setAmount(event.getAmount() * 1.5f);
                particlesAround(player, target, ParticleTypes.ENCHANTED_HIT, 10);
            }
        }));

        // Always know the time, and get a warning when night is coming.
        reg("time_sense", b().every(100, player -> {
            long time = player.level().getDayTime() % 24000L;
            long day = player.level().getDayTime() / 24000L + 1;
            int hours = (int) ((time / 1000 + 6) % 24);
            int minutes = (int) (time % 1000 * 60 / 1000);
            String text = "Day " + day + ", " + String.format("%02d:%02d", hours, minutes);
            if (time < 12000) {
                text += " | night in " + (12000 - time) / 1200 + " min";
            } else if (time < 23000) {
                text += " | morning in " + (24000 - time) / 1200 + " min";
            }
            tell(player, text);
            if (time >= 11000 && time < 11100) {
                chat(player, "Night falls soon");
            }
        }));

        // You sense everything near you: mobs glow, and every few seconds ores, chests and players show up.
        reg("cosmic_awareness", b()
                .every(40, player -> {
                    for (LivingEntity e : livingAround(player, 32)) {
                        glow(e, 2.5);
                    }
                })
                .every(200, player -> {
                    int found = highlight(player, 8, true, true, 6, 60);
                    int players = player.server.getPlayerList().getPlayerCount() - 1;
                    tell(player, "You sense " + found + " ores and chests, and " + players + " other players");
                }));

        // Look from the spot you aim at (up to 128 blocks away) for 8 seconds, then snap back.
        reg("remote_viewing", b()
                .every(5, player -> {
                    PowerState st = state(player);
                    if (st.get("view_eye") != null && !st.active("viewing", now(player))) {
                        endRemoteView(player);
                    }
                })
                .onHurt((player, event) -> {
                    if (state(player).get("view_eye") != null) {
                        event.setCanceled(true);
                    }
                })
                .active(20, player -> {
                    Vec3 at = lookTarget(player, 128).subtract(player.getLookAngle().scale(1.5)).add(0, 1, 0);
                    ArmorStand eye = new ArmorStand(level(player), at.x, at.y, at.z);
                    eye.setInvisible(true);
                    eye.setNoGravity(true);
                    eye.setInvulnerable(true);
                    eye.setSilent(true);
                    eye.setYRot(player.getYRot());
                    eye.setYHeadRot(player.getYRot());
                    level(player).addFreshEntity(eye);
                    PowerState st = state(player);
                    st.put("view_origin", player.position());
                    st.put("view_dim", player.level().dimension());
                    st.put("view_eye", eye);
                    st.start("viewing", now(player), 160);
                    st.onClear(() -> endRemoteView(player));
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 170, 0, false, false));
                    player.setCamera(eye);
                    tell(player, "Remote viewing for 8 seconds");
                    return true;
                }));

        // See where you last died, and where the other players were a minute ago.
        reg("postcognition", b().active(10, player -> {
            player.getLastDeathLocation().ifPresentOrElse(pos -> {
                if (pos.dimension() == player.level().dimension()) {
                    chat(player, "You last died " + direction(player, Vec3.atCenterOf(pos.pos())));
                } else {
                    chat(player, "You last died in another dimension");
                }
            }, () -> chat(player, "You haven't died yet"));
            long now = now(player);
            for (ServerPlayer other : player.server.getPlayerList().getPlayers()) {
                Deque<Seen> history = HISTORY.get(other.getUUID());
                if (other == player || history == null || history.isEmpty()) {
                    continue;
                }
                Seen past = history.peekFirst();
                for (Seen seen : history) {
                    if (now - seen.time() <= 1200) {
                        past = seen;
                        break;
                    }
                }
                long ago = (now - past.time()) / 20;
                String where = past.dimension() == player.level().dimension() ? direction(player, past.pos())
                        : "in another dimension";
                chat(player, other.getName().getString() + " was " + where + " (" + ago + "s ago)");
            }
            sound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 1, 0.6f);
            return true;
        }));

        // For 10 seconds, mobs show their mood: angry marks on hostile ones, hearts on friendly ones,
        // notes on ones that only fight back.
        reg("aura_reading", b()
                .every(20, player -> {
                    if (!state(player).active("auras", now(player))) {
                        return;
                    }
                    for (LivingEntity e : livingAround(player, 24)) {
                        Vec3 head = e.position().add(0, e.getBbHeight() + 0.5, 0);
                        if (isEnemy(player, e)) {
                            privateParticles(player, head, ParticleTypes.ANGRY_VILLAGER, 1, 0.1);
                        } else if (e instanceof NeutralMob) {
                            privateParticles(player, head, ParticleTypes.NOTE, 1, 0.1);
                        } else {
                            privateParticles(player, head, ParticleTypes.HEART, 1, 0.1);
                        }
                    }
                })
                .active(15, player -> {
                    state(player).start("auras", now(player), 200);
                    tell(player, "Reading auras for 10 seconds");
                    return true;
                }));

        // Arrows and thrown things fly dead straight and hit harder.
        reg("perfect_aim", b().onSpawn((player, entity) -> {
            if (entity instanceof Projectile projectile && entity.getPersistentData()
                    .getString(PowerProjectiles.KIND_TAG).isEmpty()) {
                projectile.setNoGravity(true);
                if (projectile instanceof AbstractArrow arrow) {
                    arrow.setBaseDamage(arrow.getBaseDamage() * 1.5);
                }
                Scheduler.later(100, () -> {
                    if (projectile.isAlive() && projectile.isNoGravity()) {
                        projectile.setNoGravity(false);
                    }
                });
            }
        }));
    }
}
