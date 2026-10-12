package com.emil.xmen.apocalypse;

import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;

/** Who serves Apocalypse: his Four Horsemen from the movie and the Clan Akkaba cult. */
public enum ServantKind {
    STORM("storm", 140, 5, 0.30, BossEvent.BossBarColor.WHITE),
    PSYLOCKE("psylocke", 130, 9, 0.34, BossEvent.BossBarColor.PURPLE),
    ARCHANGEL("archangel", 140, 6, 0.30, BossEvent.BossBarColor.BLUE),
    MAGNETO("horseman_magneto", 170, 7, 0.27, BossEvent.BossBarColor.RED),
    CULTIST("akkaba_cultist", 30, 5, 0.30, null);

    public final String id;
    public final double health;
    public final double damage;
    public final double speed;
    /** Horsemen get their own health bar, cultists don't. */
    public final BossEvent.BossBarColor bar;

    ServantKind(String id, double health, double damage, double speed, BossEvent.BossBarColor bar) {
        this.id = id;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.bar = bar;
    }

    public boolean horseman() {
        return bar != null;
    }

    public static ServantKind of(EntityType<?> type) {
        if (type == ApocalypseContent.STORM.get()) {
            return STORM;
        }
        if (type == ApocalypseContent.PSYLOCKE.get()) {
            return PSYLOCKE;
        }
        if (type == ApocalypseContent.ARCHANGEL.get()) {
            return ARCHANGEL;
        }
        if (type == ApocalypseContent.HORSEMAN_MAGNETO.get()) {
            return MAGNETO;
        }
        return CULTIST;
    }
}
