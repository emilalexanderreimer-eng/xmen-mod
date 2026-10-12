"""Builds every X-Men structure and writes everything Minecraft needs to spawn them:

  data/xmen/structure/*.nbt                  the buildings (from mansion.py and places.py)
  data/xmen/worldgen/structure/*.json        how each one is placed
  data/xmen/worldgen/structure_set/*.json    how rare each one is
  data/xmen/worldgen/template_pool/*/start.json
  data/xmen/tags/worldgen/biome/has_structure/*.json   which biomes they spawn in
  data/xmen/loot_table/chests/*.json         what's in the chests

Run it from anywhere:  python3 tools/structures/build_all.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import mansion  # noqa: E402
import nbt  # noqa: E402
import places  # noqa: E402
import weapon_x_lab  # noqa: E402
from lib import Template  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "xmen")

LAND = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest",
        "minecraft:birch_forest", "minecraft:flower_forest", "minecraft:dark_forest", "minecraft:taiga",
        "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:savanna",
        "minecraft:savanna_plateau", "minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:desert",
        "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:swamp", "minecraft:cherry_grove",
        "minecraft:grove", "minecraft:badlands"]
FLAT = ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna",
        "minecraft:snowy_plains", "minecraft:desert"]
WOODS = ["minecraft:forest", "minecraft:birch_forest", "minecraft:flower_forest", "minecraft:taiga",
         "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:cherry_grove",
         "minecraft:dark_forest"]
OCEAN = ["minecraft:ocean", "minecraft:deep_ocean"]
WARM_OCEAN = ["minecraft:warm_ocean", "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean"]
COLD_OCEAN = ["minecraft:cold_ocean", "minecraft:deep_cold_ocean"]
ALL_OCEAN = OCEAN + WARM_OCEAN + COLD_OCEAN

NO_MONSTERS = {"monster": {"bounding_box": "full", "spawns": []}}

# name: (biomes, step, height, heightmap or None, spacing, separation, safe from monsters)
STRUCTURES = {
    "x_mansion": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest",
                   "minecraft:birch_forest", "minecraft:flower_forest"], "surface_structures",
                  -mansion.G, "WORLD_SURFACE_WG", 64, 32, True),
    "apocalypse_pyramid": (["minecraft:desert"], "surface_structures", -26, "WORLD_SURFACE_WG", 64, 32, False),
    "fallen_sentinel": (LAND, "surface_structures", -4, "WORLD_SURFACE_WG", 20, 8, False),
    "brotherhood_hideout": (FLAT + WOODS, "surface_structures", -3, "WORLD_SURFACE_WG", 34, 12, False),
    "asteroid_m": (ALL_OCEAN, "surface_structures", 170, None, 72, 36, False),
    "sentinel_factory": (FLAT, "surface_structures", -3, "WORLD_SURFACE_WG", 48, 20, False),
    "weapon_x": (["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:ice_spikes"],
                 "surface_structures", -weapon_x_lab.G, "WORLD_SURFACE_WG", 48, 20, False),
    "sinister_lab": ("#minecraft:is_overworld", "underground_structures", -30, None, 40, 16, False),
    "hellfire_club": (["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest",
                       "minecraft:birch_forest", "minecraft:dark_forest"], "surface_structures", -2,
                      "WORLD_SURFACE_WG", 40, 16, False),
    "genosha": (OCEAN, "surface_structures", 63 - 20, None, 56, 24, False),
    "krakoa": (WARM_OCEAN, "surface_structures", 63 - 20, None, 56, 24, True),
    "muir_island": (COLD_OCEAN, "surface_structures", 63 - 12, None, 40, 16, True),
    "morlock_tunnels": (FLAT + WOODS, "underground_structures", -22, "WORLD_SURFACE_WG", 32, 12, False),
    "blackbird_crash": (LAND, "surface_structures", -4, "WORLD_SURFACE_WG", 36, 14, False),
    "safehouse": (WOODS + ["minecraft:plains", "minecraft:meadow"], "surface_structures", -2, "WORLD_SURFACE_WG",
                  24, 10, True),
    "detection_tower": (FLAT + ["minecraft:forest", "minecraft:taiga"], "surface_structures", -2,
                        "WORLD_SURFACE_WG", 30, 12, False),
}

# Cerebro and the treasure maps find structures through these tags.
MAP_TAGS = {
    "map/asteroid_m": ["xmen:asteroid_m"],
    "map/x_mansion": ["xmen:x_mansion"],
    "map/apocalypse_pyramid": ["xmen:apocalypse_pyramid"],
    "map/weapon_x": ["xmen:weapon_x"],
    "cerebro": ["xmen:" + name for name in STRUCTURES],
}


# --------------------------------------------------------------------------- loot
def item(name, weight=1, lo=1, hi=1, *functions):
    if ":" not in name:
        name = "minecraft:" + name
    entry = {"type": "minecraft:item", "name": name, "weight": weight}
    fns = []
    if hi > 1 or lo != 1:
        fns.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
    fns.extend(functions)
    if fns:
        entry["functions"] = fns
    return entry


def potion(kind, weight=1):
    return item("potion", weight, 1, 1, {"function": "minecraft:set_potion", "id": "minecraft:" + kind})


def splash(kind, weight=1):
    return item("splash_potion", weight, 1, 1, {"function": "minecraft:set_potion", "id": "minecraft:" + kind})


def book(weight=1):
    return item("book", weight, 1, 1, {"function": "minecraft:enchant_randomly"})


def enchanted(name, weight=1, levels=20):
    return item(name, weight, 1, 1, {"function": "minecraft:enchant_with_levels",
                                     "levels": {"type": "minecraft:uniform", "min": levels // 2, "max": levels}})


def treasure_map(target, title, weight=1):
    return item("map", weight, 1, 1,
                {"function": "minecraft:exploration_map", "destination": "xmen:map/" + target,
                 "decoration": "minecraft:red_x", "zoom": 2, "skip_existing_chunks": False},
                {"function": "minecraft:set_name", "name": {"text": title}, "target": "item_name"})


SUIT = [item("xmen:xmen_mask", 2), item("xmen:xmen_suit", 2), item("xmen:xmen_pants", 2), item("xmen:xmen_boots", 2)]
ADAMANTIUM_GEAR = [item("xmen:adamantium_sword"), item("xmen:adamantium_pickaxe"), item("xmen:adamantium_helmet"),
                   item("xmen:adamantium_chestplate"), item("xmen:adamantium_leggings"),
                   item("xmen:adamantium_boots")]


def pool(lo, hi, entries):
    return {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": entries}


def chance(percent, entry):
    """A pool that gives the entry with the given chance."""
    return {"rolls": 1, "entries": [dict(entry, weight=percent), {"type": "minecraft:empty", "weight": 100 - percent}]}


LOOT = {
    "mansion": [pool(3, 6, [item("book", 10, 1, 3), item("bread", 10, 2, 5), item("cooked_beef", 8, 1, 4),
                            item("emerald", 6, 1, 4), item("iron_ingot", 6, 1, 4), item("paper", 6, 2, 8),
                            item("experience_bottle", 4, 1, 3), item("golden_apple", 2), book(3)] + SUIT)],
    "mansion_kitchen": [pool(4, 8, [item("bread", 10, 2, 6), item("cooked_beef", 8, 2, 5),
                                    item("cooked_chicken", 8, 2, 5), item("baked_potato", 8, 2, 6),
                                    item("pumpkin_pie", 5, 1, 3), item("cake", 2), item("cookie", 6, 4, 12),
                                    item("apple", 6, 2, 5), item("milk_bucket", 3)])],
    "mansion_dorm": [pool(2, 5, [item("book", 8), item("cookie", 8, 2, 8), item("emerald", 5, 1, 3),
                                 item("music_disc_cat", 1), item("music_disc_blocks", 1), item("candle", 5, 1, 3),
                                 item("writable_book", 4), item("name_tag", 2), item("compass", 2)] + SUIT)],
    "mansion_study": [pool(3, 6, [book(6), item("golden_apple", 3), item("emerald", 6, 3, 8),
                                  item("experience_bottle", 5, 2, 5), item("clock", 2), item("spyglass", 2),
                                  item("xmen:mutant_serum", 1)])],
    "mansion_lab": [pool(3, 6, [potion("healing", 10), potion("strong_healing", 4), potion("regeneration", 6),
                                splash("healing", 5), item("golden_apple", 3), item("glistering_melon_slice", 4, 1, 3),
                                item("golden_carrot", 5, 2, 5), item("honey_bottle", 4, 1, 3)])],
    "mansion_armory": [pool(3, 6, SUIT * 2 + [item("iron_sword", 3), item("crossbow", 2), item("arrow", 4, 8, 24),
                                              item("shield", 3), enchanted("diamond_sword", 1, 25),
                                              item("diamond", 2, 1, 2), item("spectral_arrow", 2, 4, 12)]),
                       chance(25, treasure_map("apocalypse_pyramid", "Map to Apocalypse's Pyramid")),
                       chance(20, treasure_map("weapon_x", "Map to the Weapon X Facility"))],
    "hangar": [pool(3, 6, [item("iron_ingot", 10, 2, 6), item("redstone", 10, 4, 12), item("gold_ingot", 6, 1, 4),
                           item("copper_ingot", 8, 2, 8), item("firework_rocket", 6, 3, 8),
                           item("xmen:sentinel_part", 2), item("elytra", 1), item("observer", 3),
                           item("piston", 4, 1, 3)])],
    "danger_room": [pool(2, 4, [item("iron_ingot", 10, 2, 6), item("gold_ingot", 8, 1, 4), item("emerald", 8, 2, 6),
                                item("diamond", 3, 1, 2), item("experience_bottle", 8, 2, 6),
                                item("golden_apple", 3), book(4)] + SUIT)],
    "blackbird": [pool(3, 6, [item("firework_rocket", 10, 4, 12), item("iron_ingot", 8, 2, 6),
                              item("redstone", 8, 4, 10), item("elytra", 2), item("compass", 4),
                              potion("healing", 4)] + SUIT)],
    "genosha": [pool(3, 6, [item("iron_ingot", 10, 1, 5), item("gold_ingot", 6, 1, 3), item("emerald", 6, 1, 4),
                            item("bread", 8, 1, 4), item("book", 6, 1, 3), item("xmen:sentinel_part", 4, 1, 2),
                            item("iron_block", 1), book(2)])],
    "krakoa": [pool(3, 6, [item("golden_apple", 4), potion("healing", 6), potion("regeneration", 4),
                           item("emerald", 6, 2, 6), item("melon_slice", 8, 3, 8), item("sweet_berries", 8, 4, 10),
                           item("cherry_sapling", 3), item("spore_blossom", 2), item("xmen:mutant_serum", 1)])],
    "morlock": [pool(2, 5, [item("rotten_flesh", 10, 2, 6), item("bread", 8, 1, 3), item("iron_nugget", 8, 3, 9),
                            item("string", 6, 2, 6), item("candle", 5, 1, 3), item("emerald", 3, 1, 2),
                            item("diamond", 1), item("leather_boots", 3)])],
    "asteroid_m": [pool(4, 8, [item("iron_block", 6, 1, 3), item("iron_ingot", 10, 4, 12), item("netherite_scrap", 3),
                               item("diamond", 5, 1, 3), item("ender_pearl", 5, 1, 3), item("gold_ingot", 6, 2, 6),
                               enchanted("iron_chestplate", 3, 30), item("xmen:magneto_helmet", 1)])],
    "brotherhood": [pool(3, 6, [item("iron_sword", 5), item("crossbow", 4), item("arrow", 8, 6, 16),
                                item("gold_ingot", 6, 1, 4), item("emerald", 6, 1, 4), item("tnt", 3, 1, 2),
                                item("fire_charge", 5, 1, 4), item("bread", 6, 2, 4)]),
                    chance(30, treasure_map("asteroid_m", "Map to Asteroid M"))],
    "sentinel_factory": [pool(4, 8, [item("iron_ingot", 10, 3, 9), item("redstone", 10, 6, 16),
                                     item("xmen:sentinel_part", 8, 1, 3), item("gold_ingot", 5, 1, 4),
                                     item("diamond", 2, 1, 2), item("comparator", 3), item("repeater", 4, 1, 3),
                                     item("iron_block", 2)])],
    "fallen_sentinel": [pool(2, 4, [item("iron_ingot", 10, 1, 5), item("redstone", 10, 3, 9),
                                    item("xmen:sentinel_part", 6, 1, 2), item("gold_nugget", 6, 2, 8),
                                    item("copper_ingot", 6, 1, 4)])],
    "weapon_x": [pool(3, 6, [item("iron_ingot", 8, 2, 6), item("xmen:adamantium_ingot", 4, 1, 2),
                             item("xmen:healing_factor_serum", 5), item("xmen:weapon_x_rifle", 3),
                             item("xmen:inhibitor_collar", 2), item("xmen:mutant_serum", 2),
                             item("bread", 6, 2, 5), item("cooked_porkchop", 6, 2, 5), potion("healing", 4)]
                  + SUIT)],
    "weapon_x_armory": [pool(3, 6, [item("xmen:weapon_x_rifle", 8), item("xmen:inhibitor_collar", 4),
                                    item("xmen:adamantium_ingot", 4, 1, 3), item("xmen:sentinel_part", 3, 1, 2),
                                    item("iron_chestplate", 3), item("shield", 3), item("tnt", 2, 1, 3)] + SUIT),
                        chance(10, item("xmen:adamantium_claws"))],
    "weapon_x_lab": [pool(3, 6, [item("xmen:healing_factor_serum", 10, 1, 2), item("xmen:mutant_serum", 4),
                                 item("xmen:inhibitor_collar", 3), item("xmen:adamantium_ingot", 4, 1, 2),
                                 potion("regeneration", 4), splash("slowness", 3), item("golden_apple", 3),
                                 item("experience_bottle", 5, 2, 5)])],
    "weapon_x_vault": [pool(3, 5, [item("xmen:adamantium_ingot", 1, 2, 6)]),
                       pool(1, 2, ADAMANTIUM_GEAR + [item("xmen:adamantium_claws", 2)]),
                       chance(40, item("xmen:healing_factor_serum", 1, 1, 3)),
                       chance(15, item("xmen:adamantium_skeleton"))],
    "sinister_lab": [pool(3, 6, [potion("healing", 5), potion("strength", 4), potion("swiftness", 4),
                                 item("ender_pearl", 6, 1, 3), item("emerald", 6, 2, 6), item("diamond", 4, 1, 3),
                                 item("fermented_spider_eye", 5, 1, 3), item("xmen:mutant_serum", 2)])],
    "hellfire": [pool(3, 6, [item("emerald", 10, 3, 10), item("gold_ingot", 8, 2, 6), item("diamond", 4, 1, 3),
                             book(6), item("clock", 3), item("golden_apple", 3), item("gold_block", 2),
                             item("cake", 2)])],
    "pyramid": [pool(4, 8, [item("gold_ingot", 10, 2, 8), item("diamond", 5, 1, 3), item("emerald", 8, 2, 6),
                            item("bone", 8, 2, 6), item("golden_apple", 3), item("enchanted_golden_apple", 1),
                            book(4), item("gold_block", 2)])],
    "pyramid_treasure": [pool(3, 5, [item("diamond", 6, 2, 5), item("gold_block", 6, 1, 3), item("emerald_block", 3),
                                     item("netherite_ingot", 1), item("enchanted_golden_apple", 2),
                                     enchanted("diamond_sword", 2, 30)]),
                         chance(50, item("xmen:celestial_orb"))],
    "safehouse": [pool(3, 6, [item("bread", 10, 2, 5), item("cooked_salmon", 8, 1, 4), item("torch", 8, 4, 12),
                              item("leather_chestplate", 3), item("map", 3), item("emerald", 3, 1, 2),
                              item("iron_ingot", 4, 1, 3), potion("healing", 3)])],
    "muir_island": [pool(3, 6, [item("book", 10, 1, 4), item("paper", 8, 2, 8), potion("healing", 5),
                                potion("water_breathing", 4), book(4), item("cooked_cod", 6, 2, 5),
                                item("emerald", 4, 1, 3)]),
                    chance(60, treasure_map("x_mansion", "Map to the X-Mansion"))],
    "detection_tower": [pool(2, 5, [item("arrow", 10, 6, 16), item("crossbow", 4), item("iron_ingot", 6, 1, 4),
                                    item("bread", 8, 2, 4), item("spyglass", 3), item("redstone", 6, 2, 8)])],
}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    structure_dir = os.path.join(DATA, "structure")
    os.makedirs(structure_dir, exist_ok=True)

    templates = [mansion.build()] + [make() for make in places.ALL]
    names = {t.name for t in templates}
    assert names == set(STRUCTURES), names ^ set(STRUCTURES)
    used_loot = set()
    for t in templates:
        path, blocks, palette, mobs = t.save(structure_dir)
        size = os.path.getsize(path) // 1024
        print(f"{t.name:22s} {t.sx}x{t.sy}x{t.sz}  {blocks:7d} blocks  {palette:3d} kinds  {mobs:2d} mobs  {size} KB")
        root = nbt.read(path)
        assert root["size"] == [t.sx, t.sy, t.sz]
        for (state, data) in t.blocks.values():
            if data and "LootTable" in data:
                used_loot.add(data["LootTable"].split("/")[-1])

    # an empty arena for the game tests that check the structures
    Template("empty", 3, 3, 3).save(structure_dir)

    missing = used_loot - set(LOOT)
    assert not missing, f"chests use loot tables that don't exist: {missing}"

    for name, (biomes, step, height, heightmap, spacing, separation, safe) in STRUCTURES.items():
        tag = biomes if isinstance(biomes, str) else "#xmen:has_structure/" + name
        if not isinstance(biomes, str):
            write_json(os.path.join(DATA, "tags", "worldgen", "biome", "has_structure", name + ".json"),
                       {"values": [{"id": b, "required": False} for b in biomes]})
        structure = {
            "type": "minecraft:jigsaw",
            "biomes": tag,
            "step": step,
            "spawn_overrides": NO_MONSTERS if safe else {},
            "terrain_adaptation": "none",
            "start_pool": f"xmen:{name}/start",
            "size": 1,
            "start_height": {"absolute": height},
            "max_distance_from_center": 80,
            "use_expansion_hack": False,
        }
        if heightmap:
            structure["project_start_to_heightmap"] = heightmap
        write_json(os.path.join(DATA, "worldgen", "structure", name + ".json"), structure)
        write_json(os.path.join(DATA, "worldgen", "template_pool", name, "start.json"), {
            "fallback": "minecraft:empty",
            "elements": [{"weight": 1, "element": {"element_type": "minecraft:single_pool_element",
                                                   "location": "xmen:" + name, "projection": "rigid",
                                                   "processors": "minecraft:empty"}}],
        })
        salt = 1963_0000 + sum(ord(c) * (i + 1) for i, c in enumerate(name)) % 9999
        write_json(os.path.join(DATA, "worldgen", "structure_set", name + ".json"), {
            "structures": [{"structure": "xmen:" + name, "weight": 1}],
            "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation,
                          "salt": salt},
        })

    for tag, values in MAP_TAGS.items():
        write_json(os.path.join(DATA, "tags", "worldgen", "structure", tag + ".json"), {"values": values})

    for name, pools in LOOT.items():
        write_json(os.path.join(DATA, "loot_table", "chests", name + ".json"),
                   {"type": "minecraft:chest", "pools": pools})
    print(f"{len(STRUCTURES)} structures, {len(LOOT)} loot tables written to {DATA}")


if __name__ == "__main__":
    main()
