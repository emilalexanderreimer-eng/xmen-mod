"""All the other X-Men places. Each function returns one Template."""
import math

from lib import Template, AIR

LIGHT = "sea_lantern"


# ======================================================================= Apocalypse Pyramid
def _colossus(t, x, y, z, facing="north", pedestal=2):
    """A standing stone statue of Apocalypse: crested helmet with fins, shoulder plates, cape. About 15 tall."""
    f = -1 if facing == "north" else 1          # which z side is his front
    t.fill(x - 3, y, z - 3, x + 3, y + pedestal - 1, z + 3, "chiseled_sandstone")
    t.fill(x - 3, y + pedestal - 1, z - 3, x + 3, y + pedestal - 1, z + 3, "cut_sandstone")
    y0 = y + pedestal
    stone, armor, skin = "smooth_sandstone", "cut_sandstone", "sandstone"
    for lx in (x - 2, x - 1, x + 1, x + 2):     # legs
        t.fill(lx, y0, z - 1, lx, y0 + 4, z + 1, armor)
    t.fill(x - 1, y0 + 1, z + f * 2, x + 1, y0 + 4, z + f * 2, "chiseled_sandstone")  # tabard
    t.fill(x - 2, y0 + 5, z - 1, x + 2, y0 + 9, z + 1, armor)   # chest
    t.fill(x - 2, y0 + 5, z - 1, x + 2, y0 + 5, z + 1, "gold_block")   # belt
    t.set(x, y0 + 7, z + f * 2, "gold_block")                  # chest emblem
    for ax in (x - 3, x + 3):                                    # arms
        t.fill(ax, y0 + 4, z, ax, y0 + 8, z, stone)
    for ax in (x - 4, x - 3, x + 3, x + 4):                     # shoulder plates
        t.fill(ax, y0 + 9, z - 1, ax, y0 + 9, z + 1, "chiseled_sandstone")
    t.fill(x - 1, y0 + 10, z - 1, x + 1, y0 + 12, z + 1, skin)  # head
    t.set(x - 1, y0 + 11, z + f * 2, "blue_terracotta")         # eyes
    t.set(x + 1, y0 + 11, z + f * 2, "blue_terracotta")
    t.set(x, y0 + 10, z + f * 2, skin)
    t.fill(x - 1, y0 + 13, z - 1, x + 1, y0 + 13, z + 1, "gold_block")   # helmet
    t.fill(x, y0 + 14, z - 2, x, y0 + 14, z + 2, "gold_block")           # crest
    t.set(x, y0 + 15, z, "gold_block")
    for sx in (x - 2, x + 2):                                            # fins
        t.fill(sx, y0 + 11, z, sx, y0 + 13, z - f, "gold_block")
    t.fill(x - 2, y0 + 1, z - f * 2, x + 2, y0 + 9, z - f * 2, "blue_terracotta")   # cape


def _obelisk(t, x, y, z, height=11):
    for h in range(height):
        state = "chiseled_sandstone" if h % 4 == 2 else "cut_sandstone"
        t.fill(x, y + h, z, x + 1, y + h, z + 1, state)
    t.fill(x, y + height, z, x + 1, y + height, z + 1, "gold_block")


def _stairs_ring(t, a, b, za, zb, y, mat):
    """The outer ring of one pyramid layer as stairs, so the sides look smooth like the real thing."""
    for x in range(a + 1, b):
        t.set(x, y, za, f"{mat}[facing=south,half=bottom,shape=straight]")
        t.set(x, y, zb, f"{mat}[facing=north,half=bottom,shape=straight]")
    for z in range(za + 1, zb):
        t.set(a, y, z, f"{mat}[facing=east,half=bottom,shape=straight]")
        t.set(b, y, z, f"{mat}[facing=west,half=bottom,shape=straight]")
    t.set(a, y, za, f"{mat}[facing=south,half=bottom,shape=outer_left]")
    t.set(b, y, za, f"{mat}[facing=south,half=bottom,shape=outer_right]")
    t.set(a, y, zb, f"{mat}[facing=north,half=bottom,shape=outer_right]")
    t.set(b, y, zb, f"{mat}[facing=north,half=bottom,shape=outer_left]")


def apocalypse_pyramid():
    """Modeled on the pyramid from the opening of X-Men: Apocalypse (2016): a huge smooth limestone pyramid with a
    gold capstone and a crystal tip that catches the sun. Inside, a long hall leads to the transference chamber
    where Apocalypse sleeps on his altar between gold veins and statues of himself. Under the floor is the empty
    upside-down pyramid that the altar falls into at the end of the movie's opening, with the treasure at the
    bottom. Four Horsemen (Storm, Psylocke, Archangel, Magneto) guard the corners."""
    G = 26
    SX, SY, SZ = 69, G + 36, 101
    CX, CZ = 34, 66
    TOP = 33
    t = Template("apocalypse_pyramid", SX, SY, SZ, seed=2)
    t.fill(0, G - 3, 0, SX - 1, G - 1, SZ - 1, "sandstone")
    t.fill(0, G, 0, SX - 1, G, SZ - 1, "sand")
    t.clear(0, G + 1, 0, SX - 1, SY - 1, SZ - 1)

    # ------------------------------------------------------------ the pyramid: 67 wide, smooth sides, gold top
    for i in range(TOP + 1):
        a, b, za, zb = CX - TOP + i, CX + TOP - i, CZ - TOP + i, CZ + TOP - i
        y = G + i
        if i >= TOP - 5:
            t.fill(a, y, za, b, y, zb, "gold_block")
        elif i == 0:
            t.fill(a, y, za, b, y, zb, "sandstone")
            t.walls(a, y, za, b, y, zb, "cut_sandstone")
        else:
            t.fill(a + 1, y, za + 1, b - 1, y, zb - 1, "sandstone")
            _stairs_ring(t, a, b, za, zb, y, "smooth_sandstone_stairs")
    # the crystal capstone: the sun shines through it straight down onto the altar
    for i in range(TOP - 5, TOP - 1):
        t.fill(CX - 1, G + i, CZ - 1, CX + 1, G + i, CZ + 1, "glass")
    t.fill(CX - 1, G + TOP - 1, CZ - 1, CX + 1, G + TOP - 1, CZ + 1, "light_blue_stained_glass")
    t.set(CX, G + TOP, CZ, "light_blue_stained_glass")
    t.clear(CX - 1, G + 17, CZ - 1, CX + 1, G + TOP - 6, CZ + 1)   # light shaft

    # ------------------------------------------------------------ the gate: an Egyptian pylon on the north face
    for h in range(16):
        s = h // 4
        t.fill(24 + s, G + h, 28, 44 - s, G + h, 46, "cut_sandstone" if h % 5 else "smooth_sandstone")
    t.fill(27, G + 15, 28, 41, G + 15, 46, "gold_block")
    t.clear(31, G + 1, 28, 37, G + 10, 49)
    t.fill(31, G, 28, 37, G, 49, "cut_sandstone")
    t.fill(30, G + 11, 28, 38, G + 11, 28, "chiseled_sandstone")
    # winged sun disc over the gate
    t.fill(33, G + 12, 28, 35, G + 14, 28, "gold_block")
    t.fill(28, G + 13, 28, 32, G + 13, 28, "blue_terracotta")
    t.fill(36, G + 13, 28, 40, G + 13, 28, "blue_terracotta")
    t.fill(29, G + 12, 28, 32, G + 12, 28, "light_blue_terracotta")
    t.fill(36, G + 12, 28, 39, G + 12, 28, "light_blue_terracotta")
    # hieroglyph walls and gold veins along the gate hall
    for z in range(29, 50):
        for x in (30, 38):
            for y in range(G + 1, G + 10):
                t.set(x, y, z, "chiseled_sandstone" if (y + z) % 3 == 0 else "cut_sandstone")
            if z % 4 == 1:
                t.fill(x, G + 1, z, x, G + 9, z, "xmen:celestial_vein[lit=false]")
    t.fill(34, G, 30, 34, G, 49, "xmen:celestial_vein[lit=false]")

    # ------------------------------------------------------------ avenue of obelisks and the two colossi
    t.fill(29, G, 0, 39, G, 27, "cut_sandstone")
    t.fill(31, G, 0, 37, G, 27, "smooth_sandstone")
    for z in range(1, 27, 4):
        t.fill(31, G, z, 37, G, z, "chiseled_sandstone")
    for z in (2, 9, 16):
        _obelisk(t, 25, G + 1, z)
        _obelisk(t, 42, G + 1, z)
    _colossus(t, 26, G + 1, 23, "north")
    _colossus(t, 42, G + 1, 23, "north")

    # ------------------------------------------------------------ ring hall around the chamber
    t.clear(19, G + 1, 50, 49, G + 6, 82)
    t.fill(22, G + 1, 53, 46, G + 6, 79, "sandstone")
    t.fill(19, G, 50, 49, G, 82, "cut_sandstone")
    for p in range(20, 49, 4):
        for (x, z) in ((p, 49), (p, 83)):
            if z == 49 and 30 <= x <= 38:
                continue
            t.fill(x, G + 1, z, x, G + 6, z, "xmen:celestial_vein[lit=false]")
    for p in range(52, 82, 4):
        for (x, z) in ((18, p), (50, p)):
            t.fill(x, G + 1, z, x, G + 6, z, "xmen:celestial_vein[lit=false]")

    # ------------------------------------------------------------ the transference chamber
    t.room(24, G, 56, 44, G + 17, 76, "chiseled_sandstone", "cut_sandstone", "smooth_sandstone")
    t.clear(CX - 1, G + 17, CZ - 1, CX + 1, G + 17, CZ + 1)
    for x in range(25, 44):
        for z in range(57, 76):
            if (x + z) % 2 == 0:
                t.set(x, G, z, "smooth_sandstone")
    for z in range(57, 76):
        for y in range(G + 1, G + 17):
            for x in (24, 44):
                t.set(x, y, z, "chiseled_sandstone" if (y + z) % 4 else "cut_sandstone")
    for x in range(25, 44):
        for y in range(G + 1, G + 17):
            for z in (56, 76):
                t.set(x, y, z, "chiseled_sandstone" if (y + x) % 4 else "cut_sandstone")
    for p in range(26, 44, 3):      # gold veins running up the walls
        t.fill(p, G + 1, 56, p, G + 16, 56, "xmen:celestial_vein[lit=false]")
        t.fill(p, G + 1, 76, p, G + 16, 76, "xmen:celestial_vein[lit=false]")
    for p in range(58, 76, 3):
        t.fill(24, G + 1, p, 24, G + 16, p, "xmen:celestial_vein[lit=false]")
        t.fill(44, G + 1, p, 44, G + 16, p, "xmen:celestial_vein[lit=false]")
    t.fill(25, G + 16, 57, 43, G + 16, 57, "blue_terracotta")
    t.fill(25, G + 16, 75, 43, G + 16, 75, "blue_terracotta")
    t.fill(25, G + 16, 57, 25, G + 16, 75, "blue_terracotta")
    t.fill(43, G + 16, 57, 43, G + 16, 75, "blue_terracotta")
    # doors from the ring hall
    t.clear(32, G + 1, 53, 36, G + 7, 56)
    t.clear(33, G + 1, 76, 35, G + 4, 79)
    t.clear(22, G + 1, 65, 24, G + 4, 67)
    t.clear(44, G + 1, 65, 46, G + 4, 67)
    # gold veins in the floor, all running to the altar
    t.fill(CX, G, 50, CX, G, 75, "xmen:celestial_vein[lit=false]")
    t.fill(25, G, CZ, 43, G, CZ, "xmen:celestial_vein[lit=false]")
    for d in range(4, 9):
        for sx in (-1, 1):
            for sz in (-1, 1):
                t.set(CX + sx * d, G, CZ + sz * d, "xmen:celestial_vein[lit=false]")
    # the dais with its two altars; its middle is made of veins (it gives way when he dies)
    t.fill(CX - 3, G + 1, CZ - 3, CX + 3, G + 1, CZ + 3, "cut_sandstone")
    _stairs_ring(t, CX - 4, CX + 4, CZ - 4, CZ + 4, G + 1, "smooth_sandstone_stairs")
    t.fill(CX - 2, G + 1, CZ - 2, CX + 2, G + 1, CZ + 2, "xmen:celestial_vein[lit=false]")
    for ax in (CX - 3, CX + 3):
        t.fill(ax, G + 2, CZ - 1, ax, G + 2, CZ + 1, "smooth_quartz")
        t.fill(ax, G + 3, CZ - 1, ax, G + 3, CZ + 1, "smooth_quartz_slab[type=bottom]")
    # four statues of him watching over the altar
    _colossus(t, 28, G + 1, 60, "south", pedestal=1)
    _colossus(t, 40, G + 1, 60, "south", pedestal=1)
    _colossus(t, 28, G + 1, 72, "north", pedestal=1)
    _colossus(t, 40, G + 1, 72, "north", pedestal=1)
    t.mob(CX, G + 2, CZ, "xmen:apocalypse", extra={"BeamHeight": TOP - 2}, yaw=180.0)

    # ------------------------------------------------------------ the Four Horsemen's rooms
    rooms = [("xmen:storm", 10, 46, 18, 50, ("white_terracotta", "light_blue_terracotta")),
             ("xmen:psylocke", 50, 46, 50, 50, ("purple_terracotta", "black_terracotta")),
             ("xmen:archangel", 10, 77, 18, 79, ("blue_terracotta", "light_gray_terracotta")),
             ("xmen:horseman_magneto", 50, 77, 50, 79, ("gray_terracotta", "red_terracotta"))]
    for mob, x1, z1, door_x, door_z, (floor_a, floor_b) in rooms:
        x2, z2 = x1 + 8, z1 + 10
        t.room(x1, G, z1, x2, G + 9, z2, "chiseled_sandstone", "cut_sandstone", "smooth_sandstone")
        for x in range(x1 + 1, x2):
            for z in range(z1 + 1, z2):
                t.set(x, G, z, floor_a if (x + z) % 2 else floor_b)
        t.clear(door_x, G + 1, door_z, door_x, G + 3, door_z + 2)
        for (vx, vz) in ((x1 + 1, z1 + 1), (x2 - 1, z1 + 1), (x1 + 1, z2 - 1), (x2 - 1, z2 - 1)):
            t.fill(vx, G + 1, vz, vx, G + 8, vz, "xmen:celestial_vein[lit=false]")
        far_x = x1 + 1 if x1 < CX else x2 - 1
        t.chest(far_x, G + 1, (z1 + z2) // 2, "east" if x1 < CX else "west", "pyramid")
        t.mob((x1 + x2) // 2, G + 1, (z1 + z2) // 2, mob, yaw=90.0 if x1 < CX else 270.0)
    # Storm's thunderclouds, Psylocke's crystals, Archangel's blades, Magneto's metal
    t.fill(11, G + 8, 47, 17, G + 8, 55, "white_wool")
    t.scatter(51, G + 1, 47, 57, G + 1, 55, "amethyst_cluster[facing=up,waterlogged=false]", 0.08,
              lambda s: s is None or s == "minecraft:air")
    t.fill(11, G + 8, 78, 17, G + 8, 86, "iron_bars")
    t.fill(52, G + 1, 78, 57, G + 3, 78, "iron_block")
    t.fill(52, G + 1, 86, 57, G + 3, 86, "iron_block")

    # ------------------------------------------------------------ Clan Akkaba, his worshippers
    for (x, z) in ((32, 36), (36, 42), (20, 66), (48, 66), (30, 81), (38, 51)):
        t.mob(x, G + 1, z, "xmen:akkaba_cultist", yaw=180.0)

    # ------------------------------------------------------------ the treasure vault under the altar
    # Under the dais is a gold seal. When Apocalypse dies it crumbles and opens a wide staircase down to a vault
    # with a lava lake; the treasure sits on an island you reach by jumping across stepping stones.
    t.fill(CX - 2, G, CZ - 2, CX + 2, G, CZ + 2, "xmen:celestial_vein[lit=false]")
    F = 12
    steps = G - 1 - F                       # 13 steps down
    for k in range(steps):
        z, y = CZ - 2 + k, G - 1 - k
        t.fill(CX - 3, y - 1, z, CX + 3, min(G - 1, y + 5), z, "smooth_sandstone")
    t.room(20, F, CZ + steps - 3, 48, F + 10, CZ + 30, "chiseled_sandstone", "cut_sandstone", "smooth_sandstone")
    for k in range(steps):
        z, y = CZ - 2 + k, G - 1 - k
        t.fill(CX - 2, y, z, CX + 2, y, z, "smooth_sandstone_stairs[facing=north,half=bottom,shape=straight]")
        t.clear(CX - 2, y + 1, z, CX + 2, min(G - 1, y + 4), z)
    zl = CZ + steps - 2                     # first row of the vault floor
    # the lava lake and the island
    t.fill(21, F - 2, zl + 4, 47, F - 2, CZ + 29, "sandstone")
    t.fill(21, F - 1, zl + 4, 47, F, CZ + 29, "lava")
    ix, iz = CX, zl + 12
    t.disc(ix, F - 1, iz, 4, "chiseled_sandstone")
    t.disc(ix, F, iz, 4, "chiseled_sandstone")
    t.disc(ix, F + 1, iz, 3, "gold_block")
    t.set(ix, F + 2, iz, "xmen:celestial_vein[lit=false]")
    for (x, z, loot) in ((ix - 2, iz, "pyramid_treasure"), (ix + 2, iz, "pyramid_treasure"),
                         (ix, iz - 2, "pyramid_treasure"), (ix, iz + 2, "pyramid")):
        t.chest(x, F + 2, z, "north" if z >= iz else "south", loot)
    for (x, z) in ((CX - 1, zl + 5), (CX + 1, zl + 7)):      # stepping stones over the lava
        t.fill(x, F - 1, z, x, F, z, "chiseled_sandstone")
    # gold veins up the walls light the vault
    for x in range(22, 48, 4):
        for z in (CZ + steps - 3, CZ + 30):
            if z == CZ + steps - 3 and CX - 3 <= x <= CX + 3:
                continue
            t.fill(x, F + 1, z, x, F + 9, z, "xmen:celestial_vein[lit=false]")
    for z in range(zl + 1, CZ + 30, 4):
        for x in (20, 48):
            t.fill(x, F + 1, z, x, F + 9, z, "xmen:celestial_vein[lit=false]")
    t.clear(CX - 2, F + 2, CZ + steps - 3, CX + 2, F + 5, CZ + steps - 3)
    return t


# ======================================================================= Fallen Sentinel
def fallen_sentinel():
    t = Template("fallen_sentinel", 44, 14, 22, seed=3)
    G = 4
    body, dark, face = "purple_concrete", "gray_concrete", "light_gray_concrete"
    # lying on its back, head to the west, half sunk into the ground
    t.ellipsoid(8, G + 2, 11, 5, 4, 5, face)          # head
    t.set(5, G + 4, 9, "red_concrete")
    t.set(5, G + 4, 13, "red_concrete")
    t.fill(4, G + 2, 9, 4, G + 2, 13, "black_concrete")
    t.ellipsoid(8, G + 6, 11, 4, 1, 4, body)          # head top
    t.fill(13, G, 6, 26, G + 5, 16, body)             # chest
    t.fill(14, G + 5, 8, 25, G + 6, 14, dark)
    t.fill(16, G + 6, 10, 18, G + 7, 12, "red_concrete")   # chest light
    t.fill(13, G - 1, 2, 24, G + 2, 5, body)          # arms
    t.fill(13, G - 1, 17, 24, G + 2, 20, body)
    t.fill(24, G - 1, 1, 27, G + 2, 5, dark)          # hands
    t.fill(24, G - 1, 17, 27, G + 2, 21, dark)
    t.fill(27, G, 7, 41, G + 3, 10, body)             # legs
    t.fill(27, G, 12, 41, G + 3, 15, body)
    t.fill(40, G, 6, 43, G + 5, 10, dark)             # feet
    t.fill(40, G, 12, 43, G + 5, 16, dark)
    # broken: holes, rust and moss
    t.ruin(0, G, 0, 43, G + 8, 21, 0.12)
    t.scatter(0, G, 0, 43, G + 8, 21, "magenta_concrete", 0.08, lambda s: s and "purple" in s)
    t.scatter(0, G, 0, 43, G + 8, 21, "moss_block", 0.05, lambda s: s and "concrete" in s)
    # a way into the chest, with loot inside
    t.clear(15, G + 1, 8, 24, G + 4, 14)
    t.clear(14, G + 1, 10, 14, G + 2, 12)
    t.fill(15, G, 8, 24, G, 14, "iron_block")
    t.chest(20, G + 1, 11, "west", "fallen_sentinel")
    t.set(23, G + 1, 9, "redstone_block")
    t.set(23, G + 1, 13, "redstone_lamp[lit=true]")
    t.set(17, G + 4, 11, "redstone_lamp[lit=true]")
    # the ground around it
    for x in range(0, 44):
        for z in range(0, 22):
            if t.rand.random() < 0.15 and t.get(x, G, z) is None:
                t.set(x, G, z, "coarse_dirt")
    return t


# ======================================================================= Brotherhood Hideout
def brotherhood_hideout():
    G = 3
    t = Template("brotherhood_hideout", 32, G + 12, 26, seed=4)
    t.fill(0, G - 2, 0, 31, G, 25, "packed_mud")
    t.clear(0, G + 1, 0, 31, G + 11, 25)
    t.fill(0, G, 0, 31, G, 25, "gravel")
    # warehouse
    t.room(2, G, 2, 29, G + 9, 23, "gray_concrete", "polished_andesite", "dark_oak_planks")
    for x in range(2, 30, 4):
        t.fill(x, G + 1, 2, x, G + 8, 2, "iron_block")
        t.fill(x, G + 1, 23, x, G + 8, 23, "iron_block")
    t.fill(3, G + 9, 3, 28, G + 9, 22, "dark_oak_planks")
    for x in range(4, 28, 3):
        t.fill(x, G + 6, 2, x + 1, G + 7, 2, "red_stained_glass")
        t.fill(x, G + 6, 23, x + 1, G + 7, 23, "red_stained_glass")
    # big sliding door
    t.clear(13, G + 1, 2, 18, G + 5, 2)
    t.fill(13, G + 6, 2, 18, G + 6, 2, "red_concrete")
    # the Brotherhood's helmet sign (red)
    for x, y in ((14, 8), (15, 9), (16, 9), (17, 8)):
        t.set(x, G + y - 1, 1, "red_concrete")
    # crates
    for x, z in ((4, 5), (5, 5), (4, 6), (25, 18), (26, 18), (26, 19), (4, 19), (4, 20)):
        t.set(x, G + 1, z, "barrel[facing=up]")
    t.barrel(5, G + 2, 5, "brotherhood")
    t.barrel(26, G + 2, 18, "brotherhood")
    t.fill(10, G + 1, 12, 21, G + 1, 14, "dark_oak_planks")
    t.fill(10, G + 2, 12, 21, G + 2, 14, "red_carpet")
    t.chest(15, G + 1, 21, "north", "brotherhood")
    t.chest(16, G + 1, 21, "north", "brotherhood")
    # upstairs office on a platform
    t.fill(20, G + 4, 16, 28, G + 4, 22, "dark_oak_planks")
    for i in range(4):
        t.set(19 - i, G + 1 + i, 19, "dark_oak_stairs[facing=east]")
        t.fill(19 - i, G + 1, 19, 19 - i, G + i, 19, "dark_oak_planks")
    for x in range(20, 29):
        t.set(x, G + 5, 16, "iron_bars")
    t.chest(27, G + 5, 21, "west", "brotherhood")
    t.set(24, G + 5, 21, "cartography_table")
    for x in range(6, 27, 6):
        for z in (8, 17):
            t.set(x, G + 8, z, "lantern[hanging=true]")
    # members
    for kind, x, z in (("pyro", 8, 10), ("avalanche", 22, 8), ("blob", 15, 17), ("toad", 9, 18),
                       ("mystique", 24, 19), ("thug", 12, 6), ("thug", 20, 12)):
        y = G + 5 if kind == "mystique" else G + 1
        t.mob(x, y, z, "vindicator", kind) if kind == "thug" else t.npc(x, y, z, kind)
    return t


# ======================================================================= Asteroid M
def asteroid_m():
    N = 47
    t = Template("asteroid_m", N, 40, N, seed=5)
    c = N // 2
    rock = ["stone", "stone", "andesite", "tuff", "cobblestone", "iron_ore", "deepslate"]
    # the floating rock, flat on top
    for y in range(0, 16):
        r = 6 + (y / 15) ** 0.7 * 16
        for x in range(N):
            for z in range(N):
                wobble = math.sin(x * 0.7) * 1.2 + math.cos(z * 0.6) * 1.2
                if math.hypot(x - c, z - c) <= r + wobble:
                    t.set(x, y, z, t.rand.choice(rock))
    t.disc(c, 15, c, 21, "polished_andesite")
    # citadel walls with towers
    t.cylinder(c, 16, c, 15, 22, "iron_block", hollow=True)
    t.cylinder(c, 16, c, 14, 22, AIR)
    t.disc(c, 23, c, 15, "polished_deepslate")
    t.disc(c, 23, c, 13, "purple_stained_glass")
    for ang in range(0, 360, 60):
        x = round(c + math.cos(math.radians(ang)) * 15)
        z = round(c + math.sin(math.radians(ang)) * 15)
        t.cylinder(x, 16, z, 2, 30, "polished_deepslate")
        t.set(x, 31, z, "lightning_rod")
        t.set(x, 30, z, "redstone_lamp[lit=true]")
    t.clear(c - 1, 16, c - 15, c + 1, 19, c - 13)   # gate
    # inner hall
    t.disc(c, 15, c, 14, "smooth_stone")
    t.disc(c, 15, c, 6, "red_concrete")
    t.disc(c, 15, c, 4, "purple_concrete")
    for ang in range(0, 360, 45):
        x = round(c + math.cos(math.radians(ang)) * 10)
        z = round(c + math.sin(math.radians(ang)) * 10)
        t.fill(x, 16, z, x, 22, z, "iron_block")
        t.set(x, 21, z, LIGHT)
    # Magneto's tower on the roof (just for looks) and his dais in the middle of the hall
    t.cylinder(c, 24, c, 5, 32, "iron_block", hollow=True)
    t.disc(c, 23, c, 5, "polished_deepslate")
    t.disc(c, 33, c, 6, "polished_deepslate")
    t.set(c, 34, c, "lightning_rod")
    t.disc(c, 16, c, 3, "red_concrete")
    t.disc(c, 17, c, 2, "purple_concrete")
    t.fill(c - 1, 18, c + 2, c + 1, 19, c + 2, "red_concrete")
    t.set(c, 20, c + 2, "red_concrete")
    t.mob(c, 19, c, "xmen:magneto")   # the real Magneto (Java: entity/MagnetoBoss), floating over his dais
    t.chest(c - 4, 16, c, "east", "asteroid_m")
    t.chest(c + 4, 16, c - 1, "west", "asteroid_m")
    t.chest(c - 11, 16, c + 4, "east", "asteroid_m")
    t.chest(c + 11, 16, c - 4, "west", "asteroid_m")
    # metal scraps floating around it
    for i in range(10):
        ang = t.rand.random() * math.tau
        r = 22 + t.rand.random() * 1.5
        x, z = round(c + math.cos(ang) * r), round(c + math.sin(ang) * r)
        y = 8 + t.rand.randint(0, 12)
        t.fill(x, y, z, x + t.rand.randint(0, 1), y, z + t.rand.randint(0, 1),
               t.rand.choice(["iron_block", "raw_iron_block", "stone"]))
    return t


# ======================================================================= Sentinel Factory
def sentinel_factory():
    G = 3
    t = Template("sentinel_factory", 64, G + 30, 48, seed=6)
    t.fill(0, G - 2, 0, 63, G, 47, "stone")
    t.fill(0, G, 0, 63, G, 47, "gray_concrete_powder")
    t.clear(0, G + 1, 0, 63, G + 29, 47)
    # fence around the yard
    for x in range(0, 64):
        t.fill(x, G + 1, 0, x, G + 2, 0, "iron_bars")
        t.fill(x, G + 1, 47, x, G + 2, 47, "iron_bars")
    for z in range(0, 48):
        t.fill(0, G + 1, z, 0, G + 2, z, "iron_bars")
        t.fill(63, G + 1, z, 63, G + 2, z, "iron_bars")
    t.clear(28, G + 1, 0, 35, G + 2, 0)
    # main hall
    t.room(4, G, 6, 59, G + 16, 41, "light_gray_concrete", "smooth_stone", "gray_concrete")
    for x in range(4, 60, 5):
        t.fill(x, G + 1, 6, x, G + 15, 6, "iron_block")
        t.fill(x, G + 1, 41, x, G + 15, 41, "iron_block")
    for x in range(6, 58, 5):
        t.fill(x, G + 10, 6, x + 2, G + 13, 6, "glass")
        t.fill(x, G + 10, 41, x + 2, G + 13, 41, "glass")
    t.clear(29, G + 1, 6, 34, G + 7, 6)
    t.fill(29, G + 8, 6, 34, G + 8, 6, "yellow_concrete")
    # "TRASK" stripe
    t.fill(10, G + 15, 5, 53, G + 15, 5, "red_concrete")
    # smokestacks
    for x in (12, 26, 40, 52):
        t.cylinder(x, G + 16, 44, 2, G + 28, "bricks", hollow=True)
        t.set(x, G + 16, 44, "campfire[lit=true]")
    for x in (12, 26, 40, 52):
        t.cylinder(x, G + 1, 44, 2, G + 15, "bricks")
    # assembly line: conveyor with Sentinel parts
    t.fill(8, G, 18, 55, G, 20, "black_concrete")
    for x in range(8, 56):
        t.set(x, G + 1, 19, "rail[shape=east_west]")
    for x in range(10, 54, 8):
        t.fill(x, G + 1, 17, x + 2, G + 3, 17, "purple_concrete")    # torsos waiting on the line
        t.set(x + 1, G + 4, 17, "light_gray_concrete")
        t.fill(x, G + 6, 18, x + 2, G + 6, 20, "iron_block")         # robot arms above
        t.set(x + 1, G + 5, 19, "chain[axis=y]")
        t.fill(x + 1, G + 7, 19, x + 1, G + 15, 19, "chain[axis=y]")
    # big Sentinel skeletons being built
    for x in (14, 44):
        t.fill(x, G + 1, 30, x + 1, G + 6, 31, "iron_block")
        t.fill(x + 4, G + 1, 30, x + 5, G + 6, 31, "iron_block")
        t.fill(x, G + 7, 29, x + 5, G + 12, 32, "purple_concrete")
        t.fill(x + 1, G + 13, 29, x + 4, G + 15, 32, "light_gray_concrete")
        t.set(x + 1, G + 14, 29, "red_concrete")
        t.set(x + 4, G + 14, 29, "red_concrete")
        for y in range(G + 1, G + 16, 3):
            t.set(x - 2, y, 30, "scaffolding[bottom=false,distance=0,waterlogged=false]")
    # offices and the control room
    t.fill(5, G + 6, 33, 58, G + 6, 40, "smooth_stone_slab[type=top]")
    for x in range(5, 59):
        t.set(x, G + 7, 33, "iron_bars")
    for i in range(6):
        t.set(57, G + 1 + i, 27 + i, "polished_andesite_stairs[facing=south]")
        t.fill(57, G + 1, 27 + i, 57, G + i, 27 + i, "polished_andesite")
    t.clear(57, G + 7, 33, 57, G + 8, 33)
    t.fill(30, G + 7, 39, 36, G + 8, 40, "black_stained_glass")
    t.chest(10, G + 7, 39, "north", "sentinel_factory")
    t.chest(50, G + 7, 39, "north", "sentinel_factory")
    t.chest(6, G + 1, 8, "south", "sentinel_factory")
    t.chest(58, G + 1, 8, "south", "sentinel_factory")
    for x in range(8, 57, 8):
        for z in (12, 25, 37):
            t.set(x, G + 15, z, "lantern[hanging=true]")
    # guards, small Sentinels and the giant prototype
    for x, z in ((20, 10), (42, 10), (32, 24), (10, 36), (54, 36)):
        t.mob(x, G + 1, z, "pillager", "trask_guard")
    for x, z in ((24, 26), (38, 26), (30, 36)):
        t.mob(x, G + 1, z, "iron_golem", "sentinel")
    t.mob(32, G + 1, 30, "iron_golem", "giant_sentinel")
    for x, z in ((20, 37), (44, 37)):
        t.mob(x, G + 7, z, "pillager", "trask_guard")
    return t


# ======================================================================= Weapon X
def weapon_x():
    """The big underground Weapon X facility lives in its own file."""
    import weapon_x_lab
    return weapon_x_lab.build()


# ======================================================================= Mister Sinister's lab
def sinister_lab():
    t = Template("sinister_lab", 42, 14, 42, seed=8)
    t.room(0, 0, 0, 41, 13, 41, "polished_blackstone_bricks", "polished_blackstone", "polished_blackstone_bricks")
    t.clear(1, 1, 1, 40, 12, 40)
    t.scatter(0, 0, 0, 41, 13, 41, "cracked_polished_blackstone_bricks", 0.15,
              lambda s: s == "minecraft:polished_blackstone_bricks")
    t.fill(1, 0, 1, 40, 0, 40, "polished_blackstone")
    # dim red light from the ceiling
    for x in range(4, 39, 6):
        for z in range(4, 39, 6):
            t.set(x, 13, z, "shroomlight" if (x + z) % 12 == 8 else "redstone_lamp[lit=true]")
    # pipes along the walls
    for z in (2, 39):
        t.fill(1, 10, z, 40, 10, z, "iron_block")
        t.fill(1, 11, z, 40, 11, z, "polished_basalt[axis=x]")
    # clone tanks along the side walls: a lit base, glass all around, a lid and pipes into the ceiling.
    # Each one holds a sleeping clone of Sinister (Java: entity/SinisterClone) that smashes out when he calls it.
    tanks = [(3, 4), (3, 12), (3, 26), (3, 34), (36, 4), (36, 12), (36, 26), (36, 34)]
    for x, z in tanks:
        t.fill(x, 1, z, x + 2, 1, z + 2, "polished_blackstone")
        t.set(x + 1, 1, z + 1, "sea_lantern")
        t.fill(x, 2, z, x + 2, 4, z + 2, "red_stained_glass")
        t.clear(x + 1, 2, z + 1, x + 1, 4, z + 1)
        t.fill(x, 5, z, x + 2, 5, z + 2, "iron_block")
        t.set(x + 1, 5, z + 1, "redstone_lamp[lit=true]")
        t.fill(x + 1, 6, z + 1, x + 1, 12, z + 1, "chain[axis=y]")
        yaw = -90.0 if x < 20 else 90.0   # facing into the room
        t.mob(x + 1, 2, z + 1, "xmen:sinister_clone", extra={"Dormant": True, "NoAI": True}, yaw=yaw)
    # operating tables where the spawners used to be, with lamps hanging over them
    for x, z in ((10, 20), (31, 20)):
        t.fill(x - 1, 1, z, x + 1, 1, z, "smooth_stone_slab[type=top]")
        t.set(x - 2, 1, z, "anvil[facing=east]")
        t.set(x + 2, 1, z, "brewing_stand")
        t.fill(x, 8, z, x, 12, z, "chain[axis=y]")
        t.set(x, 7, z, "lantern[hanging=true]")
        t.set(x, 2, z, "red_carpet")
    # Sinister's dais in the middle: a raised black disc with his red diamond, and red light around it
    t.disc(20.5, 0, 20.5, 8, "crimson_nylium")
    t.disc(20.5, 0, 20.5, 7, "polished_blackstone")
    t.disc(20.5, 1, 20.5, 4, "polished_blackstone_bricks")
    t.disc(20.5, 1, 20.5, 4, "polished_blackstone", hollow=True)
    for i in range(4):
        for dx, dz in ((i, 3 - i), (-1 - i, 3 - i), (i, -4 + i), (-1 - i, -4 + i)):
            t.set(21 + dx, 1, 21 + dz, "red_concrete")
    t.set(20, 1, 20, "red_concrete")
    t.set(21, 1, 20, "red_concrete")
    t.set(20, 1, 21, "red_concrete")
    t.set(21, 1, 21, "red_concrete")
    for x, z in ((15, 15), (26, 15), (15, 26), (26, 26)):
        t.fill(x, 1, z, x, 4, z, "polished_blackstone_wall")
        t.set(x, 5, z, "redstone_lamp[lit=true]")
    # walkways shaped like his diamond on the floor around the dais
    for i in range(6):
        t.set(20 - i, 0, 8 + i, "crying_obsidian")
        t.set(21 + i, 0, 8 + i, "crying_obsidian")
        t.set(20 - i, 0, 33 - i, "crying_obsidian")
        t.set(21 + i, 0, 33 - i, "crying_obsidian")
    # Sinister himself (Java: entity/SinisterBoss), standing on his dais and facing the entrance
    t.mob(20, 2, 21, "xmen:mister_sinister", yaw=180.0)
    # his desk, his gene archive (rows of samples) and chests
    t.fill(17, 1, 36, 24, 1, 37, "polished_blackstone_slab[type=top]")
    t.set(20, 1, 38, "polished_blackstone_stairs[facing=north]")
    t.set(18, 2, 36, "brewing_stand")
    t.set(23, 2, 36, "lectern[facing=north]")
    t.chest(17, 1, 39, "north", "sinister_lab")
    t.chest(24, 1, 39, "north", "sinister_lab")
    t.chest(39, 1, 20, "west", "sinister_lab")
    # gene archive along the north wall: a counter full of samples
    for x in range(9, 33):
        if x not in (19, 20, 21, 22):
            t.set(x, 1, 1, "polished_blackstone_slab[type=top]")
    for x in range(9, 33):
        if x not in (19, 20, 21, 22):
            t.set(x, 2, 1, "brewing_stand" if x % 2 else "red_stained_glass_pane")
    # escape tunnel heading up (ends in a dead end of stone, players dig the rest)
    t.clear(20, 1, 0, 21, 3, 1)
    return t


# ======================================================================= Hellfire Club
def hellfire_club():
    G = 2
    t = Template("hellfire_club", 36, G + 18, 30, seed=9)
    t.fill(0, G - 2, 0, 35, G, 29, "dirt")
    t.fill(0, G, 0, 35, G, 29, "grass_block")
    t.clear(0, G + 1, 0, 35, G + 17, 29)
    t.room(3, G, 5, 32, G + 12, 26, "deepslate_bricks", "dark_oak_planks", "deepslate_bricks")
    t.fill(4, G + 6, 6, 31, G + 6, 25, "dark_oak_planks")
    for x in range(5, 31, 4):
        for y in (G + 2, G + 8):
            t.fill(x, y, 5, x + 1, y + 2, 5, "red_stained_glass")
            t.fill(x, y, 26, x + 1, y + 2, 26, "red_stained_glass")
    # roof
    for i in range(7):
        t.fill(2 + i, G + 13 + i, 4 + i, 33 - i, G + 13 + i, 27 - i, "blackstone")
        if i < 6:
            t.clear(3 + i, G + 13 + i, 5 + i, 32 - i, G + 13 + i, 26 - i)
    # entrance with red carpet and torches
    t.fill(16, G, 0, 19, G, 4, "red_wool")
    t.clear(17, G + 1, 5, 18, G + 3, 5)
    t.door(17, G + 1, 5, "south", "dark_oak", "left")
    t.door(18, G + 1, 5, "south", "dark_oak", "right")
    for x in (15, 20):
        t.fill(x, G + 1, 4, x, G + 3, 4, "blackstone_wall")
        t.set(x, G + 4, 4, "soul_lantern[hanging=false]")
    # ground floor: ballroom
    t.fill(4, G, 6, 31, G, 25, "black_concrete")
    t.fill(10, G, 10, 25, G, 21, "red_wool")
    for x, z in ((12, 15), (23, 15)):
        t.fill(x, G + 5, z, x + 1, G + 5, z + 1, "gold_block")
        t.set(x, G + 4, z, "lantern[hanging=true]")
    for x, z in ((6, 8), (29, 8), (6, 23), (29, 23)):
        t.table(x, G + 1, z, "red_carpet", "dark_oak_fence")
    t.chest(5, G + 1, 15, "east", "hellfire")
    # stairs up
    for i in range(6):
        t.set(30, G + 1 + i, 13 + i, "dark_oak_stairs[facing=south]")
        t.fill(30, G + 1, 13 + i, 30, G + i, 13 + i, "dark_oak_planks")
        t.clear(30, G + 6, 13 + i, 30, G + 6, 13 + i)
    # upstairs: the Inner Circle's room
    t.fill(4, G + 6, 6, 29, G + 6, 25, "red_wool")
    t.fill(16, G + 7, 6, 16, G + 11, 25, "deepslate_bricks")
    t.clear(16, G + 7, 15, 16, G + 8, 16)
    for z in range(9, 23, 3):
        t.set(8, G + 7, z, "dark_oak_stairs[facing=east]")
        t.set(12, G + 7, z, "dark_oak_stairs[facing=west]")
    t.fill(10, G + 7, 9, 10, G + 7, 22, "dark_oak_fence")
    t.fill(10, G + 8, 9, 10, G + 8, 22, "black_carpet")
    t.fill(20, G + 7, 22, 28, G + 7, 24, "white_concrete")      # the White Queen's throne
    t.set(24, G + 8, 24, "quartz_stairs[facing=north]")
    t.chest(27, G + 7, 7, "south", "hellfire")
    t.chest(21, G + 7, 7, "south", "hellfire")
    for x in range(6, 30, 6):
        t.set(x, G + 11, 15, "lantern[hanging=true]")
    t.npc(24, G + 8, 22, "white_queen", yaw=180.0)
    for x, z in ((8, 12), (26, 12), (18, 20), (18, 9)):
        t.mob(x, G + 1, z, "vindicator", "hellfire_guard")
    t.mob(22, G + 7, 15, "vindicator", "hellfire_guard")
    # hedges in front
    for x in list(range(1, 15)) + list(range(21, 35)):
        t.set(x, G + 1, 1, "dark_oak_leaves[persistent=true]")
    return t


# ======================================================================= Genosha
def island(t, cx, cz, rx, rz, W, top="sand", body="sandstone", seed_noise=0.0, flat=4, flat_r=0.8):
    """An island whose beach is at template y W (sea level), flat on top (W + flat). Fills down to y 0."""
    for x in range(t.sx):
        for z in range(t.sz):
            d = math.hypot((x - cx) / rx, (z - cz) / rz)
            d += math.sin(x * 0.4 + seed_noise) * 0.05 + math.cos(z * 0.35 + seed_noise) * 0.05
            if d > 1.0:
                continue
            height = W + round(min(1.0, (1 - d) / (1 - flat_r)) * flat)
            depth = int(d * W)
            t.fill(x, depth, z, x, height - 1, z, body)
            t.set(x, height, z, top)


def genosha():
    W = 20
    N = 72
    t = Template("genosha", N, W + 32, N, seed=10)
    island(t, 36, 36, 33, 31, W, "sand", "sandstone", 1.0)
    for x in range(N):     # grass in the middle
        for z in range(N):
            if math.hypot(x - 36, z - 36) < 24:
                for y in range(W + 5, W, -1):
                    if t.get(x, y, z) == "minecraft:sand":
                        t.set(x, y, z, "grass_block" if t.rand.random() < 0.6 else "coarse_dirt")
                        break
    base = W + 4
    t.clear(4, base + 1, 4, N - 5, base + 30, N - 5)
    # streets
    t.fill(12, base, 34, 60, base, 37, "gray_concrete_powder")
    t.fill(34, base, 12, 37, base, 60, "gray_concrete_powder")
    # ruined towers of the mutant city
    towers = [(16, 16, 9, 18), (52, 16, 8, 24), (16, 50, 8, 14), (52, 50, 10, 20), (22, 28, 6, 10),
              (48, 28, 6, 12)]
    for x, z, w, h in towers:
        t.room(x - w // 2, base, z - w // 2, x + w // 2, base + h, z + w // 2, "white_concrete", "smooth_stone",
               "white_concrete")
        for y in range(base + 3, base + h, 4):
            t.fill(x - w // 2 + 1, y, z - w // 2, x + w // 2 - 1, y + 1, z - w // 2, "glass")
            t.fill(x - w // 2 + 1, y, z + w // 2, x + w // 2 - 1, y + 1, z + w // 2, "glass")
            t.fill(x - w // 2, y - 2, z - w // 2, x + w // 2, y - 2, z + w // 2, "smooth_stone_slab[type=top]")
            t.clear(x - w // 2 + 1, y - 2, z - w // 2 + 1, x - w // 2 + 2, y - 2, z - w // 2 + 2)
        t.clear(x, base + 1, z - w // 2, x + 1, base + 2, z - w // 2)
        t.ruin(x - w // 2, base + h // 3, z - w // 2, x + w // 2, base + h, z + w // 2, 0.35)
        t.scatter(x - w // 2, base, z - w // 2, x + w // 2, base + h, z + w // 2, "cracked_stone_bricks", 0.1,
                  lambda s: s == "minecraft:white_concrete")
        t.chest(x - w // 2 + 1, base + 1, z + w // 2 - 1, "east", "genosha")
    # rubble on the ground
    t.scatter(6, base + 1, 6, N - 7, base + 1, N - 7, "cobblestone", 0.03, lambda s: s is None or s == "minecraft:air")
    t.scatter(6, base + 1, 6, N - 7, base + 1, N - 7, "gravel", 0.02, lambda s: s is None or s == "minecraft:air")
    # Magneto statue in the central square (broken arm)
    cx = cz = 36
    t.fill(cx - 3, base, cz - 3, cx + 3, base + 1, cz + 3, "polished_andesite")
    t.fill(cx - 1, base + 2, cz - 1, cx + 1, base + 3, cz, "iron_block")            # legs
    t.fill(cx - 1, base + 4, cz - 1, cx + 1, base + 7, cz, "red_concrete")          # body with cape
    t.fill(cx - 1, base + 4, cz + 1, cx + 1, base + 7, cz + 1, "purple_concrete")
    t.fill(cx - 2, base + 6, cz - 1, cx - 2, base + 9, cz - 1, "red_concrete")      # raised arm
    t.fill(cx - 1, base + 8, cz - 1, cx + 1, base + 9, cz, "red_concrete")          # helmet
    t.set(cx, base + 8, cz - 1, "light_gray_concrete")
    t.fill(cx + 2, base + 2, cz + 2, cx + 4, base + 2, cz + 3, "red_concrete")      # fallen arm
    # wrecked Sentinels
    for sx, sz in ((20, 40), (50, 40)):
        t.ellipsoid(sx, base + 1, sz, 3, 2, 3, "light_gray_concrete")
        t.fill(sx + 3, base + 1, sz - 2, sx + 12, base + 3, sz + 2, "purple_concrete")
        t.ruin(sx - 3, base, sz - 3, sx + 12, base + 4, sz + 3, 0.25)
        t.set(sx - 2, base + 2, sz, "red_concrete")
        t.chest(sx + 6, base + 1, sz, "north", "fallen_sentinel")
    # scavengers
    for x, z in ((30, 20), (44, 22), (26, 46), (46, 48), (36, 56), (14, 36), (58, 36)):
        t.mob(x, base + 1, z, "husk", "scavenger")
    # a dock
    t.fill(34, W, 0, 37, W, 8, "spruce_planks")
    for z in (0, 4):
        t.fill(34, 0, z, 34, W - 1, z, "spruce_log")
        t.fill(37, 0, z, 37, W - 1, z, "spruce_log")
    return t


# ======================================================================= Krakoa
def giant_flower(t, x, y, z, height, petal):
    t.fill(x, y, z, x, y + height, z, "moss_block")
    top = y + height
    t.disc(x, top, z, 2.5, petal)
    t.set(x, top, z, "yellow_concrete")
    t.set(x, top + 1, z, "shroomlight")
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        t.set(x + dx * 3, top + 1, z + dz * 3, petal)


def krakoa():
    W = 20
    N = 70
    t = Template("krakoa", N, W + 36, N, seed=11)
    island(t, 35, 35, 32, 30, W, "grass_block", "dirt", 2.0, flat=2)
    t.clear(3, W + 5, 3, N - 4, W + 35, N - 4)
    # the living island: a big hill with a face in the rock looking north
    t.ellipsoid(35, W + 3, 38, 14, 14, 12, "moss_block", only_y=lambda y: y >= W + 3)
    t.ellipsoid(35, W + 3, 38, 12, 12, 10, "dirt", only_y=lambda y: W + 3 <= y < W + 12)
    for x in range(N):
        for z in range(N):
            for y in range(W + 30, W, -1):
                s = t.get(x, y, z)
                if s and s != "minecraft:air":
                    if s in ("minecraft:moss_block", "minecraft:grass_block") and t.rand.random() < 0.25:
                        t.set(x, y + 1, z, t.rand.choice(["short_grass", "fern", "azalea", "flowering_azalea",
                                                          "pink_tulip", "allium", "poppy", "blue_orchid"]))
                    break
    def face(x1, y1, x2, y2, state):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(20, 40):
                    if t.get(x, y, z) == "minecraft:moss_block":
                        t.set(x, y, z, state)
                        break
    face(29, W + 9, 31, W + 10, "black_concrete")     # eyes
    face(39, W + 9, 41, W + 10, "black_concrete")
    face(30, W + 3, 40, W + 3, "black_concrete")      # lip
    t.clear(32, W + 4, 20, 38, W + 6, 34)              # mouth = cave entrance
    # the habitat cave inside the hill
    t.ellipsoid(35, W + 6, 38, 8, 4, 6, AIR, only_y=lambda y: y >= W + 4)
    t.disc(35, W + 3, 38, 7, "moss_block")
    t.set(35, W + 4, 42, "xmen:krakoa_gate")
    for x, z in ((31, 36), (39, 36), (35, 40)):
        t.set(x, W + 9, z, "shroomlight")
    t.chest(30, W + 4, 39, "east", "krakoa")
    t.chest(40, W + 4, 39, "west", "krakoa")
    # giant flowers all over the island
    for x, z, h, petal in ((12, 20, 9, "pink_concrete"), (58, 18, 11, "red_concrete"),
                           (14, 52, 8, "magenta_concrete"), (58, 54, 10, "pink_concrete"),
                           (22, 12, 7, "orange_concrete"), (50, 10, 7, "magenta_concrete"),
                           (8, 36, 8, "red_concrete"), (62, 36, 9, "pink_concrete")):
        gy = W + 1
        for y in range(W + 8, W - 2, -1):
            s = t.get(x, y, z)
            if s and s != "minecraft:air":
                gy = y + 1
                break
        giant_flower(t, x, gy, z, h, petal)
    # mutants living here
    for x, z in ((30, 16), (40, 18), (20, 30), (50, 30), (35, 37), (33, 40), (26, 50), (46, 52)):
        y = W + 4 if 28 <= x <= 42 and 33 <= z <= 44 else None
        if y is None:
            y = W + 1
            for yy in range(W + 12, W - 2, -1):
                s = t.get(x, yy, z)
                if s and s not in ("minecraft:air",) and "grass" not in s and "fern" not in s \
                        and "tulip" not in s and "allium" not in s and "poppy" not in s and "orchid" not in s \
                        and "azalea" not in s:
                    y = yy + 1
                    break
        t.npc(x, y, z, "krakoa_mutant")
    # a beach with a dock
    t.fill(34, W, 0, 36, W, 6, "bamboo_planks")
    return t


# ======================================================================= Muir Island
def muir_island():
    W = 12
    N = 38
    t = Template("muir_island", N, W + 26, N, seed=12)
    island(t, 19, 19, 17, 16, W, "grass_block", "stone", 3.0, flat=3, flat_r=0.75)
    t.scatter(0, W, 0, N - 1, W + 4, N - 1, "mossy_cobblestone", 0.08, lambda s: s == "minecraft:grass_block")
    base = W + 3
    t.clear(4, base + 1, 4, N - 5, base + 22, N - 5)
    t.fill(4, base, 4, N - 5, base, N - 5, "grass_block")
    t.fill(4, W, 4, N - 5, base - 1, N - 5, "stone")
    # lighthouse
    for y in range(base, base + 18):
        t.disc(9, y, 9, 3, "white_concrete" if (y // 3) % 2 else "red_concrete", hollow=True)
    t.disc(9, base, 9, 3, "stone_bricks")
    t.disc(9, base + 18, 9, 4, "stone_bricks")
    t.cylinder(9, base + 19, 9, 2, base + 20, "glass", hollow=True)
    t.set(9, base + 19, 9, "redstone_lamp[lit=true]")
    t.set(9, base + 21, 9, "stone_brick_slab[type=bottom]")
    t.clear(9, base + 1, 6, 9, base + 2, 6)
    for i in range(17):
        ang = i * 0.8
        x = round(9 + math.cos(ang) * 1.6)
        z = round(9 + math.sin(ang) * 1.6)
        t.set(x, base + 1 + i, z, "stone_bricks")
    t.clear(round(9 + math.cos(16 * 0.8) * 1.6), base + 18, round(9 + math.sin(16 * 0.8) * 1.6),
            round(9 + math.cos(16 * 0.8) * 1.6), base + 18, round(9 + math.sin(16 * 0.8) * 1.6))
    # research station
    t.room(16, base, 14, 32, base + 6, 30, "light_gray_concrete", "polished_andesite", "gray_concrete")
    for x in range(18, 31, 4):
        t.fill(x, base + 2, 14, x + 1, base + 4, 14, "glass")
        t.fill(x, base + 2, 30, x + 1, base + 4, 30, "glass")
    t.clear(23, base + 1, 14, 24, base + 2, 14)
    t.fill(17, base + 1, 29, 31, base + 3, 29, "bookshelf")
    t.set(20, base + 1, 22, "lectern[facing=south]")
    t.set(28, base + 1, 18, "brewing_stand")
    t.set(29, base + 1, 18, "cauldron")
    t.fill(26, base + 1, 22, 30, base + 1, 24, "white_concrete")
    t.chest(17, base + 1, 16, "east", "muir_island")
    t.chest(31, base + 1, 26, "west", "muir_island")
    t.bed(18, base + 1, 25, "east", "light_blue")
    for x in range(19, 31, 5):
        t.set(x, base + 5, 22, "lantern[hanging=true]")
    t.npc(24, base + 1, 20, "moira")
    t.npc(21, base + 1, 26, "scientist")
    # stairs down to the jetty
    t.fill(22, W, 0, 25, W, 4, "spruce_planks")
    for i in range(3):
        t.set(23, W + 1 + i, 5 + i, "stone_brick_stairs[facing=south]")
        t.set(24, W + 1 + i, 5 + i, "stone_brick_stairs[facing=south]")
    return t


# ======================================================================= Morlock Tunnels
def morlock_tunnels():
    G = 22
    N = 48
    t = Template("morlock_tunnels", N, G + 3, N, seed=13)

    def tunnel(x1, z1, x2, z2):
        if z1 == z2:
            t.fill(x1, 0, z1 - 3, x2, 7, z1 + 3, "mossy_stone_bricks")
            t.mix(x1, 0, z1 - 3, x2, 7, z1 + 3, ["stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks"])
            t.clear(x1, 1, z1 - 2, x2, 5, z1 + 2)
            t.clear(x1, 6, z1 - 1, x2, 6, z1 + 1)
            t.fill(x1, 0, z1 - 1, x2, 0, z1 + 1, "water")
            t.fill(x1, 0, z1 - 1, x2, 0, z1 + 1, "water")
            t.fill(x1, -1, z1 - 1, x2, -1, z1 + 1, "stone")
            t.fill(x1, 1, z1 - 2, x2, 1, z1 - 2, "stone_brick_slab[type=bottom]")
            t.fill(x1, 1, z1 + 2, x2, 1, z1 + 2, "stone_brick_slab[type=bottom]")
            t.fill(x1, 0, z1 - 2, x2, 0, z1 - 2, "stone_bricks")
            t.fill(x1, 0, z1 + 2, x2, 0, z1 + 2, "stone_bricks")
        else:
            t.fill(x1 - 3, 0, z1, x1 + 3, 7, z2, "stone_bricks")
            t.mix(x1 - 3, 0, z1, x1 + 3, 7, z2, ["stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks"])
            t.clear(x1 - 2, 1, z1, x1 + 2, 5, z2)
            t.clear(x1 - 1, 6, z1, x1 + 1, 6, z2)
            t.fill(x1 - 1, 0, z1, x1 + 1, 0, z2, "water")
            t.fill(x1 - 2, 0, z1, x1 - 2, 0, z2, "stone_bricks")
            t.fill(x1 + 2, 0, z1, x1 + 2, 0, z2, "stone_bricks")
            t.fill(x1 - 2, 1, z1, x1 - 2, 1, z2, "stone_brick_slab[type=bottom]")
            t.fill(x1 + 2, 1, z1, x1 + 2, 1, z2, "stone_brick_slab[type=bottom]")

    tunnel(0, 24, 47, 24)
    tunnel(24, 0, 24, 47)
    # the crossing: a bigger chamber where the Morlocks live
    t.room(14, 0, 14, 34, 9, 34, "stone_bricks", "stone_bricks", "stone_bricks")
    t.mix(14, 0, 14, 34, 9, 34, ["stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks"])
    t.clear(15, 1, 15, 33, 8, 33)
    t.fill(15, 0, 15, 33, 0, 33, "stone_bricks")
    t.fill(23, 0, 15, 25, 0, 33, "water")
    t.fill(15, 0, 23, 33, 0, 25, "water")
    t.fill(23, 0, 23, 25, 0, 25, "stone_bricks")
    for x, z in ((24, 22), (24, 26), (22, 24), (26, 24)):
        t.set(x, 0, z, "spruce_slab[type=top]")
    for x, z in ((18, 18), (30, 18), (18, 30), (30, 30)):
        t.set(x, 1, z, "campfire[lit=true]")
    for x, z, f in ((16, 16, "east"), (16, 31, "east"), (32, 16, "west"), (32, 31, "west")):
        t.bed(x, 1, z, f, "brown")
    t.chest(16, 1, 20, "east", "morlock")
    t.chest(32, 1, 28, "west", "morlock")
    t.barrel(20, 1, 32, "morlock")
    for x, z in ((20, 20), (28, 20), (20, 28), (28, 28)):
        t.set(x, 8, z, "lantern[hanging=true]")
    for x in (6, 42):
        t.set(x, 6, 24, "lantern[hanging=true]")
    for z in (6, 42):
        t.set(24, 6, z, "lantern[hanging=true]")
    for x, z in ((19, 22), (29, 26), (21, 30), (27, 17), (8, 23), (40, 25)):
        t.npc(x, 1, z, "morlock")
    # manhole shaft up to the surface
    t.fill(31, 9, 32, 33, G, 34, "stone_bricks")
    t.fill(32, 1, 34, 32, G, 34, "stone_bricks")
    t.clear(32, 1, 33, 32, G, 33)
    for y in range(1, G + 1):
        t.set(32, y, 33, "ladder[facing=north]")
    t.set(32, G + 1, 33, "iron_trapdoor[facing=north,half=bottom,open=false]")
    return t


# ======================================================================= Blackbird crash site
def blackbird_crash():
    G = 4
    t = Template("blackbird_crash", 34, G + 14, 34, seed=14)
    # crater
    t.ellipsoid(17, G + 1, 17, 14, 5, 14, AIR, only_y=lambda y: y >= G - 2)
    t.ellipsoid(17, G - 3, 17, 14, 4, 14, "coarse_dirt", only_y=lambda y: y <= G - 2)
    for x in range(34):
        for z in range(34):
            if math.hypot(x - 17, z - 17) < 14:
                t.set(x, G - 2 if math.hypot(x - 17, z - 17) < 10 else G - 1, z,
                      t.rand.choice(["coarse_dirt", "coarse_dirt", "gravel", "blackstone", "magma_block"]))
    # the jet, nose buried, tail up
    body = "black_concrete"
    for i in range(20):
        y = G - 1 + i // 3
        z = 6 + i
        w = 1 if i < 3 else 2
        t.fill(17 - w, y, z, 17 + w, y + 1, z, body)
    for i in range(10):
        span = 3 + i
        z = 14 + i
        y = G + 1 + (z - 6) // 3
        t.fill(17 - span, y, z, 17 - 4, y, z, body)
        if i < 6:
            t.fill(17 + 4, y, z, 17 + span, y, z, body)   # the right wing is broken off
    t.fill(26, G - 1, 6, 30, G - 1, 9, body)          # broken wing lying in the dirt
    for dx in (-3, 3):
        t.fill(17 + dx, G + 7, 22, 17 + dx, G + 10, 25, body)
    t.fill(16, G + 1, 8, 18, G + 1, 10, "black_stained_glass")
    t.ruin(10, G - 2, 4, 26, G + 12, 28, 0.08)
    t.clear(16, G + 2, 15, 18, G + 3, 22)          # open cabin
    t.fill(16, G + 1, 15, 18, G + 1, 22, "black_concrete")
    t.chest(17, G + 2, 18, "south", "blackbird")
    t.chest(17, G + 2, 21, "north", "hangar")
    for x, z in ((8, 12), (25, 24), (12, 27), (22, 8)):
        t.set(x, G - 1, z, "campfire[lit=true]")
    t.set(17, G + 1, 25, "magma_block")
    t.scatter(4, G - 2, 4, 30, G - 1, 30, "iron_block", 0.02)
    return t


# ======================================================================= Mutant Safehouse
def safehouse():
    G = 2
    t = Template("safehouse", 13, G + 10, 11, seed=15)
    t.fill(0, G - 1, 0, 12, G, 10, "dirt")
    t.fill(0, G, 0, 12, G, 10, "grass_block")
    t.clear(0, G + 1, 0, 12, G + 9, 10)
    t.room(1, G, 1, 11, G + 5, 9, "spruce_planks", "spruce_planks", "spruce_planks")
    for x, z in ((1, 1), (11, 1), (1, 9), (11, 9)):
        t.fill(x, G, z, x, G + 5, z, "spruce_log")
    for i in range(4):
        t.fill(0, G + 6 + i, i, 12, G + 6 + i, i, "spruce_stairs[facing=south]")
        t.fill(0, G + 6 + i, 10 - i, 12, G + 6 + i, 10 - i, "spruce_stairs[facing=north]")
        t.fill(1, G + 6 + i, i + 1, 11, G + 6 + i, 9 - i, "spruce_planks")
    t.fill(0, G + 10, 4, 12, G + 10, 6, "spruce_slab[type=bottom]")
    t.clear(6, G + 1, 1, 6, G + 2, 1)
    t.door(6, G + 1, 1, "south", "spruce")
    for x in (3, 9):
        t.set(x, G + 2, 1, "glass")
        t.set(x, G + 2, 9, "glass")
    t.set(1, G + 2, 5, "glass")
    t.set(11, G + 2, 5, "glass")
    t.bed(3, G + 1, 6, "south", "light_blue")
    t.chest(9, G + 1, 8, "north", "safehouse")
    t.set(10, G + 1, 8, "furnace[facing=north]")
    t.set(10, G + 1, 7, "crafting_table")
    t.set(6, G + 4, 5, "lantern[hanging=true]")
    t.set(9, G + 1, 3, "red_carpet")
    t.npc(6, G + 1, 5, "refugee")
    t.set(3, G + 1, 0, "campfire[lit=false]")
    return t


# ======================================================================= Detection Tower
def detection_tower():
    G = 2
    N = 33   # the big empty area around the tower is the alarm zone
    t = Template("detection_tower", N, G + 24, N, seed=16)
    c = N // 2
    t.fill(c - 5, G - 1, c - 5, c + 5, G, c + 5, "gravel")
    t.clear(c - 5, G + 1, c - 5, c + 5, G + 23, c + 5)
    for dx in (-3, 3):     # four legs
        for dz in (-3, 3):
            t.fill(c + dx, G + 1, c + dz, c + dx, G + 16, c + dz, "iron_block")
    # ladder in the middle
    t.fill(c, G + 1, c + 1, c, G + 16, c + 1, "stone_bricks")
    # cabin on top
    t.room(c - 4, G + 16, c - 4, c + 4, G + 21, c + 4, "light_gray_concrete", "stone_bricks", "gray_concrete")
    for y in range(G + 1, G + 17):
        t.set(c, y, c, "ladder[facing=north]")
    for x in range(c - 3, c + 4):
        for z in (c - 4, c + 4):
            t.fill(x, G + 18, z, x, G + 19, z, "glass")
    for z in range(c - 3, c + 4):
        for x in (c - 4, c + 4):
            t.fill(x, G + 18, z, x, G + 19, z, "glass")
    t.set(c, G + 20, c - 2, "bell[attachment=ceiling,facing=north]")
    for x, z in ((c - 3, c - 3), (c + 3, c - 3), (c - 3, c + 3), (c + 3, c + 3)):
        t.set(x, G + 22, z, "redstone_lamp[lit=true]")
        t.set(x, G + 23, z, "lightning_rod")
    t.set(c, G + 22, c, "redstone_lamp[lit=true]")
    t.chest(c + 3, G + 17, c + 2, "west", "detection_tower")
    t.set(c - 3, G + 17, c + 2, "cartography_table")
    t.mob(c - 2, G + 17, c - 2, "pillager", "tower_guard")
    t.mob(c + 2, G + 1, c - 2, "pillager", "tower_guard")
    t.mob(c - 2, G + 1, c + 2, "pillager", "tower_guard")
    # a sign-like warning: red wool stripes on the legs
    for dx in (-3, 3):
        for dz in (-3, 3):
            t.set(c + dx, G + 8, c + dz, "red_concrete")
    return t


ALL = [apocalypse_pyramid, fallen_sentinel, brotherhood_hideout, asteroid_m, sentinel_factory, weapon_x,
       sinister_lab, hellfire_club, genosha, krakoa, muir_island, morlock_tunnels, blackbird_crash, safehouse,
       detection_tower]
