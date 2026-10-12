package com.emil.xmen.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.power.Power;
import com.emil.xmen.power.PowerCategory;

import net.minecraft.ChatFormatting;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Right-click to find every mutant in the world and the nearest X-Men places. Telepaths see further. */
public class CerebroBlock extends Block {
    private static final Map<UUID, Long> LAST_USE = new HashMap<>();

    public CerebroBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            long now = level.getGameTime();
            Long last = LAST_USE.get(player.getUUID());
            if (last != null && now - last < 60) {
                return InteractionResult.SUCCESS;
            }
            LAST_USE.put(player.getUUID(), now);
            List<Vec3> dots = new ArrayList<>();
            List<Vec3> places = new ArrayList<>();
            scan(serverLevel, pos, serverPlayer, dots, places);
            activate(serverLevel, pos, serverPlayer, dots, places);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static boolean isTelepath(ServerPlayer player) {
        Power power = PlayerPowers.get(player);
        return power != null && power.category() == PowerCategory.MIND_AND_PSYCHIC;
    }

    public static boolean hiddenFromTelepaths(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModContent.MAGNETO_HELMET.get());
    }

    // ------------------------------------------------------------------ the sphere lighting up
    /** A running Cerebro: shows dots on the sphere for every mutant (red) and place (gold) it found. */
    private record Session(ServerLevel level, Vec3 center, long end, List<Vec3> mutants, List<Vec3> places,
            List<Vec3> people) {
    }

    private static final List<Session> SESSIONS = new ArrayList<>();
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1.0f, 0.15f, 0.1f), 1.6f);
    private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.8f, 0.2f), 1.8f);
    private static final DustParticleOptions WHITE = new DustParticleOptions(new Vector3f(0.85f, 0.9f, 1.0f), 0.9f);
    private static final int SPHERE = 15;
    private static final double DOT_RADIUS = 10.5;

    private static void activate(ServerLevel level, BlockPos pos, ServerPlayer user, List<Vec3> mutants,
            List<Vec3> places) {
        // light the panels of the sphere ring by ring, starting next to the console
        Block panel = ModContent.CEREBRO_PANEL.get();
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-SPHERE, -SPHERE, -SPHERE),
                pos.offset(SPHERE, SPHERE, SPHERE))) {
            BlockState state = level.getBlockState(at);
            if (state.is(panel) && !state.getValue(CerebroPanelBlock.LIT) && !level.getBlockTicks().hasScheduledTick(at,
                    panel)) {
                int delay = 2 + (int) Math.sqrt(at.distSqr(pos)) * 2;
                level.scheduleTick(at.immutable(), panel, delay);
            }
        }
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 2.0f, 0.6f);
        level.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 2.0f, 0.8f);
        user.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, true, false));
        // the whole world as little white dots, mutants red, places gold
        List<Vec3> people = new ArrayList<>();
        RandomSource random = RandomSource.create(pos.asLong());
        for (int i = 0; i < 260; i++) {
            double yaw = random.nextDouble() * Math.PI * 2;
            double pitch = Math.asin(random.nextDouble() * 1.6 - 0.8);
            people.add(new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch)));
        }
        Vec3 center = Vec3.atCenterOf(pos).add(0, 1.0, 0);
        SESSIONS.removeIf(s -> s.level == level && s.center.distanceToSqr(center) < 4);
        SESSIONS.add(new Session(level, center, level.getGameTime() + CerebroPanelBlock.GLOW_TICKS, mutants, places,
                people));
    }

    /** Direction on the sphere for something at 'target': the compass direction, higher up the further away. */
    private static Vec3 onSphere(BlockPos from, Vec3 target, double maxDistance) {
        double dx = target.x - from.getX();
        double dz = target.z - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double yaw = Math.atan2(dz, dx);
        double pitch = -0.5 + Math.min(1.0, distance / maxDistance) * 1.4;
        return new Vec3(Math.cos(yaw) * Math.cos(pitch), Math.sin(pitch), Math.sin(yaw) * Math.cos(pitch));
    }

    public static void tickSessions() {
        SESSIONS.removeIf(s -> s.level.getGameTime() > s.end);
        for (Session session : SESSIONS) {
            long time = session.level.getGameTime();
            if (time % 4 != 0) {
                continue;
            }
            for (Vec3 dir : session.people) {
                if (session.level.random.nextInt(3) == 0) {
                    dot(session, dir, WHITE);
                }
            }
            for (Vec3 dir : session.mutants) {
                dot(session, dir, RED);
            }
            for (Vec3 dir : session.places) {
                dot(session, dir, GOLD);
            }
            if (time % 40 == 0) {
                session.level.playSound(null, BlockPos.containing(session.center), SoundEvents.BEACON_AMBIENT,
                        SoundSource.BLOCKS, 1.5f, 1.4f);
            }
        }
    }

    private static void dot(Session session, Vec3 dir, DustParticleOptions color) {
        Vec3 at = session.center.add(dir.scale(DOT_RADIUS));
        session.level.sendParticles(color, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
    }

    public static void clearSessions() {
        SESSIONS.clear();
    }

    // ------------------------------------------------------------------ the list in chat
    private static void scan(ServerLevel level, BlockPos pos, ServerPlayer user, List<Vec3> dots,
            List<Vec3> placeDots) {
        boolean telepath = isTelepath(user);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0f, 1.5f);
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 80, 1.5,
                1.0, 1.5, 0.5);
        user.sendSystemMessage(Component.literal("=== Cerebro ===").withStyle(ChatFormatting.AQUA,
                ChatFormatting.BOLD));
        if (telepath) {
            user.sendSystemMessage(Component.literal("Your telepathy boosts Cerebro: you can sense much further.")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // mutants (players with powers)
        int found = 0;
        for (ServerPlayer other : level.getServer().getPlayerList().getPlayers()) {
            if (other == user) {
                continue;
            }
            Power power = PlayerPowers.get(other);
            if (power == null) {
                continue;
            }
            if (hiddenFromTelepaths(other)) {
                found++;
                user.sendSystemMessage(Component.literal("??? - a mind Cerebro can't read (wearing a strange helmet)")
                        .withStyle(ChatFormatting.DARK_GRAY));
                continue;
            }
            if (other.level() != level) {
                if (telepath) {
                    found++;
                    user.sendSystemMessage(Component.literal(other.getName().getString() + " - ")
                            .append(power.title()).append(Component.literal(" (in another dimension)")
                                    .withStyle(ChatFormatting.GRAY)));
                }
                continue;
            }
            double distance = other.position().distanceTo(Vec3.atCenterOf(pos));
            if (!telepath && distance > 2000) {
                continue;
            }
            found++;
            dots.add(onSphere(pos, other.position(), 2000));
            if (distance < 300) {
                other.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0, true, false));
            }
            user.sendSystemMessage(Component.literal(other.getName().getString() + " - ").append(power.title())
                    .append(Component.literal(" - " + (int) distance + " blocks " + direction(pos, other.position())
                            + (telepath ? " at " + other.getBlockX() + " " + other.getBlockY() + " "
                                    + other.getBlockZ() : ""))
                            .withStyle(ChatFormatting.GRAY)));
        }
        // the other mutant characters around (heroes, villains, Morlocks...) show up as red dots too
        for (net.minecraft.world.entity.Mob mob : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                new net.minecraft.world.phys.AABB(pos).inflate(256), m -> m instanceof com.emil.xmen.entity.MutantNpc)) {
            dots.add(onSphere(pos, mob.position(), 400));
        }
        if (found == 0) {
            user.sendSystemMessage(Component.literal("No other mutants found.").withStyle(ChatFormatting.GRAY));
        }

        // places
        int radius = telepath ? 100 : 50;
        List<Object[]> places = new ArrayList<>();
        for (ResourceKey<Structure> key : ModStructures.ALL) {
            BlockPos at = ModStructures.findNearest(level, key, pos, radius);
            if (at == null) {
                continue;
            }
            double distance = Math.sqrt(at.distSqr(new BlockPos(pos.getX(), at.getY(), pos.getZ())));
            if (key == ModStructures.X_MANSION && distance < 160) {
                continue;  // that's the mansion we're standing in
            }
            places.add(new Object[] {key, at, distance});
            placeDots.add(onSphere(pos, Vec3.atCenterOf(at), radius * 16));
        }
        places.sort(Comparator.comparingDouble(p -> (Double) p[2]));
        user.sendSystemMessage(Component.literal("Nearest places:").withStyle(ChatFormatting.AQUA));
        int shown = 0;
        for (Object[] place : places) {
            if (shown++ >= (telepath ? 10 : 6)) {
                break;
            }
            @SuppressWarnings("unchecked")
            ResourceKey<Structure> key = (ResourceKey<Structure>) place[0];
            BlockPos at = (BlockPos) place[1];
            user.sendSystemMessage(Component.literal(ModStructures.displayName(key)).withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" - " + (int) (double) (Double) place[2] + " blocks "
                            + direction(pos, Vec3.atCenterOf(at)) + " (" + at.getX() + ", " + at.getZ() + ")")
                            .withStyle(ChatFormatting.GRAY)));
        }
        if (places.isEmpty()) {
            user.sendSystemMessage(Component.literal("Nothing found nearby.").withStyle(ChatFormatting.GRAY));
        }
    }

    private static String direction(BlockPos from, Vec3 to) {
        double dx = to.x - (from.getX() + 0.5);
        double dz = to.z - (from.getZ() + 0.5);
        String ns = Math.abs(dz) > Math.abs(dx) / 2 ? (dz < 0 ? "north" : "south") : "";
        String ew = Math.abs(dx) > Math.abs(dz) / 2 ? (dx < 0 ? "west" : "east") : "";
        String dir = ns + (ns.isEmpty() || ew.isEmpty() ? "" : "-") + ew;
        return dir.isEmpty() ? "here" : dir;
    }
}
