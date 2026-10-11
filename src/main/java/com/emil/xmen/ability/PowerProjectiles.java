package com.emil.xmen.ability;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Thrown power projectiles (acid, plasma, bombs, seeds...). They fly like snowballs that look like any
 * item, and run their own code when they hit something.
 */
public final class PowerProjectiles {
    public static final String KIND_TAG = "xmen_projectile";

    @FunctionalInterface
    public interface ImpactHandler {
        /** owner may be null if the thrower left the game. */
        void onImpact(ServerPlayer owner, Projectile projectile, HitResult hit);
    }

    private static final Map<String, ImpactHandler> HANDLERS = new HashMap<>();

    private PowerProjectiles() {
    }

    public static void register(String kind, ImpactHandler handler) {
        HANDLERS.put(kind, handler);
    }

    /** Throws a projectile that looks like look, flying where the player aims. */
    public static Snowball shoot(ServerPlayer player, String kind, ItemStack look, float speed, float inaccuracy,
            boolean gravity) {
        Snowball snowball = new Snowball(player.level(), player);
        snowball.setItem(look);
        snowball.getPersistentData().putString(KIND_TAG, kind);
        snowball.setNoGravity(!gravity);
        Vec3 eye = player.getEyePosition();
        snowball.setPos(eye.x, eye.y - 0.1, eye.z);
        snowball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, speed, inaccuracy);
        player.level().addFreshEntity(snowball);
        return snowball;
    }

    /** Returns true if the projectile was one of ours and was handled. */
    static boolean handle(Projectile projectile, HitResult hit) {
        String kind = projectile.getPersistentData().getString(KIND_TAG);
        if (kind.isEmpty()) {
            return false;
        }
        ImpactHandler handler = HANDLERS.get(kind);
        if (handler == null) {
            return false;
        }
        ServerPlayer owner = projectile.getOwner() instanceof ServerPlayer p ? p : null;
        handler.onImpact(owner, projectile, hit);
        return true;
    }
}
