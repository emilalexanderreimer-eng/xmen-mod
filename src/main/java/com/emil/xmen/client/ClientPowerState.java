package com.emil.xmen.client;

import com.emil.xmen.power.Power;
import com.emil.xmen.power.Powers;

/** The local player's power, as last told by the server. Plain data, safe to touch on either side. */
public final class ClientPowerState {
    private static String powerId = "";
    private static int cooldownTicks;

    private ClientPowerState() {
    }

    public static void set(String id) {
        powerId = id;
        cooldownTicks = 0;
    }

    public static void setCooldown(int ticks) {
        cooldownTicks = ticks;
    }

    public static int cooldownTicks() {
        return cooldownTicks;
    }

    /** Counts the cooldown down, once per client tick. */
    public static void tick() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
    }

    public static Power get() {
        return Powers.get(powerId);
    }
}
