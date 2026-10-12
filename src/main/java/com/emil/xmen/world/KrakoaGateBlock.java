package com.emil.xmen.world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A living flower gate. The gate on Krakoa takes you to the nearest X-Mansion,
 * every other gate (like the one in the mansion garden) takes you to the nearest Krakoa.
 */
public class KrakoaGateBlock extends Block {
    private static final Map<BlockPos, BlockPos> GATE_CACHE = new HashMap<>();

    public KrakoaGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click to travel between Krakoa and the X-Mansion")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            travel(serverLevel, pos, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static void travel(ServerLevel level, BlockPos pos, ServerPlayer player) {
        boolean onKrakoa = ModStructures.isInside(level, pos, ModStructures.KRAKOA);
        ResourceKey<Structure> targetKey = onKrakoa ? ModStructures.X_MANSION : ModStructures.KRAKOA;
        String name = ModStructures.displayName(targetKey);
        BlockPos found = ModStructures.findNearest(level, targetKey, pos, 100);
        if (found == null) {
            player.displayClientMessage(Component.literal("No gate answers. There's no " + name + " close enough.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        player.displayClientMessage(Component.literal("The gate opens to " + name + "...")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 60,
                0.6, 1.0, 0.6, 0.05);
        BlockPos gate = findGate(level, targetKey, found);
        double x;
        double y;
        double z;
        if (gate != null) {
            x = gate.getX() + 0.5;
            y = gate.getY() + 1;
            z = gate.getZ() + 0.5;
        } else {
            level.getChunk(found);
            int top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    found.getX(), found.getZ());
            x = found.getX() + 0.5;
            y = top;
            z = found.getZ() + 0.5;
        }
        player.teleportTo(level, x, y, z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS,
                1.0f, 0.8f);
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, x, y + 1, z, 60, 0.6, 1.0, 0.6, 0.05);
    }

    /** Finds the gate block inside the structure that starts near 'found'. */
    private static BlockPos findGate(ServerLevel level, ResourceKey<Structure> key, BlockPos found) {
        BlockPos cached = GATE_CACHE.get(found);
        if (cached != null && level.getBlockState(cached).is(ModContent.KRAKOA_GATE.get())) {
            return cached;
        }
        Structure structure = ModStructures.get(level, key);
        ChunkPos chunkPos = new ChunkPos(found);
        ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.STRUCTURE_STARTS);
        StructureStart start = chunk.getStartForStructure(structure);
        if (start == null || !start.isValid()) {
            return null;
        }
        BoundingBox box = start.getBoundingBox();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int bx = box.minX(); bx <= box.maxX(); bx++) {
            for (int bz = box.minZ(); bz <= box.maxZ(); bz++) {
                for (int by = box.minY(); by <= box.maxY(); by++) {
                    p.set(bx, by, bz);
                    if (level.getBlockState(p).is(ModContent.KRAKOA_GATE.get())) {
                        BlockPos gate = p.immutable();
                        GATE_CACHE.put(found, gate);
                        return gate;
                    }
                }
            }
        }
        return null;
    }
}
