package com.emil.xmen.power;

public enum PowerCategory {
    ENERGY_AND_BLASTS("Energy & Blasts"),
    FIRE_AND_HEAT("Fire & Heat"),
    ICE_AND_WATER("Ice & Water"),
    WEATHER_AND_AIR("Weather & Air"),
    EARTH_STONE_AND_METAL("Earth, Stone & Metal"),
    MOVEMENT_AND_TRAVEL("Movement & Travel"),
    BODY_AND_STRENGTH("Body & Strength"),
    HEALING_AND_LIFE("Healing & Life"),
    SENSES_AND_DETECTION("Senses & Detection"),
    MIND_AND_PSYCHIC("Mind & Psychic"),
    SHAPESHIFTING("Shapeshifting"),
    NATURE_AND_ANIMALS("Nature & Animals"),
    LIGHT_SHADOW_AND_STEALTH("Light, Shadow & Stealth"),
    REALITY_TIME_AND_SPACE("Reality, Time & Space"),
    TECH_MATTER_AND_SPECIAL("Tech, Matter & Special");

    private final String displayName;

    PowerCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
