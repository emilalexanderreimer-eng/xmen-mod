package com.emil.xmen.world;

import java.util.List;

import com.emil.xmen.XMenMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import com.mojang.datafixers.util.Pair;

/** The X-Men structures. Their buildings and spawn rules live in data/xmen (made by tools/structures). */
public final class ModStructures {
    public static final ResourceKey<Structure> X_MANSION = key("x_mansion");
    public static final ResourceKey<Structure> APOCALYPSE_PYRAMID = key("apocalypse_pyramid");
    public static final ResourceKey<Structure> FALLEN_SENTINEL = key("fallen_sentinel");
    public static final ResourceKey<Structure> BROTHERHOOD_HIDEOUT = key("brotherhood_hideout");
    public static final ResourceKey<Structure> ASTEROID_M = key("asteroid_m");
    public static final ResourceKey<Structure> SENTINEL_FACTORY = key("sentinel_factory");
    public static final ResourceKey<Structure> WEAPON_X = key("weapon_x");
    public static final ResourceKey<Structure> SINISTER_LAB = key("sinister_lab");
    public static final ResourceKey<Structure> HELLFIRE_CLUB = key("hellfire_club");
    public static final ResourceKey<Structure> GENOSHA = key("genosha");
    public static final ResourceKey<Structure> KRAKOA = key("krakoa");
    public static final ResourceKey<Structure> MUIR_ISLAND = key("muir_island");
    public static final ResourceKey<Structure> MORLOCK_TUNNELS = key("morlock_tunnels");
    public static final ResourceKey<Structure> BLACKBIRD_CRASH = key("blackbird_crash");
    public static final ResourceKey<Structure> SAFEHOUSE = key("safehouse");
    public static final ResourceKey<Structure> DETECTION_TOWER = key("detection_tower");

    public static final List<ResourceKey<Structure>> ALL = List.of(X_MANSION, APOCALYPSE_PYRAMID, FALLEN_SENTINEL,
            BROTHERHOOD_HIDEOUT, ASTEROID_M, SENTINEL_FACTORY, WEAPON_X, SINISTER_LAB, HELLFIRE_CLUB, GENOSHA, KRAKOA,
            MUIR_ISLAND, MORLOCK_TUNNELS, BLACKBIRD_CRASH, SAFEHOUSE, DETECTION_TOWER);

    public static final TagKey<Structure> CEREBRO_TAG = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, "cerebro"));

    private ModStructures() {
    }

    private static ResourceKey<Structure> key(String name) {
        return ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath(XMenMod.MOD_ID, name));
    }

    /** "x_mansion" -> "X-Mansion" and so on, for chat messages. */
    public static String displayName(ResourceKey<Structure> key) {
        return switch (key.location().getPath()) {
            case "x_mansion" -> "X-Mansion";
            case "apocalypse_pyramid" -> "Apocalypse's Pyramid";
            case "fallen_sentinel" -> "Fallen Sentinel";
            case "brotherhood_hideout" -> "Brotherhood Hideout";
            case "asteroid_m" -> "Asteroid M";
            case "sentinel_factory" -> "Sentinel Factory";
            case "weapon_x" -> "Weapon X Facility";
            case "sinister_lab" -> "Mister Sinister's Lab";
            case "hellfire_club" -> "Hellfire Club";
            case "genosha" -> "Genosha";
            case "krakoa" -> "Krakoa";
            case "muir_island" -> "Muir Island";
            case "morlock_tunnels" -> "Morlock Tunnels";
            case "blackbird_crash" -> "Blackbird Crash Site";
            case "safehouse" -> "Mutant Safehouse";
            case "detection_tower" -> "Mutant Detection Tower";
            default -> key.location().getPath();
        };
    }

    public static Structure get(ServerLevel level, ResourceKey<Structure> key) {
        return level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(key);
    }

    /** True if the position is inside one of the pieces of that structure. */
    public static boolean isInside(ServerLevel level, BlockPos pos, ResourceKey<Structure> key) {
        Structure structure = get(level, key);
        return structure != null && level.structureManager().getStructureWithPieceAt(pos, structure).isValid();
    }

    /** Where the nearest one of these structures is, or null if there's none in range. */
    public static BlockPos findNearest(ServerLevel level, ResourceKey<Structure> key, BlockPos from, int chunkRadius) {
        Holder<Structure> holder = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolder(key)
                .orElse(null);
        if (holder == null) {
            return null;
        }
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder), from, chunkRadius, false);
        return found == null ? null : found.getFirst();
    }
}
