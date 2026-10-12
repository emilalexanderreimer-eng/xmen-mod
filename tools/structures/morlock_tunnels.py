"""The Morlock Tunnels: the hidden mutant town under the city.

From the surface, an old boarded-up subway entrance (green globe lamps, "SUBWAY" sign) leads down to an
abandoned station with white tiles, a wrecked graffiti train and the Morlocks' camp on the platform. From the
platform, a working minecart line (press the button) runs through a tunnel and down a long slope to the Alley:
a huge cave where the Morlocks live, with shacks, a market, a sewer canal and a waterfall from a broken pipe.
In the Alley:
  - Healer (right-click) heals you,
  - Caliban (right-click) senses your power and the nearest mutant place,
  - Callisto, the Morlocks' leader, waits in her fighting pit. Right-click her twice to challenge her; beat her
    and the iron door behind the pit opens to the Morlock hoard.
An old manhole with a ladder is the second way in and out.

Coordinates: x east, y up, z south. y G is the surface.
"""
import math

import nbt
from lib import Template, AIR

N = 80
G = 40           # surface
S = 24           # station floor (pit/track bed); platforms at S+1, people stand at S+2
A = 6            # Alley floor; people stand at A+1
SY = G + 7

ROCK = ["stone"] * 6 + ["andesite", "tuff", "cobblestone", "stone", "diorite"]
TILE = ["white_concrete"] * 6 + ["white_terracotta", "light_gray_concrete", "calcite"]
SCRAP = ["spruce_planks", "spruce_planks", "stripped_spruce_log[axis=y]", "weathered_cut_copper",
         "oxidized_cut_copper", "exposed_cut_copper", "mud_bricks", "iron_block", "dark_oak_planks"]
GRAFFITI = ["lime_concrete", "magenta_concrete", "yellow_concrete", "orange_concrete", "cyan_concrete",
            "red_concrete", "light_blue_concrete"]

# The Alley cave
AX, AZ, ARX, ARZ = 46, 56, 30.0, 21.0
# Callisto's pit
PX, PZ, PR = 64, 62, 6


def _sign(text1, text2="", text3="", text4="", glow=False, color="black"):
    lines = [f'"{t}"' for t in (text1, text2, text3, text4)]
    side = {"messages": lines, "color": color, "has_glowing_text": nbt.Byte(1 if glow else 0)}
    return {"front_text": side, "back_text": {"messages": ['""'] * 4, "color": "black",
                                              "has_glowing_text": nbt.Byte(0)},
            "is_waxed": nbt.Byte(1)}


def build():
    t = Template("morlock_tunnels", N, SY, N, seed=13)
    r = t.rand

    def rock(x1, y1, z1, x2, y2, z2):
        """Solid ground around a space, only where nothing was built yet (so caves and lava can't leak in)."""
        for x in range(max(0, min(x1, x2)), min(N - 1, max(x1, x2)) + 1):
            for y in range(max(0, min(y1, y2)), min(SY - 1, max(y1, y2)) + 1):
                for z in range(max(0, min(z1, z2)), min(N - 1, max(z1, z2)) + 1):
                    if t.get(x, y, z) is None:
                        t.set(x, y, z, "deepslate" if y < 4 and r.random() < 0.5 else r.choice(ROCK))

    def alley_d(x, z):
        return ((x - AX) / ARX) ** 2 + ((z - AZ) / ARZ) ** 2

    def ceiling(x, z):
        d = alley_d(x, z)
        return int(A + 5 + 10 * max(0.0, 1 - d) + math.sin(x * 0.5) * 0.8 + math.cos(z * 0.45) * 0.8)

    # ================================================================== shells first
    rock(4, S - 2, 6, 72, S + 11, 30)                      # station hall
    rock(71, S - 1, 20, 77, G - 1, 48)                     # stairs down from the street
    rock(0, A - 2, 16, 6, S + 6, 60)                       # minecart tunnel
    rock(0, A - 2, 52, 18, A + 6, 60)
    for x in range(N):                                     # the Alley
        for z in range(N):
            d = alley_d(x, z) + math.sin(x * 0.7 + z * 0.3) * 0.04
            if d <= 1.18:
                rock(x, 0, z, x, min(ceiling(x, z) + 2, S - 2), z)

    # ================================================================== the street: old subway entrance
    t.fill(69, G, 39, 79, G, 51, "smooth_stone")
    t.mix(69, G, 39, 79, G, 51, ["smooth_stone", "smooth_stone", "stone", "cracked_stone_bricks", "andesite"])
    t.fill(69, G - 3, 39, 79, G - 1, 51, "stone")
    t.fill(70, G, 40, 78, G, 40, "gray_concrete")
    # the stairwell: 15 steps down to the north
    for k in range(15):
        z = 46 - k
        y = G - k
        roofed = y + 5 < G
        t.clear(73, y, z, 75, y + 5 if roofed else G, z)
        t.fill(73, y, z, 75, y, z, "stone_brick_stairs[facing=south,half=bottom,shape=straight]")
        t.fill(72, y - 1, z, 76, y - 1, z, "stone_bricks")
        top = y + 5 if roofed else G
        for x in (72, 76):
            for yy in range(y, top + 1):
                t.set(x, yy, z, r.choice(TILE) if yy < G else "stone_bricks")
        if roofed:
            t.fill(72, y + 5, z, 76, y + 5, z, "stone_bricks")
    t.fill(73, G - 1, 47, 75, G - 1, 47, "stone_bricks")
    for z in range(41, 48):                                # green railings
        for x in (72, 76):
            t.set(x, G + 1, z, "iron_bars")
    for x in range(72, 77):
        t.set(x, G + 1, 41, "iron_bars")
    for x in (72, 76):                                     # the green globe lamps
        t.fill(x, G + 1, 47, x, G + 3, 47, "green_terracotta")
        t.set(x, G + 4, 47, "verdant_froglight")
    t.fill(73, G + 2, 41, 75, G + 3, 41, "green_concrete")
    t.set(74, G + 3, 42, "oak_wall_sign[facing=south]", _sign("SUBWAY", "", "STATION CLOSED", "keep out"))

    # landing at the bottom with the old turnstiles
    t.fill(72, S, 21, 76, S + 1, 31, "polished_andesite")
    t.clear(73, S + 2, 22, 75, S + 6, 31)
    t.fill(72, S + 7, 21, 76, S + 7, 31, "stone_bricks")
    for z in range(21, 32):
        t.fill(72, S + 2, z, 72, S + 6, z, r.choice(TILE))
        t.fill(76, S + 2, z, 76, S + 6, z, r.choice(TILE))
    t.fill(72, S + 2, 21, 76, S + 6, 21, "white_concrete")
    t.set(73, S + 2, 28, "iron_bars")
    t.set(75, S + 2, 28, "iron_bars")
    t.set(74, S + 6, 30, "chain[axis=y]")
    t.set(74, S + 5, 30, "lantern[hanging=true]")

    # ================================================================== the abandoned station
    x1, x2, z1, z2 = 6, 70, 8, 28
    for x in range(x1, x2 + 1):
        for z in range(z1, z2 + 1):
            for y in range(S, S + 10):
                edge = x in (x1, x2) or z in (z1, z2) or y in (S, S + 9)
                if edge:
                    t.set(x, y, z, r.choice(TILE) if y not in (S, S + 9) else "stone_bricks")
                else:
                    t.set(x, y, z, AIR)
    # mosaic band and name tablets on the long walls
    for x in range(x1 + 1, x2):
        for z in (z1, z2):
            t.set(x, S + 7, z, "green_terracotta" if x % 4 else "brown_terracotta")
    for x in range(12, 66, 13):
        for z, facing in ((z1, "south"), (z2, "north")):
            t.fill(x - 1, S + 4, z, x + 1, S + 5, z, "terracotta")
            t.set(x, S + 5, z + (1 if z == z1 else -1), f"oak_wall_sign[facing={facing}]",
                  _sign("", "BOWERY", "", "", glow=True, color="white"))
    # graffiti on the walls (the X of the X-Men, and Morlock scribbles)
    t.scatter(x1, S + 1, z1, x2, S + 6, z2, "magenta_concrete", 0.015, lambda s: s and "white" in s)
    t.scatter(x1, S + 1, z1, x2, S + 6, z2, "lime_concrete", 0.015, lambda s: s and "white" in s)
    for i in range(5):     # a big red X sprayed on the south wall
        t.set(30 + i, S + 2 + i, z2, "red_concrete")
        t.set(34 - i, S + 2 + i, z2, "red_concrete")
    # platforms (people stand at S+2), with yellow edges
    t.fill(x1 + 1, S + 1, 9, x2 - 1, S + 1, 14, "polished_andesite")
    t.fill(x1 + 1, S + 1, 21, x2 - 1, S + 1, 27, "polished_andesite")
    t.fill(64, S + 1, 15, x2 - 1, S + 1, 20, "polished_andesite")      # walkway across at the east end
    t.mix(x1 + 1, S + 1, 9, x2 - 1, S + 1, 27, ["polished_andesite", "polished_andesite", "andesite",
                                               "cracked_stone_bricks"])
    t.fill(x1 + 1, S, 15, 63, S, 20, "gravel")
    t.clear(x1 + 1, S + 1, 15, 63, S + 1, 20)
    t.fill(x1 + 1, S + 1, 14, 63, S + 1, 14, "yellow_concrete")
    t.fill(x1 + 1, S + 1, 21, 63, S + 1, 21, "yellow_concrete")
    # green steel columns
    for x in range(10, 66, 6):
        for z in (11, 24):
            t.fill(x, S + 2, z, x, S + 8, z, "oxidized_cut_copper")
            t.set(x, S + 8, z, "oxidized_cut_copper")
    # lights: most of the old ones are dead, the Morlocks hung lanterns
    for x in range(9, 68, 8):
        for z in (12, 24):
            t.set(x, S + 9, z, "sea_lantern" if r.random() < 0.3 else "redstone_lamp[lit=false]")
    for x in range(13, 66, 8):
        for z in (10, 26):
            t.set(x, S + 8, z, "chain[axis=y]")
            t.set(x, S + 7, z, "lantern[hanging=true]")
    # rails on track 1 (where the wreck stands)
    for x in range(x1 + 1, 64):
        t.set(x, S + 1, 16, "rail[shape=east_west]")
    t.fill(64, S + 2, 16, 64, S + 2, 16, "yellow_concrete")
    # the wrecked subway train on track 1
    c1, c2 = 20, 42
    t.fill(c1, S + 1, 15, c2, S + 1, 17, "gray_concrete")
    for x in range(c1, c2 + 1):
        for z in (15, 17):
            for y in range(S + 2, S + 5):
                window = y == S + 3 and x % 3 != 0 and x not in (c1, c2)
                t.set(x, y, z, "gray_stained_glass_pane" if window else
                      ("light_gray_concrete" if r.random() > 0.25 else r.choice(GRAFFITI)))
        t.clear(x, S + 2, 16, x, S + 4, 16)
        t.set(x, S + 5, 15, "smooth_stone_slab[type=bottom]")
        t.set(x, S + 5, 17, "smooth_stone_slab[type=bottom]")
        t.set(x, S + 5, 16, "smooth_stone")
    for x in (c1, c2):
        t.fill(x, S + 2, 16, x, S + 4, 16, "light_gray_concrete")
        t.set(x, S + 3, 16, "black_stained_glass")
    for x in (25, 31, 37):          # doors to the north platform
        t.clear(x, S + 2, 15, x + 1, S + 3, 15)
    t.ruin(c1, S + 5, 15, c2, S + 5, 17, 0.15)
    t.barrel(c1 + 1, S + 2, 16, "morlock")
    t.set(c1 + 7, S + 2, 16, "brown_carpet")
    t.set(c1 + 14, S + 2, 16, "candle[candles=2,lit=true]")
    # a second car that came off the rails and crashed into the platform
    for x in range(46, 58):
        z0 = 16 - (x - 46) // 4
        t.fill(x, S + 1, z0 - 1, x, S + 4, z0 + 1, "light_gray_concrete")
        t.set(x, S + 3, z0 - 1, "gray_stained_glass_pane" if x % 3 else "light_gray_concrete")
    t.ruin(46, S + 2, 12, 58, S + 4, 17, 0.2)

    # the Morlocks' camp on the south platform
    for x, z in ((14, 26), (17, 26)):
        t.bed(x, S + 2, z - 1, "south", "brown")
    t.set(22, S + 2, 25, "campfire[lit=true,signal_fire=false,facing=north]")
    t.fill(20, S + 2, 23, 20, S + 2, 23, "stripped_spruce_log[axis=x]")
    t.fill(24, S + 2, 23, 24, S + 2, 23, "stripped_spruce_log[axis=x]")
    t.chest(12, S + 2, 26, "north", "morlock")
    t.barrel(56, S + 2, 26, "morlock")
    t.set(57, S + 2, 26, "barrel[facing=up]")
    t.set(56, S + 3, 26, "barrel[facing=up]")
    t.scatter(x1 + 1, S + 2, 9, x2 - 1, S + 2, 27, "cobweb", 0.004, lambda s: s == "minecraft:air")
    t.scatter(x1 + 1, S + 2, 9, x2 - 1, S + 2, 27, "moss_carpet", 0.02, lambda s: s == "minecraft:air")
    # the ticket booth at the east end
    t.fill(65, S + 2, 9, 68, S + 4, 12, "stripped_dark_oak_log[axis=y]")
    t.fill(65, S + 3, 12, 68, S + 3, 12, "glass_pane")
    t.clear(66, S + 2, 10, 67, S + 4, 11)
    t.set(66, S + 4, 13, "oak_wall_sign[facing=south]", _sign("TOKENS", "", "the Alley", "ride the cart west"))
    t.fill(65, S + 5, 9, 68, S + 5, 12, "dark_oak_slab[type=bottom]")
    # the way in from the stairs
    t.clear(69, S + 2, 22, 73, S + 5, 26)
    t.fill(69, S + 1, 22, 73, S + 1, 26, "polished_andesite")

    # ================================================================== the minecart line to the Alley
    def powered(x, y, z, shape, always=True):
        t.set(x, y, z, f"powered_rail[shape={shape},powered={'true' if always else 'false'}]")
        if always:
            t.set(x, y - 1, z, "redstone_block")

    # station end: buffer stop with a button (sit in the cart and press it)
    for x in range(4, 64):
        t.set(x, S + 1, 19, "rail[shape=east_west]")
    powered(63, S + 1, 19, "east_west", always=False)
    t.fill(64, S + 2, 19, 64, S + 3, 19, "yellow_concrete")
    t.set(63, S + 2, 19, "stone_button[face=wall,facing=west,powered=false]")
    t.set(63, S + 3, 19, "oak_wall_sign[facing=west]", _sign("THE ALLEY", "sit in the cart", "and press",
                                                             "the button"))
    for x in (55, 47, 39, 31, 23, 15, 9):
        powered(x, S + 1, 19, "east_west")
    # tunnel west out of the station, then south and down
    t.clear(1, S + 1, 18, 6, S + 4, 20)
    t.clear(2, S + 1, 18, 4, S + 4, 23)
    t.fill(1, S, 18, 6, S, 22, "stone_bricks")
    for x in range(4, 7):
        t.set(x, S + 1, 19, "rail[shape=east_west]")
    t.set(3, S + 1, 19, "rail[shape=south_east]")
    t.set(3, S + 1, 20, "rail[shape=north_south]")
    t.set(3, S + 1, 21, "rail[shape=north_south]")
    for z in range(22, 40):
        y = S + 1 - (z - 21)
        t.fill(1, y - 1, z, 5, y + 5, z, "stone_bricks")
        t.mix(1, y - 1, z, 5, y + 5, z, ["stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks"])
        t.clear(2, y, z, 4, y + 3, z)
        t.clear(2, y + 1, z - 1, 4, y + 4, z - 1)
        powered(3, y, z, "ascending_north")
        if z % 5 == 0:
            t.set(2, y + 3, z, "lantern[hanging=false]")
    for z in range(40, 57):
        t.fill(1, A, z, 5, A + 5, z, "stone_bricks")
        t.mix(1, A, z, 5, A + 5, z, ["stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks"])
        t.clear(2, A + 1, z, 4, A + 4, z)
        t.set(3, A + 1, z, "rail[shape=north_south]")
    for z in (44, 50):
        powered(3, A + 1, z, "north_south")
    t.set(3, A + 1, 56, "rail[shape=north_east]")
    for x in range(4, 16):
        t.fill(x, A, 54, x, A + 5, 58, "stone_bricks")
        t.mix(x, A, 54, x, A + 5, 58, ["stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks"])
        t.clear(x, A + 1, 55, x, A + 4, 57)
        t.set(x, A + 1, 56, "rail[shape=east_west]")
    t.clear(2, A + 1, 55, 4, A + 4, 57)
    t.set(3, A + 1, 56, "rail[shape=north_east]")
    powered(9, A + 1, 56, "east_west")

    # ================================================================== the Alley
    for x in range(N):
        for z in range(N):
            d = alley_d(x, z) + math.sin(x * 0.7 + z * 0.3) * 0.04
            if d > 1.0:
                continue
            top = ceiling(x, z)
            t.set(x, A, z, r.choice(["packed_mud", "stone", "gravel", "coarse_dirt", "mud_bricks", "stone"]))
            t.set(x, A - 1, z, "stone")
            t.clear(x, A + 1, z, x, top, z)
            t.set(x, top + 1, z, r.choice(ROCK))
            roll = r.random()
            if roll < 0.035:
                t.set(x, top + 1, z, r.choice(["shroomlight", "ochre_froglight", "verdant_froglight"]))
            elif roll < 0.09 and top > A + 6:
                t.set(x, top, z, "pointed_dripstone[thickness=tip,vertical_direction=down]")
                t.set(x, top + 1, z, "dripstone_block")
    # floor details
    t.scatter(16, A, 35, 76, A, 77, "podzol", 0.05, lambda s: s in ("minecraft:packed_mud", "minecraft:coarse_dirt"))
    for x in range(16, 77):
        for z in range(35, 78):
            if t.get(x, A, z) == "minecraft:podzol" and t.get(x, A + 1, z) == "minecraft:air":
                t.set(x, A + 1, z, r.choice(["brown_mushroom", "red_mushroom", "moss_carpet"]))
    # the arrival deck where the minecart stops, with its buffer and button
    t.clear(15, A + 1, 53, 21, A + 4, 59)
    t.fill(16, A, 53, 21, A, 59, "spruce_planks")
    t.fill(15, A + 1, 56, 15, A + 3, 56, "yellow_concrete")
    t.set(14, A + 2, 56, "stone_button[face=wall,facing=west,powered=false]")
    powered(14, A + 1, 56, "east_west", always=False)
    t.set(14, A + 3, 56, "oak_wall_sign[facing=west]", _sign("THE STATION", "sit in the cart", "and press",
                                                             "the button"))
    for x, z in ((17, 53), (17, 59), (21, 53), (21, 59)):
        t.lamp_post(x, A + 1, z, 2)

    # the sewer canal, fed by a waterfall from a broken pipe in the ceiling
    for x in range(22, 72):
        for z in range(48, 51):
            if alley_d(x, z) < 0.9:
                t.set(x, A - 1, z, "stone_bricks")
                t.set(x, A, z, "water")
                t.clear(x, A + 1, z, x, A + 1, z)
        for z in (47, 51):
            if alley_d(x, z) < 0.9:
                t.set(x, A, z, "stone_bricks" if r.random() > 0.2 else "mossy_stone_bricks")
    for x in range(38, 43):
        for z in range(46, 53):
            t.set(x, A - 1, z, "stone_bricks")
            t.set(x, A, z, "water")
    for x in range(37, 44):
        for z in (45, 53):
            t.set(x, A, z, "mossy_stone_bricks")
    for z in range(45, 54):
        t.set(37, A, z, "mossy_stone_bricks")
        t.set(43, A, z, "mossy_stone_bricks")
    # the pipe: a rusty copper tube out of the ceiling, still full of water
    for y in range(A + 11, A + 17):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                t.set(40 + dx, y, 49 + dz, "weathered_cut_copper" if (dx or dz) else "water")
    t.set(40, A + 10, 49, "water[level=8]")
    for y in range(A + 1, A + 10):
        t.set(40, y, 49, "water[level=8]")
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        t.set(40 + dx, A + 10, 49 + dz, "oxidized_cut_copper_slab[type=top]")
    # bridges
    for bx in (28, 50, 62):
        t.fill(bx - 1, A + 1, 47, bx + 1, A + 1, 51, "spruce_slab[type=bottom]")
        t.set(bx - 2, A + 1, 47, "spruce_fence")
        t.set(bx + 2, A + 1, 51, "spruce_fence")

    # shacks along the north side of the canal
    def shack(cx, cz, door="south", wool="brown_wool"):
        t.fill(cx - 2, A, cz - 2, cx + 2, A, cz + 2, "spruce_planks")
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if x in (cx - 2, cx + 2) or z in (cz - 2, cz + 2):
                    t.fill(x, A + 1, z, x, A + 3, z, r.choice(SCRAP))
                else:
                    t.clear(x, A + 1, z, x, A + 3, z)
        t.fill(cx - 3, A + 4, cz - 3, cx + 3, A + 4, cz + 3, wool)
        t.fill(cx - 2, A + 4, cz - 2, cx + 2, A + 4, cz + 2, r.choice(["spruce_planks", "weathered_cut_copper"]))
        dz = 2 if door == "south" else -2
        t.clear(cx, A + 1, cz + dz, cx, A + 2, cz + dz)
        t.set(cx - 2, A + 2, cz, "glass_pane")
        t.set(cx + 2, A + 2, cz, "glass_pane")
        t.bed(cx - 1, A + 1, cz - 1, "east", r.choice(["brown", "gray", "red", "green"]))
        t.barrel(cx + 1, A + 1, cz + 1, "morlock")
        t.set(cx + 1, A + 1, cz - 1, "candle[candles=3,lit=true]")

    for cx, wool in ((31, "red_wool"), (39, "blue_wool"), (53, "brown_wool"), (61, "green_wool")):
        shack(cx, 42, "south", wool)
    shack(24, 66, "north", "gray_wool")
    shack(46, 74, "north", "orange_wool")

    # the market and the fire in the middle of the town
    for x, z, wool in ((36, 57, "red_wool"), (44, 57, "yellow_wool"), (52, 57, "cyan_wool"),
                       (36, 68, "purple_wool"), (52, 68, "lime_wool")):
        for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            t.fill(x + dx, A + 1, z + dz, x + dx, A + 2, z + dz, "spruce_fence")
        t.fill(x - 1, A + 3, z - 1, x + 1, A + 3, z + 1, wool)
        t.set(x, A + 1, z, "barrel[facing=up]")
        t.barrel(x - 1, A + 1, z, "morlock")
    for dx, dz in ((0, 0), (1, 0), (0, 1), (1, 1)):
        t.set(43 + dx, A + 1, 62 + dz, "campfire[lit=true,signal_fire=false,facing=north]")
    for x, z, axis in ((41, 62, "z"), (41, 63, "z"), (46, 62, "z"), (46, 63, "z"), (43, 60, "x"), (44, 60, "x"),
                       (43, 65, "x"), (44, 65, "x")):
        t.set(x, A + 1, z, f"stripped_spruce_log[axis={axis}]")
    # strings of lanterns hanging from the ceiling
    for x, z in ((30, 46), (40, 44), (52, 46), (60, 46), (36, 62), (52, 62), (40, 70), (54, 72), (28, 56),
                 (60, 54), (24, 62), (46, 52)):
        top = ceiling(x, z)
        if top > A + 8:
            t.fill(x, A + 7, z, x, top, z, "chain[axis=y]")
            t.set(x, A + 6, z, "lantern[hanging=true]")

    # Healer's hut (west): white beds and potions
    hx, hz = 26, 58
    t.fill(hx - 3, A, hz - 3, hx + 3, A, hz + 3, "white_terracotta")
    for x in range(hx - 3, hx + 4):
        for z in range(hz - 3, hz + 4):
            if x in (hx - 3, hx + 3) or z in (hz - 3, hz + 3):
                t.fill(x, A + 1, z, x, A + 4, z, r.choice(["white_concrete", "white_terracotta", "birch_planks"]))
            else:
                t.clear(x, A + 1, z, x, A + 4, z)
    t.fill(hx - 4, A + 5, hz - 4, hx + 4, A + 5, hz + 4, "white_wool")
    t.clear(hx + 3, A + 1, hz, hx + 3, A + 2, hz)
    t.set(hx + 4, A + 3, hz + 1, "oak_wall_sign[facing=east]", _sign("HEALER", "right-click him", "to be healed"))
    t.bed(hx - 2, A + 1, hz - 2, "east", "white")
    t.bed(hx - 2, A + 1, hz + 2, "east", "white")
    t.set(hx + 2, A + 1, hz - 2, "brewing_stand")
    t.chest(hx + 2, A + 1, hz + 2, "west", "morlock_healer")
    t.set(hx, A + 4, hz, "lantern[hanging=true]")
    t.set(hx - 2, A + 1, hz, "green_carpet")

    # Caliban's den (south-west): dark, candles everywhere
    kx, kz = 34, 72
    t.fill(kx - 3, A, kz - 2, kx + 3, A, kz + 2, "black_wool")
    for x in range(kx - 3, kx + 4):
        for z in range(kz - 2, kz + 3):
            if x in (kx - 3, kx + 3) or z in (kz - 2, kz + 2):
                t.fill(x, A + 1, z, x, A + 3, z, r.choice(["blackstone", "polished_blackstone_bricks", "basalt"]))
            else:
                t.clear(x, A + 1, z, x, A + 3, z)
    t.fill(kx - 3, A + 4, kz - 2, kx + 3, A + 4, kz + 2, "blackstone")
    t.clear(kx, A + 1, kz - 2, kx, A + 2, kz - 2)
    t.set(kx + 1, A + 3, kz - 3, "oak_wall_sign[facing=north]", _sign("CALIBAN", "he feels", "every mutant"))
    for x, z in ((kx - 2, kz - 1), (kx + 2, kz - 1), (kx - 2, kz + 1), (kx + 2, kz + 1), (kx, kz + 1)):
        t.set(x, A + 1, z, "candle[candles=4,lit=true]")

    # ================================================================== Callisto's fighting pit
    for x in range(PX - PR - 2, PX + PR + 3):
        for z in range(PZ - PR - 2, PZ + PR + 3):
            d = math.hypot(x - PX, z - PZ)
            if d <= PR - 0.5:
                t.set(x, A - 3, z, r.choice(["packed_mud", "mud_bricks", "coarse_dirt"]))
                t.clear(x, A - 2, z, x, A, z)
                t.set(x, A - 4, z, "stone")
            elif d <= PR + 0.5:
                t.fill(x, A - 3, z, x, A, z, r.choice(["stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks"]))
                t.set(x, A + 1, z, "iron_bars")
    for i in range(3):                                                 # steps down on the west side
        x = PX - PR + i
        t.clear(x, A - 2, PZ, x, A + 1, PZ)
        t.fill(x, A - 3, PZ, x, A - 1 - i, PZ, "stone_bricks")
        t.set(x, A - i, PZ, "stone_brick_stairs[facing=west,half=bottom,shape=straight]")
    # her throne of junk on the south side
    t.set(PX, A - 2, PZ + PR - 1, "polished_blackstone_stairs[facing=south,half=bottom,shape=straight]")
    t.set(PX - 1, A - 2, PZ + PR - 1, "polished_blackstone_slab[type=bottom]")
    t.set(PX + 1, A - 2, PZ + PR - 1, "polished_blackstone_slab[type=bottom]")
    t.fill(PX - 1, A - 2, PZ + PR, PX + 1, A + 2, PZ + PR, "polished_blackstone")
    t.set(PX, A + 3, PZ + PR, "skeleton_skull[rotation=8]")
    t.set(PX, A - 2, PZ + PR - 2, "red_carpet")
    for x, z in ((PX - 4, PZ + 3), (PX + 4, PZ + 3), (PX - 4, PZ - 3), (PX + 4, PZ - 3)):
        t.set(x, A - 2, z, "campfire[lit=true,signal_fire=false,facing=north]")
    # the Morlock hoard behind an iron door on the east side (it opens when Callisto is beaten)
    vx = PX + PR
    t.fill(vx, A - 3, PZ - 2, vx + 4, A, PZ + 2, "stone_bricks")
    t.clear(vx + 1, A - 2, PZ - 1, vx + 3, A - 1, PZ + 1)
    t.fill(vx + 1, A - 3, PZ - 1, vx + 3, A - 3, PZ + 1, "gold_block")
    t.door(vx, A - 2, PZ, "west", wood="iron")
    t.chest(vx + 3, A - 2, PZ - 1, "west", "morlock_hoard")
    t.chest(vx + 3, A - 2, PZ + 1, "west", "morlock_hoard")
    t.set(vx + 3, A - 2, PZ, "gold_block")
    t.set(vx - 1, A, PZ, "oak_wall_sign[facing=west]", _sign("THE HOARD", "beat Callisto", "and this door", "opens"))
    # banner-like red and black cloth around the pit
    for x, z in ((PX - 3, PZ - PR - 2), (PX + 3, PZ - PR - 2), (PX, PZ - PR - 2)):
        t.fill(x, A + 1, z, x, A + 3, z, "spruce_fence")
        t.fill(x, A + 4, z, x, A + 5, z, "red_wool")

    # ================================================================== the old manhole (second way out)
    mx, mz = 50, 74
    top = ceiling(mx, mz)
    t.fill(mx - 1, top + 1, mz - 1, mx + 1, G, mz + 1, "stone_bricks")
    t.clear(mx, top + 1, mz, mx, G, mz)
    t.fill(mx, A + 1, mz + 1, mx, top, mz + 1, "stone_bricks")
    t.clear(mx, A + 1, mz, mx, top, mz)
    for y in range(A + 1, G):
        t.set(mx, y, mz, "ladder[facing=north]")
    t.set(mx, G, mz, "spruce_trapdoor[facing=north,half=top,open=false]")
    t.fill(mx - 1, G, mz - 1, mx + 1, G, mz - 1, "polished_andesite")
    t.set(mx - 1, G, mz, "polished_andesite")
    t.set(mx + 1, G, mz, "polished_andesite")
    t.fill(mx - 1, G, mz + 1, mx + 1, G, mz + 1, "polished_andesite")

    # ================================================================== the people
    t.npc(PX, A - 2, PZ + 3, "callisto", yaw=180.0)
    t.npc(hx - 1, A + 1, hz, "healer", yaw=90.0)
    t.npc(kx, A + 1, kz, "caliban", yaw=180.0)
    t.npc(36, A + 1, 59, "masque")
    t.npc(45, A + 1, 66, "leech")
    t.npc(50, S + 2, 24, "marrow")
    t.npc(30, S + 2, 12, "morlock")
    t.npc(19, A + 1, 61, "morlock")
    for x, z in ((33, 46), (55, 46), (48, 60), (54, 70), (58, 55), (40, 66)):
        t.npc(x, A + 1, z, "morlock")
    # a minecart waiting at each end of the line
    for x, y, z in ((63, S + 1, 19), (14, A + 1, 56)):
        t.entities.append({"pos": [nbt.Double(x + 0.5), nbt.Double(y + 0.0625), nbt.Double(z + 0.5)],
                           "blockPos": [x, y, z],
                           "nbt": {"id": "minecraft:minecart",
                                   "Pos": [nbt.Double(x + 0.5), nbt.Double(y + 0.0625), nbt.Double(z + 0.5)],
                                   "Rotation": [nbt.Float(0.0), nbt.Float(0.0)]}})
    return t
