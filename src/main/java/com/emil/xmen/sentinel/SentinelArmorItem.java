package com.emil.xmen.sentinel;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Sentinel armor, built from the plating of the factory's Sentinels.
 * <ul>
 * <li>Helmet: Sentinel scanner. Hostile mobs near you glow, even through walls.</li>
 * <li>Full set: adaptive plating. After something hurts you, the same kind of attack (fire, arrows, explosions,
 * swords...) only does 40% damage for the next 20 seconds. A new kind of attack makes it adapt again.</li>
 * </ul>
 */
public class SentinelArmorItem extends ArmorItem {
    private static final String KEY = "xmen_sentinel_armor";
    private static final int ADAPT_TICKS = 20 * 20;

    public SentinelArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        if (getType() == Type.HELMET) {
            tooltip.add(Component.literal("Sentinel scanner: nearby monsters glow").withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.literal("Full set: adaptive plating").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Takes less damage from the last kind of attack that hit you")
                .withStyle(ChatFormatting.GRAY));
    }

    static void registerEvents(IEventBus bus) {
        bus.addListener(SentinelArmorItem::onIncomingDamage);
        bus.addListener(SentinelArmorItem::onPlayerTick);
    }

    private static boolean wearing(ServerPlayer player, EquipmentSlot slot) {
        return player.getItemBySlot(slot).getItem() instanceof SentinelArmorItem;
    }

    private static boolean fullSet(ServerPlayer player) {
        return wearing(player, EquipmentSlot.HEAD) && wearing(player, EquipmentSlot.CHEST)
                && wearing(player, EquipmentSlot.LEGS) && wearing(player, EquipmentSlot.FEET);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !fullSet(player)) {
            return;
        }
        Threat threat = Threat.of(event.getSource());
        if (threat == null) {
            return;
        }
        CompoundTag data = player.getPersistentData().getCompound(KEY);
        long now = player.level().getGameTime();
        boolean adapted = data.getInt("Threat") == threat.ordinal() + 1 && data.getLong("Until") > now;
        if (adapted) {
            event.setAmount(event.getAmount() * 0.4f);
            ((ServerLevel) player.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(),
                    player.getY() + 1, player.getZ(), 6, 0.3, 0.5, 0.3, 0.1);
        } else {
            player.displayClientMessage(Component.literal("Sentinel armor adapting to " + threat.description + "...")
                    .withStyle(ChatFormatting.GOLD), true);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
        }
        data.putInt("Threat", threat.ordinal() + 1);
        data.putLong("Until", now + ADAPT_TICKS);
        player.getPersistentData().put(KEY, data);
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 40 != 0
                || !wearing(player, EquipmentSlot.HEAD)) {
            return;
        }
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(24),
                mob -> mob instanceof Enemy && mob.isAlive() && !(mob instanceof SentinelMarkX s && s.isAlly()))) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 50, 0, true, false));
        }
    }
}
