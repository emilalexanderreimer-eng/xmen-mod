package com.emil.xmen.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The metal plates that make up the inside of the Cerebro sphere. When Cerebro starts they light up one ring
 * after the other and go dark again when it's done. A scheduled tick switches them: dark -> lit -> dark.
 */
public class CerebroPanelBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int GLOW_TICKS = 320;

    public CerebroPanelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_CLIENTS);
        } else {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, GLOW_TICKS);
        }
    }
}
