package com.emil.xmen.world;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Right-click to start the Danger Room training: three waves, a reward after each one. */
public class DangerRoomConsoleBlock extends Block {
    public DangerRoomConsoleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click to start a training session").withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            DangerRoom.start(serverLevel, pos, serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
