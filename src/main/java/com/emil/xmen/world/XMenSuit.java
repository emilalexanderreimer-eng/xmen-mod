package com.emil.xmen.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;

/** Wearing the whole X-Men suit makes you tougher and faster, like a real team member. */
public final class XMenSuit {
    private XMenSuit() {
    }

    public static boolean wearsFullSuit(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModContent.XMEN_MASK.get())
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModContent.XMEN_SUIT.get())
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModContent.XMEN_PANTS.get())
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModContent.XMEN_BOOTS.get());
    }

    /** Called every 2 seconds. */
    public static void tick(ServerPlayer player) {
        if (wearsFullSuit(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 0, true, false, true));
        }
    }
}
