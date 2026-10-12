package com.emil.xmen.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.emil.xmen.XMenMod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/** The Danger Room: waves of training robots that spawn around the console. */
public final class DangerRoom {
    private static final ResourceKey<LootTable> REWARD = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "chests/danger_room"));
    private static final String[] WAVE_NAMES = {"Training Robots", "Laser Drones", "Sentinel Simulation"};
    private static final Map<BlockPos, Session> SESSIONS = new HashMap<>();

    private DangerRoom() {
    }

    private static final class Session {
        final ServerLevel level;
        final BlockPos console;
        final UUID player;
        final List<UUID> mobs = new ArrayList<>();
        int wave;
        int pause;

        Session(ServerLevel level, BlockPos console, UUID player) {
            this.level = level;
            this.console = console;
            this.player = player;
        }
    }

    public static void start(ServerLevel level, BlockPos console, ServerPlayer player) {
        Session running = SESSIONS.get(console);
        if (running != null) {
            player.displayClientMessage(Component.literal("Training is already running: wave " + running.wave
                    + " of 3").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        Session session = new Session(level, console.immutable(), player.getUUID());
        SESSIONS.put(session.console, session);
        player.sendSystemMessage(Component.literal("DANGER ROOM: training session started. Beat all 3 waves!")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        nextWave(session, player);
    }

    /** Called every server tick. */
    public static void tick() {
        Iterator<Session> it = SESSIONS.values().iterator();
        while (it.hasNext()) {
            Session s = it.next();
            ServerPlayer player = s.level.getServer().getPlayerList().getPlayer(s.player);
            boolean gone = player == null || player.isDeadOrDying() || player.level() != s.level
                    || player.distanceToSqr(Vec3.atCenterOf(s.console)) > 48 * 48;
            if (gone) {
                clear(s);
                if (player != null) {
                    player.sendSystemMessage(Component.literal("DANGER ROOM: training ended.")
                            .withStyle(ChatFormatting.GRAY));
                }
                it.remove();
                continue;
            }
            if (s.pause > 0) {
                if (--s.pause == 0) {
                    nextWave(s, player);
                }
                continue;
            }
            s.mobs.removeIf(id -> {
                Entity e = s.level.getEntity(id);
                return e == null || !e.isAlive();
            });
            if (s.mobs.isEmpty()) {
                reward(s, player);
                if (s.wave >= WAVE_NAMES.length) {
                    player.sendSystemMessage(Component.literal("DANGER ROOM: all waves beaten! Well done, X-Man.")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    it.remove();
                } else {
                    player.displayClientMessage(Component.literal("Wave " + s.wave + " beaten! Next wave in 5 seconds")
                            .withStyle(ChatFormatting.GREEN), true);
                    s.pause = 100;
                }
            }
        }
    }

    private static void nextWave(Session s, ServerPlayer player) {
        s.wave++;
        player.sendSystemMessage(Component.literal("Wave " + s.wave + ": " + WAVE_NAMES[s.wave - 1])
                .withStyle(ChatFormatting.YELLOW));
        s.level.playSound(null, s.console, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0f, 0.6f);
        switch (s.wave) {
            case 1 -> {
                for (int i = 0; i < 4; i++) {
                    spawn(s, player, EntityType.ZOMBIE, "training_robot");
                }
            }
            case 2 -> {
                for (int i = 0; i < 3; i++) {
                    spawn(s, player, EntityType.ZOMBIE, "training_robot");
                    spawn(s, player, EntityType.SKELETON, "laser_drone");
                }
            }
            default -> {
                spawn(s, player, EntityType.IRON_GOLEM, "training_sentinel");
                spawn(s, player, EntityType.SKELETON, "laser_drone");
                spawn(s, player, EntityType.SKELETON, "laser_drone");
            }
        }
    }

    private static void spawn(Session s, ServerPlayer player, EntityType<? extends Mob> type, String kind) {
        Mob mob = type.create(s.level);
        if (mob == null) {
            return;
        }
        BlockPos at = spot(s);
        mob.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, s.level.random.nextFloat() * 360, 0);
        mob.addTag("xmen." + kind);
        mob.addTag("xmen.no_loot");
        s.level.addFreshEntity(mob);
        mob.setTarget(player);
        s.mobs.add(mob.getUUID());
        s.level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 30,
                0.4, 0.8, 0.4, 0.1);
    }

    /** A free spot 4 to 11 blocks from the console, on the same floor. */
    private static BlockPos spot(Session s) {
        for (int i = 0; i < 40; i++) {
            double angle = s.level.random.nextDouble() * Math.PI * 2;
            double r = 4 + s.level.random.nextDouble() * 7;
            BlockPos p = s.console.offset((int) Math.round(Math.cos(angle) * r), 0, (int) Math.round(Math.sin(angle) * r));
            if (s.level.getBlockState(p).getCollisionShape(s.level, p).isEmpty()
                    && s.level.getBlockState(p.above()).getCollisionShape(s.level, p.above()).isEmpty()
                    && s.level.getBlockState(p.above(2)).getCollisionShape(s.level, p.above(2)).isEmpty()
                    && !s.level.getBlockState(p.below()).getCollisionShape(s.level, p.below()).isEmpty()) {
                return p;
            }
        }
        return s.console.above();
    }

    private static void reward(Session s, ServerPlayer player) {
        LootTable table = s.level.getServer().reloadableRegistries().getLootTable(REWARD);
        Vec3 at = Vec3.atCenterOf(s.console).add(0, 1, 0);
        LootParams params = new LootParams.Builder(s.level).withParameter(LootContextParams.ORIGIN, at)
                .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.CHEST);
        for (ItemStack stack : table.getRandomItems(params)) {
            ItemEntity item = new ItemEntity(s.level, at.x, at.y, at.z, stack);
            item.setDefaultPickUpDelay();
            s.level.addFreshEntity(item);
        }
        s.level.playSound(null, s.console, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    private static void clear(Session s) {
        for (UUID id : s.mobs) {
            Entity e = s.level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        s.mobs.clear();
    }

    public static void clearAll() {
        for (Session s : SESSIONS.values()) {
            clear(s);
        }
        SESSIONS.clear();
    }
}
