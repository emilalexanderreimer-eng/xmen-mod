package com.emil.xmen.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Makes blocks show a glowing outline through walls for a while (ores, chests). */
public final class Highlights {
    public static final String TEMP_TAG = "xmen_temp";

    private Highlights() {
    }

    /** Outlines the block at pos for seconds, in the given RGB color. */
    public static void block(ServerLevel level, BlockPos pos, int color, double seconds) {
        BlockState state = level.getBlockState(pos);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:block_display");
        tag.put("block_state", NbtUtils.writeBlockState(state));
        tag.putBoolean("Glowing", true);
        tag.putInt("glow_color_override", color);
        Entity display = EntityType.loadEntityRecursive(tag, level, e -> {
            e.moveTo(pos.getX() + 0.001, pos.getY() + 0.001, pos.getZ() + 0.001, 0, 0);
            return e;
        });
        if (display == null) {
            return;
        }
        level.addFreshEntity(display);
        display.getPersistentData().putBoolean(TEMP_TAG, true);
        Scheduler.later((int) (seconds * 20), display::discard);
    }

    /** Leftover highlights from before a restart are removed when they load. */
    static void onEntityLoaded(Entity entity) {
        if (entity.getPersistentData().getBoolean(TEMP_TAG)) {
            entity.discard();
        }
    }
}
