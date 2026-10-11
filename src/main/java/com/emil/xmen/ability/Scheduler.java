package com.emil.xmen.ability;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Runs code a number of server ticks from now. */
public final class Scheduler {
    private record Task(long runAt, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static long now;

    private Scheduler() {
    }

    public static void later(int ticks, Runnable action) {
        TASKS.add(new Task(now + Math.max(1, ticks), action));
    }

    /** Runs action every period ticks, count times (the first run is one period from now). */
    public static void repeat(int period, int count, Runnable action) {
        for (int i = 1; i <= count; i++) {
            later(period * i, action);
        }
    }

    static void tick() {
        now++;
        if (TASKS.isEmpty()) {
            return;
        }
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext();) {
            Task task = it.next();
            if (task.runAt() <= now) {
                due.add(task);
                it.remove();
            }
        }
        for (Task task : due) {
            try {
                task.action().run();
            } catch (RuntimeException e) {
                com.emil.xmen.XMenMod.LOGGER.error("Power task failed", e);
            }
        }
    }
}
