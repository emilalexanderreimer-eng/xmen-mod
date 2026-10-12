package com.emil.xmen.world;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** Right-click to slide the basketball court open or closed above the X-Jet hangar. */
public class HangarControlBlock extends Block {
    private static final int RANGE = 28;

    public HangarControlBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level instanceof ServerLevel server) {
            BlockPos start = null;
            for (BlockPos at : BlockPos.betweenClosed(pos.offset(-RANGE, -RANGE, -RANGE),
                    pos.offset(RANGE, RANGE, RANGE))) {
                if (server.getBlockState(at).getBlock() instanceof CourtFloorBlock) {
                    start = at.immutable();
                    break;
                }
            }
            if (start == null) {
                player.displayClientMessage(Component.literal("No hangar roof found nearby.")
                        .withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.SUCCESS;
            }
            boolean open = !server.getBlockState(start).getValue(CourtFloorBlock.OPEN);
            Set<BlockPos> floor = new HashSet<>();
            ArrayDeque<BlockPos> todo = new ArrayDeque<>();
            todo.add(start);
            while (!todo.isEmpty() && floor.size() < 2000) {
                BlockPos at = todo.poll();
                if (!floor.contains(at) && server.getBlockState(at).getBlock() instanceof CourtFloorBlock) {
                    floor.add(at);
                    for (Direction dir : Direction.Plane.HORIZONTAL) {
                        todo.add(at.relative(dir));
                    }
                }
            }
            AABB area = null;
            for (BlockPos at : floor) {
                server.setBlock(at, server.getBlockState(at).setValue(CourtFloorBlock.OPEN, open), Block.UPDATE_CLIENTS);
                area = area == null ? new AABB(at) : area.minmax(new AABB(at));
            }
            if (open && area != null) {
                // anyone standing on the court floats down gently instead of falling into the hangar
                for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, area.expandTowards(0, 3, 0))) {
                    entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0));
                }
            }
            server.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0f, 0.5f);
            if (area != null) {
                server.playSound(null, BlockPos.containing(area.getCenter()), SoundEvents.PISTON_CONTRACT,
                        SoundSource.BLOCKS, 2.0f, 0.5f);
            }
            player.displayClientMessage(Component.literal(open ? "Hangar roof opening - the X-Jet is cleared for "
                    + "takeoff!" : "Hangar roof closing.").withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
