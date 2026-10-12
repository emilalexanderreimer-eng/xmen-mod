package com.emil.xmen.sentinel;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * The kinds of attack a Sentinel's adaptive plating can learn to resist, like the future Sentinels in
 * Days of Future Past that adapt to every mutant power thrown at them.
 */
public enum Threat {
    MELEE("swords and punches"),
    PROJECTILE("arrows and shots"),
    FIRE("fire"),
    COLD("ice"),
    EXPLOSION("explosions"),
    POWER("mutant powers");

    public final String description;

    Threat(String description) {
        this.description = description;
    }

    /** What kind of attack this damage is, or null for things like falling or drowning. */
    public static Threat of(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return FIRE;
        }
        if (source.is(DamageTypeTags.IS_FREEZING)) {
            return COLD;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return EXPLOSION;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return PROJECTILE;
        }
        if (source.getEntity() == null) {
            return null;
        }
        if (source.getDirectEntity() == source.getEntity() && source.getEntity() instanceof LivingEntity
                && !source.is(DamageTypeTags.WITCH_RESISTANT_TO) && !source.is(DamageTypeTags.IS_LIGHTNING)) {
            return MELEE;
        }
        return POWER;
    }
}
