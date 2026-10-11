package com.emil.xmen.power;

public enum PowerType {
    /** Used with the power key, has a cooldown. */
    ACTIVE("Active"),
    /** Always on. */
    PASSIVE("Passive"),
    /** Always-on effects plus a key ability. */
    BOTH("Both");

    private final String displayName;

    PowerType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean usesKey() {
        return this != PASSIVE;
    }
}
