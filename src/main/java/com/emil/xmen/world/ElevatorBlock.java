package com.emil.xmen.world;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Stand on it and jump to ride up to the next elevator block above, sneak to ride down.
 * Elevator blocks work in a straight line up and down, up to 64 blocks apart.
 */
public class ElevatorBlock extends Block {
    private static final int RANGE = 64;

    public ElevatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.displayClientMessage(Component.literal("Stand on the elevator: jump to go up, sneak to go down")
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.literal("Jump to go up, sneak to go down").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Place more elevators straight above or below").withStyle(ChatFormatting.GRAY));
    }

    /** Called when the player jumps (up) or sneaks (down) while standing on an elevator. */
    public static void ride(ServerPlayer player, boolean up) {
        ServerLevel level = player.serverLevel();
        BlockPos start = BlockPos.containing(player.getX(), player.getY() - 0.2, player.getZ());
        if (!level.getBlockState(start).is(ModContent.ELEVATOR.get())) {
            return;
        }
        int step = up ? 1 : -1;
        for (int i = 2; i <= RANGE; i++) {
            BlockPos pos = start.offset(0, i * step, 0);
            if (level.isOutsideBuildHeight(pos)) {
                break;
            }
            if (level.getBlockState(pos).is(ModContent.ELEVATOR.get()) && isFree(level, pos.above())
                    && isFree(level, pos.above(2))) {
                player.teleportTo(level, player.getX(), pos.getY() + 1, player.getZ(), player.getYRot(),
                        player.getXRot());
                player.resetFallDistance();
                level.playSound(null, pos.above(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8f,
                        up ? 1.6f : 1.2f);
                return;
            }
        }
        player.displayClientMessage(Component.literal(up ? "No elevator above" : "No elevator below")
                .withStyle(ChatFormatting.GRAY), true);
    }

    private static boolean isFree(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }
}
