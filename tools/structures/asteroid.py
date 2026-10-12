"""Asteroid M, Magneto's floating sanctuary: a big hollowed-out rock high above the ocean.

  Surface      the citadel (Magneto's boss arena under a purple glass dome), a landing pad, an observatory, a field
               of magnetic antennas, a mutant garden, craters full of raw iron
  Level 1      a ring corridor around the machine chamber, the Brotherhood's quarters, the hangar (open to the sky,
               with a parked shuttle), the plastic prison from X2 and Magneto's study with the chess board from X1
  Level 2      the floor of the machine chamber (Magneto's mutation machine from X1, with Rogue trapped inside),
               the magnetic core, a vault and an amethyst geode
  Around it    small floating rocks, some with treasure

Stairs: from the citadel floor down to level 1 (south side of the hall), and inside the machine chamber from the
balcony down to level 2. Magneto himself is Java code (entity/MagnetoBoss), placed over his dais in the citadel.
"""
import math

from lib import Template, AIR

N = 95          # the template is N x N
H = 72          # and H high
C = N // 2      # center
T = 34          # top of the rock: the surface floor (people walk at T + 1)
L1 = 22         # floor of level 1
L2 = 12         # floor of level 2 (bottom of the machine chamber)
LIGHT = "sea_lantern"
ROCK = ["stone", "stone", "andesite", "tuff", "tuff", "cobblestone", "deepslate", "cobbled_deepslate", "iron_ore",
        "stone", "andesite"]


def rock_radius(y, angle):
    """How far the rock reaches from the center at height y in a direction: wide on top, pointed below."""
    frac = max(y, 0) / T
    r = 6 + 38 * frac ** 0.55
    return r + 2.5 * math.sin(3 * angle + 1.0) + 1.8 * math.cos(5 * angle + 0.4) + 1.2 * math.sin(7 * angle + y * 0.3)


def build():
    t = Template("asteroid_m", N, H, N, seed=5)
    rock(t)
    floating_rocks(t)
    surface(t)
    citadel(t)
    machine_chamber(t)
    level_one(t)
    level_two(t)
    t.connect_all()
    return t


# =========================================================================== the rock
def rock(t):
    r = t.rand
    for y in range(0, T + 1):
        for x in range(N):
            for z in range(N):
                dx, dz = x - C, z - C
                if math.hypot(dx, dz) <= rock_radius(y, math.atan2(dz, dx)):
                    t.set(x, y, z, r.choice(ROCK))
    # spikes hanging from the bottom
    for _ in range(34):
        ang = r.random() * math.tau
        dist = r.random() * 30
        x, z = round(C + math.cos(ang) * dist), round(C + math.sin(ang) * dist)
        top = next((y for y in range(T) if t.get(x, y, z) not in (None, "minecraft:air")), None)
        if top is None:
            continue
        length = r.randint(3, 9)
        for i in range(length):
            rad = 2.2 * (1 - i / length)
            t.disc(x, top - i, z, rad, r.choice(ROCK))
    # veins of metal in the rock
    for _ in range(140):
        x, y, z = r.randint(5, N - 6), r.randint(2, T - 1), r.randint(5, N - 6)
        if t.get(x, y, z) not in (None, "minecraft:air"):
            t.ellipsoid(x, y, z, 1.4, 1.1, 1.4, r.choice(["raw_iron_block", "iron_ore", "deepslate_iron_ore"]))
    # the top: rough rock outside, smooth paving inside
    for x in range(N):
        for z in range(N):
            if t.get(x, T, z) not in (None, "minecraft:air"):
                d = math.hypot(x - C, z - C)
                t.set(x, T, z, "polished_andesite" if d < 24 else r.choice(
                    ["stone", "andesite", "tuff", "gravel", "cobblestone", "stone"]))
                t.clear(x, T + 1, z, x, T + 1, z)
    # craters with raw iron at the bottom
    for ang, dist in ((0.6, 32), (2.3, 34), (3.6, 30), (4.4, 35), (5.5, 31)):
        x, z = round(C + math.cos(ang) * dist), round(C + math.sin(ang) * dist)
        t.ellipsoid(x, T + 1, z, 3.5, 2.5, 3.5, AIR)
        t.disc(x, T - 2, z, 1.6, "raw_iron_block")
        t.set(x, T - 1, z, "iron_ore")


def floating_rocks(t):
    """Small chunks of rock floating around the asteroid. Some hide a chest."""
    for i, (x, y, z, rad, loot) in enumerate(((9, 22, 10, 4.0, True), (85, 16, 12, 3.5, False),
                                              (11, 12, 84, 4.5, True), (84, 26, 83, 3.5, True),
                                              (47, 3, 6, 3.0, False), (88, 6, 50, 3.0, False))):
        t.ellipsoid(x, y, z, rad, rad * 0.7, rad, "stone")
        t.ellipsoid(x, y - 1, z, rad * 0.8, rad * 0.7, rad * 0.8, "tuff")
        t.ellipsoid(x, y, z, rad * 0.4, rad * 0.4, rad * 0.4, "raw_iron_block")
        top = round(y + rad * 0.7)
        t.disc(x, top, z, rad * 0.8, "andesite")
        if loot:
            t.chest(x, top + 1, z, "south", "asteroid_m")
            t.set(x + 1, top + 1, z, "lantern")
    # loose metal floating in the air
    for _ in range(26):
        ang = t.rand.random() * math.tau
        dist = 40 + t.rand.random() * 5
        x, z = round(C + math.cos(ang) * dist), round(C + math.sin(ang) * dist)
        y = t.rand.randint(8, 40)
        t.fill(x, y, z, x + t.rand.randint(0, 1), y + t.rand.randint(0, 1), z,
               t.rand.choice(["iron_block", "raw_iron_block", "stone", "anvil", "chain[axis=y]"]))


# =========================================================================== the surface
def surface(t):
    r = t.rand
    W = T + 1  # where people walk
    # road from the citadel gate north to the landing pad
    t.fill(C - 2, T, 6, C + 2, T, C - 17, "polished_blackstone")
    t.fill(C - 3, T, 6, C - 3, T, C - 17, "polished_blackstone_bricks")
    t.fill(C + 3, T, 6, C + 3, T, C - 17, "polished_blackstone_bricks")
    for z in range(10, C - 17, 6):
        for x in (C - 4, C + 4):
            t.set(x, W, z, "iron_bars")
            t.set(x, W + 1, z, "iron_bars")
            t.set(x, W + 2, z, "lantern")
    # landing pad sticking out over the edge
    t.disc(C, T, 7, 6.5, "iron_block")
    t.disc(C, T, 7, 5, "light_gray_concrete")
    t.disc(C, T, 7, 2, "purple_concrete")
    for ang in range(0, 360, 45):
        t.set(round(C + math.cos(math.radians(ang)) * 6), T, round(7 + math.sin(math.radians(ang)) * 6), LIGHT)
    for x, z in ((C - 5, 3), (C + 5, 3), (C - 5, 11), (C + 5, 11)):
        t.line(x, T - 1, z, C + (x - C) // 3, T - 9, 14, "iron_block")   # struts into the rock

    # observatory (east)
    ox, oz = C + 28, C - 14
    t.disc(ox, T, oz, 6, "polished_deepslate")
    t.ellipsoid(ox, W, oz, 5.5, 5.5, 5.5, "tinted_glass", hollow=True, only_y=lambda y: y >= W)
    t.ellipsoid(ox, W, oz, 5.5, 5.5, 5.5, "glass", hollow=True, only_y=lambda y: y >= W + 2)
    t.clear(ox - 6, W, oz - 1, ox - 4, W + 2, oz + 1)  # door
    t.line(ox, W, oz, ox + 2, W + 4, oz, "polished_deepslate")   # telescope
    t.set(ox + 3, W + 5, oz, "end_rod[facing=up]")
    t.set(ox - 1, W, oz, "polished_deepslate_stairs[facing=east]")
    t.chest(ox, W, oz + 3, "north", "asteroid_m")
    t.set(ox + 3, W, oz - 2, "lectern[facing=west]")
    t.set(ox - 2, W, oz - 3, LIGHT)

    # magnetic antennas (west)
    for i, (x, z) in enumerate(((C - 30, C - 10), (C - 34, C - 2), (C - 31, C + 7), (C - 25, C - 16),
                                (C - 26, C + 13))):
        hgt = 9 + (i * 3) % 7
        t.disc(x, T, z, 1.5, "oxidized_cut_copper")
        t.set(x, W, z, "lodestone")
        t.fill(x, W + 1, z, x, W + hgt, z, "iron_bars")
        for k in range(W + 3, W + hgt, 3):
            t.set(x, k, z, "waxed_copper_block")
        t.set(x, W + hgt + 1, z, "lightning_rod")
        t.set(x + 1, W + hgt - 1, z, "chain[axis=y]")
    t.chest(C - 29, W, C - 4, "east", "asteroid_m")

    # mutant garden (south-east): the sanctuary Magneto promised mutants
    gx, gz = C + 24, C + 22
    t.disc(gx, T, gz, 8, "grass_block")
    t.disc(gx, T - 1, gz, 8, "dirt")
    t.disc(gx, T, gz, 2.2, "water")
    t.disc(gx, T - 1, gz, 2.2, "clay")
    for x, z in ((gx - 5, gz - 3), (gx + 4, gz + 4), (gx - 2, gz + 6)):
        t.fill(x, W, z, x, W + 3, z, "cherry_log")
        t.ellipsoid(x, W + 4, z, 2.6, 1.8, 2.6, "cherry_leaves[persistent=true]")
    for _ in range(30):
        x, z = gx + r.randint(-7, 7), gz + r.randint(-7, 7)
        if t.get(x, T, z) == "minecraft:grass_block" and t.get(x, W, z) in (None, "minecraft:air"):
            t.set(x, W, z, r.choice(["poppy", "allium", "cornflower", "short_grass", "short_grass", "azure_bluet"]))
    t.set(gx + 3, W, gz - 4, "spruce_stairs[facing=south]")
    t.set(gx + 4, W, gz - 4, "spruce_stairs[facing=south]")
    t.set(gx - 6, W, gz + 1, "lantern")

    # lights in the ground so nothing spawns at night
    for x in range(6, N - 6, 7):
        for z in range(6, N - 6, 7):
            if t.get(x, T, z) in ("minecraft:stone", "minecraft:andesite", "minecraft:tuff", "minecraft:gravel",
                                  "minecraft:cobblestone", "minecraft:polished_andesite") \
                    and t.get(x, W, z) in (None, "minecraft:air"):
                t.set(x, T, z, LIGHT)


# =========================================================================== the citadel (boss arena)
def citadel(t):
    W = T + 1
    R = 17
    top = W + 11
    t.cylinder(C, W, C, R, top, "iron_block", hollow=True)
    t.cylinder(C, W, C, R - 1, top + 9, AIR)
    t.disc(C, top, C, R, "polished_deepslate", hollow=True)
    # purple glass dome
    t.ellipsoid(C, top, C, R, 9, R, "purple_stained_glass", hollow=True, only_y=lambda y: y > top)
    t.ellipsoid(C, top, C, R - 1, 8, R - 1, AIR, only_y=lambda y: y > top)
    # dark stripes on the walls with windows
    for ang in range(0, 360, 15):
        x = round(C + math.cos(math.radians(ang)) * R)
        z = round(C + math.sin(math.radians(ang)) * R)
        t.fill(x, W, z, x, top, z, "polished_deepslate" if ang % 30 == 0 else "iron_block")
        if ang % 30 == 15:
            t.fill(x, W + 4, z, x, W + 7, z, "purple_stained_glass")
    # towers
    for ang in range(0, 360, 45):
        x = round(C + math.cos(math.radians(ang)) * (R + 1))
        z = round(C + math.sin(math.radians(ang)) * (R + 1))
        t.cylinder(x, W, z, 2, top + 8, "polished_deepslate")
        t.disc(x, top + 9, z, 2.5, "iron_block")
        t.set(x, top + 10, z, "lightning_rod")
        t.set(x, top + 8, z, "redstone_lamp[lit=true]")
    # gate to the north
    t.clear(C - 2, W, C - R - 2, C + 2, W + 5, C - R + 1)
    t.fill(C - 3, W + 6, C - R - 2, C + 3, W + 6, C - R - 2, "polished_deepslate")
    t.set(C, W + 7, C - R - 2, "purple_banner[rotation=8]")
    # floor
    t.disc(C, T, C, R - 1, "smooth_stone")
    t.disc(C, T, C, 13, "polished_deepslate", hollow=True)
    t.disc(C, T, C, 7, "red_concrete")
    t.disc(C, T, C, 5, "purple_concrete")
    for ang in range(0, 360, 30):
        t.line(C, T, C, round(C + math.cos(math.radians(ang)) * 12), T, round(C + math.sin(math.radians(ang)) * 12),
               "iron_block")
    t.disc(C, T, C, 2, "red_concrete")
    # pillars with lights
    for ang in range(22, 382, 45):
        x = round(C + math.cos(math.radians(ang)) * 11)
        z = round(C + math.sin(math.radians(ang)) * 11)
        t.fill(x, W, z, x, top, z, "iron_block")
        t.set(x, W + 4, z, LIGHT)
        t.set(x, W + 9, z, LIGHT)
    # Magneto's dais and throne
    t.disc(C, W, C, 3, "red_concrete")
    t.disc(C, W + 1, C, 2, "purple_concrete")
    t.fill(C - 1, W + 2, C + 2, C + 1, W + 3, C + 2, "red_concrete")
    t.set(C, W + 4, C + 2, "red_concrete")
    t.set(C - 2, W + 2, C + 2, "lightning_rod")
    t.set(C + 2, W + 2, C + 2, "lightning_rod")
    t.mob(C, W + 4, C, "xmen:magneto")   # the real Magneto (Java: entity/MagnetoBoss), floating over his dais
    t.chest(C - 4, W, C, "east", "asteroid_m")
    t.chest(C + 4, W, C - 1, "west", "asteroid_m")
    # hanging lights in the dome
    for ang in range(0, 360, 90):
        x = round(C + math.cos(math.radians(ang)) * 8)
        z = round(C + math.sin(math.radians(ang)) * 8)
        t.fill(x, top + 1, z, x, top + 5, z, "chain[axis=y]")
        t.set(x, top, z, "lantern[hanging=true]")
    t.fill(C, top + 1, C, C, top + 8, C, "chain[axis=y]")
    t.set(C, top, C, LIGHT)

    # stairs down to level 1 (south part of the hall): a hole in the floor with steps going east
    z1, z2 = C + 12, C + 14
    t.clear(C - 6, T, z1, C + 5, T, z2)
    for i in range(11):
        x, y = C - 6 + i, T - 1 - i
        t.fill(x, y, z1, x, y, z2, "polished_deepslate_stairs[facing=west,half=bottom]")
        t.clear(x, y + 1, z1, x, y + 4, z2)
        t.fill(x, y - 1, z1, x, y - 1, z2, "polished_deepslate")
        t.fill(x, y, z1 - 1, x, y + 3, z1 - 1, "polished_deepslate")
        t.fill(x, y, z2 + 1, x, y + 3, z2 + 1, "polished_deepslate")
    for x in range(C - 6, C + 6):
        t.set(x, W, z1 - 1, "iron_bars")
        t.set(x, W, z2 + 1, "iron_bars")
    t.fill(C + 6, W, z1 - 1, C + 6, W, z2 + 1, "iron_bars")
    t.set(C - 7, W, z1 - 1, "iron_bars")
    t.set(C - 7, W, z2 + 1, "iron_bars")


# =========================================================================== the mutation machine (X1)
def machine_chamber(t):
    R = 9
    t.cylinder(C, L2, C, R + 1, T - 4, "polished_deepslate")
    t.cylinder(C, L2 + 1, C, R, T - 5, AIR)
    t.disc(C, L2, C, R, "deepslate_tiles")
    t.disc(C, L2, C, 6, "iron_block", hollow=True)
    for ang in range(0, 360, 30):
        t.set(round(C + math.cos(math.radians(ang)) * 8), L2, round(C + math.sin(math.radians(ang)) * 8), LIGHT)
    for ang in range(0, 360, 40):
        x = round(C + math.cos(math.radians(ang)) * (R + 1))
        z = round(C + math.sin(math.radians(ang)) * (R + 1))
        t.set(x, L2 + 4, z, LIGHT)
        t.set(x, L1 + 4, z, LIGHT)
    # two big rings crossing each other around the middle, iron and copper
    cy, rr = L2 + 7, 5.5
    for k in range(72):
        a = math.tau * k / 72
        y = round(cy + math.sin(a) * rr)
        h = round(math.cos(a) * rr)
        block = "waxed_copper_block" if k % 6 < 3 else "iron_block"
        t.set(C + h, y, C, block)
        t.set(C, y, C + h, block)
    for h in (-6, 6):
        t.set(C + h, cy, C, "end_rod[facing=" + ("east" if h > 0 else "west") + "]")
        t.set(C, cy, C + h, "end_rod[facing=" + ("south" if h > 0 else "north") + "]")
    # the platform in the middle with Rogue trapped in a glass cell
    t.fill(C - 1, L2 + 1, C - 1, C + 1, L2 + 2, C + 1, "iron_block")
    t.fill(C - 1, L2 + 3, C - 1, C + 1, L2 + 5, C + 1, "glass")
    t.clear(C, L2 + 3, C, C, L2 + 4, C)
    t.set(C, L2 + 2, C, LIGHT)
    t.npc(C, L2 + 3, C, "captive_rogue", "rogue")
    for x, z in ((C - 3, C - 3), (C + 3, C - 3), (C - 3, C + 3), (C + 3, C + 3)):
        t.fill(x, L2 + 1, z, x, L2 + 3, z, "polished_deepslate_wall")
        t.set(x, L2 + 4, z, "lightning_rod")
    # control desk
    t.fill(C - 2, L2 + 1, C - 7, C + 2, L2 + 1, C - 7, "polished_blackstone")
    t.set(C - 1, L2 + 2, C - 7, "lever[face=floor,facing=north]")
    t.set(C + 1, L2 + 2, C - 7, "comparator[facing=north]")
    # balcony on level 1 all the way around
    for x in range(C - R, C + R + 1):
        for z in range(C - R, C + R + 1):
            d = math.hypot(x - C, z - C)
            if 6.7 < d <= R + 0.3:
                t.set(x, L1, z, "iron_block" if (x + z) % 2 else "polished_deepslate")
            elif 6.0 < d <= 6.7:
                t.set(x, L1 + 1, z, "iron_bars")
    # stairs from the balcony down to the chamber floor (east side, going south)
    for i in range(10):
        z, y = C - 5 + i, L1 - i
        for x in (C + 7, C + 8):
            t.set(x, y, z, "polished_deepslate_stairs[facing=north,half=bottom]")
            t.clear(x, y + 1, z, x, y + 3, z)
    t.clear(C + 7, L1, C - 5, C + 8, L1, C + 4)
    for z in range(C - 5, C + 5):
        t.set(C + 6, L1 + 1, z, AIR)


# =========================================================================== level 1
def level_one(t):
    F = L1
    # ring corridor around the chamber
    for x in range(C - 16, C + 17):
        for z in range(C - 16, C + 17):
            d = math.hypot(x - C, z - C)
            if 11.5 <= d <= 14.5:
                t.set(x, F, z, "polished_deepslate")
                t.clear(x, F + 1, z, x, F + 4, z)
                t.set(x, F + 5, z, "deepslate_tiles")
            elif 10.5 <= d < 11.5 or 14.5 < d <= 15.5:
                t.fill(x, F + 1, z, x, F + 4, z, "polished_deepslate")
                t.set(x, F + 5, z, "deepslate_tiles")
    for ang in range(0, 360, 30):
        t.set(round(C + math.cos(math.radians(ang)) * 13), F + 5, round(C + math.sin(math.radians(ang)) * 13), LIGHT)
    # doorways from the ring into the balcony
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for s in range(9, 12):
            for w in (-1, 0, 1):
                x, z = C + dx * s + dz * w, C + dz * s + dx * w
                t.clear(x, F + 1, z, x, F + 3, z)
                t.set(x, F, z, "polished_deepslate")

    def corridor(x1, z1, x2, z2):
        t.fill(x1, F, z1, x2, F, z2, "polished_deepslate")
        t.clear(x1, F + 1, z1, x2, F + 4, z2)
        t.fill(x1, F + 5, z1, x2, F + 5, z2, "deepslate_tiles")

    # ---- west: the Brotherhood's quarters
    corridor(C - 18, C - 1, C - 14, C + 1)
    x1, x2, z1, z2 = C - 30, C - 18, C - 7, C + 7
    t.room(x1, F, z1, x2, F + 6, z2, "polished_deepslate", "dark_oak_planks", "deepslate_tiles")
    t.clear(x2, F + 1, C - 1, x2, F + 3, C + 1)
    for i, z in enumerate(range(z1 + 1, z2, 3)):
        t.bed(x1 + 1, F + 1, z, "east", "purple" if i % 2 else "red")
        t.barrel(x1 + 3, F + 1, z, "asteroid_quarters", "west")
    t.table(C - 24, F + 1, C - 1, "dark_oak_pressure_plate", "dark_oak_fence")
    t.table(C - 24, F + 1, C + 1, "dark_oak_pressure_plate", "dark_oak_fence")
    for z in (C - 1, C + 1):
        t.set(C - 25, F + 1, z, "dark_oak_stairs[facing=east]")
        t.set(C - 23, F + 1, z, "dark_oak_stairs[facing=west]")
    t.set(C - 24, F + 2, C, "lantern")
    t.fill(x2 - 1, F + 1, z2 - 1, x2 - 1, F + 2, z2 - 1, "iron_block")
    t.chest(x2 - 2, F + 1, z2 - 1, "north", "asteroid_quarters")
    for x in (x1 + 3, x2 - 3):
        for z in (z1 + 3, z2 - 3):
            t.set(x, F + 6, z, LIGHT)
    t.set(C - 22, F + 3, z1, "red_wall_banner[facing=south]")

    # ---- east: the hangar, open to the sky, with Magneto's shuttle
    corridor(C + 14, C - 1, C + 18, C + 1)
    x1, x2, z1, z2 = C + 18, N - 1, C - 9, C + 9
    t.fill(x1, F, z1, x2, F, z2, "iron_block")
    t.fill(x1 + 1, F, z1 + 2, x2, F, z2 - 2, "light_gray_concrete")
    t.clear(x1 + 1, F + 1, z1 + 1, x2, F + 9, z2 - 1)
    t.walls(x1, F + 1, z1, x2, F + 9, z2, "polished_deepslate")
    t.fill(x1, F + 10, z1, x2, F + 10, z2, "deepslate_tiles")
    t.clear(x1 + 1, F + 1, z1 + 1, x2, F + 9, z2 - 1)   # the far end is open
    t.clear(x1, F + 1, C - 1, x1, F + 3, C + 1)
    for x in range(x1 + 3, x2, 5):
        t.set(x, F, z1 + 2, LIGHT)
        t.set(x, F, z2 - 2, LIGHT)
        t.set(x, F + 10, C, LIGHT)
    # the shuttle
    sx, sy, sz = C + 31, F + 3, C
    t.ellipsoid(sx, sy, sz, 7, 2.2, 3, "light_gray_concrete", hollow=True)
    t.ellipsoid(sx + 4, sy + 1, sz, 2.5, 1.4, 2, "light_blue_stained_glass")
    t.fill(sx - 4, sy - 2, sz - 6, sx + 1, sy - 2, sz + 6, "gray_concrete")   # wings
    t.fill(sx - 7, sy - 1, sz - 1, sx - 7, sy + 1, sz + 1, "purple_concrete")
    t.set(sx - 8, sy, sz, "magma_block")
    t.fill(sx - 2, F + 1, sz, sx - 2, sy - 2, sz, "iron_bars")
    t.fill(sx + 3, F + 1, sz, sx + 3, sy - 2, sz, "iron_bars")
    t.clear(sx - 1, sy - 1, sz - 3, sx + 1, sy, sz - 3)   # hatch
    t.chest(sx, sy - 1, sz, "north", "asteroid_m")
    for z in (z1 + 1, z2 - 1):
        t.barrel(x1 + 2, F + 1, z, "asteroid_quarters")
        t.barrel(x1 + 3, F + 1, z, "asteroid_quarters")
        t.set(x1 + 2, F + 2, z, "barrel[facing=up]")

    # ---- north: the plastic prison from X2, a cell hanging over a pit, not a scrap of metal in it
    corridor(C - 1, C - 18, C + 1, C - 14)
    x1, x2, z1, z2 = C - 8, C + 8, C - 32, C - 18
    t.room(x1, F - 6, z1, x2, F + 8, z2, "white_concrete", "white_concrete", "white_concrete")
    t.fill(x1 + 1, F, z1 + 1, x2 - 1, F, z1 + 2, "white_concrete")   # ledge
    t.fill(x1 + 1, F, z2 - 2, x2 - 1, F, z2 - 1, "white_concrete")   # ledge by the door
    t.clear(C - 1, F + 1, z2, C + 1, F + 3, z2)
    cz = (z1 + z2) // 2
    t.fill(C - 1, F, cz + 3, C + 1, F, z2 - 3, "white_stained_glass")    # glass bridge
    t.room(C - 3, F - 1, cz - 3, C + 3, F + 4, cz + 3, "white_stained_glass", "quartz_block", "white_stained_glass")
    t.clear(C - 1, F, cz + 3, C + 1, F + 2, cz + 3)
    t.bed(C - 2, F, cz - 1, "north", "white")
    t.table(C + 1, F, cz - 1, "white_carpet", "quartz_pillar")
    t.set(C + 2, F, cz - 1, "quartz_stairs[facing=west]")
    t.set(C, F + 3, cz, "sea_lantern")
    t.set(C + 2, F, cz + 1, "white_carpet")
    t.chest(C + 2, F, cz - 2, "west", "asteroid_vault")
    for x in (x1 + 2, x2 - 2):
        for z in (z1 + 2, z2 - 2):
            t.set(x, F + 8, z, LIGHT)
    t.set(C, F - 6, cz, LIGHT)

    # ---- south: Magneto's study with the chess board (the X1 ending)
    corridor(C - 1, C + 14, C + 1, C + 18)
    x1, x2, z1, z2 = C - 9, C + 9, C + 18, C + 31
    t.room(x1, F, z1, x2, F + 6, z2, "dark_oak_planks", "dark_oak_planks", "dark_oak_planks")
    t.clear(C - 1, F + 1, z1, C + 1, F + 3, z1)
    t.fill(C - 2, F, z1 + 1, C + 2, F, z2 - 2, "red_wool")
    for x in range(x1 + 1, x2):
        for y in range(F + 1, F + 5):
            if x not in (C - 1, C, C + 1):
                t.set(x, y, z2 - 1, "bookshelf")
    for z in range(z1 + 2, z2 - 1):
        t.fill(x1 + 1, F + 1, z, x1 + 1, F + 4, z, "bookshelf")
    # the chess board on a table: 8x8, black and white, with pieces
    bx, bz = C - 4, C + 21
    for i in range(8):
        for j in range(8):
            t.set(bx + i, F + 1, bz + j, "smooth_quartz" if (i + j) % 2 == 0 else "polished_blackstone")
    back = ["quartz_pillar", "end_rod[facing=up]", "chiseled_quartz_block", "lightning_rod", "end_rod[facing=up]",
            "chiseled_quartz_block", "end_rod[facing=up]", "quartz_pillar"]
    back_b = ["polished_blackstone_wall", "chain[axis=y]", "chiseled_polished_blackstone", "lightning_rod",
              "chain[axis=y]", "chiseled_polished_blackstone", "chain[axis=y]", "polished_blackstone_wall"]
    for i in range(8):
        t.set(bx + i, F + 2, bz, back[i])
        t.set(bx + i, F + 2, bz + 1, "white_candle[candles=1,lit=false]")
        t.set(bx + i, F + 2, bz + 6, "black_candle[candles=1,lit=false]")
        t.set(bx + i, F + 2, bz + 7, back_b[i])
    t.set(bx - 1, F + 1, bz + 3, "dark_oak_stairs[facing=east]")
    t.set(bx + 8, F + 1, bz + 4, "dark_oak_stairs[facing=west]")
    t.set(C + 6, F + 1, z2 - 3, "lectern[facing=north]")
    t.chest(x2 - 1, F + 1, z1 + 2, "west", "asteroid_vault")
    for x in (x1 + 3, x2 - 3):
        for z in (z1 + 3, z2 - 3):
            t.set(x, F + 6, z, LIGHT)
    t.set(C, F + 6, C + 25, LIGHT)


# =========================================================================== level 2
def level_two(t):
    F = L2

    def tunnel(x1, z1, x2, z2):
        t.fill(x1, F, z1, x2, F, z2, "deepslate_tiles")
        t.clear(x1, F + 1, z1, x2, F + 4, z2)

    # ---- west: the magnetic core, a column of lodestone that keeps the asteroid in the sky
    tunnel(C - 16, C - 1, C - 10, C + 1)
    x1, x2, z1, z2 = C - 26, C - 16, C - 5, C + 5
    t.room(x1, F, z1, x2, F + 9, z2, "iron_block", "polished_deepslate", "iron_block")
    t.clear(x2, F + 1, C - 1, x2, F + 3, C + 1)
    cx = (x1 + x2) // 2
    t.fill(cx, F + 1, C, cx, F + 8, C, "lodestone")
    for y in range(F + 1, F + 9):
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            t.set(cx + dx, y, C + dz, "waxed_copper_block" if y % 2 else "iron_bars")
    for x, z in ((x1 + 2, z1 + 2), (x2 - 2, z1 + 2), (x1 + 2, z2 - 2), (x2 - 2, z2 - 2)):
        t.fill(x, F + 1, z, x, F + 8, z, "redstone_lamp[lit=true]")
    t.set(cx, F, C, LIGHT)

    # ---- vault next to the core, behind an iron door
    x1, x2, z1, z2 = C - 25, C - 17, C + 5, C + 12
    t.room(x1, F, z1, x2, F + 5, z2, "iron_block", "iron_block", "iron_block")
    t.door(C - 21, F + 1, z1, "south", "iron")
    t.set(C - 20, F + 2, z1 - 1, "stone_button[face=wall,facing=north]")
    t.set(C - 20, F + 2, z1 + 1, "stone_button[face=wall,facing=south]")
    t.chest(x1 + 1, F + 1, z2 - 1, "north", "asteroid_vault")
    t.chest(x2 - 1, F + 1, z2 - 1, "north", "asteroid_vault")
    t.fill(C - 22, F + 1, z2 - 1, C - 20, F + 1, z2 - 1, "gold_block")
    t.set(C - 21, F + 5, C + 8, LIGHT)

    # ---- east: an amethyst geode inside the rock
    tunnel(C + 10, C - 1, C + 15, C + 1)
    gx, gy, gz = C + 20, F + 3, C
    t.ellipsoid(gx, gy, gz, 6, 5, 6, "smooth_basalt")
    t.ellipsoid(gx, gy, gz, 5, 4, 5, "calcite")
    t.ellipsoid(gx, gy, gz, 4.2, 3.4, 4.2, "amethyst_block")
    t.ellipsoid(gx, gy, gz, 3.4, 2.6, 3.4, AIR)
    t.clear(C + 15, F + 1, C - 1, gx - 3, F + 3, C + 1)
    for x in range(gx - 4, gx + 5):
        for y in range(gy - 3, gy + 4):
            for z in range(gz - 4, gz + 5):
                if t.get(x, y, z) == "minecraft:amethyst_block" and t.rand.random() < 0.15:
                    for dx, dy, dz, face in ((0, 1, 0, "up"), (0, -1, 0, "down"), (1, 0, 0, "east"),
                                             (-1, 0, 0, "west"), (0, 0, 1, "south"), (0, 0, -1, "north")):
                        if t.get(x + dx, y + dy, z + dz) == "minecraft:air":
                            t.set(x + dx, y + dy, z + dz, f"amethyst_cluster[facing={face}]")
                            break
                    else:
                        continue
                    if t.rand.random() < 0.3:
                        t.set(x, y, z, "budding_amethyst")
    t.fill(gx - 1, gy - 3, gz - 1, gx + 1, gy - 3, gz + 1, "amethyst_block")
    t.chest(gx, gy - 2, gz, "west", "asteroid_vault")
    t.set(gx + 2, gy - 2, gz + 1, "sea_lantern")
