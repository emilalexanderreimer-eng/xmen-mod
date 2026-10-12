package com.emil.xmen.sentinel;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Built from the Mark X's core. Use it on the ground and a reprogrammed Sentinel stands up there to protect you.
 * Right-click it to switch between following you and guarding a spot, give it Sentinel Parts to repair it, and
 * sneak + right-click with an empty hand to pack it back into this chip.
 */
public class ControlChipItem extends Item {
    public ControlChipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        SentinelMarkX sentinel = new SentinelMarkX(SentinelContent.MARK_X.get(), level);
        sentinel.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180.0f, 0.0f);
        sentinel.setYHeadRot(player.getYRot() + 180.0f);
        sentinel.makeAlly(player);
        if (!level.noCollision(sentinel)) {
            player.displayClientMessage(Component.literal("There's no room for a Sentinel here.")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(sentinel);
        level.sendParticles(SentinelMarkX.BLUE, sentinel.getX(), sentinel.getY() + 1.5, sentinel.getZ(), 50, 0.6, 1.2,
                0.6, 0);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.3f);
        player.sendSystemMessage(Component.literal("SENTINEL: REPROGRAMMED. NEW DIRECTIVE: PROTECT "
                + player.getGameProfile().getName().toUpperCase() + ".").withStyle(ChatFormatting.AQUA,
                        ChatFormatting.BOLD));
        context.getItemInHand().consume(1, player);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.literal("Use on the ground: build your own Sentinel").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Right-click it: follow / guard here").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Give it Sentinel Parts to repair it").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + right-click: pack it up again").withStyle(ChatFormatting.GRAY));
    }
}
