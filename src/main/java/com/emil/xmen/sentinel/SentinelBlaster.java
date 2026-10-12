package com.emil.xmen.sentinel;

import java.util.List;

import com.emil.xmen.world.ModContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** A Sentinel's palm laser rebuilt into a hand blaster. Right-click to fire a red beam. Repair it with parts. */
public class SentinelBlaster extends Item {
    private static final double RANGE = 32;

    public SentinelBlaster(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, 14);
        if (level instanceof ServerLevel server) {
            Vec3 from = player.getEyePosition();
            Vec3 end = from.add(player.getLookAngle().scale(RANGE));
            BlockHitResult wall = level.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, player));
            if (wall.getType() == HitResult.Type.BLOCK) {
                end = wall.getLocation();
            }
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, from, end,
                    new AABB(from, end).inflate(1), e -> e instanceof LivingEntity && e.isAlive() && e != player);
            if (hit != null) {
                end = hit.getLocation();
                hit.getEntity().hurt(player.damageSources().indirectMagic(player, player), 8);
                hit.getEntity().igniteForSeconds(2);
            }
            Vec3 muzzle = from.add(player.getLookAngle().scale(0.8)).add(0, -0.25, 0);
            SentinelMarkX.beam(server, muzzle, end, SentinelMarkX.RED, 0.35);
            server.sendParticles(ParticleTypes.LAVA, end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GUARDIAN_ATTACK,
                    SoundSource.PLAYERS, 1.0f, 1.8f);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModContent.SENTINEL_PART.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click to fire a Sentinel laser").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Repair it with Sentinel Parts in an anvil").withStyle(ChatFormatting.DARK_GRAY));
    }
}
