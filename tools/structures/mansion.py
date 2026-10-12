"""The X-Mansion: Xavier's School above ground, the X-Men's secret base below.

Layout (looking from above, north is up):
  - front garden with fountain, hedges and a Krakoa gate (z 0..21)
  - the main house (x 12..67, z 22..45) with three floors and an attic
  - two back wings (x 12..25 and x 54..67, z 46..62) around a basketball court
Underground, reached by the elevator next to the east classroom:
  - B1 (floor y=14): Cerebro, the medical lab, the war room
  - B2 (floor y=2): the Danger Room and the X-Jet hangar
"""
from lib import Template, AIR

G = 26          # template y of the ground (grass) layer
F1 = G          # ground floor
F2 = G + 6      # first floor
F3 = G + 12     # attic floor
ROOF = G + 18   # where the roof starts
B1 = 14         # upper basement floor
B2 = 2          # lower basement floor
SX, SY, SZ = 80, G + 32, 80

LIFT_X, LIFT_Z = 56, 43   # center of the 3x3 elevator cabin

WALL = "bricks"
TRIM = "stone_bricks"
LAB_WALL = "light_gray_concrete"
LAB_FLOOR = "smooth_stone"
LIGHT = "sea_lantern"


def build():
    t = Template("x_mansion", SX, SY, SZ, seed=1963)

    # ---------------------------------------------------------------- ground
    t.clear(0, G + 1, 0, SX - 1, G + 12, SZ - 1)          # flatten hills over the whole grounds
    t.fill(0, G - 5, 0, SX - 1, G - 1, SZ - 1, "dirt")
    t.fill(0, G, 0, SX - 1, G, SZ - 1, "grass_block")

    garden(t)
    house(t)
    wings(t)
    court(t)
    basement_b1(t)
    basement_b2(t)
    elevator(t)
    people(t)
    return t


# -------------------------------------------------------------------- garden
def garden(t):
    # main path from the north edge to the front door
    t.fill(37, G, 0, 42, G, 21, "gravel")
    t.fill(36, G, 0, 36, G, 21, "stone_bricks")
    t.fill(43, G, 0, 43, G, 21, "stone_bricks")
    # gate pillars and a low wall along the north edge
    for x in list(range(0, 36)) + list(range(44, 80)):
        t.set(x, G + 1, 0, "stone_brick_wall")
    for x in (34, 35, 44, 45):
        t.fill(x, G + 1, 0, x, G + 3, 0, "stone_bricks")
        t.set(x, G + 4, 0, "lantern[hanging=false]")
    # side walls along the garden
    for z in range(0, 80):
        t.set(0, G + 1, z, "stone_brick_wall")
        t.set(79, G + 1, z, "stone_brick_wall")
    for x in range(0, 80):
        t.set(x, G + 1, 79, "stone_brick_wall")

    # fountain
    t.disc(40, G, 11, 5, "stone_bricks")
    t.disc(40, G + 1, 11, 5, "stone_brick_wall", hollow=True)
    t.disc(40, G, 11, 4, "water")
    t.fill(40, G, 11, 40, G + 3, 11, "quartz_pillar")
    t.set(40, G + 4, 11, "water")
    t.fill(37, G, 0, 42, G, 5, "gravel")

    # hedges and flower beds
    for z in range(3, 20):
        for x in (30, 49):
            t.set(x, G + 1, z, "oak_leaves[persistent=true]")
    flowers = ["poppy", "dandelion", "blue_orchid", "allium", "azure_bluet", "red_tulip", "oxeye_daisy",
               "cornflower", "lily_of_the_valley", "white_tulip"]
    for z in range(3, 20, 2):
        t.set(31, G + 1, z, t.rand.choice(flowers))
        t.set(48, G + 1, z, t.rand.choice(flowers))
    for x, z in ((24, 6), (56, 6), (24, 16), (56, 16), (6, 30), (73, 30), (6, 60), (73, 60)):
        tree(t, x, G + 1, z)
    for x, z in ((35, 18), (44, 18), (35, 4), (44, 4)):
        t.lamp_post(x, G + 1, z)

    # benches
    for x in (26, 52):
        for z in (10, 12):
            t.set(x, G + 1, z, "spruce_stairs[facing=east]" if x < 40 else "spruce_stairs[facing=west]")

    # Krakoa gate in a ring of flowers (west garden)
    t.disc(14, G, 11, 4, "moss_block")
    for x in range(10, 19):
        for z in range(7, 16):
            if t.get(x, G, z) == "minecraft:moss_block" and (x, z) != (14, 11):
                t.set(x, G + 1, z, t.rand.choice(["pink_tulip", "peony", "rose_bush", "allium", "poppy"]))
    t.set(14, G + 1, 11, "xmen:krakoa_gate")
    t.clear(14, G + 2, 11, 14, G + 3, 11)
    t.set(13, G + 1, 11, AIR)
    t.set(15, G + 1, 11, AIR)
    t.set(14, G + 1, 10, AIR)
    t.set(14, G + 1, 12, AIR)

    # a small pond with a dock on the east side
    t.ellipsoid(64, G, 11, 7, 2, 4, "water", only_y=lambda y: y <= G)
    t.fill(58, G, 11, 60, G, 11, "spruce_planks")


def tree(t, x, y, z):
    t.fill(x, y, z, x, y + 4, z, "oak_log[axis=y]")
    t.ellipsoid(x, y + 5, z, 2.6, 2, 2.6, "oak_leaves[persistent=true]")
    t.fill(x, y, z, x, y + 4, z, "oak_log[axis=y]")


# --------------------------------------------------------------------- house
def window_row(t, x1, x2, z, y, every=4):
    for x in range(x1 + 2, x2 - 1, every):
        t.fill(x, y + 2, z, x + 1, y + 3, z, "glass")


def window_col(t, z1, z2, x, y, every=4):
    for z in range(z1 + 2, z2 - 1, every):
        t.fill(x, y + 2, z, x, y + 3, z + 1, "glass")


def ceiling_lights(t, x1, z1, x2, z2, y, step=5):
    for x in range(x1 + 2, x2 - 1, step):
        for z in range(z1 + 2, z2 - 1, step):
            t.set(x, y, z, "lantern[hanging=true]")


def house(t):
    X1, X2, Z1, Z2 = 12, 67, 22, 45
    # shell
    t.fill(X1, F1, Z1, X2, ROOF, Z2, WALL)
    t.clear(X1 + 1, F1 + 1, Z1 + 1, X2 - 1, ROOF - 1, Z2 - 1)
    for y in (F1, F2, F3):
        t.fill(X1 + 1, y, Z1 + 1, X2 - 1, y, Z2 - 1, "dark_oak_planks" if y == F1 else "spruce_planks")
    # stone trim: corners and floor bands
    for x in (X1, X2):
        for z in (Z1, Z2):
            t.fill(x, F1, z, x, ROOF, z, TRIM)
    for y in (F1, F2, F3, ROOF):
        t.walls(X1, y, Z1, X2, y, Z2, TRIM)
    # windows on every floor
    for y in (F1, F2, F3):
        window_row(t, X1, X2, Z1, y)
        window_row(t, X1, X2, Z2, y)
        window_col(t, Z1, Z2, X1, y)
        window_col(t, Z1, Z2, X2, y)

    # porch and front door
    t.fill(34, F1, 18, 45, F1, 21, "stone_bricks")
    for x in (34, 45):
        t.fill(x, F1 + 1, 18, x, F2 - 1, 18, "quartz_pillar")
    t.fill(34, F2, 18, 45, F2, 21, "stone_brick_slab[type=bottom]")
    for x in range(36, 44):
        t.set(x, F1, 17, "stone_brick_stairs[facing=south]")
    t.clear(38, F1 + 1, Z1, 41, F1 + 3, Z1)
    t.door(39, F1 + 1, Z1, "south", "dark_oak", "left")
    t.door(40, F1 + 1, Z1, "south", "dark_oak", "right")
    t.fill(38, F1 + 1, Z1, 38, F1 + 3, Z1, TRIM)
    t.fill(41, F1 + 1, Z1, 41, F1 + 3, Z1, TRIM)
    t.fill(39, F1 + 3, Z1, 40, F1 + 3, Z1, TRIM)
    # big X over the entrance
    for i in range(5):
        t.set(37 + i + 1, F2 + 1 + i, Z1, "yellow_concrete")
        t.set(42 - i - 1, F2 + 1 + i, Z1, "yellow_concrete")

    # ---------------- ground floor
    # inner walls: entrance hall x 33..46
    for x in (32, 47):
        t.fill(x, F1 + 1, Z1 + 1, x, F2 - 1, Z2 - 1, "stripped_dark_oak_log[axis=y]")
        t.clear(x, F1 + 1, 27, x, F1 + 2, 28)
        t.clear(x, F1 + 1, 39, x, F1 + 2, 40)
    # west rooms split at z 34, east rooms split at z 34
    t.fill(X1 + 1, F1 + 1, 34, 31, F2 - 1, 34, "stripped_dark_oak_log[axis=x]")
    t.fill(48, F1 + 1, 34, X2 - 1, F2 - 1, 34, "stripped_dark_oak_log[axis=x]")
    t.clear(22, F1 + 1, 34, 23, F1 + 2, 34)
    t.clear(57, F1 + 1, 34, 58, F1 + 2, 34)

    # entrance hall: red carpet, chandelier and a grand staircase to the first floor
    t.fill(38, F1 + 1, Z1 + 1, 41, F1 + 1, Z2 - 6, "red_carpet")
    t.fill(33, F1, Z1 + 1, 46, F1, Z2 - 1, "polished_andesite")
    t.fill(37, F1, Z1 + 1, 42, F1, Z2 - 1, "smooth_quartz")
    for i in range(6):   # two staircases climbing to the back gallery
        for x in (34, 35):
            t.set(x, F1 + 1 + i, Z2 - 7 + i, "dark_oak_stairs[facing=south]")
            t.fill(x, F1 + 1, Z2 - 7 + i, x, F1 + i, Z2 - 7 + i, "dark_oak_planks")
        for x in (44, 45):
            t.set(x, F1 + 1 + i, Z2 - 7 + i, "dark_oak_stairs[facing=south]")
            t.fill(x, F1 + 1, Z2 - 7 + i, x, F1 + i, Z2 - 7 + i, "dark_oak_planks")
    # open the first floor above the stairs and the hall so the hall is two floors tall
    t.clear(33, F2, Z1 + 1, 46, F2, 29)
    t.clear(34, F2, Z2 - 7, 35, F2, Z2 - 2)
    t.clear(44, F2, Z2 - 7, 45, F2, Z2 - 2)
    for x in range(33, 47):
        t.set(x, F2 + 1, 30, "dark_oak_fence")
    t.fill(39, F3 - 1, 30, 40, F3 - 1, 31, "chain[axis=y]")
    t.fill(38, F3 - 2, 29, 41, F3 - 2, 32, "gold_block")
    for x, z in ((38, 29), (41, 29), (38, 32), (41, 32)):
        t.set(x, F3 - 3, z, "lantern[hanging=true]")
    for z in (25, 30, 35):
        t.set(33, F1 + 1, z, "potted_fern")
        t.set(46, F1 + 1, z, "potted_fern")
    t.set(39, F1 + 4, Z2 - 1, "yellow_banner")  # no wall banner: keeps it simple

    # library (west front)
    lib_x1, lib_x2, lib_z1, lib_z2 = X1 + 1, 31, Z1 + 1, 33
    for z in range(lib_z1, lib_z2 + 1):
        t.fill(lib_x1, F1 + 1, z, lib_x1, F2 - 1, z, "bookshelf")
    for x in range(lib_x1, lib_x2 + 1):
        t.fill(x, F1 + 1, lib_z2, x, F1 + 3, lib_z2, "bookshelf")
    for x in range(16, 30, 4):
        t.fill(x, F1 + 1, 25, x, F1 + 3, 30, "bookshelf")
    t.set(30, F1 + 1, 24, "lectern[facing=south]")
    t.set(28, F1 + 1, 32, "enchanting_table")
    t.chest(14, F1 + 1, 32, "east", "mansion")
    t.fill(lib_x1 + 1, F1, lib_z1, lib_x2, F1, lib_z2 - 1, "dark_oak_planks")
    t.fill(18, F1 + 1, 24, 18, F1 + 1, 24, "red_carpet")
    ceiling_lights(t, lib_x1, lib_z1, lib_x2, lib_z2, F2 - 1)

    # kitchen and dining room (west back)
    t.fill(X1 + 1, F1, 35, 31, F1, Z2 - 1, "white_terracotta")
    for x in range(X1 + 1, 20):
        t.set(x, F1 + 1, Z2 - 1, "smoker[facing=north]" if x % 3 == 0 else "furnace[facing=north]"
              if x % 3 == 1 else "barrel[facing=up]")
    t.chest(20, F1 + 1, Z2 - 1, "north", "mansion_kitchen")
    t.set(21, F1 + 1, Z2 - 1, "cauldron")
    t.set(22, F1 + 1, Z2 - 1, "crafting_table")
    for x in range(16, 29):  # long dining table
        t.set(x, F1 + 1, 39, "dark_oak_fence")
        t.set(x, F1 + 2, 39, "white_carpet")
        if x % 2 == 0:
            t.set(x, F1 + 1, 38, "dark_oak_stairs[facing=south]")
            t.set(x, F1 + 1, 40, "dark_oak_stairs[facing=north]")
    ceiling_lights(t, X1 + 1, 35, 31, Z2 - 1, F2 - 1)

    # classroom (east front)
    t.fill(48, F1 + 1, Z1 + 1, X2 - 1, F1 + 1, Z1 + 1, AIR)
    t.fill(52, F1 + 1, Z1 + 1, 62, F1 + 3, Z1 + 1, "black_concrete")  # blackboard
    for x in range(50, 65, 3):
        for z in range(26, 33, 3):
            t.set(x, F1 + 1, z, "oak_fence")
            t.set(x, F1 + 2, z, "oak_pressure_plate")
            t.set(x, F1 + 1, z + 1, "oak_stairs[facing=north]")
    t.set(57, F1 + 1, 24, "lectern[facing=south]")
    ceiling_lights(t, 48, Z1 + 1, X2 - 1, 33, F2 - 1)

    # elevator lobby and lounge (east back)
    t.fill(48, F1, 35, X2 - 1, F1, Z2 - 1, "polished_andesite")
    for x, z in ((50, 37), (50, 41), (64, 37)):
        t.set(x, F1 + 1, z, "red_wool")
        t.set(x + 1, F1 + 1, z, "red_wool")
    t.set(63, F1 + 1, 41, "jukebox")
    t.set(49, F1 + 1, 44, "potted_bamboo")
    ceiling_lights(t, 48, 35, X2 - 1, Z2 - 1, F2 - 1)
    # back doors from the hall into the courtyard
    t.clear(39, F1 + 1, Z2, 40, F1 + 2, Z2)

    # ---------------- first floor: hallway with dorm rooms on both sides, Professor X's study
    for zwall in (29, 38):
        t.fill(X1 + 1, F2 + 1, zwall, 32, F3 - 1, zwall, "white_terracotta")
        t.fill(47, F2 + 1, zwall, X2 - 1, F3 - 1, zwall, "white_terracotta")
    t.fill(X1 + 1, F2, 30, X2 - 1, F2, 37, "red_carpet")
    t.fill(X1 + 1, F2, 30, X2 - 1, F2, 37, "spruce_planks")
    t.fill(X1 + 1, F2 + 1, 33, X2 - 1, F2 + 1, 34, "red_carpet")
    beds = ["red", "blue", "yellow", "green", "purple", "orange", "cyan", "lime"]
    room_x = list(range(X1 + 1, 32, 5)) + list(range(48, X2 - 3, 5))
    for i, rx in enumerate(room_x):
        # room walls between dorms
        t.fill(rx + 4, F2 + 1, Z1 + 1, rx + 4, F3 - 1, 28, "white_terracotta")
        t.fill(rx + 4, F2 + 1, 39, rx + 4, F3 - 1, Z2 - 1, "white_terracotta")
        # front rooms (north) and back rooms (south)
        t.clear(rx + 2, F2 + 1, 29, rx + 2, F2 + 2, 29)
        t.clear(rx + 2, F2 + 1, 38, rx + 2, F2 + 2, 38)
        color = beds[i % len(beds)]
        t.bed(rx + 1, F2 + 1, 25, "north", color)
        t.bed(rx + 1, F2 + 1, 42, "south", color)
        t.chest(rx + 3, F2 + 1, Z1 + 1, "south", "mansion_dorm")
        t.set(rx + 3, F2 + 1, Z2 - 1, "bookshelf")
        t.set(rx + 2, F3 - 1, 25, "lantern[hanging=true]")
        t.set(rx + 2, F3 - 1, 42, "lantern[hanging=true]")
    # Professor X's study is the last room on the east side (back), bigger, with a desk
    t.clear(62, F2 + 1, 39, X2 - 1, F3 - 1, Z2 - 1)
    t.fill(59, F2 + 1, 39, 59, F3 - 1, Z2 - 1, "bookshelf")
    t.clear(59, F2 + 1, 41, 59, F2 + 2, 41)
    t.fill(60, F2, 39, X2 - 1, F2, Z2 - 1, "red_wool")
    t.fill(61, F2 + 1, 42, 64, F2 + 1, 42, "dark_oak_slab[type=top]")
    t.set(66, F2 + 1, 44, "potted_red_tulip")
    t.chest(66, F2 + 1, 39, "west", "mansion_study")
    t.set(64, F3 - 1, 42, "lantern[hanging=true]")
    ceiling_lights(t, X1 + 1, 30, X2 - 1, 37, F3 - 1, 6)

    # ---------------- attic: storage
    t.fill(X1 + 1, F3, Z1 + 1, X2 - 1, F3, Z2 - 1, "spruce_planks")
    for x in range(X1 + 3, X2 - 2, 6):
        t.barrel(x, F3 + 1, Z1 + 2, "mansion")
        t.set(x + 1, F3 + 1, Z1 + 2, "barrel[facing=up]")
    for x in range(X1 + 4, X2 - 2, 8):
        t.set(x, F3 + 4, 33, "lantern[hanging=true]")
    # stairs from the first floor hallway to the attic (west end of the hallway)
    for i in range(6):
        t.set(X1 + 2 + i, F2 + 1 + i, 31, "spruce_stairs[facing=east]")
        t.fill(X1 + 2 + i, F2 + 1, 31, X1 + 2 + i, F2 + i, 31, "spruce_planks")
        t.clear(X1 + 2 + i, F3, 30, X1 + 2 + i, F3, 32)
    roof_x(t, X1, X2, Z1, Z2, ROOF)


def roof_x(t, x1, x2, z1, z2, y0, mat="deepslate_tile"):
    """A roof whose ridge runs east-west."""
    depth = z2 - z1
    for i in range((depth + 2) // 2 + 1):
        y = y0 + i
        za, zb = z1 - 1 + i, z2 + 1 - i
        if za > zb:
            break
        if za == zb:
            t.fill(x1 - 1, y, za, x2 + 1, y, za, f"{mat}_slab[type=bottom]")
            break
        t.fill(x1 - 1, y, za, x2 + 1, y, za, f"{mat}_stairs[facing=south]")
        t.fill(x1 - 1, y, zb, x2 + 1, y, zb, f"{mat}_stairs[facing=north]")
        if zb - za > 1:
            t.fill(x1, y, za + 1, x1, y, zb - 1, WALL)      # gable ends
            t.fill(x2, y, za + 1, x2, y, zb - 1, WALL)
            t.clear(x1 + 1, y, za + 1, x2 - 1, y, zb - 1)
    for x in range(x1 + 6, x2 - 4, 12):   # chimneys
        t.fill(x, y0, z1 + 4, x, y0 + 10, z1 + 4, "bricks")
        t.set(x, y0 + 11, z1 + 4, "campfire[lit=false]")


def roof_z(t, x1, x2, z1, z2, y0, mat="deepslate_tile"):
    """A roof whose ridge runs north-south."""
    width = x2 - x1
    for i in range((width + 2) // 2 + 1):
        y = y0 + i
        xa, xb = x1 - 1 + i, x2 + 1 - i
        if xa > xb:
            break
        if xa == xb:
            t.fill(xa, y, z1 - 1, xa, y, z2 + 1, f"{mat}_slab[type=bottom]")
            break
        t.fill(xa, y, z1 - 1, xa, y, z2 + 1, f"{mat}_stairs[facing=east]")
        t.fill(xb, y, z1 - 1, xb, y, z2 + 1, f"{mat}_stairs[facing=west]")
        if xb - xa > 1:
            t.fill(xa + 1, y, z2, xb - 1, y, z2, WALL)
            t.clear(xa + 1, y, z1, xb - 1, y, z2 - 1)


def wings(t):
    for x1, x2, name in ((12, 25, "west"), (54, 67, "east")):
        z1, z2 = 46, 62
        t.fill(x1, F1, z1, x2, F3, z2, WALL)
        t.clear(x1 + 1, F1 + 1, z1, x2 - 1, F3 - 1, z2 - 1)
        t.fill(x1 + 1, F1, z1, x2 - 1, F1, z2 - 1, "dark_oak_planks")
        t.fill(x1 + 1, F2, z1, x2 - 1, F2, z2 - 1, "spruce_planks")
        for y in (F1, F2, F3):
            t.walls(x1, y, z1, x2, y, z2, TRIM)
        for x in (x1, x2):
            t.fill(x, F1, z2, x, F3, z2, TRIM)
        for y in (F1, F2):
            window_col(t, z1, z2, x1, y)
            window_col(t, z1, z2, x2, y)
            window_row(t, x1, x2, z2, y)
        # openings into the main house
        t.clear(x1 + 6, F1 + 1, 45, x1 + 7, F1 + 2, 45)
        t.clear(x1 + 6, F2 + 1, 45, x1 + 7, F2 + 2, 45)
        t.fill(x1 + 6, F1 + 1, 45, x1 + 7, F1 + 1, 45, AIR)
        roof_z(t, x1, x2, z1 + 1, z2, F3)
        if name == "west":
            # gym: training dummies (hay), weights
            for z in range(49, 60, 4):
                t.fill(15, F1 + 1, z, 15, F1 + 2, z, "hay_block")
                t.set(15, F1 + 3, z, "carved_pumpkin[facing=east]")
            t.fill(19, F1 + 1, 50, 22, F1 + 1, 52, "blue_wool")
            t.fill(19, F1 + 1, 55, 22, F1 + 1, 57, "yellow_wool")
            t.chest(24, F1 + 1, 60, "west", "mansion")
            # rec room upstairs
            t.set(16, F2 + 1, 50, "jukebox")
            t.fill(18, F2 + 1, 54, 21, F2 + 1, 56, "green_wool")
            t.set(14, F2 + 1, 60, "cartography_table")
            t.chest(24, F2 + 1, 60, "west", "mansion_dorm")
        else:
            # greenhouse downstairs (Storm's plants)
            t.fill(55, F1, 47, 66, F1, 61, "farmland[moisture=7]")
            for x in range(55, 67):
                for z in range(47, 62):
                    if (x + z) % 4 == 0:
                        t.set(x, F1, z, "water")
                    else:
                        t.set(x, F1 + 1, z, t.rand.choice(["wheat[age=7]", "carrots[age=7]", "potatoes[age=7]",
                                                           "beetroots[age=3]"]))
            t.fill(55, F1, 47, 66, F1, 47, "dark_oak_planks")
            t.fill(55, F1 + 1, 47, 66, F1 + 1, 47, AIR)
            # infirmary upstairs
            for z in range(48, 61, 3):
                t.bed(56, F2 + 1, z, "east", "white")
            t.set(64, F2 + 1, 50, "brewing_stand")
            t.chest(65, F2 + 1, 55, "west", "mansion_lab")
            t.set(64, F2 + 1, 60, "cauldron")
        for z in range(49, 61, 5):
            t.set((x1 + x2) // 2, F2 - 1, z, "lantern[hanging=true]")
            t.set((x1 + x2) // 2, F3 - 1, z, "lantern[hanging=true]")
        # stairs up inside each wing (along the inner wall)
        sx = x2 - 2 if name == "west" else x1 + 2
        for i in range(6):
            t.set(sx, F1 + 1 + i, 50 + i, "spruce_stairs[facing=south]")
            t.fill(sx, F1 + 1, 50 + i, sx, F1 + i, 50 + i, "spruce_planks")
            t.clear(sx, F2, 50 + i, sx, F2, 50 + i)


def court(t):
    """Basketball court between the wings (the hangar roof is under it, like in the movies)."""
    t.fill(28, G, 49, 51, G, 76, "smooth_stone")
    t.fill(29, G, 50, 50, G, 75, "orange_terracotta")
    t.walls(29, G, 50, 50, G, 75, "white_concrete")
    t.fill(29, G, 62, 50, G, 62, "white_concrete")
    t.disc(39.5, G, 62, 3, "white_concrete", hollow=True)
    for z, face in ((51, "south"), (74, "north")):
        t.fill(39, G + 1, z, 40, G + 4, z, "gray_concrete")
        t.fill(38, G + 5, z, 41, G + 6, z, "white_concrete")
        dz = 1 if face == "south" else -1
        t.set(39, G + 5, z + dz, "iron_bars")
        t.set(40, G + 5, z + dz, "iron_bars")
    for x in (28, 51):
        for z in range(52, 75, 6):
            t.lamp_post(x, G + 1, z, 4)


# --------------------------------------------------------------- basement B1
def lab_room(t, x1, y1, z1, x2, y2, z2, wall=LAB_WALL, floor=LAB_FLOOR):
    t.room(x1, y1, z1, x2, y2, z2, wall, floor, wall)
    for x in range(x1 + 2, x2 - 1, 4):
        for z in range(z1 + 2, z2 - 1, 4):
            t.set(x, y2, z, LIGHT)


def basement_b1(t):
    top = B1 + 7
    # main corridor (east-west) at z 41..45, connecting everything to the elevator
    lab_room(t, 20, B1, 40, 62, top, 46, "iron_block", "polished_deepslate")
    t.fill(21, B1, 43, 61, B1, 43, "yellow_concrete")
    for x in range(22, 61, 3):
        t.set(x, top, 43, LIGHT)

    # Cerebro: a round dark room with a walkway to the helmet in the middle (north of the corridor)
    cx, cz = 30, 26
    t.cylinder(cx, B1 - 1, cz, 11, top + 2, "polished_blackstone")
    t.cylinder(cx, B1, cz, 10, top + 1, AIR)
    t.disc(cx, B1 - 1, cz, 10, "black_concrete")
    t.disc(cx, top + 2, cz, 10, "black_concrete")
    for r in (4, 7, 10):
        t.disc(cx, top + 2, cz, r, "blue_stained_glass", hollow=True)
    t.disc(cx, top + 3, cz, 10, "polished_blackstone")
    for r in range(2, 11, 4):
        t.disc(cx, B1 - 1, cz, r, "light_blue_concrete", hollow=True)
    t.fill(cx - 1, B1, cz, cx + 1, B1, 39, "smooth_quartz")       # walkway
    t.disc(cx, B1, cz, 2.5, "smooth_quartz")
    t.fill(cx - 1, B1 + 1, cz + 3, cx - 1, B1 + 1, 39, "quartz_slab[type=bottom]")
    t.fill(cx + 1, B1 + 1, cz + 3, cx + 1, B1 + 1, 39, "quartz_slab[type=bottom]")
    t.set(cx, B1 + 1, cz, "xmen:cerebro")
    t.set(cx, top, cz, "end_rod[facing=down]")
    for x, z in ((cx - 7, cz), (cx + 7, cz), (cx, cz - 7)):
        t.fill(x, B1, z, x, top, z, "light_blue_stained_glass")
    t.fill(cx - 2, B1, 36, cx + 2, B1 + 4, 40, "iron_block")   # short tunnel from the corridor
    t.clear(cx - 1, B1 + 1, 35, cx + 1, B1 + 3, 40)
    t.fill(cx - 1, B1, 36, cx + 1, B1, 39, "smooth_quartz")

    # medical lab (south-east of the corridor)
    lab_room(t, 44, B1, 47, 60, top, 60, "white_concrete", "white_concrete")
    t.clear(50, B1 + 1, 46, 52, B1 + 3, 46)
    t.clear(50, B1 + 1, 47, 52, B1 + 3, 47)
    for z in range(49, 59, 3):
        t.bed(46, B1 + 1, z, "east", "white")
    for x in range(53, 59, 2):
        t.set(x, B1 + 1, 59, "brewing_stand")
    t.chest(59, B1 + 1, 50, "west", "mansion_lab")
    t.chest(59, B1 + 1, 52, "west", "mansion_lab")
    t.set(59, B1 + 1, 55, "cauldron")
    t.fill(55, B1 + 1, 48, 58, B1 + 1, 48, "white_stained_glass")
    t.fill(52, B1 + 1, 53, 54, B1 + 2, 55, "light_blue_stained_glass")  # healing tank
    t.set(53, B1 + 1, 54, "water")
    t.set(53, B1 + 2, 54, "water")

    # war room (north-east of the corridor): big table with a map of the world
    lab_room(t, 44, B1, 27, 60, top, 39, "gray_concrete", "polished_deepslate")
    t.clear(50, B1 + 1, 39, 52, B1 + 3, 40)
    t.fill(48, B1 + 1, 31, 56, B1 + 1, 35, "polished_blackstone")
    t.fill(49, B1 + 1, 32, 55, B1 + 1, 34, "light_blue_stained_glass")
    for x in range(48, 57, 2):
        t.set(x, B1 + 1, 30, "polished_blackstone_stairs[facing=south]")
        t.set(x, B1 + 1, 36, "polished_blackstone_stairs[facing=north]")
    t.chest(45, B1 + 1, 28, "east", "mansion_armory")
    t.chest(45, B1 + 1, 30, "east", "mansion_armory")
    t.set(59, B1 + 1, 28, "cartography_table")
    t.fill(46, B1 + 2, 38, 58, B1 + 4, 38, "black_stained_glass")   # screens

    # armory (west end of the corridor, south side)
    lab_room(t, 20, B1, 47, 34, top, 58, "iron_block", "polished_deepslate")
    t.clear(26, B1 + 1, 46, 28, B1 + 3, 47)
    for x in range(22, 33, 2):
        t.set(x, B1 + 1, 57, "polished_blackstone_bricks")
    t.chest(22, B1 + 1, 48, "south", "mansion_armory")
    t.chest(32, B1 + 1, 48, "south", "mansion_armory")
    t.set(27, B1 + 1, 56, "anvil[facing=north]")
    t.set(29, B1 + 1, 56, "smithing_table")
    t.set(25, B1 + 1, 56, "grindstone[face=floor,facing=north]")


# --------------------------------------------------------------- basement B2
def basement_b2(t):
    top = B2 + 11
    # corridor from the elevator: north to the Danger Room, south to the hangar
    lab_room(t, 52, B2, 24, 60, B2 + 5, 47, "iron_block", "polished_deepslate")
    t.fill(56, B2, 25, 56, B2, 46, "yellow_concrete")
    # Danger Room: big metal hall, the console in the middle starts the training
    t.room(18, B2 - 1, 14, 46, top + 1, 42, "iron_block", "iron_block", "iron_block")
    t.clear(19, B2, 15, 45, top, 41)
    t.fill(19, B2, 15, 45, B2, 41, "polished_deepslate")
    t.fill(19, B2 - 1, 15, 45, B2 - 1, 41, "iron_block")
    for x in range(19, 46):
        for z in range(15, 42):
            if x % 4 == 0 and z % 4 == 0:
                t.set(x, B2, z, "yellow_concrete")
                t.set(x, top + 1, z, LIGHT)
    for y in range(B2 + 1, top + 1, 3):
        for z in range(16, 41, 4):
            t.set(18, y, z, LIGHT)
            t.set(46, y, z, LIGHT)
    t.set(32, B2, 28, "polished_blackstone")
    t.set(32, B2 + 1, 28, "xmen:danger_room_console")
    t.clear(46, B2 + 1, 27, 52, B2 + 3, 29)  # door from the corridor
    t.fill(47, B2, 27, 51, B2, 29, "polished_deepslate")
    t.fill(47, B2 + 4, 26, 51, B2 + 4, 30, "iron_block")
    t.walls(47, B2 + 1, 26, 51, B2 + 3, 30, "iron_block")
    t.clear(47, B2 + 1, 27, 51, B2 + 3, 29)
    t.chest(44, B2 + 1, 16, "south", "danger_room")

    # X-Jet hangar
    t.room(26, B2 - 1, 49, 62, top, 78, "polished_deepslate", "polished_deepslate", "iron_block")
    t.clear(27, B2, 50, 61, top - 1, 77)
    t.fill(27, B2 - 1, 50, 61, B2 - 1, 77, "gray_concrete")
    t.fill(43, B2 - 1, 50, 45, B2 - 1, 77, "yellow_concrete")
    for x in range(28, 61, 4):
        for z in range(51, 77, 5):
            t.set(x, top, z, LIGHT)
    t.clear(55, B2, 47, 57, B2 + 3, 49)   # door from the corridor
    t.fill(55, B2 - 1, 47, 57, B2 - 1, 49, "polished_deepslate")
    # big blast door at the south end (sealed)
    t.fill(34, B2, 78, 54, top - 1, 78, "iron_block")
    t.fill(34, B2 + 5, 78, 54, B2 + 5, 78, "yellow_concrete")
    blackbird(t, 44, B2, 64)
    # workbenches and parts along the walls
    for z in range(52, 76, 4):
        t.set(27, B2, z, "smithing_table")
        t.chest(27, B2, z + 1, "east", "hangar")
    t.set(60, B2, 52, "anvil[facing=west]")
    t.chest(60, B2, 53, "west", "hangar")


def blackbird(t, cx, y, cz):
    """The X-Jet (Blackbird): a black stealth jet, nose pointing north."""
    body = "black_concrete"
    # fuselage
    for dz in range(-11, 12):
        w = 1 if dz < -8 else 2
        t.fill(cx - w, y + 2, cz + dz, cx + w, y + 3, cz + dz, body)
    t.fill(cx, y + 2, cz - 13, cx, y + 2, cz - 12, body)   # nose
    t.fill(cx - 1, y + 4, cz - 8, cx + 1, y + 4, cz - 5, "black_stained_glass")  # cockpit
    # wings
    for dz in range(-3, 9):
        span = 3 + (dz + 3)
        t.fill(cx - span, y + 2, cz + dz, cx + span, y + 2, cz + dz, body)
    # tail fins
    for dx in (-3, 3):
        t.fill(cx + dx, y + 3, cz + 7, cx + dx, y + 6, cz + 10, body)
    # engines
    for dx in (-2, 2):
        t.set(cx + dx, y + 2, cz + 12, "magma_block")
    # X logo on the wings
    for i in range(3):
        t.set(cx - 7 + i, y + 2, cz + 3 + i, "red_concrete")
        t.set(cx - 5 - i, y + 2, cz + 3 + i, "red_concrete")
        t.set(cx + 7 - i, y + 2, cz + 3 + i, "red_concrete")
        t.set(cx + 5 + i, y + 2, cz + 3 + i, "red_concrete")
    # landing gear
    for dx, dz in ((0, -9), (-4, 4), (4, 4)):
        t.fill(cx + dx, y, cz + dz, cx + dx, y + 1, cz + dz, "iron_bars")
    # boarding ramp
    for i in range(2):
        t.set(cx, y + i, cz + 10 + 2 - i, "smooth_stone_slab[type=bottom]" if i == 0 else "smooth_stone")


# ---------------------------------------------------------------- elevator
def elevator(t):
    """Three stops: X-Jet/Danger Room level, Cerebro level, and the ground floor lounge."""
    stops = [(B2, "iron_block"), (B1, "iron_block"), (F1, TRIM)]
    x1, x2, z1, z2 = LIFT_X - 2, LIFT_X + 2, LIFT_Z - 2, LIFT_Z + 2
    for floor, wall in stops:
        # cabin: 3x3 elevator floor, glass at the back, open towards the west
        t.walls(x1, floor + 1, z1, x2, floor + 3, z2, wall)
        t.fill(x2, floor + 1, z1 + 1, x2, floor + 3, z2 - 1, "glass")
        t.clear(x1 + 1, floor + 1, z1 + 1, x2 - 1, floor + 3, z2 - 1)
        t.clear(x1, floor + 1, z1 + 1, x1, floor + 3, z2 - 1)
        t.clear(x1 - 1, floor + 1, z1 + 1, x1 - 1, floor + 3, z2 - 1)
        t.fill(x1 + 1, floor, z1 + 1, x2 - 1, floor, z2 - 1, "xmen:elevator")
        t.fill(x1, floor + 4, z1, x2, floor + 4, z2, wall)
        t.set(LIFT_X, floor + 4, LIFT_Z, LIGHT)
    # solid shaft between the stops so nobody falls in
    for (lo, _), (hi, _) in zip(stops, stops[1:]):
        t.fill(x1, lo + 5, z1, x2, hi - 1, z2, "stone")


# ------------------------------------------------------------------ people
def people(t):
    t.mob(63, F2 + 1, 43, "wandering_trader", "professor_x", {"NoAI": True, "Invulnerable": True,
                                                              "DespawnDelay": 0}, yaw=180.0)
    spots = [(40, F1 + 1, 30), (25, F1 + 1, 27), (55, F1 + 1, 30), (20, F1 + 1, 40), (58, F1 + 1, 38),
             (40, G + 1, 14), (22, G + 1, 10), (58, G + 1, 14), (39, G + 1, 60), (44, G + 1, 66),
             (20, F2 + 1, 33), (52, F2 + 1, 33)]
    for x, y, z in spots:
        t.mob(x, y, z, "villager", "student",
              {"VillagerData": {"profession": "minecraft:nitwit", "type": "minecraft:plains", "level": 1}})
