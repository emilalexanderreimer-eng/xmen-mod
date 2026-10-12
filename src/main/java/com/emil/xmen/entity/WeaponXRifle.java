package com.emil.xmen.entity;

import java.util.List;

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

/** The rifle Weapon X soldiers carry. Right-click to fire. No ammo needed, but it wears out. */
public class WeaponXRifle extends Item {
    private static final double RANGE = 40;

    public WeaponXRifle(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.getCooldowns().addCooldown(this, 10);
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
                hit.getEntity().hurt(player.damageSources().playerAttack(player), 6);
            }
            Vec3 muzzle = from.add(player.getLookAngle().scale(0.8)).add(0, -0.2, 0);
            WeaponXMob.trail(server, muzzle, end, ParticleTypes.CRIT, 0.9);
            server.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z, 3, 0.05, 0.05, 0.05, 0.01);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST,
                    SoundSource.PLAYERS, 1.2f, 1.7f);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click to shoot").withStyle(ChatFormatting.GRAY));
    }
}
