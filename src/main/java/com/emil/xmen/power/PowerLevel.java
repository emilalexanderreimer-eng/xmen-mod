package com.emil.xmen.power;

import net.minecraft.ChatFormatting;
import net.minecraft.util.RandomSource;

/** Marvel mutant classes, from weakest to strongest, with the chance to roll each one. */
public enum PowerLevel {
    EPSILON("Epsilon", 30, ChatFormatting.GRAY),
    DELTA("Delta", 25, ChatFormatting.GREEN),
    GAMMA("Gamma", 20, ChatFormatting.AQUA),
    BETA("Beta", 14, ChatFormatting.BLUE),
    ALPHA("Alpha", 9, ChatFormatting.GOLD),
    OMEGA("Omega", 2, ChatFormatting.LIGHT_PURPLE);

    private final String displayName;
    private final int chancePercent;
    private final ChatFormatting color;

    PowerLevel(String displayName, int chancePercent, ChatFormatting color) {
        this.displayName = displayName;
        this.chancePercent = chancePercent;
        this.color = color;
    }

    public String displayName() {
        return displayName;
    }

    public int chancePercent() {
        return chancePercent;
    }

    public ChatFormatting color() {
        return color;
    }

    public static PowerLevel roll(RandomSource random) {
        int total = 0;
        for (PowerLevel level : values()) {
            total += level.chancePercent;
        }
        int pick = random.nextInt(total);
        for (PowerLevel level : values()) {
            pick -= level.chancePercent;
            if (pick < 0) {
                return level;
            }
        }
        return EPSILON;
    }
}
