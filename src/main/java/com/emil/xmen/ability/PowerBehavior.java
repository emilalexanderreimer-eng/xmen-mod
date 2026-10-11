package com.emil.xmen.ability;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * What a power actually does, built from small reusable pieces: always-on potion effects,
 * attribute bonuses, flight, damage immunities, a ticking hook and a key ability with a cooldown.
 */
public final class PowerBehavior {
    /** Runs when the power key is pressed. Return true if the power was used, which starts the cooldown. */
    @FunctionalInterface
    public interface Activation {
        boolean activate(ServerPlayer player);
    }

    @FunctionalInterface
    public interface Ticker {
        void tick(ServerPlayer player);
    }

    @FunctionalInterface
    public interface Immunity {
        boolean blocks(ServerPlayer player, DamageSource source);
    }

    public record EffectBonus(Holder<MobEffect> effect, int amplifier) {
    }

    public record AttributeBonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
    }

    private final List<EffectBonus> effects;
    private final List<AttributeBonus> attributes;
    private final List<Ticker> tickers;
    private final List<Immunity> immunities;
    private final boolean flight;
    private final Activation activation;
    private final int cooldownTicks;

    private PowerBehavior(Builder builder) {
        this.effects = List.copyOf(builder.effects);
        this.attributes = List.copyOf(builder.attributes);
        this.tickers = List.copyOf(builder.tickers);
        this.immunities = List.copyOf(builder.immunities);
        this.flight = builder.flight;
        this.activation = builder.activation;
        this.cooldownTicks = builder.cooldownTicks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<EffectBonus> effects() {
        return effects;
    }

    public List<AttributeBonus> attributes() {
        return attributes;
    }

    public List<Ticker> tickers() {
        return tickers;
    }

    public boolean flight() {
        return flight;
    }

    public Activation activation() {
        return activation;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public boolean isImmune(ServerPlayer player, DamageSource source) {
        for (Immunity immunity : immunities) {
            if (immunity.blocks(player, source)) {
                return true;
            }
        }
        return false;
    }

    public static final class Builder {
        private final List<EffectBonus> effects = new ArrayList<>();
        private final List<AttributeBonus> attributes = new ArrayList<>();
        private final List<Ticker> tickers = new ArrayList<>();
        private final List<Immunity> immunities = new ArrayList<>();
        private boolean flight;
        private Activation activation;
        private int cooldownTicks;

        /** An always-on potion effect. Amplifier 0 is level I. */
        public Builder effect(Holder<MobEffect> effect, int amplifier) {
            effects.add(new EffectBonus(effect, amplifier));
            return this;
        }

        public Builder attribute(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
            attributes.add(new AttributeBonus(attribute, amount, operation));
            return this;
        }

        public Builder flight() {
            this.flight = true;
            return this;
        }

        public Builder tick(Ticker ticker) {
            tickers.add(ticker);
            return this;
        }

        public Builder immuneTo(Immunity immunity) {
            immunities.add(immunity);
            return this;
        }

        /** The key ability and its cooldown in seconds. */
        public Builder active(double cooldownSeconds, Activation activation) {
            this.activation = activation;
            this.cooldownTicks = (int) Math.round(cooldownSeconds * 20);
            return this;
        }

        public PowerBehavior build() {
            return new PowerBehavior(this);
        }
    }
}
