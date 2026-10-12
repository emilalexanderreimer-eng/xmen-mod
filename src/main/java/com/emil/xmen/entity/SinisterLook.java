package com.emil.xmen.entity;

import java.util.UUID;

/** What the client needs to draw Mister Sinister and his clones. */
public interface SinisterLook {
    /** True while he holds his hands out to use a power (the model raises its arms). */
    boolean isCasting();

    /** True for a clone still asleep in its tank (head bowed, arms hanging). */
    boolean isDormant();

    /** The player whose look he has stolen right now, or null. */
    UUID getDisguise();

    /** Only the real Sinister wears his cape. */
    boolean hasCape();
}
