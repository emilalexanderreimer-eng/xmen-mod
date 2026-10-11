package com.emil.xmen.ability;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.emil.xmen.XMenMod;
import com.emil.xmen.network.CooldownPayload;
import com.emil.xmen.power.Power;
import com.emil.xmen.power.PlayerPowers;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.network.PacketDistributor;

/** Applies a player's power behaviour each tick and keeps track of cooldowns. */
public final class PowerRuntime {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "power");
    private static final int EFFECT_TICKS = 260;

    private static final Set<Holder<Attribute>> TOUCHED_ATTRIBUTES = new LinkedHashSet<>();
    private static final Map<UUID, Long> READY_AT = new HashMap<>();

    private PowerRuntime() {
    }

    static void trackAttribute(Holder<Attribute> attribute) {
        TOUCHED_ATTRIBUTES.add(attribute);
    }

    private static PowerBehavior behavior(ServerPlayer player) {
        Power power = PlayerPowers.get(player);
        return power == null ? null : PowerBehaviors.get(power.id());
    }

    /** Re-applies attribute bonuses. Call on login, respawn and power change. */
    public static void refresh(ServerPlayer player) {
        for (Holder<Attribute> attribute : TOUCHED_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(MODIFIER_ID);
            }
        }
        PowerBehavior behavior = behavior(player);
        if (behavior != null) {
            for (PowerBehavior.AttributeBonus bonus : behavior.attributes()) {
                AttributeInstance instance = player.getAttribute(bonus.attribute());
                if (instance != null) {
                    instance.addOrUpdateTransientModifier(
                            new AttributeModifier(MODIFIER_ID, bonus.amount(), bonus.operation()));
                }
            }
        }
        updateFlight(player, behavior);
    }

    public static void onPowerChanged(ServerPlayer player, Power oldPower) {
        if (oldPower != null) {
            PowerBehavior old = PowerBehaviors.get(oldPower.id());
            if (old != null) {
                for (PowerBehavior.EffectBonus bonus : old.effects()) {
                    MobEffectInstance current = player.getEffect(bonus.effect());
                    if (current != null && current.isAmbient()) {
                        player.removeEffect(bonus.effect());
                    }
                }
            }
        }
        PowerBehaviors.clearState(player);
        READY_AT.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new CooldownPayload(0));
        refresh(player);
    }

    public static void tick(ServerPlayer player) {
        PowerBehavior behavior = behavior(player);
        if (behavior == null) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            for (PowerBehavior.EffectBonus bonus : behavior.effects()) {
                player.addEffect(new MobEffectInstance(bonus.effect(), EFFECT_TICKS, bonus.amplifier(), true, false,
                        true));
            }
            updateFlight(player, behavior);
        }
        for (PowerBehavior.Ticker ticker : behavior.tickers()) {
            ticker.tick(player);
        }
    }

    // Survival players only fly through a power, so anyone in survival who may fly without one loses it.
    private static void updateFlight(ServerPlayer player, PowerBehavior behavior) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean wants = behavior != null && behavior.flight();
        if (wants != player.getAbilities().mayfly) {
            player.getAbilities().mayfly = wants;
            if (!wants) {
                player.getAbilities().flying = false;
            }
            player.onUpdateAbilities();
        }
    }

    public static boolean isImmune(ServerPlayer player, DamageSource source) {
        PowerBehavior behavior = behavior(player);
        return behavior != null && behavior.isImmune(player, source);
    }

    /** Called when the power key is pressed. Returns false if the power has no key ability. */
    public static boolean use(ServerPlayer player) {
        PowerBehavior behavior = behavior(player);
        if (behavior == null || behavior.activation() == null) {
            return false;
        }
        long now = player.level().getGameTime();
        long readyAt = READY_AT.getOrDefault(player.getUUID(), 0L);
        if (now < readyAt) {
            Abilities.tell(player, "Ready in " + (int) Math.ceil((readyAt - now) / 20.0) + "s");
            return true;
        }
        if (behavior.activation().activate(player) && behavior.cooldownTicks() > 0) {
            READY_AT.put(player.getUUID(), now + behavior.cooldownTicks());
            PacketDistributor.sendToPlayer(player, new CooldownPayload(behavior.cooldownTicks()));
        }
        return true;
    }

    public static void onLogout(ServerPlayer player) {
        READY_AT.remove(player.getUUID());
        PowerBehaviors.clearState(player);
    }
}
