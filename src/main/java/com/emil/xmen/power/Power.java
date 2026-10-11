package com.emil.xmen.power;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public record Power(String id, String name, String basedOn, PowerLevel level, PowerCategory category,
        PowerType type, String description) {

    /** "Optic Blasts [Beta]" in the level's color. */
    public MutableComponent title() {
        return Component.literal(name).withStyle(level.color())
                .append(Component.literal(" [" + level.displayName() + "]").withStyle(level.color()));
    }
}
