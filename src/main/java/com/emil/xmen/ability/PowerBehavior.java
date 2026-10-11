package com.emil.xmen.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * What a power actually does, built from small reusable pieces: always-on potion effects, attribute
 * bonuses, flight, damage immunities, ticking code, event hooks and a key ability with a cooldown.
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

    /** The player hit something (melee hits only unless the hook checks otherwise). Can change the damage. */
    @FunctionalInterface
    public interface AttackHook {
        void onAttack(ServerPlayer player, LivingEntity target, LivingIncomingDamageEvent event);
    }

    /** The player is about to take damage. Can change or cancel it. */
    @FunctionalInterface
    public interface HurtHook {
        void onHurt(ServerPlayer player, LivingIncomingDamageEvent event);
    }

    /** The player is about to die. Return true to stop the death (and heal the player yourself). */
    @FunctionalInterface
    public interface DeathHook {
        boolean preventDeath(ServerPlayer player, DamageSource source);
    }

    @FunctionalInterface
    public interface KillHook {
        void onKill(ServerPlayer player, LivingEntity victim, Collection<ItemEntity> drops);
    }

    @FunctionalInterface
    public interface BreakHook {
        void onBreak(ServerPlayer player, BlockPos pos, BlockState state);
    }

    /** Return true if mob should not be allowed to target the player. */
    @FunctionalInterface
    public interface TargetFilter {
        boolean ignores(ServerPlayer player, Mob mob);
    }

    /** A projectile is about to hit the player. Return true to stop it. */
    @FunctionalInterface
    public interface ProjectileHook {
        boolean stops(ServerPlayer player, Projectile projectile);
    }

    @FunctionalInterface
    public interface EatHook {
        void onEat(ServerPlayer player, ItemStack food);
    }

    /** The player landed after a fall. Return true to cancel the fall damage. */
    @FunctionalInterface
    public interface FallHook {
        boolean onFall(ServerPlayer player, float distance);
    }

    @FunctionalInterface
    public interface SpawnHook {
        void onSpawn(ServerPlayer player, Entity spawned);
    }

    @FunctionalInterface
    public interface XpHook {
        void onXp(ServerPlayer player, int amount);
    }

    public record EffectBonus(Holder<MobEffect> effect, int amplifier) {
    }

    public record AttributeBonus(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
    }

    final List<EffectBonus> effects;
    final List<AttributeBonus> attributes;
    final List<Ticker> tickers;
    final List<Immunity> immunities;
    final List<AttackHook> attackHooks;
    final List<HurtHook> hurtHooks;
    final List<DeathHook> deathHooks;
    final List<KillHook> killHooks;
    final List<BreakHook> breakHooks;
    final List<TargetFilter> targetFilters;
    final List<ProjectileHook> projectileHooks;
    final List<EatHook> eatHooks;
    final List<FallHook> fallHooks;
    final List<SpawnHook> spawnHooks;
    final List<XpHook> xpHooks;
    final boolean flight;
    final boolean keepXp;
    final boolean silent;
    final Activation activation;
    final int cooldownTicks;

    private PowerBehavior(Builder b) {
        this.effects = List.copyOf(b.effects);
        this.attributes = List.copyOf(b.attributes);
        this.tickers = List.copyOf(b.tickers);
        this.immunities = List.copyOf(b.immunities);
        this.attackHooks = List.copyOf(b.attackHooks);
        this.hurtHooks = List.copyOf(b.hurtHooks);
        this.deathHooks = List.copyOf(b.deathHooks);
        this.killHooks = List.copyOf(b.killHooks);
        this.breakHooks = List.copyOf(b.breakHooks);
        this.targetFilters = List.copyOf(b.targetFilters);
        this.projectileHooks = List.copyOf(b.projectileHooks);
        this.eatHooks = List.copyOf(b.eatHooks);
        this.fallHooks = List.copyOf(b.fallHooks);
        this.spawnHooks = List.copyOf(b.spawnHooks);
        this.xpHooks = List.copyOf(b.xpHooks);
        this.flight = b.flight;
        this.keepXp = b.keepXp;
        this.silent = b.silent;
        this.activation = b.activation;
        this.cooldownTicks = b.cooldownTicks;
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
        private final List<AttackHook> attackHooks = new ArrayList<>();
        private final List<HurtHook> hurtHooks = new ArrayList<>();
        private final List<DeathHook> deathHooks = new ArrayList<>();
        private final List<KillHook> killHooks = new ArrayList<>();
        private final List<BreakHook> breakHooks = new ArrayList<>();
        private final List<TargetFilter> targetFilters = new ArrayList<>();
        private final List<ProjectileHook> projectileHooks = new ArrayList<>();
        private final List<EatHook> eatHooks = new ArrayList<>();
        private final List<FallHook> fallHooks = new ArrayList<>();
        private final List<SpawnHook> spawnHooks = new ArrayList<>();
        private final List<XpHook> xpHooks = new ArrayList<>();
        private boolean flight;
        private boolean keepXp;
        private boolean silent;
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

        public Builder keepXp() {
            this.keepXp = true;
            return this;
        }

        /** The player makes no vibrations (sculk sensors, wardens). */
        public Builder silent() {
            this.silent = true;
            return this;
        }

        public Builder tick(Ticker ticker) {
            tickers.add(ticker);
            return this;
        }

        /** Runs ticker every period ticks. */
        public Builder every(int period, Ticker ticker) {
            tickers.add(player -> {
                if (player.tickCount % period == 0) {
                    ticker.tick(player);
                }
            });
            return this;
        }

        public Builder immuneTo(Immunity immunity) {
            immunities.add(immunity);
            return this;
        }

        public Builder onAttack(AttackHook hook) {
            attackHooks.add(hook);
            return this;
        }

        public Builder onHurt(HurtHook hook) {
            hurtHooks.add(hook);
            return this;
        }

        public Builder onDeath(DeathHook hook) {
            deathHooks.add(hook);
            return this;
        }

        public Builder onKill(KillHook hook) {
            killHooks.add(hook);
            return this;
        }

        public Builder onBreak(BreakHook hook) {
            breakHooks.add(hook);
            return this;
        }

        public Builder ignoredBy(TargetFilter filter) {
            targetFilters.add(filter);
            return this;
        }

        public Builder stopsProjectiles(ProjectileHook hook) {
            projectileHooks.add(hook);
            return this;
        }

        public Builder onEat(EatHook hook) {
            eatHooks.add(hook);
            return this;
        }

        public Builder onFall(FallHook hook) {
            fallHooks.add(hook);
            return this;
        }

        public Builder onSpawn(SpawnHook hook) {
            spawnHooks.add(hook);
            return this;
        }

        public Builder onXp(XpHook hook) {
            xpHooks.add(hook);
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
