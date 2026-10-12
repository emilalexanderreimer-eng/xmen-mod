package com.emil.xmen.world;

import java.util.List;

import com.emil.xmen.entity.Blackbird;
import com.emil.xmen.entity.ModEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/** Places the X-Jet on the ground. */
public class BlackbirdItem extends Item {
    public BlackbirdItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Blackbird jet = ModEntities.BLACKBIRD.get().create(level);
        if (jet == null) {
            return InteractionResult.FAIL;
        }
        float yaw = context.getPlayer() != null ? context.getPlayer().getYRot() : 0;
        jet.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        if (!level.noCollision(jet, jet.getBoundingBox())) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(Component.literal("Not enough room for the X-Jet here.")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResult.FAIL;
        }
        level.addFreshEntity(jet);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click to get in. Look where you want to fly,").withStyle(
                ChatFormatting.GRAY));
        tooltip.add(Component.literal("W = faster, S = slower, Shift = get out.").withStyle(ChatFormatting.GRAY));
    }
}
