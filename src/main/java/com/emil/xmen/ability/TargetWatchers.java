package com.emil.xmen.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;

/** Powers that react when a mob starts hunting the player. */
final class TargetWatchers {
    private TargetWatchers() {
    }

    static void onTargeted(ServerPlayer player, Mob mob) {
        if (PowerRuntime.has(player, "danger_sense") && mob.getTarget() != player) {
            Abilities.tell(player, "Danger! " + mob.getName().getString() + ", "
                    + Abilities.direction(player, mob.position()));
            Abilities.buff(player, MobEffects.MOVEMENT_SPEED, 0);
            Abilities.sound(player, SoundEvents.NOTE_BLOCK_BELL, 0.8f, 1.6f);
        }
    }
}
