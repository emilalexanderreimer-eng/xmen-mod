package com.emil.xmen.client;

import com.emil.xmen.power.Power;
import com.emil.xmen.power.Powers;

/** The local player's power, as last told by the server. Plain data, safe to touch on either side. */
public final class ClientPowerState {
    private static String powerId = "";

    private ClientPowerState() {
    }

    public static void set(String id) {
        powerId = id;
    }

    public static Power get() {
        return Powers.get(powerId);
    }
}
