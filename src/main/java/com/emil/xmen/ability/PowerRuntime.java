package com.emil.xmen.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.emil.xmen.XMenMod;
import com.emil.xmen.network.CooldownPayload;
import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.power.Power;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.network.PacketDistributor;

/** Applies a player's power behaviour each tick, keeps track of cooldowns and borrowed powers. */
public final class PowerRuntime {
    public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "power");
    public static final ResourceLocation EXTRA_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "power_extra");
    private static final int EFFECT_TICKS = 260;

    private record Borrowed(String powerId, long until) {
    }

    private static final Set<Holder<Attribute>> TOUCHED_ATTRIBUTES = new LinkedHashSet<>();
    private static final Map<UUID, Long> READY_AT = new HashMap<>();
    /** A power used instead of the player's own one for a while (Power Mimic, Power Absorption). */
    private static final Map<UUID, Borrowed> OVERRIDES = new HashMap<>();
    /** A second power on top of the player's own one (Power Granting). */
    private static final Map<UUID, Borrowed> EXTRAS = new HashMap<>();
    /** Players whose powers are switched off until a time (Power Negation, Power Absorption). */
    private static final Map<UUID, Long> SUPPRESSED = new HashMap<>();
    /** Players whose cooldowns are halved until a time (Power Boost). */
    private static final Map<UUID, Long> BOOSTED = new HashMap<>();
    /** Which behaviours were applied last, to notice when they change. */
    private static final Map<UUID, List<PowerBehavior>> APPLIED = new HashMap<>();

    private PowerRuntime() {
    }

    static void trackAttribute(Holder<Attribute> attribute) {
        TOUCHED_ATTRIBUTES.add(attribute);
    }

    private static long now(ServerPlayer player) {
        return player.level().getGameTime();
    }

    /** The behaviours active for player right now: their own (or a borrowed one) plus any granted extra. */
    public static List<PowerBehavior> behaviors(ServerPlayer player) {
        long now = now(player);
        Long suppressedUntil = SUPPRESSED.get(player.getUUID());
        if (suppressedUntil != null && now < suppressedUntil) {
            return List.of();
        }
        List<PowerBehavior> list = new ArrayList<>(2);
        Borrowed override = OVERRIDES.get(player.getUUID());
        String mainId;
        if (override != null && now < override.until()) {
            mainId = override.powerId();
        } else {
            Power power = PlayerPowers.get(player);
            mainId = power == null ? null : power.id();
        }
        PowerBehavior main = mainId == null ? null : PowerBehaviors.get(mainId);
        if (main != null) {
            list.add(main);
        }
        Borrowed extra = EXTRAS.get(player.getUUID());
        if (extra != null && now < extra.until()) {
            PowerBehavior extraBehavior = PowerBehaviors.get(extra.powerId());
            if (extraBehavior != null && extraBehavior != main) {
                list.add(extraBehavior);
            }
        }
        return list;
    }

    /** The id of the power currently in use (borrowed powers count), or null. */
    public static String activePowerId(ServerPlayer player) {
        Borrowed override = OVERRIDES.get(player.getUUID());
        if (override != null && now(player) < override.until()) {
            return override.powerId();
        }
        Power power = PlayerPowers.get(player);
        return power == null ? null : power.id();
    }

    public static boolean has(ServerPlayer player, String powerId) {
        PowerBehavior wanted = PowerBehaviors.get(powerId);
        return wanted != null && behaviors(player).contains(wanted);
    }

    public static void borrow(ServerPlayer player, String powerId, int ticks) {
        OVERRIDES.put(player.getUUID(), new Borrowed(powerId, now(player) + ticks));
        refresh(player);
    }

    public static void grantExtra(ServerPlayer player, String powerId, int ticks) {
        EXTRAS.put(player.getUUID(), new Borrowed(powerId, now(player) + ticks));
        refresh(player);
    }

    public static void suppress(ServerPlayer player, int ticks) {
        SUPPRESSED.put(player.getUUID(), now(player) + ticks);
        PowerState.clear(player);
        refresh(player);
    }

    public static boolean isSuppressed(ServerPlayer player) {
        Long until = SUPPRESSED.get(player.getUUID());
        return until != null && now(player) < until;
    }

    public static void boost(ServerPlayer player, int ticks) {
        BOOSTED.put(player.getUUID(), now(player) + ticks);
    }

    /** Re-applies attribute bonuses and flight. Call on login, respawn and power change. */
    public static void refresh(ServerPlayer player) {
        for (Holder<Attribute> attribute : TOUCHED_ATTRIBUTES) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance != null) {
                instance.removeModifier(MODIFIER_ID);
                instance.removeModifier(EXTRA_MODIFIER_ID);
            }
        }
        List<PowerBehavior> behaviors = behaviors(player);
        for (int i = 0; i < behaviors.size(); i++) {
            ResourceLocation id = i == 0 ? MODIFIER_ID : EXTRA_MODIFIER_ID;
            for (PowerBehavior.AttributeBonus bonus : behaviors.get(i).attributes()) {
                AttributeInstance instance = player.getAttribute(bonus.attribute());
                if (instance != null) {
                    instance.addOrUpdateTransientModifier(new AttributeModifier(id, bonus.amount(),
                            bonus.operation()));
                }
            }
        }
        APPLIED.put(player.getUUID(), behaviors);
        updateFlight(player, behaviors);
    }

    public static void onPowerChanged(ServerPlayer player, Power oldPower) {
        removeEffectsOf(player, APPLIED.getOrDefault(player.getUUID(), List.of()));
        if (oldPower != null) {
            PowerBehavior old = PowerBehaviors.get(oldPower.id());
            if (old != null) {
                removeEffectsOf(player, List.of(old));
            }
        }
        OVERRIDES.remove(player.getUUID());
        PowerState.clear(player);
        Morphs.set(player, null);
        READY_AT.remove(player.getUUID());
        PacketDistributor.sendToPlayer(player, new CooldownPayload(0));
        refresh(player);
    }

    private static void removeEffectsOf(ServerPlayer player, List<PowerBehavior> behaviors) {
        for (PowerBehavior behavior : behaviors) {
            for (PowerBehavior.EffectBonus bonus : behavior.effects()) {
                MobEffectInstance current = player.getEffect(bonus.effect());
                if (current != null && current.isAmbient()) {
                    player.removeEffect(bonus.effect());
                }
            }
        }
    }

    public static void tick(ServerPlayer player) {
        List<PowerBehavior> behaviors = behaviors(player);
        List<PowerBehavior> applied = APPLIED.get(player.getUUID());
        if (applied != null && !applied.equals(behaviors)) {
            removeEffectsOf(player, applied);
            refresh(player);
        }
        if (player.tickCount % 20 == 0) {
            for (PowerBehavior behavior : behaviors) {
                for (PowerBehavior.EffectBonus bonus : behavior.effects()) {
                    player.addEffect(new MobEffectInstance(bonus.effect(), EFFECT_TICKS, bonus.amplifier(), true,
                            false, true));
                }
            }
            updateFlight(player, behaviors);
        }
        for (PowerBehavior behavior : behaviors) {
            for (PowerBehavior.Ticker ticker : behavior.tickers()) {
                ticker.tick(player);
            }
        }
    }

    // Survival players only fly through a power, so anyone in survival who may fly without one loses it.
    private static void updateFlight(ServerPlayer player, List<PowerBehavior> behaviors) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        boolean wants = PowerState.of(player).flag("flight");
        for (PowerBehavior behavior : behaviors) {
            wants |= behavior.flight();
        }
        if (wants != player.getAbilities().mayfly) {
            player.getAbilities().mayfly = wants;
            if (!wants) {
                player.getAbilities().flying = false;
            }
            player.onUpdateAbilities();
        }
    }

    /** Lets a power switch flight on or off for a while (for example a dragon form). */
    public static void setTemporaryFlight(ServerPlayer player, boolean on) {
        PowerState.of(player).setFlag("flight", on);
        updateFlight(player, behaviors(player));
    }

    public static boolean isImmune(ServerPlayer player, DamageSource source) {
        for (PowerBehavior behavior : behaviors(player)) {
            if (behavior.isImmune(player, source)) {
                return true;
            }
        }
        return false;
    }

    /** Called when the power key is pressed. Returns false if the power has no key ability. */
    public static boolean use(ServerPlayer player) {
        if (isSuppressed(player)) {
            Abilities.tell(player, "Your powers are switched off right now");
            return true;
        }
        PowerBehavior behavior = null;
        for (PowerBehavior candidate : behaviors(player)) {
            if (candidate.activation() != null) {
                behavior = candidate;
                break;
            }
        }
        if (behavior == null) {
            return false;
        }
        long now = now(player);
        long readyAt = READY_AT.getOrDefault(player.getUUID(), 0L);
        if (now < readyAt) {
            Abilities.tell(player, "Ready in " + (int) Math.ceil((readyAt - now) / 20.0) + "s");
            return true;
        }
        if (behavior.activation().activate(player) && behavior.cooldownTicks() > 0) {
            int cooldown = behavior.cooldownTicks();
            Long boostedUntil = BOOSTED.get(player.getUUID());
            if (boostedUntil != null && now < boostedUntil) {
                cooldown /= 2;
            }
            READY_AT.put(player.getUUID(), now + cooldown);
            PacketDistributor.sendToPlayer(player, new CooldownPayload(cooldown));
        }
        return true;
    }

    public static void onLogout(ServerPlayer player) {
        READY_AT.remove(player.getUUID());
        APPLIED.remove(player.getUUID());
        PowerState.clear(player);
        Morphs.set(player, null);
    }
}
