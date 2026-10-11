package com.emil.xmen.client;

import java.util.HashMap;
import java.util.Map;

/** Which players currently look like a mob, by entity id. Plain data, safe to touch on either side. */
public final class ClientMorphs {
    private static final Map<Integer, String> MORPHS = new HashMap<>();

    private ClientMorphs() {
    }

    public static void set(int entityId, String entityType) {
        if (entityType.isEmpty()) {
            MORPHS.remove(entityId);
        } else {
            MORPHS.put(entityId, entityType);
        }
    }

    /** The mob type id (like "minecraft:wolf") the player with entityId looks like, or null. */
    public static String get(int entityId) {
        return MORPHS.get(entityId);
    }

    public static void clear() {
        MORPHS.clear();
    }
}
