package com.emil.xmen.world;

import java.util.List;

import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.power.Power;
import com.emil.xmen.power.PowerLevel;
import com.emil.xmen.power.Powers;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Mutant Serum: gives you a new random power (any level).
 * Celestial Orb (from Apocalypse): gives you a new random power that is Beta, Alpha or Omega.
 */
public class RerollItem extends Item {
    private final boolean celestial;

    public RerollItem(Properties properties, boolean celestial) {
        super(properties);
        this.celestial = celestial;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return celestial;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (celestial) {
            tooltip.add(Component.literal("Right-click: get a new power").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Always Beta, Alpha or Omega").withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.add(Component.literal("Right-click: get a new random power").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            Power old = PlayerPowers.get(serverPlayer);
            Power power;
            int tries = 0;
            do {
                power = celestial ? Powers.rollAtLeast(level.random, PowerLevel.BETA) : Powers.roll(level.random);
            } while (old != null && power.id().equals(old.id()) && ++tries < 20);
            PlayerPowers.set(serverPlayer, power);
            serverPlayer.sendSystemMessage(Component.literal(celestial
                    ? "The Celestial Orb rewrites your genes. Your new power is "
                    : "The serum burns through your veins. Your new power is ")
                    .withStyle(ChatFormatting.YELLOW).append(power.title()));
            serverPlayer.sendSystemMessage(Component.literal(power.description()).withStyle(ChatFormatting.GRAY));
            serverLevel.sendParticles(celestial ? ParticleTypes.END_ROD : ParticleTypes.HAPPY_VILLAGER,
                    player.getX(), player.getY() + 1, player.getZ(), 60, 0.5, 1.0, 0.5, 0.1);
            serverLevel.playSound(null, player.blockPosition(), celestial ? SoundEvents.END_PORTAL_SPAWN
                    : SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, celestial ? 0.6f : 1.0f, 1.0f);
            stack.consume(1, player);
            player.getCooldowns().addCooldown(this, 40);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
