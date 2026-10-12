package com.emil.xmen.entity;

import java.util.List;

import com.emil.xmen.XMenMod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** The mutant items from the Weapon X program: claws, the adamantium skeleton, the healing serum, the collar. */
public final class WeaponXItems {
    private WeaponXItems() {
    }

    private static void lines(List<Component> tooltip, String... lines) {
        for (String line : lines) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
    }

    // ------------------------------------------------------------------ Adamantium Claws
    /** Wolverine's claws: very fast slashes that keep the target bleeding. Adamantium never breaks. */
    public static class Claws extends SwordItem {
        public Claws(Tier tier, Properties properties) {
            super(tier, properties);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0), attacker);
            if (attacker.level() instanceof ServerLevel level) {
                level.sendParticles(WeaponXMob.BLOOD, target.getX(), target.getY() + target.getBbHeight() * 0.6,
                        target.getZ(), 8, 0.3, 0.3, 0.3, 0);
            }
            return super.hurtEnemy(stack, target, attacker);
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                TooltipFlag flag) {
            lines(tooltip, "Snikt! Fast slashes that make enemies bleed", "Adamantium: never breaks");
        }
    }

    // ------------------------------------------------------------------ Adamantium Skeleton
    private static final String BONDED = "xmen_adamantium_skeleton";
    private static final ResourceLocation ARMOR_ID = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID,
            "adamantium_skeleton_armor");
    private static final ResourceLocation TOUGHNESS_ID = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID,
            "adamantium_skeleton_toughness");
    private static final ResourceLocation KNOCKBACK_ID = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID,
            "adamantium_skeleton_knockback");
    private static final ResourceLocation DAMAGE_ID = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID,
            "adamantium_skeleton_damage");

    public static boolean isBonded(Player player) {
        return player.getPersistentData().getBoolean(BONDED);
    }

    /**
     * Gives the player Wolverine's bonding: adamantium laced into every bone. Forever (it stays after death):
     * more armor, armor toughness, knockback resistance and punch damage, and falls only hurt half as much.
     */
    static void bond(Player player) {
        player.getPersistentData().putBoolean(BONDED, true);
        add(player.getAttribute(Attributes.ARMOR), ARMOR_ID, 4);
        add(player.getAttribute(Attributes.ARMOR_TOUGHNESS), TOUGHNESS_ID, 3);
        add(player.getAttribute(Attributes.KNOCKBACK_RESISTANCE), KNOCKBACK_ID, 0.3);
        add(player.getAttribute(Attributes.ATTACK_DAMAGE), DAMAGE_ID, 2);
    }

    private static void add(AttributeInstance attribute, ResourceLocation id, double amount) {
        if (attribute != null && !attribute.hasModifier(id)) {
            attribute.addPermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static class Skeleton extends Item {
        public Skeleton(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (isBonded(player)) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(Component.literal("Your bones are already adamantium")
                            .withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResultHolder.fail(stack);
            }
            if (level instanceof ServerLevel server) {
                bond(player);
                player.hurt(player.damageSources().magic(), 6);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
                server.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1, player.getZ(),
                        40, 0.4, 0.8, 0.4, 0.1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE,
                        SoundSource.PLAYERS, 1.0f, 0.6f);
                player.sendSystemMessage(Component.literal("Adamantium floods into your bones. They will never break.")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                TooltipFlag flag) {
            lines(tooltip, "Right-click: bond adamantium to your skeleton, like Wolverine",
                    "Forever: +4 armor, +3 toughness, +2 punch damage,", "less knockback and half fall damage",
                    "It hurts. A lot.");
        }
    }

    // ------------------------------------------------------------------ Healing Factor Serum
    /** Drink it: a short healing factor, like Wolverine's. Also cures poison and wither. */
    public static class Serum extends Item {
        public Serum(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            return ItemUtils.startUsingInstantly(level, player, hand);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public int getUseDuration(ItemStack stack, LivingEntity entity) {
            return 32;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!level.isClientSide()) {
                entity.removeEffect(MobEffects.POISON);
                entity.removeEffect(MobEffects.WITHER);
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 45, 1));
                entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 120, 1));
                entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 20, 0));
            }
            if (entity instanceof Player player && player.getAbilities().instabuild) {
                return stack;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            if (entity instanceof Player player && !player.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                player.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
            return stack;
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                TooltipFlag flag) {
            lines(tooltip, "Weapon X's copy of Wolverine's healing factor", "Regeneration II, Absorption II,",
                    "Resistance, and cures poison and wither");
        }
    }

    // ------------------------------------------------------------------ Inhibitor Collar
    /** The collar from the movies that shuts a mutant's powers off. Use it on any mob, even a boss. */
    public static class Collar extends Item {
        public Collar(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target,
                InteractionHand hand) {
            if (player.level() instanceof ServerLevel level) {
                suppress(target, 20 * 30);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getEyeY(), target.getZ(), 20,
                        0.3, 0.2, 0.3, 0.05);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.CHAIN_PLACE,
                        SoundSource.PLAYERS, 1.0f, 0.8f);
                player.displayClientMessage(Component.literal("Powers suppressed for 30 seconds")
                        .withStyle(ChatFormatting.RED), true);
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND);
                player.getCooldowns().addCooldown(this, 40);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide());
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                TooltipFlag flag) {
            lines(tooltip, "Right-click a mob to shut off its powers for 30 seconds",
                    "Weapon X bosses can't use their special attacks while collared");
        }
    }

    private static final String SUPPRESSED = "xmen_suppressed_until";

    static void suppress(LivingEntity target, int ticks) {
        target.getPersistentData().putLong(SUPPRESSED, target.level().getGameTime() + ticks);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0));
    }

    /** True while an inhibitor collar has this mob's powers switched off. */
    public static boolean isSuppressed(LivingEntity entity) {
        return entity.getPersistentData().getLong(SUPPRESSED) > entity.level().getGameTime();
    }

    // ------------------------------------------------------------------ events
    static void onClone(PlayerEvent.Clone event) {
        if (event.getOriginal().getPersistentData().getBoolean(BONDED) && event.getEntity() instanceof ServerPlayer p) {
            bond(p);
        }
    }

    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isBonded(player)
                && event.getSource().is(DamageTypeTags.IS_FALL)) {
            event.setAmount(event.getAmount() * 0.5f);
        }
    }
}
