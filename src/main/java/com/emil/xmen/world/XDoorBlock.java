package com.emil.xmen.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The big round X-doors of the lower level (like in the movies). A door is 3x3 of these blocks; PART says which
 * piece of the picture each block shows. It slides open by itself when someone walks up and closes again behind.
 */
public class XDoorBlock extends Block {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 8);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final double REACH = 3.5;

    public XDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 4).setValue(AXIS, Direction.Axis.X)
                .setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, AXIS, OPEN);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(OPEN);
    }

    /** All blocks of the door this block belongs to. */
    private static List<BlockPos> door(Level level, BlockPos start) {
        List<BlockPos> found = new ArrayList<>();
        ArrayDeque<BlockPos> todo = new ArrayDeque<>();
        todo.add(start);
        while (!todo.isEmpty() && found.size() < 16) {
            BlockPos pos = todo.poll();
            if (found.contains(pos) || !(level.getBlockState(pos).getBlock() instanceof XDoorBlock)) {
                continue;
            }
            found.add(pos);
            for (Direction dir : Direction.values()) {
                todo.add(pos.relative(dir));
            }
        }
        return found;
    }

    private static void setOpen(ServerLevel level, List<BlockPos> blocks, boolean open) {
        for (BlockPos pos : blocks) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof XDoorBlock && state.getValue(OPEN) != open) {
                level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_CLIENTS);
            }
        }
        BlockPos center = blocks.get(blocks.size() / 2);
        level.playSound(null, center, open ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                SoundSource.BLOCKS, 0.8f, open ? 1.4f : 1.2f);
        level.playSound(null, center, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS, 0.8f, 0.7f);
    }

    /** Called for every player a few times a second: opens doors they walk up to. */
    public static void openNear(ServerPlayer player) {
        if (player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockPos feet = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-3, -1, -3), feet.offset(3, 3, 3))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof XDoorBlock block && !state.getValue(OPEN)) {
                List<BlockPos> blocks = door(level, pos.immutable());
                setOpen(level, blocks, true);
                for (BlockPos part : blocks) {
                    level.scheduleTick(part, block, 30);
                }
                return;
            }
        }
    }

    /** Closes the door again once nobody is standing in or near it. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OPEN) || state.getValue(PART) != 4) {
            return;  // the middle block decides for the whole door
        }
        AABB area = new AABB(pos).inflate(REACH);
        boolean someone = !level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && !e.isSpectator() && (e instanceof ServerPlayer
                        || e.getBoundingBox().intersects(new AABB(pos).inflate(1.0)))).isEmpty();
        if (someone) {
            level.scheduleTick(pos, this, 20);
            return;
        }
        setOpen(level, door(level, pos), false);
    }
}
