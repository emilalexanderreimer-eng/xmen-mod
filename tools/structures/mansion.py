"""The X-Mansion, built to look like the movies (Hatley Castle): a grey stone castle with steep dark roofs,
a round tower, a fountain in the driveway and a basketball court behind. The X-Men live in it.

Below it is the secret silver level from the films:
  - long silver corridors with glowing floor edges and big round X-doors that open by themselves
  - Cerebro: a huge dark sphere with a narrow bridge to the console in the middle; it lights up when used
  - the Danger Room, the med lab, the war room and the suit room
  - the hangar with the X-Jet right under the basketball court (the court floor slides open)

Layout seen from above (north is up, x goes east, z goes south):
  z  0..29   gate, driveway around the fountain, lawns
  z 30..56   the main house (x 22..82), round tower on the north-west corner, porch in front
  z 56..74   two wings (x 18..34 kitchen, x 70..86 gym)
  z 57..61   terrace, z 62..88 basketball court (x 40..64), garden behind
Underground (floor S1): corridor along z 49..53 from x 14 to x 92.
"""
import math

from lib import Template, AIR

G = 36            # template y of the ground (grass) layer
F1 = G            # ground floor (the floor block)
F2 = G + 7        # first floor
F3 = G + 13       # attic floor
ROOF = G + 19     # the roofs start here
S1 = 18           # floor of the secret silver level
SX, SY, SZ = 104, G + 34, 104

LIFT = (44, 45, 46, 47)   # elevator x1, z1, x2, z2 (3x3)

STONE = ["stone_bricks"] * 6 + ["tuff_bricks"] * 2 + ["cracked_stone_bricks", "andesite"]
TRIM = "polished_andesite"
ROOF_BLOCK = "deepslate_tiles"
ROOF_STAIRS = "deepslate_tile_stairs"
PANEL = "dark_oak_planks"
SILVER = "xmen:silver_panel"
SFLOOR = "xmen:silver_floor"
SLIGHT = "xmen:silver_light"


def build():
    t = Template("x_mansion", SX, SY, SZ, seed=2000)

    # ---------------------------------------------------------------- ground
    t.clear(0, G + 1, 0, SX - 1, G + 16, SZ - 1)          # flatten hills over the whole grounds
    t.fill(0, G - 4, 0, SX - 1, G - 1, SZ - 1, "dirt")
    t.fill(0, G, 0, SX - 1, G, SZ - 1, "grass_block")

    secret_level(t)       # first, so the house and the court can sit on top of it
    grounds(t)
    house(t)
    tower(t)
    wings(t)
    court(t)
    elevator(t)
    people(t)
    return t


# ==================================================================== helpers
def stair(t, x, y, z, facing, block=ROOF_STAIRS, half="bottom"):
    t.set(x, y, z, f"{block}[facing={facing},half={half},shape=straight,waterlogged=false]")


def stone_walls(t, x1, z1, x2, z2, y1, y2):
    for x in range(x1, x2 + 1):
        for z in (z1, z2):
            t.mix(x, y1, z, x, y2, z, STONE)
    for z in range(z1, z2 + 1):
        for x in (x1, x2):
            t.mix(x, y1, z, x, y2, z, STONE)
    # light stone bands at each floor and corner stones (like the castle's trim)
    for y in (y1, F2, F3):
        if y1 <= y <= y2:
            t.walls(x1, y, z1, x2, y, z2, TRIM)
    for x, z in ((x1, z1), (x1, z2), (x2, z1), (x2, z2)):
        for y in range(y1, y2 + 1, 2):
            t.set(x, y, z, "chiseled_stone_bricks")


def windows_x(t, z, x1, x2, rows, step=4, width=2, skip=()):
    """Tall windows in a wall that runs along x (at z)."""
    for x in range(x1, x2 - width + 1, step):
        if any(a <= x <= b or a <= x + width - 1 <= b for a, b in skip):
            continue
        for y0, h in rows:
            t.fill(x, y0, z, x + width - 1, y0 + h - 1, z, "glass_pane")
            t.fill(x, y0 - 1, z, x + width - 1, y0 - 1, z, TRIM)
            t.fill(x, y0 + h, z, x + width - 1, y0 + h, z, TRIM)


def windows_z(t, x, z1, z2, rows, step=4, width=2, skip=()):
    for z in range(z1, z2 - width + 1, step):
        if any(a <= z <= b or a <= z + width - 1 <= b for a, b in skip):
            continue
        for y0, h in rows:
            t.fill(x, y0, z, x, y0 + h - 1, z + width - 1, "glass_pane")
            t.fill(x, y0 - 1, z, x, y0 - 1, z + width - 1, TRIM)
            t.fill(x, y0 + h, z, x, y0 + h, z + width - 1, TRIM)


def gable_roof_x(t, x1, x2, z1, z2, y0):
    """Steep roof along x: slopes face north and south, ridge in the middle. Gable walls at both ends."""
    half = (z2 - z1) // 2
    for i in range(half + 1):
        y = y0 + i
        for x in range(x1 - 1, x2 + 2):
            stair(t, x, y, z1 - 1 + i, "south")
            stair(t, x, y, z2 + 1 - i, "north")
            if i > 0:
                t.set(x, y - 1, z1 - 1 + i, ROOF_BLOCK)
                t.set(x, y - 1, z2 + 1 - i, ROOF_BLOCK)
    top = y0 + half
    t.fill(x1 - 1, top, z1 + half, x2 + 1, top, z2 - half, ROOF_BLOCK)
    # gable end walls
    for i in range(half):
        for x in (x1, x2):
            t.mix(x, y0 + i, z1 + i, x, y0 + i, z2 - i, STONE)
    return top


def gable_roof_z(t, z1, z2, x1, x2, y0):
    half = (x2 - x1) // 2
    for i in range(half + 1):
        y = y0 + i
        for z in range(z1 - 1, z2 + 2):
            stair(t, x1 - 1 + i, y, z, "east")
            stair(t, x2 + 1 - i, y, z, "west")
            if i > 0:
                t.set(x1 - 1 + i, y - 1, z, ROOF_BLOCK)
                t.set(x2 + 1 - i, y - 1, z, ROOF_BLOCK)
    top = y0 + half
    t.fill(x1 + half, top, z1 - 1, x2 - half, top, z2 + 1, ROOF_BLOCK)
    for i in range(half):
        for z in (z1, z2):
            t.mix(x1 + i, y0 + i, z, x2 - i, y0 + i, z, STONE)
    return top


def chimney(t, x, z, y1, y2):
    t.mix(x, y1, z, x + 1, y2, z + 1, ["stone_bricks", "stone_bricks", "cracked_stone_bricks"])
    t.fill(x, y2 + 1, z, x + 1, y2 + 1, z + 1, "stone_brick_slab[type=bottom]")
    t.set(x, y2, z, "campfire[lit=true,signal_fire=false,facing=north]")
    t.set(x + 1, y2, z + 1, "campfire[lit=true,signal_fire=false,facing=north]")


def chandelier(t, x, y, z):
    t.set(x, y, z, "chain[axis=y]")
    t.set(x, y - 1, z, "lantern[hanging=true]")


def rug(t, x1, z1, x2, z2, y, color, border):
    t.fill(x1, y, z1, x2, y, z2, f"{border}_carpet")
    t.fill(x1 + 1, y, z1 + 1, x2 - 1, y, z2 - 1, f"{color}_carpet")


def bookshelf_wall_x(t, z, x1, x2, y1, y2):
    t.fill(x1, y1, z, x2, y2, z, "bookshelf")


def chair(t, x, y, z, facing, wood="dark_oak"):
    """Chair you look at from 'facing' (the seat faces that way)."""
    back = {"north": "south", "south": "north", "east": "west", "west": "east"}[facing]
    stair(t, x, y, z, back, f"{wood}_stairs")


def x_door(t, x0, y0, z0, axis):
    """A 3x3 round X-door. axis 'x': the door is in a wall that runs along x (you walk through along z)."""
    for row in range(3):
        for col in range(3):
            part = row * 3 + col
            y = y0 + 2 - row
            if axis == "x":
                t.set(x0 + col, y, z0, f"xmen:x_door[part={part},axis=x,open=false]")
            else:
                t.set(x0, y, z0 + col, f"xmen:x_door[part={part},axis=z,open=false]")
    # a lit frame around the door
    if axis == "x":
        for x in range(x0 - 1, x0 + 4):
            t.set(x, y0 + 3, z0, SLIGHT if x in (x0 - 1, x0 + 3) else SILVER)
        t.fill(x0 - 1, y0, z0, x0 - 1, y0 + 2, z0, SILVER)
        t.fill(x0 + 3, y0, z0, x0 + 3, y0 + 2, z0, SILVER)
    else:
        for z in range(z0 - 1, z0 + 4):
            t.set(x0, y0 + 3, z, SLIGHT if z in (z0 - 1, z0 + 3) else SILVER)
        t.fill(x0, y0, z0 - 1, x0, y0 + 2, z0 - 1, SILVER)
        t.fill(x0, y0, z0 + 3, x0, y0 + 2, z0 + 3, SILVER)


def silver_room(t, x1, z1, x2, z2, height, wall=SILVER, floor=SFLOOR):
    """Interior x1..x2, z1..z2 on the silver level; walls and ceiling around it."""
    t.room(x1 - 1, S1, z1 - 1, x2 + 1, S1 + height + 1, z2 + 1, wall, floor, wall)
    for x in range(x1 + 1, x2, 4):
        for z in range(z1 + 1, z2, 4):
            t.set(x, S1 + height + 1, z, SLIGHT)


# ==================================================================== the secret level
def secret_level(t):
    corridor(t)
    cerebro(t)
    danger_room(t)
    hangar(t)
    med_lab(t)
    war_room(t)
    suit_room(t)


def corridor(t):
    x1, x2, z1, z2 = 14, 92, 49, 53
    t.room(x1 - 1, S1, z1 - 1, x2 + 1, S1 + 6, z2 + 1, SILVER, SFLOOR, SILVER)
    # the angled upper corners of the movie corridors
    t.fill(x1, S1 + 5, z1, x2, S1 + 5, z1, SILVER)
    t.fill(x1, S1 + 5, z2, x2, S1 + 5, z2, SILVER)
    # glowing strips along the floor edges and lights in the ceiling
    t.fill(x1, S1, z1, x2, S1, z1, SLIGHT)
    t.fill(x1, S1, z2, x2, S1, z2, SLIGHT)
    for x in range(x1 + 2, x2, 5):
        t.set(x, S1 + 6, 51, SLIGHT)
    # a big X on the floor at the end of each corridor
    for i in range(5):
        t.set(x1 + i, S1, z1 + i, "black_concrete")
        t.set(x1 + 4 - i, S1, z1 + i, "black_concrete")


def cerebro(t):
    cx, cy, cz = 35, S1 + 2, 29
    # outer metal shell, then the dark panels inside it, hollow
    t.ellipsoid(cx, cy, cz, 13.5, 13.5, 13.5, SILVER)
    t.ellipsoid(cx, cy, cz, 12.5, 12.5, 12.5, "xmen:cerebro_panel[lit=false]", hollow=True)
    # the walkway from the door to the middle
    for z in range(cz + 3, cz + 15):
        for x in (cx - 1, cx, cx + 1):
            t.set(x, S1, z, SFLOOR)
        t.set(cx - 1, S1, z, SLIGHT if z % 2 else SFLOOR)
        t.set(cx + 1, S1, z, SLIGHT if z % 2 else SFLOOR)
        t.clear(cx - 1, S1 + 1, z, cx + 1, S1 + 3, z)
        if z <= cz + 11:
            t.set(cx - 2, S1 + 1, z, "light_gray_stained_glass_pane")
            t.set(cx + 2, S1 + 1, z, "light_gray_stained_glass_pane")
    # round platform with the console
    t.disc(cx, S1, cz, 3, SFLOOR)
    t.disc(cx, S1, cz, 3, SLIGHT, hollow=True)
    for x in range(cx - 4, cx + 5):
        for z in range(cz - 4, cz + 5):
            d = math.hypot(x - cx, z - cz)
            if 3.4 <= d < 4.4 and not (abs(x - cx) <= 1 and z > cz):
                t.set(x, S1 + 1, z, "light_gray_stained_glass_pane")
    t.set(cx, S1 + 1, cz - 1, "xmen:cerebro")
    # if you fall off: a pillar with a ladder back up to the platform
    t.fill(cx, 7, cz - 3, cx, S1 - 1, cz - 3, SILVER)
    for y in range(8, S1 + 1):
        t.set(cx, y, cz - 4, "ladder[facing=north]")
    t.set(cx, S1 + 1, cz - 4, AIR)
    # the helmet hangs over the console on a cable
    t.set(cx, S1 + 6, cz - 1, "chain[axis=y]")
    t.set(cx, S1 + 5, cz - 1, "chain[axis=y]")
    t.set(cx, S1 + 4, cz - 1, "iron_trapdoor[facing=north,half=top,open=false]")
    # entrance tunnel from the corridor (z 42..47) with the X-door at the corridor wall
    t.room(cx - 3, S1, 41, cx + 3, S1 + 5, 48, SILVER, SFLOOR, SILVER)
    t.clear(cx - 1, S1 + 1, 38, cx + 1, S1 + 3, 47)
    t.fill(cx - 1, S1, 38, cx + 1, S1, 47, SFLOOR)
    t.fill(cx - 2, S1, 42, cx - 2, S1, 47, SLIGHT)
    t.fill(cx + 2, S1, 42, cx + 2, S1, 47, SLIGHT)
    x_door(t, cx - 1, S1 + 1, 48, "x")
    # sign-like letters above the door: CEREBRO in light
    t.fill(cx - 2, S1 + 5, 47, cx + 2, S1 + 5, 47, SLIGHT)


def danger_room(t):
    x1, x2, z1, z2 = 14, 38, 58, 82
    t.room(x1 - 1, S1, z1 - 1, x2 + 1, S1 + 13, z2 + 1, "xmen:danger_room_wall", "xmen:danger_room_wall",
           "xmen:danger_room_wall")
    # observation booth with a window over the door
    t.room(21, S1 + 6, 54, 31, S1 + 10, z1 - 1, SILVER, SFLOOR, SILVER)
    t.fill(22, S1 + 7, z1 - 1, 30, S1 + 9, z1 - 1, "light_blue_stained_glass")
    t.fill(25, S1 + 7, 55, 27, S1 + 7, 55, "polished_deepslate")
    t.set(26, S1 + 8, 55, SLIGHT)
    t.set(26, S1 + 10, 56, SLIGHT)
    # passage from the corridor with the door, and a ladder up to the booth
    t.room(23, S1, 54, 29, S1 + 5, z1 - 1, SILVER, SFLOOR, SILVER)
    t.clear(25, S1 + 1, 55, 27, S1 + 3, z1 - 1)
    x_door(t, 25, S1 + 1, 54, "x")
    for y in range(S1 + 1, S1 + 7):
        t.set(24, y, 55, "ladder[facing=east]")
    # the console in the middle starts the training (Java: DangerRoom)
    t.set(26, S1 + 1, 70, "xmen:danger_room_console")
    t.set(26, S1, 70, "xmen:silver_light")
    t.chest(26, S1 + 1, 59, "south", "danger_room")


def hangar(t):
    x1, x2, z1, z2 = 38, 66, 60, 90
    t.room(x1 - 1, S1, z1 - 1, x2 + 1, G - 1, z2 + 1, SILVER, SFLOOR, SILVER)
    # the roof is the basketball court: an opening the jet can fly through
    t.clear(41, G - 1, 63, 63, G - 1, 87)
    # yellow and black safety stripes along the walls, a big X under the jet
    for x in range(x1, x2 + 1):
        for z in (z1, z2):
            t.set(x, S1, z, "yellow_concrete" if (x // 2) % 2 else "black_concrete")
    for z in range(z1, z2 + 1):
        for x in (x1, x2):
            t.set(x, S1, z, "yellow_concrete" if (z // 2) % 2 else "black_concrete")
    for i in range(-8, 9):
        t.set(52 + i, S1, 75 + i, "yellow_concrete")
        t.set(52 + i, S1, 75 - i, "yellow_concrete")
    for y in range(S1 + 3, G - 2, 4):
        for z in range(z1 + 2, z2, 6):
            t.set(x1 - 1, y, z, SLIGHT)
            t.set(x2 + 1, y, z, SLIGHT)
        for x in range(x1 + 2, x2, 6):
            t.set(x, y, z2 + 1, SLIGHT)
    # passage from the corridor
    t.room(49, S1, 54, 55, S1 + 5, z1 - 1, SILVER, SFLOOR, SILVER)
    t.clear(51, S1 + 1, 55, 53, S1 + 3, z1 - 1)
    x_door(t, 51, S1 + 1, 54, "x")
    # control desk: opens the court above
    t.fill(44, S1 + 1, 61, 48, S1 + 1, 61, "polished_deepslate")
    t.set(46, S1 + 2, 61, "xmen:hangar_control")
    t.set(44, S1 + 2, 61, "xmen:silver_light")
    t.set(48, S1 + 2, 61, "xmen:silver_light")
    # tools, fuel barrels and spare parts
    for z in (66, 70, 80, 84):
        t.barrel(x2, S1 + 1, z, "mansion_armory", "west")
    t.fill(x1, S1 + 1, 86, x1 + 2, S1 + 2, 89, "iron_block")
    t.set(x1 + 1, S1 + 3, 88, "anvil[facing=north]")
    # the X-Jet itself (Java: entity/Blackbird), nose to the north
    t.mob(52, S1 + 1, 76, "xmen:blackbird", yaw=180.0)


def med_lab(t):
    x1, x2, z1, z2 = 66, 80, 38, 47
    silver_room(t, x1, z1, x2, z2, 5)
    x_door(t, 72, S1 + 1, 48, "x")
    t.fill(x1, S1 + 1, z1, x2, S1 + 1, z1, "white_concrete")
    for x in (68, 72, 76):
        t.bed(x, S1 + 1, z1 + 2, "north", "white")
        t.set(x + 1, S1 + 1, z1 + 1, "brewing_stand")
    t.fill(x1, S1 + 2, z1, x2, S1 + 3, z1, "white_stained_glass")
    for x in (67, 70, 74, 78):
        t.chest(x, S1 + 1, z2, "north", "mansion_lab")
    t.set(80, S1 + 1, 42, "cauldron")
    t.set(66, S1 + 1, 42, "lectern[facing=east,has_book=false,powered=false]")


def war_room(t):
    x1, x2, z1, z2 = 52, 64, 38, 47
    silver_room(t, x1, z1, x2, z2, 5)
    x_door(t, 57, S1 + 1, 48, "x")
    # round table with a glowing map in the middle
    t.disc(58, S1 + 1, 42, 3, "polished_deepslate")
    t.disc(58, S1 + 1, 42, 2, "light_blue_stained_glass")
    t.set(58, S1, 42, SLIGHT)
    for x, z, f in ((58, 38, "south"), (58, 46, "north"), (53, 42, "east"), (63, 42, "west"),
                    (55, 39, "south"), (61, 39, "south"), (55, 45, "north"), (61, 45, "north")):
        chair(t, x, S1 + 1, z, f, "polished_blackstone_brick")
    # big screen on the north wall
    t.fill(54, S1 + 2, z1 - 1, 62, S1 + 4, z1 - 1, "light_blue_stained_glass")
    t.fill(54, S1 + 2, z1 - 2, 62, S1 + 4, z1 - 2, SLIGHT)
    t.chest(52, S1 + 1, 47, "east", "mansion_armory")


def suit_room(t):
    x1, x2, z1, z2 = 70, 86, 56, 68
    silver_room(t, x1, z1, x2, z2, 5)
    t.clear(77, S1 + 1, 55, 79, S1 + 3, 55)
    x_door(t, 77, S1 + 1, 54, "x")
    # suits on stands along the walls behind glass, like in the movie
    suit = [{"id": "xmen:xmen_boots", "count": 1}, {"id": "xmen:xmen_pants", "count": 1},
            {"id": "xmen:xmen_suit", "count": 1}, {"id": "xmen:xmen_mask", "count": 1}]
    for x in range(72, 86, 3):
        t.set(x, S1, z2, SLIGHT)
        t.mob(x, S1 + 1, z2, "armor_stand", extra={"ArmorItems": suit, "ShowArms": True}, yaw=180.0)
        t.set(x - 1, S1 + 1, z2, "light_gray_stained_glass_pane")
        t.set(x - 1, S1 + 2, z2, "light_gray_stained_glass_pane")
    # lockers
    for z in range(58, 67, 2):
        t.barrel(x1, S1 + 1, z, "mansion_armory", "east")
        t.set(x1, S1 + 2, z, "iron_trapdoor[facing=east,half=bottom,open=false]")
    t.chest(x2, S1 + 1, 60, "west", "mansion_armory")


# ==================================================================== above ground
def grounds(t):
    # stone wall around the front with the big gate
    for x in range(0, SX):
        if not 46 <= x <= 58:
            t.set(x, G + 1, 0, "stone_brick_wall")
            t.set(x, G + 2, 0, "stone_brick_wall" if x % 6 else AIR)
    for z in range(0, 30):
        t.set(0, G + 1, z, "stone_brick_wall")
        t.set(SX - 1, G + 1, z, "stone_brick_wall")
    for x in (45, 59):
        t.fill(x, G + 1, 0, x, G + 4, 0, "stone_bricks")
        t.set(x, G + 5, 0, "chiseled_stone_bricks")
        t.set(x, G + 6, 0, "lantern[hanging=false]")
    for x in range(46, 59):
        t.fill(x, G + 1, 0, x, G + 3, 0, "iron_bars")
    # a big X in the gate
    for i in range(3):
        t.set(50 + i, G + 1 + i, 0, "iron_block")
        t.set(54 - i, G + 1 + i, 0, "iron_block")
    # driveway: gravel road, a circle around the fountain, up to the porch
    fx, fz = 52, 13
    t.fill(49, G, 0, 55, G, 4, "gravel")
    for x in range(fx - 11, fx + 12):
        for z in range(fz - 11, fz + 12):
            d = math.hypot(x - fx, z - fz)
            if 6.5 <= d <= 10.5:
                t.set(x, G, z, "gravel")
            elif 10.5 < d <= 11.5:
                t.set(x, G + 1, z, "oak_leaves[persistent=true,distance=1,waterlogged=false]")
    t.fill(49, G, 22, 55, G, 29, "gravel")
    t.clear(49, G + 1, 1, 55, G + 1, 29)
    # the fountain
    t.disc(fx, G, fz, 5, "stone_bricks")
    t.disc(fx, G, fz, 4, "water")
    t.disc(fx, G - 1, fz, 4, "stone_bricks")
    for x in range(fx - 6, fx + 7):
        for z in range(fz - 6, fz + 7):
            d = math.hypot(x - fx, z - fz)
            if 4.5 < d <= 5.5:
                t.set(x, G + 1, z, "stone_brick_wall")
    t.fill(fx, G, fz, fx, G + 3, fz, "chiseled_stone_bricks")
    t.set(fx, G + 4, fz, "water")
    # lawns with big old trees
    for x, z, h in ((12, 12, 7), (92, 12, 7), (10, 34, 6), (94, 36, 8), (8, 88, 7), (96, 90, 6), (30, 96, 6),
                    (76, 96, 7), (20, 20, 6), (84, 22, 6)):
        tree(t, x, z, h)
    # flower beds along the front of the house
    for x in list(range(28, 45)) + list(range(60, 78)):
        t.set(x, G + 1, 28, "rose_bush[half=lower]" if x % 3 == 0 else "oak_leaves[persistent=true,distance=1,waterlogged=false]")
        if x % 3 == 0:
            t.set(x, G + 2, 28, "rose_bush[half=upper]")
    # Krakoa gate hidden in a little garden in the far corner (Java: KrakoaGateBlock)
    t.fill(92, G + 1, 74, 98, G + 1, 74, "azalea_leaves[persistent=true,distance=1,waterlogged=false]")
    t.fill(92, G + 1, 82, 98, G + 1, 82, "azalea_leaves[persistent=true,distance=1,waterlogged=false]")
    t.fill(92, G + 1, 74, 92, G + 1, 82, "azalea_leaves[persistent=true,distance=1,waterlogged=false]")
    t.clear(92, G + 1, 77, 92, G + 1, 79)
    t.fill(97, G + 1, 77, 97, G + 4, 81, "moss_block")
    t.set(97, G + 1, 78, "xmen:krakoa_gate")
    t.set(97, G + 2, 78, "xmen:krakoa_gate")
    t.clear(96, G + 1, 78, 96, G + 2, 78)
    for z in range(75, 82):
        t.set(95, G + 1, z, "flowering_azalea" if z % 2 else "pink_tulip")


def tree(t, x, z, h):
    t.fill(x, G + 1, z, x, G + h, z, "dark_oak_log[axis=y]")
    t.ellipsoid(x, G + h, z, 3.5, 2.5, 3.5, "oak_leaves[persistent=true,distance=1,waterlogged=false]")
    t.fill(x, G + 1, z, x, G + h - 1, z, "dark_oak_log[axis=y]")


def house(t):
    x1, x2, z1, z2 = 22, 82, 30, 56
    top = ROOF - 1
    # inside floors
    t.clear(x1 + 1, F1 + 1, z1 + 1, x2 - 1, top, z2 - 1)
    t.fill(x1, F1, z1, x2, F1, z2, PANEL)
    t.fill(x1, F2, z1, x2, F2, z2, "spruce_planks")
    t.fill(x1, F3, z1, x2, F3, z2, "spruce_planks")
    stone_walls(t, x1, z1, x2, z2, F1, top)
    rows = [(F1 + 2, 3), (F2 + 2, 3), (F3 + 2, 2)]
    windows_x(t, z1, x1 + 3, x2 - 2, rows, skip=[(45, 59)])
    windows_x(t, z2, x1 + 3, x2 - 2, rows, skip=[(49, 54)])
    windows_z(t, x1, z1 + 8, z2 - 2, rows)
    windows_z(t, x2, z1 + 3, z2 - 2, rows)
    # roof with chimneys
    ridge = gable_roof_x(t, x1, x2, z1, z2, ROOF)
    # a cross gable over the entrance
    for i in range(7):
        t.mix(45 + i, ROOF + i, z1, 59 - i, ROOF + i, z1, STONE)
        for z in range(z1 - 1, z1 + 8):
            stair(t, 44 + i, ROOF + i, z, "east")
            stair(t, 60 - i, ROOF + i, z, "west")
    t.fill(51, ROOF + 2, z1, 53, ROOF + 4, z1, "glass_pane")
    t.fill(51, ROOF + 1, z1, 53, ROOF + 1, z1, TRIM)
    t.fill(52, ROOF + 7, z1 - 1, 52, ROOF + 7, z1 + 7, ROOF_BLOCK)
    for x in (30, 52, 74):
        chimney(t, x, z2 - 5, ROOF, ridge + 3)
    porch(t)
    inside_ground_floor(t)
    inside_first_floor(t)
    inside_attic(t)
    ivy(t, x1, z1, x2, z2)


def porch(t):
    # stone porch with arches and a balcony on top (the castle's main entrance)
    x1, x2, z1 = 45, 59, 23
    t.fill(x1, G, z1, x2, G, 29, TRIM)
    for x in (x1, x2):
        for z in (z1, 26):
            t.fill(x, G + 1, z, x, G + 5, z, "stone_bricks")
            t.set(x, G + 1, z, "chiseled_stone_bricks")
    for x in range(x1, x2 + 1):
        stair(t, x, G + 5, z1, "north", "stone_brick_stairs", "top") if x not in (x1, x2) else None
    t.fill(x1, G + 6, z1, x2, G + 6, 29, "stone_bricks")
    for x in range(x1, x2 + 1):
        for z in (z1,):
            t.set(x, G + 7, z, "stone_brick_wall" if (x - x1) % 2 == 0 else AIR)
    t.fill(x1, G + 7, z1, x1, G + 7, 29, "stone_brick_wall")
    t.fill(x2, G + 7, z1, x2, G + 7, 29, "stone_brick_wall")
    t.fill(x1 + 1, G + 6, z1 + 1, x2 - 1, G + 6, 29, "polished_andesite")
    for x in (x1 + 1, x2 - 1):
        t.set(x, G + 7, z1 + 1, "lantern[hanging=false]")
    for x in (x1 + 3, x2 - 3):
        t.set(x, G + 5, 27, "chain[axis=y]")
        t.set(x, G + 4, 27, "lantern[hanging=true]")
    # steps and the big front doors
    for x in range(48, 57):
        stair(t, x, G, z1 - 1, "south", "stone_brick_stairs")
    t.clear(51, G + 1, 30, 52, G + 2, 30)
    double_door(t, 51, G + 1, 30, "south")
    t.fill(53, G + 1, 30, 53, G + 2, 30, "stone_bricks")
    t.fill(50, G + 3, 30, 53, G + 3, 30, "chiseled_stone_bricks")


def ivy(t, x1, z1, x2, z2):
    for x in range(x1, x2 + 1):
        if 44 <= x <= 60:
            continue
        if t.rand.random() < 0.35:
            h = t.rand.randint(3, 12)
            for y in range(G + 1, G + 1 + h):
                if t.get(x, y, z1 - 1) in (None, "minecraft:air") and t.get(x, y, z1) not in (None,
                                                                                            "minecraft:air") \
                        and "pane" not in (t.get(x, y, z1) or ""):
                    t.set(x, y, z1 - 1, "vine[south=true]")
    for z in range(z1, z2 + 1):
        for x, side, face in ((x1 - 1, "east", x1), (x2 + 1, "west", x2)):
            if t.rand.random() < 0.3:
                for y in range(G + 1, G + 1 + t.rand.randint(3, 10)):
                    if t.get(x, y, z) in (None, "minecraft:air") and "pane" not in (t.get(face, y, z) or "") \
                            and t.get(face, y, z) not in (None, "minecraft:air"):
                        t.set(x, y, z, f"vine[{side}=true]")


def double_door(t, x, y, z, facing, wood="dark_oak"):
    """Two doors next to each other (the second one at x + 1 or z + 1) with their hinges on the outside,
    so they open away from each other like a real double door."""
    second = (x + 1, z) if facing in ("north", "south") else (x, z + 1)
    # the hinge side is seen from the way the door faces: "right" of south is west, of west is north, ...
    first_hinge = "right" if facing in ("south", "west") else "left"
    other = "left" if first_hinge == "right" else "right"
    t.door(x, y, z, facing, wood, first_hinge)
    t.door(second[0], y, second[1], facing, wood, other)


def wall_x(t, z, x1, x2, y1, y2, block=PANEL):
    t.fill(x1, y1, z, x2, y2, z, block)


def wall_z(t, x, z1, z2, y1, y2, block=PANEL):
    t.fill(x, y1, z1, x, y2, z2, block)


def inside_ground_floor(t):
    y1, y2 = F1 + 1, F2 - 1
    wall_z(t, 43, 31, 55, y1, y2)
    wall_z(t, 60, 31, 55, y1, y2)
    wall_x(t, 44, 23, 81, y1, y2)
    # doors
    for x, z, f in ((43, 37, "east"), (60, 37, "east"), (43, 50, "east"), (60, 50, "east")):
        double_door(t, x, y1, z, f)
    for x, z in ((30, 44), (70, 44), (51, 44)):
        double_door(t, x, y1, z, "south")
    # back door to the terrace
    double_door(t, 51, y1, 56, "south")

    # ---- the entrance hall: two floors high with a grand staircase up to the gallery
    t.clear(46, F2, 31, 57, F2, 40)
    rug(t, 49, 31, 55, 41, F1 + 1, "red", "black")
    for i in range(7):
        for x in range(50, 55):
            stair(t, x, F1 + 1 + i, 35 + i, "south", "dark_oak_stairs")
        if i < 6:
            t.set(50, F1 + 2 + i, 35 + i, "dark_oak_fence")
            t.set(54, F1 + 2 + i, 35 + i, "dark_oak_fence")
    t.fill(49, F2, 42, 55, F2, 43, PANEL)
    t.clear(49, F1 + 1, 42, 55, F2 - 1, 43)
    t.set(52, F3 - 1, 36, "chain[axis=y]")
    t.set(52, F3 - 2, 36, "chain[axis=y]")
    t.set(52, F3 - 3, 36, "lantern[hanging=true]")
    t.set(46, F1 + 1, 31, "potted_fern")
    t.set(57, F1 + 1, 31, "potted_fern")

    # ---- Professor X's study: desk, fireplace, chess table and books
    rug(t, 30, 33, 40, 41, F1 + 1, "blue", "light_blue")
    for z in range(31, 44):
        if t.get(42, F1 + 1, z) == "minecraft:air" and z not in (37, 38):
            t.fill(42, F1 + 1, z, 42, F1 + 4, z, "bookshelf")
    for x in range(30, 42):
        if x not in (30, 31):
            t.fill(x, F1 + 1, 43, x, F1 + 4, 43, "bookshelf")
    # big desk with the professor behind it
    for x in range(33, 38):
        t.set(x, F1 + 1, 35, "dark_oak_stairs[facing=north,half=top,shape=straight,waterlogged=false]")
    t.set(33, F1 + 2, 35, "lantern[hanging=false]")
    t.set(37, F1 + 2, 35, "flower_pot")
    t.chest(31, F1 + 1, 31, "south", "mansion_study")
    # fireplace on the west wall
    t.fill(28, F1 + 1, 37, 28, F1 + 4, 41, "polished_blackstone_bricks")
    t.set(28, F1 + 1, 39, "campfire[lit=true,signal_fire=false,facing=east]")
    t.fill(29, F1 + 4, 37, 29, F1 + 4, 41, "dark_oak_slab[type=top]")
    t.set(29, F1 + 5, 38, "candle[candles=3,lit=true]")
    t.set(29, F1 + 5, 40, "candle[candles=2,lit=true]")
    # chess table (Xavier and Magneto's favourite game)
    t.set(39, F1 + 1, 39, "dark_oak_fence")
    t.set(39, F1 + 2, 39, "black_carpet")
    t.set(40, F1 + 1, 39, "dark_oak_fence")
    t.set(40, F1 + 2, 39, "white_carpet")
    chair(t, 39, F1 + 1, 38, "south")
    chair(t, 40, F1 + 1, 40, "north")
    chandelier(t, 35, F2 - 1, 38)

    # ---- library
    for z in range(46, 55, 3):
        t.fill(24, F1 + 1, z, 30, F1 + 3, z, "bookshelf")
    for x in range(23, 43):
        if x not in (28, 29):
            t.fill(x, F1 + 1, 55, x, F1 + 5, 55, "bookshelf")
    for x in (34, 38):
        t.set(x, F1 + 1, 49, "dark_oak_fence")
        t.set(x, F1 + 2, 49, "dark_oak_pressure_plate")
        chair(t, x, F1 + 1, 48, "south")
        chair(t, x, F1 + 1, 50, "north")
    t.set(36, F1 + 1, 53, "lectern[facing=north,has_book=false,powered=false]")
    t.chest(41, F1 + 1, 45, "west", "mansion")
    chandelier(t, 33, F2 - 1, 50)

    # ---- dining hall with the elevator in a corner
    for x in range(49, 58):
        t.set(x, F1 + 1, 50, "dark_oak_fence")
        t.set(x, F1 + 2, 50, "white_carpet")
        if x % 2:
            chair(t, x, F1 + 1, 49, "south")
            chair(t, x, F1 + 1, 51, "north")
    chandelier(t, 53, F2 - 1, 50)
    t.chest(59, F1 + 1, 54, "west", "mansion_kitchen")

    # ---- classroom (Storm teaches here)
    t.fill(81, F1 + 2, 34, 81, F1 + 4, 40, "black_concrete")
    t.fill(80, F1 + 1, 36, 80, F1 + 1, 38, "dark_oak_planks")
    for x in range(64, 77, 3):
        for z in range(33, 43, 3):
            t.set(x, F1 + 1, z, "oak_slab[type=top]")
            chair(t, x - 1, F1 + 1, z, "east", "oak")
    chandelier(t, 72, F2 - 1, 37)

    # ---- the rec room: sofas, TV, pool table and a fireplace
    t.fill(81, F1 + 2, 47, 81, F1 + 3, 50, "black_concrete")
    for z in range(46, 52):
        t.set(76, F1 + 1, z, "red_nether_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]")
    for x in range(77, 81):
        t.set(x, F1 + 1, 52, "red_nether_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
    rug(t, 77, 46, 80, 51, F1 + 1, "brown", "black")
    t.fill(64, F1 + 1, 47, 67, F1 + 1, 49, "green_wool")
    t.set(64, F1 + 1, 47, "dark_oak_log[axis=y]")
    t.set(67, F1 + 1, 49, "dark_oak_log[axis=y]")
    t.set(64, F1 + 1, 49, "dark_oak_log[axis=y]")
    t.set(67, F1 + 1, 47, "dark_oak_log[axis=y]")
    t.fill(61, F1 + 1, 53, 61, F1 + 4, 55, "polished_blackstone_bricks")
    t.set(61, F1 + 1, 54, "campfire[lit=true,signal_fire=false,facing=east]")
    t.set(70, F1 + 1, 55, "jukebox")
    chandelier(t, 70, F2 - 1, 50)


def inside_first_floor(t):
    y1, y2 = F2 + 1, F3 - 1
    # hallway z 42..44 with bedrooms on both sides
    wall_x(t, 41, 23, 81, y1, y2, "spruce_planks")
    wall_x(t, 45, 23, 81, y1, y2, "spruce_planks")
    t.clear(44, y1, 41, 59, y2, 41)
    for x in (44, 45, 58, 59):
        t.set(x, y1, 41, AIR)
    for x in (30, 37, 43, 60, 67, 74):
        wall_z(t, x, 31, 40, y1, y2, "spruce_planks")
        wall_z(t, x, 46, 55, y1, y2, "spruce_planks")
    # the gallery around the hall stays open; the room above the dining hall is a common room
    t.fill(46, F2, 31, 57, F2, 40, AIR)
    for z in range(31, 41):
        t.set(45, F2 + 1, z, "dark_oak_fence")
        t.set(58, F2 + 1, z, "dark_oak_fence")
    for x in range(46, 58):
        if not 50 <= x <= 54:
            t.set(x, F2 + 1, 41, "dark_oak_fence")
    t.door(52, y1, 45, "south", "spruce")
    for x in range(48, 57, 4):
        t.set(x, y1, 54, "red_nether_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
        t.set(x + 1, y1, 54, "red_nether_brick_stairs[facing=south,half=bottom,shape=straight,waterlogged=false]")
    rug(t, 47, 47, 57, 52, y1, "light_gray", "gray")
    rooms = [(23, 29), (31, 36), (38, 42), (61, 66), (68, 73), (75, 81)]
    colors = ["red", "blue", "green", "yellow", "purple", "cyan", "orange", "lime", "light_blue", "magenta",
              "pink", "gray"]
    i = 0
    for xa, xb in rooms:
        for za, zb, door_z, facing in ((31, 40, 41, "north"), (46, 55, 45, "south")):
            mid = (xa + xb) // 2
            t.door(mid, y1, door_z, facing, "spruce")
            back = za if facing == "north" else zb
            # two beds with the heads at the outer wall
            head_dir = "north" if facing == "north" else "south"
            foot_z = back + (2 if facing == "north" else -2)
            t.bed(xa, y1, foot_z, head_dir, colors[i % len(colors)])
            t.bed(xb, y1, foot_z, head_dir, colors[(i + 3) % len(colors)])
            t.chest(mid, y1, za + 4 if facing == "north" else zb - 4, "south", "mansion_dorm")
            t.set(xa, y1, (za + zb) // 2 + 1, "oak_slab[type=top]")
            t.set(xa, y1 + 1, (za + zb) // 2 + 1, "lantern[hanging=false]")
            rug(t, xa + 1, za + 2, xb - 1, zb - 2, y1, colors[(i + 5) % len(colors)], "white") \
                if xb - xa >= 4 else None
            i += 1
    for x in range(26, 80, 6):
        t.set(x, y2, 43, "lantern[hanging=true]")
    # ladder up to the attic
    for y in range(y1, F3 + 1):
        t.set(81, y, 43, "ladder[facing=west]")
    t.set(81, F3, 43, "ladder[facing=west]")


def inside_attic(t):
    y = F3 + 1
    for x in range(26, 80, 7):
        t.chest(x, y, 43, "south", "mansion")
    t.set(81, F3, 43, "ladder[facing=west]")
    for x in range(28, 78, 8):
        t.set(x, y, 40, "white_bed[facing=north,part=foot]")
        t.set(x, y, 39, "white_bed[facing=north,part=head]")
    for x in range(30, 80, 10):
        t.set(x, y, 46, "lantern[hanging=false]")


def tower(t):
    """The round tower on the north-west corner, with battlements on top."""
    cx, cz, r = 22, 30, 6
    top = G + 27
    for y in range(F1, top + 1):
        for x in range(cx - r - 1, cx + r + 2):
            for z in range(cz - r - 1, cz + r + 2):
                d = math.hypot(x - cx, z - cz)
                if d <= r - 0.6:
                    t.set(x, y, z, AIR if y not in (F1, F2, F3, ROOF + 1, top) else PANEL)
                elif d <= r + 0.4:
                    t.set(x, y, z, t.rand.choice(STONE))
    # battlements
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            d = math.hypot(x - cx, z - cz)
            if r - 0.6 < d <= r + 0.4:
                t.set(x, top + 1, z, "stone_bricks" if (x + z) % 2 else AIR)
                if (x + z) % 2:
                    t.set(x, top + 2, z, "stone_brick_slab[type=bottom]")
    # windows all around
    for y0 in (F1 + 2, F2 + 2, F3 + 2, ROOF + 3):
        for a in range(0, 360, 45):
            x = round(cx + math.cos(math.radians(a)) * r)
            z = round(cz + math.sin(math.radians(a)) * r)
            t.fill(x, y0, z, x, y0 + 1, z, "glass_pane")
    # ladder inside, holes in every floor, a flag on top
    for y in range(F1 + 1, top + 1):
        t.set(cx, y, cz + 5, "ladder[facing=north]")
    t.set(cx, top + 1, cz, "dark_oak_fence")
    t.fill(cx, top + 1, cz, cx, top + 5, cz, "dark_oak_fence")
    t.fill(cx + 1, top + 4, cz, cx + 3, top + 5, cz, "yellow_wool")
    t.set(cx + 2, top + 4, cz, "black_wool")
    t.set(cx + 2, top + 5, cz, "black_wool")
    # a way in from Professor X's study
    for x, z in ((27, 33), (28, 33)):
        t.clear(x, F1 + 1, z, x, F1 + 2, z)
    t.clear(26, F1 + 1, 32, 28, F1 + 2, 34)
    t.set(cx - 3, F1 + 1, cz + 1, "chest[facing=east]", {"LootTable": "xmen:chests/mansion_study"})


def wings(t):
    for x1, x2 in ((18, 34), (70, 86)):
        z1, z2 = 56, 74
        t.fill(x1, F1, z1, x2, F1, z2, "spruce_planks")
        t.clear(x1 + 1, F1 + 1, z1 + 1, x2 - 1, F2 - 1, z2 - 1)
        stone_walls(t, x1, z1, x2, z2, F1, F2)
        t.fill(x1, F2, z1, x2, F2, z2, "spruce_planks")
        rows = [(F1 + 2, 3)]
        windows_z(t, x1, z1 + 3, z2 - 2, rows)
        windows_z(t, x2, z1 + 3, z2 - 2, rows)
        windows_x(t, z2, x1 + 3, x2 - 2, rows)
        gable_roof_z(t, z1, z2, x1, x2, F2 + 1)
    # doors from the house (library and rec room)
    for x in (28, 76):
        double_door(t, x, F1 + 1, 56, "south")
    # the doors out onto the terrace
    t.door(34, F1 + 1, 63, "east", "dark_oak")
    t.door(70, F1 + 1, 63, "west", "dark_oak")
    # west wing: kitchen and cafeteria
    t.fill(19, F1 + 1, 57, 19, F1 + 1, 73, "smooth_stone")
    for z in range(58, 73, 3):
        t.set(19, F1 + 1, z, "smoker[facing=east,lit=false]")
        t.chest(19, F1 + 2, z + 1, "east", "mansion_kitchen") if z % 2 else None
    t.set(19, F1 + 1, 66, "cauldron")
    for z in range(60, 72, 4):
        for x in range(23, 32):
            t.set(x, F1 + 1, z, "oak_fence")
            t.set(x, F1 + 2, z, "white_carpet")
            if x % 2:
                chair(t, x, F1 + 1, z - 1, "south", "oak")
                chair(t, x, F1 + 1, z + 1, "north", "oak")
    # east wing: gym (Colossus lifts here) with a weight bench and punching bags
    for x, z in ((74, 60), (80, 60), (74, 68), (80, 68)):
        t.fill(x, F1 + 3, z, x, F1 + 6, z, "chain[axis=y]")
        t.fill(x, F1 + 1, z, x, F1 + 2, z, "brown_wool")
    t.fill(76, F1 + 1, 64, 78, F1 + 1, 64, "iron_block")
    t.set(77, F1 + 2, 64, "anvil[facing=east]")
    rug(t, 72, 58, 84, 72, F1 + 1, "blue", "gray")
    for x in range(72, 85, 4):
        t.set(x, F2 - 1, 65, "lantern[hanging=true]")
    for x in range(22, 32, 4):
        t.set(x, F2 - 1, 65, "lantern[hanging=true]")


def court(t):
    # terrace behind the house
    t.fill(35, G, 57, 69, G, 61, "polished_andesite")
    for x in range(35, 70):
        t.set(x, G + 1, 61, "stone_brick_wall" if x % 4 else "lantern[hanging=false]")
    t.clear(50, G + 1, 61, 54, G + 1, 61)
    for x in (37, 67):
        t.set(x, G + 1, 58, "potted_azalea_bush")
    # the basketball court: its floor is the roof of the hangar and slides open (Java: CourtFloorBlock)
    t.fill(40, G, 62, 64, G, 88, "white_concrete")
    t.fill(41, G, 63, 63, G, 87, "xmen:court_floor[open=false]")
    for z, d in ((61, 1), (89, -1)):
        t.fill(52, G + 1, z, 52, G + 4, z, "iron_bars")
        t.fill(51, G + 5, z, 53, G + 6, z, "white_concrete")
    t.set(52, G + 4, 61, "iron_bars")
    t.set(52, G + 4, 89, "iron_bars")
    # outside control to open the hangar roof (for landing)
    t.set(38, G + 1, 75, "polished_andesite")
    t.set(38, G + 2, 75, "xmen:hangar_control")
    # benches along the side
    for z in range(66, 86, 5):
        stair(t, 66, G + 1, z, "east", "spruce_stairs")
        stair(t, 66, G + 1, z + 1, "east", "spruce_stairs")
    # garden behind the court
    for x in range(36, 70, 2):
        t.set(x, G + 1, 92, "oak_leaves[persistent=true,distance=1,waterlogged=false]")
    for x in range(38, 68, 6):
        t.set(x, G + 1, 96, "rose_bush[half=lower]")
        t.set(x, G + 2, 96, "rose_bush[half=upper]")


def elevator(t):
    """3x3 elevator platforms: in a wood-panelled corner of the dining hall and down on the silver level.
    Jump to go up, sneak to go down (Java: ElevatorBlock)."""
    x1, z1, x2, z2 = LIFT
    for y in (F1, S1):
        t.fill(x1, y, z1, x2, y, z2, "xmen:elevator")
        t.clear(x1, y + 1, z1, x2, y + 3, z2)
    # the alcove upstairs: panelled walls with a little light
    t.fill(x2 + 1, F1 + 1, z1, x2 + 1, F1 + 4, z1, PANEL)
    t.fill(x1, F1 + 1, z2 + 1, x2 + 1, F1 + 4, z2 + 1, PANEL)
    t.fill(x1, F1 + 4, z1, x2, F1 + 4, z2, PANEL)
    # downstairs: the cabin opens onto the corridor
    t.room(x1 - 1, S1, z1 - 1, x2 + 1, S1 + 5, 48, SILVER, None, SILVER)
    t.fill(x1, S1, z1, x2, S1, z2, "xmen:elevator")
    t.clear(x1, S1 + 1, z1, x2, S1 + 4, 48)
    t.set(x1 + 1, S1 + 5, z1 + 1, SLIGHT)
    t.fill(x1, S1, 48, x2, S1, 48, SFLOOR)


def people(t):
    """The X-Men from the movies (Java: entity/MutantNpc with the skins from tools/skins.py)."""
    t.npc(35, F1 + 1, 34, "professor_x", yaw=0.0)
    t.npc(48, F1 + 1, 34, "cyclops")
    t.npc(78, F1 + 1, 37, "storm", yaw=90.0)
    t.npc(71, F1 + 1, 50, "wolverine")
    t.npc(78, F1 + 1, 48, "rogue")
    t.npc(66, F1 + 1, 52, "iceman")
    t.npc(33, F1 + 1, 51, "beast")
    t.npc(55, F1 + 1, 53, "gambit")
    t.npc(40, F2 + 1, 43, "nightcrawler")
    t.npc(66, F2 + 1, 43, "kitty_pryde")
    t.npc(45, G + 1, 59, "jubilee")
    t.npc(78, F1 + 1, 66, "colossus")
    t.npc(73, S1 + 1, 43, "jean_grey")
