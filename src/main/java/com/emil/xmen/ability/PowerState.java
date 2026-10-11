package com.emil.xmen.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

/**
 * Short-lived, per-player memory for powers: timers, toggles and small values.
 * Cleared when the power changes, on respawn and on logout, running any cleanup that powers registered.
 */
public final class PowerState {
    private static final Map<UUID, PowerState> STATES = new HashMap<>();

    private final Map<String, Long> timers = new HashMap<>();
    private final Map<String, Object> values = new HashMap<>();
    private final List<Runnable> cleanups = new ArrayList<>();

    private PowerState() {
    }

    public static PowerState of(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), id -> new PowerState());
    }

    public static void clear(ServerPlayer player) {
        PowerState state = STATES.remove(player.getUUID());
        if (state != null) {
            for (Runnable cleanup : state.cleanups) {
                cleanup.run();
            }
        }
    }

    /** Runs when this state is cleared (power change, respawn, logout). */
    public void onClear(Runnable cleanup) {
        cleanups.add(cleanup);
    }

    public void start(String key, long now, int ticks) {
        timers.put(key, now + ticks);
    }

    public boolean active(String key, long now) {
        Long end = timers.get(key);
        return end != null && now < end;
    }

    public long remaining(String key, long now) {
        Long end = timers.get(key);
        return end == null ? 0 : Math.max(0, end - now);
    }

    public void stop(String key) {
        timers.remove(key);
    }

    public boolean flag(String key) {
        return Boolean.TRUE.equals(values.get(key));
    }

    public void setFlag(String key, boolean on) {
        values.put(key, on);
    }

    /** Flips a flag and returns its new value. */
    public boolean toggle(String key) {
        boolean on = !flag(key);
        values.put(key, on);
        return on;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) values.get(key);
    }

    public void put(String key, Object value) {
        values.put(key, value);
    }

    public Object remove(String key) {
        return values.remove(key);
    }

    public double number(String key) {
        Object value = values.get(key);
        return value instanceof Number n ? n.doubleValue() : 0;
    }

    public double add(String key, double delta) {
        double value = number(key) + delta;
        values.put(key, value);
        return value;
    }
}
