package com.emil.xmen.world;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.emil.xmen.power.PlayerPowers;
import com.emil.xmen.power.Power;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Things that happen while a player is inside a structure:
 * the Detection Tower raises the alarm, Krakoa heals mutants, Asteroid M boosts magnetism,
 * Weapon X boosts claws and healing factors.
 */
public final class StructureEffects {
    private static final Map<UUID, Long> LAST_ALARM = new HashMap<>();

    private StructureEffects() {
    }

    /** Called for each player every 2 seconds. */
    public static void tick(ServerPlayer player) {
        if (player.isSpectator() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Power power = PlayerPowers.get(player);
        if (power == null) {
            return;
        }
        BlockPos pos = player.blockPosition();
        if (!player.isCreative() && ModStructures.isInside(level, pos, ModStructures.DETECTION_TOWER)) {
            alarm(level, player);
        }
        if (ModStructures.isInside(level, pos, ModStructures.KRAKOA)) {
            effect(player, new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        }
        if (power.id().equals("magnetism") && ModStructures.isInside(level, pos, ModStructures.ASTEROID_M)) {
            effect(player, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 1, true, false, true));
            effect(player, new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
        }
        if ((power.id().equals("claws") || power.id().equals("healing_factor"))
                && ModStructures.isInside(level, pos, ModStructures.WEAPON_X)) {
            effect(player, new MobEffectInstance(MobEffects.REGENERATION, 60, 1, true, false, true));
            effect(player, new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
        }
    }

    private static void effect(ServerPlayer player, MobEffectInstance effect) {
        player.addEffect(effect);
    }

    private static void alarm(ServerLevel level, ServerPlayer player) {
        long now = level.getGameTime();
        Long last = LAST_ALARM.get(player.getUUID());
        if (last != null && now - last < 1200) {
            return;
        }
        LAST_ALARM.put(player.getUUID(), now);
        player.sendSystemMessage(Component.literal("ALARM! Mutant detected: " + player.getName().getString())
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0f, 1.0f);
        level.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0f, 1.0f);
        for (int i = 0; i < 2; i++) {
            // real anti-mutant soldiers with rifles, not renamed pillagers
            com.emil.xmen.entity.MutantNpc guard = com.emil.xmen.entity.ModEntities.MUTANT.get().create(level);
            if (guard == null) {
                return;
            }
            guard.setCharacter("anti_mutant_soldier");
            double angle = level.random.nextDouble() * Math.PI * 2;
            int x = (int) (player.getX() + Math.cos(angle) * 8);
            int z = (int) (player.getZ() + Math.sin(angle) * 8);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            guard.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            guard.addTag("xmen.anti_mutant_soldier");
            guard.addTag("xmen.no_loot");
            level.addFreshEntity(guard);
            guard.setTarget(player);
        }
    }
}
