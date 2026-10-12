package com.emil.xmen.apocalypse;

import org.joml.Vector3f;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Small helpers shared by Apocalypse and his servants. */
final class PyramidFx {
    static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.25f, 0.55f, 1.0f), 1.6f);
    static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.0f, 0.8f, 0.2f), 1.4f);
    static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.75f, 0.2f, 1.0f), 1.3f);
    static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.8f, 0.15f, 0.2f), 1.3f);
    static final DustParticleOptions STEEL = new DustParticleOptions(new Vector3f(0.7f, 0.75f, 0.85f), 1.0f);

    private PyramidFx() {
    }

    /** True for Apocalypse and everyone who serves him (they never hurt each other). */
    static boolean ally(Entity entity) {
        return entity instanceof ApocalypseBoss || entity instanceof ApocalypseServant;
    }

    static void beam(ServerLevel level, Vec3 a, Vec3 b, ParticleOptions particle) {
        int steps = (int) (a.distanceTo(b) * 3);
        for (int i = 0; i <= steps; i++) {
            Vec3 p = a.lerp(b, i / (double) Math.max(steps, 1));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    static void beam(ServerLevel level, LivingEntity from, LivingEntity to, ParticleOptions particle) {
        beam(level, from.getEyePosition(), to.position().add(0, to.getBbHeight() / 2, 0), particle);
    }

    static void sound(ServerLevel level, Entity at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    static void say(ServerLevel level, Entity speaker, String text, ChatFormatting color) {
        Component line = speaker.getDisplayName().copy().withStyle(color, ChatFormatting.BOLD)
                .append(Component.literal(": \"" + text + "\"").withStyle(ChatFormatting.WHITE));
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(speaker) < 64 * 64) {
                player.sendSystemMessage(line);
            }
        }
    }

    /** Shows the bar to players within range and hides it from everyone else. */
    static void updateBar(ServerLevel level, LivingEntity mob, ServerBossEvent bar, double range, boolean show) {
        bar.setProgress(Math.max(0.0f, Math.min(1.0f, mob.getHealth() / mob.getMaxHealth())));
        for (ServerPlayer player : level.players()) {
            boolean near = show && player.distanceToSqr(mob) < range * range;
            if (near && !bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            } else if (!near && bar.getPlayers().contains(player)) {
                bar.removePlayer(player);
            }
        }
    }

    /** "Apocalypse   ❤ 412 / 600" */
    static Component barTitle(LivingEntity mob, ChatFormatting color) {
        return mob.getDisplayName().copy().withStyle(color, ChatFormatting.BOLD)
                .append(Component.literal("   ❤ " + (int) Math.ceil(mob.getHealth()) + " / "
                        + (int) mob.getMaxHealth()).withStyle(ChatFormatting.RED));
    }
}
