package com.emil.xmen.apocalypse;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Celestial gold running through Apocalypse's pyramid, like in the movie. Lights up when he wakes. */
public class VeinBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public VeinBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    /** Turns every vein around center on or off. Returns how many changed. */
    public static int setLit(ServerLevel level, BlockPos center, int radius, int below, int above, boolean lit) {
        int changed = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -below, -radius),
                center.offset(radius, above, radius))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof VeinBlock && state.getValue(LIT) != lit) {
                level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_CLIENTS);
                changed++;
            }
        }
        return changed;
    }
}
