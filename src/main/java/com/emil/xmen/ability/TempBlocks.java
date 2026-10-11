package com.emil.xmen.ability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Blocks that powers place for a while and that turn back into what was there before. */
public final class TempBlocks {
    private static final class Entry {
        final ServerLevel level;
        final BlockPos pos;
        final BlockState placed;
        final BlockState previous;
        long endTime;

        Entry(ServerLevel level, BlockPos pos, BlockState placed, BlockState previous, long endTime) {
            this.level = level;
            this.pos = pos;
            this.placed = placed;
            this.previous = previous;
            this.endTime = endTime;
        }
    }

    private static final Map<BlockPos, Entry> ENTRIES = new HashMap<>();

    private TempBlocks() {
    }

    /** Places state at pos for ticks if the spot is empty or replaceable (like grass or water). */
    public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        BlockState current = level.getBlockState(pos);
        Entry existing = ENTRIES.get(pos);
        if (existing != null && existing.level == level) {
            if (existing.placed == current) {
                level.setBlockAndUpdate(pos, state);
                ENTRIES.put(pos.immutable(), new Entry(level, pos.immutable(), state, existing.previous,
                        level.getGameTime() + ticks));
                return true;
            }
        }
        if (!current.canBeReplaced() || current.hasBlockEntity()) {
            return false;
        }
        return replace(level, pos, state, ticks);
    }

    /** Swaps any block (except unbreakable ones) for state, putting the old one back after ticks. */
    public static boolean replace(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        BlockState current = level.getBlockState(pos);
        if (current.getDestroySpeed(level, pos) < 0 || current.hasBlockEntity()) {
            return false;
        }
        Entry existing = ENTRIES.get(pos);
        BlockState previous = existing != null && existing.level == level ? existing.previous : current;
        level.setBlockAndUpdate(pos, state);
        ENTRIES.put(pos.immutable(), new Entry(level, pos.immutable(), state, previous, level.getGameTime() + ticks));
        return true;
    }

    /** Puts the original block back right away. */
    public static void restore(ServerLevel level, BlockPos pos) {
        Entry entry = ENTRIES.get(pos);
        if (entry != null && entry.level == level) {
            ENTRIES.remove(pos);
            if (level.getBlockState(pos) == entry.placed) {
                level.setBlockAndUpdate(pos, entry.previous);
            }
        }
    }

    public static boolean isTemp(BlockPos pos) {
        return ENTRIES.containsKey(pos);
    }

    static void tick() {
        if (ENTRIES.isEmpty()) {
            return;
        }
        List<Entry> done = new ArrayList<>();
        for (Iterator<Entry> it = ENTRIES.values().iterator(); it.hasNext();) {
            Entry entry = it.next();
            if (entry.level.getGameTime() >= entry.endTime) {
                done.add(entry);
                it.remove();
            }
        }
        for (Entry entry : done) {
            if (entry.level.getBlockState(entry.pos) == entry.placed) {
                entry.level.setBlockAndUpdate(entry.pos, entry.previous);
            }
        }
    }

    /** Puts everything back right away, used when the server stops. */
    static void restoreAll() {
        for (Entry entry : ENTRIES.values()) {
            if (entry.level.getBlockState(entry.pos) == entry.placed) {
                entry.level.setBlockAndUpdate(entry.pos, entry.previous);
            }
        }
        ENTRIES.clear();
    }
}
