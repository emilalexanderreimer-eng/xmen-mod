"""The Weapon X facility, modeled on the movies (X2's Alkali Lake base, X-Men Origins, X-Men: Apocalypse, Logan).

A guarded compound in the snow on top, and a freight elevator plus an emergency staircase going 44 blocks down to
three underground levels:

  Level 1 (security)    barracks, mess hall, Stryker's command center, armory, holding cells, infirmary, generators
  Level 2 (labs)        the adamantium bonding chamber, Logan's Weapon X tank, Stryker's fake Cerebro, genetics lab
  Level 3 (deep)        Weapon XI's chamber, the Transigen wing with X-24's growth tank, the adamantium vault

The people are real characters (Java: entity/WeaponX*.java), not renamed vanilla mobs.
"""
from lib import Template, FACING
import nbt

N = 96              # the template is N x N
G = 46              # surface floor
L1, L2, L3 = 34, 18, 2   # floors of the three underground levels
LIGHT = "sea_lantern"

SOLDIER = "xmen:weapon_x_soldier"
SCIENTIST = "xmen:weapon_x_scientist"
TANK = {"Contained": nbt.Byte(1)}

# freight elevator cabin (walls) and the emergency stairwell (walls), both inside the lobby of every level
CAB = (41, 49, 45, 53)
STAIR = (49, 41, 54, 46)


class Level:
    """Collects the rooms of one level, then builds them all: solid shells first, then carves the insides."""

    def __init__(self, t, floor, filler, floor_block):
        self.t, self.F, self.filler, self.floor_block = t, floor, filler, floor_block
        self.spaces = []

    def space(self, x1, z1, x2, z2, height=5, floor=None, lights=5):
        """A room (inside coordinates): floor at F, air from F+1 to F+height, ceiling above."""
        self.spaces.append((min(x1, x2), min(z1, z2), max(x1, x2), max(z1, z2), height, floor, lights))

    def passage(self, x1, z1, x2, z2, height=3):
        self.space(x1, z1, x2, z2, height, None, 0)

    def build(self):
        t, F = self.t, self.F
        for x1, z1, x2, z2, h, _, _ in self.spaces:
            t.fill(x1 - 1, F, z1 - 1, x2 + 1, F + h + 1, z2 + 1, self.filler)
        for x1, z1, x2, z2, h, floor, lights in self.spaces:
            t.clear(x1, F + 1, z1, x2, F + h, z2)
            t.fill(x1, F, z1, x2, F, z2, floor or self.floor_block)
            if lights:
                for x in range(x1 + 2, x2 - 1, lights):
                    for z in range(z1 + 2, z2 - 1, lights):
                        t.set(x, F + h + 1, z, LIGHT)


# ----------------------------------------------------------------------------------------------- helpers
def hazard(t, x1, y, z1, x2, z2):
    """Yellow and black warning stripes."""
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            t.set(x, y, z, "yellow_concrete" if (x + z) % 2 == 0 else "black_concrete")


def screens(t, x1, y1, z, x2, y2, behind_dz):
    """A wall of glowing monitors: blue glass with lights behind it."""
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            t.set(x, y, z, "light_blue_stained_glass" if (x + y) % 3 else "black_stained_glass")
            t.set(x, y, z + behind_dz, LIGHT)


def pipes(t, x1, x2, y, z):
    for x in range(x1, x2 + 1):
        t.set(x, y, z, "chain[axis=x]")


def tank(t, cx, y, cz, r, height, glass="glass", water=True):
    """A round glass tank, iron at the bottom and top."""
    t.cylinder(cx, y, cz, r, y + height - 1, glass, hollow=True)
    if water:
        t.cylinder(cx, y, cz, r - 1, y + height - 2, "water")
    t.disc(cx, y - 1, cz, r, "iron_block")
    t.disc(cx, y + height, cz, r, "iron_block")


def mob(t, x, y, z, entity, extra=None, yaw=0.0):
    t.mob(x, y, z, entity, None, extra, yaw)


def table(t, x, y, z, top="smooth_stone_slab[type=top]"):
    t.set(x, y, z, top)


def bunk(t, x, y, z, facing):
    t.bed(x, y, z, facing, "gray")


# ----------------------------------------------------------------------------------------------- shafts
def ring():
    """The 12 steps around the stairwell's core, going down clockwise (local 1..4 inside the 6x6 shaft)."""
    return [(1, 1), (2, 1), (3, 1), (4, 1), (4, 2), (4, 3), (4, 4), (3, 4), (2, 4), (1, 4), (1, 3), (1, 2)]


def direction(a, b):
    dx, dz = b[0] - a[0], b[1] - a[1]
    for name, (fx, fz) in FACING.items():
        if (fx, fz) == (dx, dz):
            return name
    raise ValueError((a, b))


def stairwell(t):
    """Emergency stairs from the surface all the way down, with a door into the lobby of every level."""
    x0, z0, x1, z1 = STAIR
    top, bottom = G, L3
    t.walls(x0, bottom - 1, z0, x1, top + 3, z1, "gray_concrete")
    t.fill(x0, bottom - 1, z0, x1, bottom - 1, z1, "polished_andesite")
    t.fill(x0, top + 4, z0, x1, top + 4, z1, "gray_concrete")
    t.clear(x0 + 1, bottom, z0 + 1, x1 - 1, top + 3, z1 - 1)
    t.fill(x0 + 2, bottom, z0 + 2, x0 + 3, top + 3, z0 + 3, "polished_andesite")
    for y in range(bottom + 2, top + 3, 4):
        t.set(x0 + 2, y, z0 + 2, LIGHT)
        t.set(x0 + 3, y, z0 + 3, LIGHT)
    steps = ring()
    corners = {(1, 1), (4, 1), (4, 4), (1, 4)}
    for i in range(top - bottom + 1):
        lx, lz = steps[i % 12]
        x, y, z = x0 + lx, top - i, z0 + lz
        if i == 0 or (lx, lz) in corners:
            t.set(x, y, z, "polished_andesite")
        else:
            up = steps[(i - 1) % 12]
            t.set(x, y, z, f"polished_andesite_stairs[facing={direction((lx, lz), up)}]")
    # doors: at every floor the step at that height opens to the lobby
    for floor in (G, L1, L2, L3):
        lx, lz = steps[(top - floor) % 12]
        if lx == 1 and lz == 1:
            dx, dz = -1, 0
        elif lx in (1, 4) and lz not in (1, 4):
            dx, dz = (-1 if lx == 1 else 1), 0
        else:
            dx, dz = 0, (-1 if lz == 1 else 1)
        t.clear(x0 + lx + dx, floor + 1, z0 + lz + dz, x0 + lx + dx, floor + 2, z0 + lz + dz)
        t.clear(x0 + lx + 2 * dx, floor + 1, z0 + lz + 2 * dz, x0 + lx + 2 * dx, floor + 2, z0 + lz + 2 * dz)


def elevator(t):
    """The freight elevator: a cabin on every level. Jump to ride up, sneak to ride down."""
    x0, z0, x1, z1 = CAB
    for floor in (G, L1, L2, L3):
        t.walls(x0, floor + 1, z0, x1, floor + 3, z1, "iron_block")
        t.fill(x0, floor + 4, z0, x1, floor + 4, z1, "iron_block")
        t.clear(x0 + 1, floor + 1, z0 + 1, x1 - 1, floor + 3, z1 - 1)
        t.clear(x0 + 1, floor + 1, z0, x1 - 1, floor + 3, z0)
        t.fill(x0 + 1, floor, z0 + 1, x1 - 1, floor, z1 - 1, "xmen:elevator")
        t.set(x0 + 2, floor + 4, z0 + 2, LIGHT)
        hazard(t, x0 + 1, floor, z0 - 1, x1 - 1, z0 - 1)
        for x in (x0, x1):
            t.set(x, floor + 1, z0 - 1, "yellow_concrete")


def lobby(level, wall):
    """The lobby around the elevator and stairs on every level, with the main corridors going off it."""
    level.space(40, 40, 55, 55, 5)


# ----------------------------------------------------------------------------------------------- surface
def surface(t):
    lo, hi = 22, 73
    t.fill(lo, G - 4, lo, hi, G - 1, hi, "stone")
    t.fill(lo, G, lo, hi, G, hi, "snow_block")
    t.clear(lo, G + 1, lo, hi, G + 16, hi)
    for x in range(lo, hi + 1):
        for z in range(lo, hi + 1):
            t.set(x, G + 1, z, "snow[layers=1]")
    # roads from the gate to the main building and the helipad
    t.fill(46, G, 58, 50, G, hi, "gray_concrete_powder")
    t.fill(30, G, 46, 45, G, 50, "gray_concrete_powder")
    t.clear(46, G + 1, 58, 50, G + 1, hi)
    t.clear(30, G + 1, 46, 45, G + 1, 50)
    # the fence all around with a gate in the south
    t.walls(25, G + 1, 25, 70, G + 3, 70, "iron_bars")
    t.walls(25, G + 4, 25, 70, G + 4, 70, "chain[axis=y]")
    for i in range(25, 71, 5):
        for x, z in ((i, 25), (i, 70), (25, i), (70, i)):
            t.fill(x, G + 1, z, x, G + 4, z, "gray_concrete")
    t.clear(45, G + 1, 70, 51, G + 4, 70)
    t.fill(44, G + 1, 70, 44, G + 4, 70, "gray_concrete")
    t.fill(52, G + 1, 70, 52, G + 4, 70, "gray_concrete")
    hazard(t, 45, G + 3, 70, 51, 70)   # the barrier over the gate
    t.clear(45, G + 1, 70, 51, G + 2, 70)
    # guard towers in the corners
    for cx, cz in ((28, 28), (67, 28), (28, 67), (67, 67)):
        for dx in (-1, 1):
            for dz in (-1, 1):
                t.fill(cx + dx, G + 1, cz + dz, cx + dx, G + 7, cz + dz, "gray_concrete")
        t.fill(cx - 2, G + 7, cz - 2, cx + 2, G + 7, cz + 2, "polished_andesite")
        t.walls(cx - 2, G + 8, cz - 2, cx + 2, G + 8, cz + 2, "iron_bars")
        for dx in (-2, 2):
            for dz in (-2, 2):
                t.fill(cx + dx, G + 8, cz + dz, cx + dx, G + 10, cz + dz, "gray_concrete")
        t.fill(cx - 2, G + 11, cz - 2, cx + 2, G + 11, cz + 2, "gray_concrete_powder")
        t.set(cx, G + 10, cz, "lantern[hanging=true]")
        for y in range(G + 1, G + 8):
            t.set(cx + 1, y, cz, "ladder[facing=south]")
    t.chest(67, G + 8, 66, "north", "weapon_x_armory")
    # helipad
    t.disc(33, G, 37, 6, "gray_concrete")
    t.disc(33, G, 37, 6.4, "yellow_concrete", hollow=True)
    t.clear(27, G + 1, 31, 39, G + 1, 43)
    for z in range(34, 41):
        t.set(31, G, z, "yellow_concrete")
        t.set(35, G, z, "yellow_concrete")
    t.fill(31, G, 37, 35, G, 37, "yellow_concrete")
    for x, z in ((27, 37), (39, 37), (33, 31), (33, 43)):
        t.set(x, G, z, LIGHT)
    # two military trucks
    for x0, z0 in ((56, 62), (58, 30)):
        t.fill(x0, G + 1, z0, x0 + 2, G + 2, z0 + 5, "black_concrete")
        t.fill(x0, G + 3, z0 + 2, x0 + 2, G + 4, z0 + 5, "green_terracotta")
        t.fill(x0, G + 3, z0, x0 + 2, G + 3, z0 + 1, "black_stained_glass")
        for z in (z0 + 1, z0 + 4):
            t.set(x0 - 1, G + 1, z, "coal_block")
            t.set(x0 + 3, G + 1, z, "coal_block")
        t.set(x0, G + 2, z0 - 1, "lantern[hanging=false]")
        t.set(x0 + 2, G + 2, z0 - 1, "lantern[hanging=false]")
    # floodlights
    for x, z in ((40, 30), (60, 44), (36, 62), (62, 66)):
        t.fill(x, G + 1, z, x, G + 5, z, "iron_bars")
        t.set(x, G + 6, z, LIGHT)
    # the main building over the shafts, with a big blast door
    t.fill(37, G, 37, 58, G, 58, "polished_andesite")
    t.walls(37, G + 1, 37, 58, G + 6, 58, "gray_concrete")
    t.fill(37, G + 7, 37, 58, G + 7, 58, "gray_concrete")
    t.clear(38, G + 1, 38, 57, G + 6, 57)
    t.fill(37, G + 1, 37, 58, G + 1, 37, "light_gray_concrete")
    for x in range(40, 57, 4):
        t.set(x, G + 6, 47, LIGHT)
        t.set(x, G + 6, 55, LIGHT)
    t.clear(45, G + 1, 58, 51, G + 4, 58)
    hazard(t, 44, G + 5, 58, 52, 58)
    for y in range(G + 1, G + 5):
        t.set(44, y, 58, "yellow_concrete" if y % 2 else "black_concrete")
        t.set(52, y, 58, "yellow_concrete" if y % 2 else "black_concrete")
    t.fill(45, G + 4, 59, 51, G + 4, 59, "iron_bars")
    # roof: red X, radar dish and antenna
    for i in range(10):
        t.set(41 + i, G + 8, 41 + i, "red_concrete")
        t.set(50 - i, G + 8, 41 + i, "red_concrete")
    t.fill(54, G + 8, 54, 54, G + 11, 54, "iron_bars")
    t.disc(54, G + 12, 54, 2, "light_gray_concrete")
    t.set(54, G + 13, 54, "lightning_rod")
    t.fill(40, G + 8, 55, 40, G + 16, 55, "iron_bars")
    t.set(40, G + 16, 55, "redstone_block")
    # inside: guard desk
    t.fill(38, G + 1, 40, 40, G + 1, 40, "gray_concrete")
    table(t, 38, G + 2, 40, "light_blue_stained_glass")
    t.chest(57, G + 1, 38, "south", "weapon_x")
    mob(t, 39, G + 1, 42, SOLDIER, yaw=90)
    mob(t, 48, G + 1, 66, SOLDIER, yaw=0)
    mob(t, 28, G + 8, 28, SOLDIER)
    mob(t, 67, G + 8, 67, SOLDIER, yaw=180)


# ----------------------------------------------------------------------------------------------- level 1
def level1(t):
    F = L1
    lv = Level(t, F, "light_gray_concrete", "polished_andesite")
    lobby(lv, "light_gray_concrete")
    lv.space(3, 46, 39, 50)    # west corridor
    lv.space(56, 46, 92, 50)   # east corridor
    lv.space(46, 3, 50, 39)    # north corridor
    lv.space(46, 56, 50, 92)   # south corridor
    lv.space(3, 3, 37, 26)     # barracks
    lv.space(3, 30, 37, 42)    # mess hall
    lv.space(58, 3, 92, 28)    # command center
    lv.space(58, 32, 92, 42)   # armory
    lv.space(3, 54, 37, 92)    # holding cells
    lv.space(58, 54, 92, 74)   # infirmary
    lv.space(58, 78, 92, 92)   # generators
    # doors between the rooms and the corridors
    lv.passage(38, 12, 45, 14)      # barracks - north corridor
    lv.passage(18, 27, 20, 29)      # barracks - mess hall
    lv.passage(18, 43, 21, 45)      # mess hall - west corridor
    lv.passage(51, 12, 57, 14)      # north corridor - command center
    lv.passage(72, 29, 74, 31)      # command center - armory
    lv.passage(72, 43, 74, 45)      # armory - east corridor
    lv.passage(38, 72, 45, 74)      # south corridor - cells
    lv.passage(51, 63, 57, 65)      # south corridor - infirmary
    lv.passage(72, 75, 74, 77)      # infirmary - generators
    lv.build()
    floor = F + 1
    # corridor details: pipes, stripes, chains
    for z in (46, 50):
        pipes(t, 3, 39, F + 5, z)
        pipes(t, 56, 92, F + 5, z)
    for x in range(6, 92, 12):
        if not 39 < x < 56:
            hazard(t, x, F, 46, x, 50)
    t.fill(46, F, 40, 50, F, 40, "yellow_concrete")

    # barracks: rows of bunk beds and lockers
    for x in range(5, 36, 4):
        bunk(t, x, floor, 4, "north")
        bunk(t, x, floor, 24, "south")
        t.barrel(x + 1, floor, 4, "weapon_x", "south")
        t.barrel(x + 1, floor, 25, "weapon_x", "north")
    t.fill(10, floor, 13, 30, floor, 15, "spruce_planks")
    t.fill(10, floor + 1, 14, 30, floor + 1, 14, "gray_carpet")
    mob(t, 12, floor, 10, SOLDIER)
    mob(t, 28, floor, 18, SOLDIER, yaw=90)
    mob(t, 20, floor, 20, SOLDIER, yaw=180)

    # mess hall: long tables and a kitchen
    for z in (33, 38):
        for x in range(6, 33):
            table(t, x, floor, z)
        for x in range(6, 33, 2):
            t.set(x, floor, z - 1, "spruce_stairs[facing=south]")
            t.set(x, floor, z + 1, "spruce_stairs[facing=north]")
    for x in range(30, 37):
        t.set(x, floor, 41, "smoker[facing=north]" if x % 2 else "furnace[facing=north]")
    t.chest(36, floor, 31, "west", "weapon_x")

    # Stryker's command center: a wall of monitors, desks and a map table
    screens(t, 60, floor + 1, 2, 90, floor + 3, -1)
    for x in range(61, 90, 3):
        t.fill(x, floor, 6, x + 1, floor, 6, "gray_concrete")
        t.set(x, floor, 7, "spruce_stairs[facing=north]")
    t.fill(70, floor, 15, 80, floor, 19, "black_concrete")
    t.fill(71, floor + 1, 16, 79, floor + 1, 18, "light_blue_carpet")
    t.fill(71, F + 6, 16, 79, F + 6, 18, LIGHT)
    t.chest(91, floor, 27, "west", "weapon_x")
    mob(t, 75, floor, 22, "xmen:william_stryker", yaw=180)
    mob(t, 66, floor, 24, SOLDIER, yaw=180)
    mob(t, 85, floor, 24, SOLDIER, yaw=180)

    # armory: weapon racks and ammo chests
    for x in range(60, 92, 3):
        t.set(x, floor, 33, "iron_block")
        t.set(x, floor + 1, 33, "iron_bars")
        t.set(x, floor + 2, 33, "iron_trapdoor[facing=south,half=bottom,open=false]")
    for x in (62, 70, 80, 88):
        t.chest(x, floor, 41, "north", "weapon_x_armory")
    t.set(66, floor, 37, "anvil[facing=east]")
    t.set(84, floor, 37, "smithing_table")
    mob(t, 75, floor, 37, SOLDIER)

    # holding cells (X-Men Origins: the mutants Stryker caught), along an aisle
    t.fill(3, F, 71, 37, F, 75, "gray_concrete")
    for x0 in range(3, 36, 6):
        for z0, z1, front in ((54, 69, 70), (77, 92, 76)):
            x1 = min(x0 + 4, 37)
            if x0 + 5 <= 37:
                t.fill(x0 + 5, floor, z0, x0 + 5, floor + 4, z1, "light_gray_concrete")
            t.fill(x0, floor, front, x1, floor + 3, front, "iron_bars")
            t.fill(x0, floor + 4, front, x1, floor + 4, front, "light_gray_concrete")
            north = front == 70
            t.bed(x0 + 1, floor, z0 + 1 if north else z1 - 1, "south" if north else "north", "white")
            t.fill(x0 + 3, floor + 3, (z0 + z1) // 2, x0 + 3, floor + 4, (z0 + z1) // 2, "chain[axis=y]")
            if (x0 // 6) % 2 == 0:
                t.set(x0 + 3, floor, (z0 + z1) // 2 + (2 if north else -2), "bone_block")
    mob(t, 10, floor, 73, SOLDIER, yaw=90)
    mob(t, 30, floor, 73, SOLDIER, yaw=270)

    # infirmary: beds with curtains, medicine
    for x in range(61, 91, 5):
        t.bed(x, floor, 56, "north", "white")
        t.bed(x, floor, 71, "south", "white")
        t.set(x + 2, floor, 56, "brewing_stand")
        t.set(x + 2, floor, 72, "cauldron")
    t.chest(91, floor, 64, "west", "weapon_x_lab")
    mob(t, 70, floor, 64, SCIENTIST)
    mob(t, 82, floor, 62, SOLDIER, yaw=90)

    # generators: big humming machines
    for x in range(61, 91, 6):
        t.fill(x, floor, 81, x + 3, floor + 3, 84, "iron_block")
        t.fill(x + 1, floor + 1, 80, x + 2, floor + 2, 80, "shroomlight")
        t.fill(x + 1, floor + 4, 82, x + 2, floor + 4, 83, "lightning_rod")
        t.fill(x, floor, 87, x + 3, floor + 1, 89, "blast_furnace[facing=north]")
    mob(t, 75, floor, 78, SOLDIER)


# ----------------------------------------------------------------------------------------------- level 2
def level2(t):
    F = L2
    lv = Level(t, F, "smooth_stone", "light_gray_concrete")
    lobby(lv, "smooth_stone")
    lv.space(3, 46, 39, 50)
    lv.space(56, 46, 92, 50)
    lv.space(46, 3, 50, 39)
    lv.space(3, 3, 37, 37, 14, "gray_concrete", 6)      # adamantium bonding chamber
    lv.space(58, 3, 92, 37, 11, "white_concrete", 6)    # Weapon X program (Logan's tank)
    lv.space(58, 56, 92, 92, 6, "white_concrete")       # genetics lab
    lv.passage(18, 38, 22, 45, 4)   # bonding chamber - west corridor
    lv.passage(38, 18, 45, 22, 4)   # bonding chamber - north corridor
    lv.passage(73, 38, 77, 45, 4)   # Weapon X program - east corridor
    lv.passage(73, 51, 77, 55, 4)   # east corridor - genetics lab
    lv.passage(18, 51, 22, 61, 9)   # west corridor - Cerebro (tall: stairs go up to the bridge)
    lv.build()
    floor = F + 1
    for z in (46, 50):
        pipes(t, 3, 39, F + 5, z)
        pipes(t, 56, 92, F + 5, z)

    # --- the adamantium bonding chamber (X2 / Origins): the big tank Logan was bonded in
    c = 20
    t.disc(c, F, c, 9, "black_concrete")
    tank(t, c, floor, c, 6, 8, "light_blue_stained_glass")
    t.fill(c - 1, floor, c - 3, c + 1, floor, c + 3, "iron_block")      # the operating table at the bottom
    t.fill(c - 1, floor + 1, c - 3, c + 1, floor + 1, c + 3, "iron_trapdoor[facing=north,half=bottom,open=false]")
    # catwalk around the top of the tank, with a ladder up
    t.disc(c, floor + 8, c, 9, "polished_andesite")
    t.disc(c, floor + 8, c, 5, "light_blue_stained_glass")   # glass floor: look down into the tank
    t.disc(c, floor + 9, c, 9.6, "iron_bars", hollow=True)
    t.fill(c, floor, c + 10, c, floor + 8, c + 10, "iron_block")
    for y in range(floor, floor + 9):
        t.set(c, y, c + 9, "ladder[facing=north]")
    t.clear(c, floor + 9, c + 9, c, floor + 9, c + 9)
    # the crane that lowered him in, and the injection needles over the tank
    t.fill(c, F + 14, 4, c, F + 14, c, "iron_block")
    for dx, dz in ((0, 0), (-2, 0), (2, 0), (0, -2), (0, 2)):
        t.fill(c + dx, floor + 10, c + dz, c + dx, F + 13, c + dz, "chain[axis=y]")
        t.set(c + dx, floor + 9, c + dz, "end_rod[facing=down]")
    # adamantium smelters along the west wall, piping molten metal to the tank
    for z in (8, 20, 32):
        t.fill(3, floor, z - 2, 6, floor + 5, z + 2, "iron_block")
        t.set(6, floor + 2, z, "orange_stained_glass")
        t.set(5, floor + 2, z, "lava")
        t.set(6, floor + 1, z, "furnace[facing=east]")
        t.fill(7, floor + 6, z, c - 7, floor + 6, z, "waxed_cut_copper")
        t.fill(c - 7, floor + 6, z, c - 7, floor + 7, z, "waxed_cut_copper")
    # control desks with monitors along the east wall
    for z in range(6, 35, 4):
        t.set(36, floor, z, "gray_concrete")
        t.set(36, floor + 1, z, "light_blue_stained_glass")
    t.chest(36, floor, 4, "west", "weapon_x_lab")
    t.chest(4, floor, 36, "north", "weapon_x_vault")
    mob(t, 28, floor, 28, "xmen:lady_deathstrike", yaw=135)
    mob(t, 33, floor, 10, SCIENTIST, yaw=90)
    mob(t, 33, floor, 26, SCIENTIST, yaw=90)
    mob(t, 12, floor, 33, SOLDIER, yaw=180)
    mob(t, 30, floor, 35, SOLDIER, yaw=180)

    # --- the Weapon X program (X-Men: Apocalypse): Logan in his tank, helmet on, cables in his back
    cx, cz = 75, 18
    t.disc(cx, F, cz, 6, "black_concrete")
    t.disc(cx, F, cz, 6.4, "yellow_concrete", hollow=True)
    tank(t, cx, floor, cz, 2, 6)
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1), (0, 0)):
        t.fill(cx + dx, floor + 7, cz + dz, cx + dx, F + 11, cz + dz, "chain[axis=y]")
    for x in (cx - 6, cx + 6):
        t.fill(x, floor, cz - 1, x, floor + 4, cz + 1, "iron_block")
    for x in range(cx - 5, cx + 6):
        t.set(x, F + 11, cz, "chain[axis=x]")
    mob(t, cx, floor, cz, "xmen:weapon_x_logan", TANK, yaw=180)
    t.set(cx, floor, cz + 5, "xmen:weapon_x_tank_console[facing=south]")
    t.set(cx - 1, floor, cz + 5, "gray_concrete")
    t.set(cx + 1, floor, cz + 5, "gray_concrete")
    t.set(cx - 1, floor + 1, cz + 5, "light_blue_stained_glass")
    t.set(cx + 1, floor + 1, cz + 5, "light_blue_stained_glass")
    hazard(t, cx - 2, F, cz + 7, cx + 2, cz + 7)
    screens(t, 60, floor + 1, 2, 90, floor + 4, -1)
    # surgery bays along the walls: operating tables, needles, x-ray screens
    for x in range(61, 91, 6):
        t.fill(x, floor, 34, x + 1, floor, 34, "iron_block")
        t.set(x, floor + 1, 34, "white_carpet")
        t.set(x + 2, floor, 35, "brewing_stand")
        t.set(x, F + 11, 34, "end_rod[facing=down]")
        t.set(x, floor + 2, 37, "white_stained_glass")
        t.set(x + 1, floor + 2, 37, "white_stained_glass")
        t.set(x, floor + 2, 38, LIGHT)
        t.set(x + 1, floor + 2, 38, LIGHT)
    t.chest(91, floor, 25, "west", "weapon_x_lab")
    mob(t, 70, floor, 28, SCIENTIST, yaw=0)
    mob(t, 82, floor, 28, SCIENTIST, yaw=0)
    mob(t, 64, floor, 22, SOLDIER, yaw=90)
    mob(t, 87, floor, 22, SOLDIER, yaw=270)

    # --- Stryker's Cerebro (X2): a round chamber, Jason in his wheelchair in the middle
    ox, oy, oz = 20, F + 7, 76
    t.ellipsoid(ox, oy, oz, 15, 8, 15, "smooth_quartz")
    t.ellipsoid(ox, oy, oz, 14, 7, 14, "air")
    for y in range(oy - 6, oy + 7, 3):
        for x in range(ox - 15, ox + 16):
            for z in range(oz - 15, oz + 16):
                if t.get(x, y, z) == "minecraft:smooth_quartz" and (x + z) % 2 == 0:
                    t.set(x, y, z, LIGHT)
    t.clear(18, floor, 61, 22, oy + 3, 63)
    t.fill(19, oy - 1, 62, 21, oy - 1, oz - 3, "polished_andesite")   # the bridge
    t.fill(18, oy, 62, 18, oy, oz - 3, "iron_bars")
    t.fill(22, oy, 62, 22, oy, oz - 3, "iron_bars")
    t.disc(ox, oy - 1, oz, 3, "polished_andesite")                      # the platform in the middle
    t.disc(ox, oy, oz, 3.4, "iron_bars", hollow=True)
    t.clear(19, oy, oz - 3, 21, oy, oz - 3)
    # the bridge starts at the door; fill the gap between the corridor floor and the bridge with stairs
    for i in range(oy - 1 - F):
        t.fill(19, F + i, 61 - (oy - 1 - F) + i + 1, 21, F + i, 61 - (oy - 1 - F) + i + 1, "polished_andesite")
    t.set(ox, oy, oz, "smooth_stone_slab[type=bottom]")
    t.set(ox - 1, oy, oz, "iron_trapdoor[facing=west,half=bottom,open=true]")
    t.set(ox + 1, oy, oz, "iron_trapdoor[facing=east,half=bottom,open=true]")
    t.mob(ox, oy, oz, "xmen:jason_stryker", None,
          {"Pos": [nbt.Double(ox + 0.5), nbt.Double(oy + 0.5), nbt.Double(oz + 0.5)]}, 0.0)

    # --- genetics lab: rows of specimen tanks, lab benches
    for x in range(61, 87, 5):
        for z in (60, 88):
            t.fill(x, floor, z, x + 2, floor + 3, z + 2, "lime_stained_glass")
            t.fill(x + 1, floor, z + 1, x + 1, floor + 2, z + 1, "water")
            t.fill(x, floor + 4, z, x + 2, floor + 4, z + 2, "iron_block")
            t.set(x + 1, F, z + 1, LIGHT)
            if x % 2:
                t.set(x + 1, floor, z + 1, "skeleton_skull[rotation=0]")
    for z in (68, 74, 80):
        for x in range(63, 89):
            table(t, x, floor, z, "white_concrete")
        for x in range(64, 89, 4):
            t.set(x, floor + 1, z, "brewing_stand")
            t.set(x + 2, floor + 1, z, "flower_pot")
    t.chest(91, floor, 70, "west", "weapon_x_lab")
    t.chest(59, floor, 84, "east", "weapon_x_lab")
    mob(t, 70, floor, 71, SCIENTIST)
    mob(t, 80, floor, 77, SCIENTIST)
    mob(t, 85, floor, 66, SCIENTIST)
    mob(t, 64, floor, 83, SOLDIER, yaw=90)
    mob(t, 75, floor, 57, SOLDIER)
    mob(t, 47, floor, 43, SOLDIER)


# ----------------------------------------------------------------------------------------------- level 3
def level3(t):
    F = L3
    lv = Level(t, F, "deepslate_tiles", "polished_deepslate")
    lobby(lv, "deepslate_tiles")
    lv.space(3, 46, 39, 50)
    lv.space(56, 46, 92, 50)
    lv.space(8, 3, 87, 35, 11, "black_concrete", 8)      # Weapon XI's chamber
    lv.space(3, 58, 37, 92, 6, "smooth_quartz")          # Transigen wing
    lv.space(58, 58, 92, 92, 6)                           # the adamantium vault level
    lv.passage(44, 36, 51, 39, 5)
    lv.passage(18, 51, 22, 57, 4)
    lv.passage(73, 51, 77, 57, 4)
    lv.build()
    floor = F + 1
    for z in (46, 50):
        pipes(t, 3, 39, F + 5, z)
        pipes(t, 56, 92, F + 5, z)
    hazard(t, 44, F, 36, 51, 39)

    # --- Weapon XI's chamber: giant cooling pipes, catwalks, and his pod on a platform
    for x in range(14, 84, 12):
        for z in (9, 29):
            if abs(x - 48) < 8 and z == 9:
                continue
            t.cylinder(x, floor, z, 2, F + 11, "polished_deepslate")
            t.disc(x, floor + 4, z, 2.4, "yellow_concrete", hollow=True)
    for x in range(10, 87):
        t.set(x, F + 7, 4, "polished_deepslate_slab[type=top]")
        t.set(x, F + 7, 34, "polished_deepslate_slab[type=top]")
    t.disc(48, F, 12, 7, "iron_block")
    t.disc(48, floor, 12, 6, "polished_deepslate")
    t.disc(48, floor, 12, 6.4, "red_concrete", hollow=True)
    for x, z in ((42, 12), (54, 12), (48, 6), (48, 18)):
        t.set(x, floor, z, "shroomlight")
    t.cylinder(48, floor + 1, 12, 2, floor + 4, "red_stained_glass", hollow=True)
    t.disc(48, floor + 5, 12, 2, "iron_block")
    t.fill(48, floor + 6, 12, 48, F + 11, 12, "chain[axis=y]")
    t.mob(48, floor + 1, 12, "xmen:weapon_xi", None, TANK, 0.0)
    for x in (40, 56):
        t.fill(x, floor, 20, x, floor + 2, 20, "iron_block")
        t.set(x, floor + 3, 20, "shroomlight")
    t.chest(10, floor, 33, "east", "weapon_x_vault")
    t.chest(85, floor, 33, "west", "weapon_x_vault")
    mob(t, 40, floor, 32, SOLDIER, yaw=180)
    mob(t, 56, floor, 32, SOLDIER, yaw=180)

    # --- Transigen wing (Logan): white and clean, X-24's growth tank, the empty children's ward
    tank(t, 20, floor, 80, 3, 6, "lime_stained_glass")
    t.mob(20, floor, 80, "xmen:x24", None, TANK, 180.0)
    t.fill(13, floor, 73, 27, floor, 73, "gray_concrete")
    for x in range(14, 27, 3):
        t.set(x, floor + 1, 73, "light_blue_stained_glass")
    for x in range(5, 36, 4):
        t.bed(x, floor, 59, "north", "light_blue")
        t.set(x + 1, floor, 59, "white_carpet")
    for x, color in ((8, "red"), (14, "yellow"), (22, "lime"), (28, "light_blue")):
        t.fill(x, floor, 63, x + 2, floor, 65, f"{color}_carpet")
    t.set(34, floor, 63, "jukebox")
    t.chest(36, floor, 90, "west", "weapon_x_lab")
    mob(t, 10, floor, 85, SCIENTIST, yaw=90)
    mob(t, 32, floor, 70, SCIENTIST, yaw=270)

    # --- the adamantium vault: thick walls, a vault door, and the chests full of adamantium
    t.fill(66, floor, 66, 86, floor + 5, 86, "iron_block")
    t.clear(68, floor, 68, 84, floor + 4, 84)
    t.fill(68, F, 68, 84, F, 84, "polished_deepslate")
    t.clear(75, floor, 66, 76, floor + 1, 67)
    t.set(75, floor, 66, "iron_door[facing=south,half=lower,hinge=left,open=false]")
    t.set(75, floor + 1, 66, "iron_door[facing=south,half=upper,hinge=left,open=false]")
    t.set(76, floor, 66, "iron_door[facing=south,half=lower,hinge=right,open=false]")
    t.set(76, floor + 1, 66, "iron_door[facing=south,half=upper,hinge=right,open=false]")
    t.set(74, floor + 1, 65, "stone_button[face=wall,facing=north]")
    t.set(77, floor + 1, 68, "stone_button[face=wall,facing=south]")
    hazard(t, 72, F, 63, 79, 65)
    for x, z, facing in ((69, 70, "east"), (69, 82, "east"), (83, 70, "west"), (83, 82, "west"),
                         (76, 83, "north")):
        t.chest(x, floor, z, facing, "weapon_x_vault")
    for x, z in ((72, 76), (80, 76), (76, 79)):
        t.fill(x, floor, z, x + 1, floor + 1, z + 1, "iron_block")
    t.set(76, floor + 4, 76, LIGHT)
    mob(t, 70, floor, 62, SOLDIER, yaw=180)
    mob(t, 82, floor, 62, SOLDIER, yaw=180)


def build():
    t = Template("weapon_x", N, G + 17, N, seed=7)
    surface(t)
    level1(t)
    level2(t)
    level3(t)
    stairwell(t)
    elevator(t)
    # a soldier in each lobby
    mob(t, 44, L1 + 1, 43, SOLDIER)
    mob(t, 44, L3 + 1, 43, SOLDIER)
    return t
