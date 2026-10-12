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
import net.minecraft.core.BlockPos;
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
            scan(serverLevel, pos, serverPlayer);
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

    private static void scan(ServerLevel level, BlockPos pos, ServerPlayer user) {
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
            user.sendSystemMessage(Component.literal(other.getName().getString() + " - ").append(power.title())
                    .append(Component.literal(" - " + (int) distance + " blocks " + direction(pos, other.position())
                            + (telepath ? " at " + other.getBlockX() + " " + other.getBlockY() + " "
                                    + other.getBlockZ() : ""))
                            .withStyle(ChatFormatting.GRAY)));
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
