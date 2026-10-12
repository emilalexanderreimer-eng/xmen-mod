"""Mister Sinister's lab: the abandoned Essex Corporation building on the surface, and a whole underground complex
beneath it, reached by a freight elevator and a spiral staircase 34 blocks deep.

  Surface       the Essex Corp building: big red diamond and ESSEX letters on the front, a radio mast with a red
                light on the roof (to spot it from far away), a lobby with the elevator and the stairs down
  Lab level     the arrival hall at the bottom of the shaft, and tunnels to:
                  Sinister's study (north-west), the generator room (north-east),
                  the specimen cells (west), the clone nursery (east, more sleeping clones),
                  the main lab with Sinister and his clone tanks (middle),
                  the room of failed experiments (south), and old rough tunnels that wind around to an old well
                  that is a second, hidden way out to the surface

Sinister and his clones are real characters (Java: entity/SinisterBoss, entity/SinisterClone).
"""
from lib import Template

N = 84              # the template is N x N
G = 40              # surface floor
F = 6               # floor of the lab level
TOP = G + 30        # template height

WALL = "polished_blackstone_bricks"
FLOOR = "polished_blackstone"
CLONE = "xmen:sinister_clone"
SLEEPING = {"Dormant": True, "NoAI": True}

# the main lab (42 x 42) sits in the middle of the lab level
LAB_X, LAB_Z = 21, 24
# the freight elevator (3x3) and the spiral staircase (5x5 with a middle column), under the lobby
LIFT = (36, 10, 38, 12)
STAIR_C = (46, 11)


class Shifted:
    """Lets the main lab be drawn in its own coordinates (0..41) and moved into place."""
    SIX = ("fill", "clear", "walls", "room", "scatter", "line", "fill_if", "mix", "ruin")

    def __init__(self, t, dx, dy, dz):
        self.t, self.d = t, (dx, dy, dz)

    def __getattr__(self, name):
        method = getattr(self.t, name)
        dx, dy, dz = self.d

        def call(*args, **kwargs):
            args = list(args)
            args[0] += dx
            args[1] += dy
            args[2] += dz
            if name in self.SIX:
                args[3] += dx
                args[4] += dy
                args[5] += dz
            return method(*args, **kwargs)
        return call


SPACES = []
PLANNING = [False]


def room(t, x1, z1, x2, z2, height=5, floor=FLOOR, wall=WALL, lights=5, light="redstone_lamp[lit=true]"):
    """A room on the lab level (inside coordinates): floor at F, air up to F+height, solid shell around.
    Rooms are only collected here; carve() builds every shell first and then every inside, so a corridor that
    touches a room's wall opens straight into it."""
    if PLANNING[0]:
        SPACES.append((x1, z1, x2, z2, height, floor, wall, lights, light))


def corridor(t, x1, z1, x2, z2):
    """A 2-wide, 3-high corridor with a lamp in the ceiling every few blocks."""
    room(t, x1, z1, x2, z2, height=3, lights=6)


def carve(t):
    for x1, z1, x2, z2, h, floor, wall, lights, light in SPACES:
        t.fill(x1 - 1, F - 1, z1 - 1, x2 + 1, F + h + 1, z2 + 1, wall)
        t.scatter(x1 - 1, F, z1 - 1, x2 + 1, F + h + 1, z2 + 1, "cracked_polished_blackstone_bricks", 0.12,
                  lambda s, wall=wall: s == "minecraft:" + wall)
    for x1, z1, x2, z2, h, floor, wall, lights, light in SPACES:
        t.clear(x1, F + 1, z1, x2, F + h, z2)
        t.fill(x1, F, z1, x2, F, z2, floor)
    for x1, z1, x2, z2, h, floor, wall, lights, light in SPACES:
        if lights:
            for x in range(x1 + (2 if x2 - x1 > 3 else 0), x2 + 1, lights):
                for z in range(z1 + (2 if z2 - z1 > 3 else 0), z2 + 1, lights):
                    t.set(x, F + h + 1, z, light)
    SPACES.clear()


def rough_tunnel(t, points, radius=1.6, rand=None):
    """An old, rough tunnel through the rock, winding through the given (x, y, z) points."""
    for (x1, y1, z1), (x2, y2, z2) in zip(points, points[1:]):
        steps = max(abs(x2 - x1), abs(z2 - z1), abs(y2 - y1), 1)
        for i in range(steps + 1):
            f = i / steps
            x, y, z = x1 + (x2 - x1) * f, y1 + (y2 - y1) * f, z1 + (z2 - z1) * f
            wobble = t.rand.uniform(-0.3, 0.4)
            t.ellipsoid(x, y + 1.5, z, radius + wobble, 1.9, radius + wobble, "air")


# ============================================================================================== surface
def letter_blocks(t, text, x, y, z, state):
    """Writes big 3x5 block letters on a wall facing north (left to right = east to west... seen from outside)."""
    font = {
        "E": ["###", "#..", "##.", "#..", "###"],
        "S": ["###", "#..", "###", "..#", "###"],
        "X": ["#.#", "#.#", ".#.", "#.#", "#.#"],
        "C": ["###", "#..", "#..", "#..", "###"],
        "O": ["###", "#.#", "#.#", "#.#", "###"],
        "R": ["##.", "#.#", "##.", "#.#", "#.#"],
        "P": ["###", "#.#", "###", "#..", "#.."],
    }
    # seen from the north, east is on the left, so letters run from high x to low x
    cx = x
    for ch in text:
        if ch == " ":
            cx -= 3
            continue
        rows = font[ch]
        for r, row in enumerate(rows):
            for c, cell in enumerate(row):
                if cell == "#":
                    t.set(cx - c, y + 4 - r, z, state)
        cx -= 4


def surface(t):
    lo_x, hi_x, lo_z, hi_z = 22, 62, 0, 30
    # ground: a flat yard (cuts into hills, fills dips) with cracked paths
    t.fill(lo_x, G - 6, lo_z, hi_x, G - 1, hi_z, "stone")
    t.fill(lo_x, G - 2, lo_z, hi_x, G - 1, hi_z, "dirt")
    t.fill(lo_x, G, lo_z, hi_x, G, hi_z, "grass_block")
    t.clear(lo_x, G + 1, lo_z, hi_x, G + 26, hi_z)
    t.scatter(lo_x, G + 1, lo_z, hi_x, G + 1, hi_z, "short_grass", 0.25)
    t.fill(39, G, 0, 43, G, 4, "gravel")
    t.scatter(39, G, 0, 43, G, 4, "cobblestone", 0.3)
    # rusty fence around the yard with a broken gate
    t.walls(lo_x + 1, G + 1, lo_z + 1, hi_x - 1, G + 2, hi_z - 1, "iron_bars")
    t.clear(38, G + 1, lo_z + 1, 44, G + 2, lo_z + 1)
    t.ruin(lo_x + 1, G + 1, lo_z + 1, hi_x - 1, G + 2, hi_z - 1, 0.04)

    # the building: x 28..56, z 5..25, two floors and a flat roof
    x1, z1, x2, z2 = 28, 5, 56, 25
    F2 = G + 6
    ROOF = G + 12
    t.fill(x1, G - 1, z1, x2, G, z2, "gray_concrete")
    t.walls(x1, G + 1, z1, x2, ROOF, z2, "bricks")
    for y in (G + 1, F2, ROOF):
        t.walls(x1, y, z1, x2, y, z2, "gray_concrete")
    for x, z in ((x1, z1), (x2, z1), (x1, z2), (x2, z2)):
        t.fill(x, G + 1, z, x, ROOF + 1, z, "gray_concrete")
    t.fill(x1, F2, z1, x2, F2, z2, "gray_concrete")
    t.fill(x1, ROOF, z1, x2, ROOF, z2, "gray_concrete")
    t.walls(x1, ROOF + 1, z1, x2, ROOF + 1, z2, "stone_brick_wall")
    t.clear(x1 + 1, G + 1, z1 + 1, x2 - 1, F2 - 1, z2 - 1)
    t.clear(x1 + 1, F2 + 1, z1 + 1, x2 - 1, ROOF - 1, z2 - 1)
    t.fill(x1 + 1, G, z1 + 1, x2 - 1, G, z2 - 1, "polished_andesite")
    t.scatter(x1 + 1, G, z1 + 1, x2 - 1, G, z2 - 1, "cracked_stone_bricks", 0.15)
    # broken windows on every side, both floors
    for y in (G + 2, F2 + 2):
        for x in range(x1 + 3, x2 - 2, 4):
            for z in (z1, z2):
                t.fill(x, y, z, x + 1, y + 2, z, "gray_stained_glass_pane")
        for z in range(z1 + 3, z2 - 2, 4):
            for x in (x1, x2):
                t.fill(x, y, z, x, y + 2, z + 1, "gray_stained_glass_pane")
    t.ruin(x1, G + 2, z1, x2, ROOF - 1, z2, 0.0)
    for (x, y, z), (state, _) in list(t.blocks.items()):
        if state.startswith("minecraft:gray_stained_glass_pane") and t.rand.random() < 0.35:
            t.set(x, y, z, "air")
    # the front (north): double glass doors, the red diamond logo and the ESSEX letters
    t.clear(41, G + 1, z1, 43, G + 3, z1)
    t.fill(41, G + 4, z1, 43, G + 4, z1, "gray_concrete")
    t.fill(40, G + 1, z1 - 1, 40, G + 4, z1 - 1, "quartz_pillar")
    t.fill(44, G + 1, z1 - 1, 44, G + 4, z1 - 1, "quartz_pillar")
    t.fill(40, G + 5, z1 - 1, 44, G + 5, z1 - 1, "smooth_quartz_slab[type=bottom]")
    letter_blocks(t, "ESSEX", 51, F2 + 1, z1 - 1, "white_concrete")
    # Sinister's red diamond as a big sign standing on the roof, lit in the middle
    for i in range(5):
        for dx in range(-i, i + 1):
            t.set(42 + dx, ROOF + 2 + i, z1 + 1, "red_concrete")
            t.set(42 + dx, ROOF + 10 - i, z1 + 1, "red_concrete")
    for y in range(ROOF + 4, ROOF + 9):
        t.set(42, y, z1 + 1, "redstone_lamp[lit=true]")
    t.set(42, ROOF + 1, z1 + 1, "polished_blackstone_wall")
    # vines growing over the walls
    for (x, y, z), (state, _) in list(t.blocks.items()):
        if G + 2 <= y <= ROOF and state == "minecraft:bricks" and t.rand.random() < 0.12:
            side = "north" if z == z1 else "south" if z == z2 else "west" if x == x1 else "east"
            nx, nz = {"north": (x, z - 1), "south": (x, z + 1), "west": (x - 1, z), "east": (x + 1, z)}[side]
            if t.get(nx, y, nz) in (None, "minecraft:air"):
                opposite = {"north": "south", "south": "north", "west": "east", "east": "west"}[side]
                t.set(nx, y, nz, f"vine[{opposite}=true]")
    # radio mast on the roof with a red light on top that you can see from far away
    t.fill(52, ROOF + 1, 21, 52, ROOF + 16, 21, "iron_bars")
    for y in range(ROOF + 2, ROOF + 16, 4):
        t.set(51, y, 21, "iron_bars")
        t.set(53, y, 21, "iron_bars")
    t.set(52, ROOF + 17, 21, "redstone_lamp[lit=true]")
    t.set(52, ROOF + 16, 21, "redstone_block")

    # lobby: reception desk, waiting chairs, the elevator and the stairs going down
    t.fill(30, G + 1, 14, 33, G + 1, 14, "dark_oak_planks")
    t.fill(33, G + 1, 15, 33, G + 1, 17, "dark_oak_planks")
    t.set(31, G + 1, 16, "dark_oak_stairs[facing=north]")
    for x in range(50, 55, 2):
        t.set(x, G + 1, 7, "dark_oak_stairs[facing=south]")
    t.set(35, G + 3, 6, "lantern[hanging=false]")
    t.set(49, G + 3, 6, "lantern[hanging=false]")
    t.fill(30, G + 1, 23, 31, G + 3, 23, "bookshelf")
    t.chest(30, G + 1, 22, "east", "sinister_lab")
    t.scatter(x1 + 1, G + 1, z1 + 1, x2 - 1, F2 - 1, z2 - 1, "cobweb", 0.01,
              lambda s: s == "minecraft:air")
    # an office upstairs with a ladder up
    t.fill(31, G + 1, 24, 31, F2 + 5, 24, "ladder[facing=north]")
    t.clear(31, F2, 24, 31, F2, 24)
    t.set(31, F2, 24, "ladder[facing=north]")
    t.fill(34, F2 + 1, 18, 38, F2 + 1, 19, "dark_oak_slab[type=top]")
    t.chest(36, F2 + 1, 21, "north", "sinister_lab")
    t.set(34, F2 + 2, 18, "lectern[facing=south]")
    t.ruin(x1 + 8, F2, z1 + 4, x1 + 14, F2, z1 + 9, 0.3)   # a hole in the floor


def lift(t):
    """Freight elevator: jump on the platform to ride up, sneak to ride down (Java: world/ElevatorBlock)."""
    x1, z1, x2, z2 = LIFT
    for y in (G, F):
        t.walls(x1 - 1, y + 1, z1 - 1, x2 + 1, y + 3, z2 + 1, "iron_bars")
        t.fill(x1 - 1, y + 4, z1 - 1, x2 + 1, y + 4, z2 + 1, "iron_block")
        t.clear(x1, y + 1, z1, x2, y + 3, z2)
        t.fill(x1, y, z1, x2, y, z2, "xmen:elevator")
        t.set(x1 + 1, y + 4, z1 + 1, "redstone_lamp[lit=true]")
    # the cabin opens to the south upstairs and downstairs
    t.clear(x1, G + 1, z2 + 1, x2, G + 3, z2 + 1)
    t.clear(x1, F + 1, z2 + 1, x2, F + 3, z2 + 1)
    # solid shaft between the two stops, so nobody falls down it
    t.fill(x1 - 1, F + 5, z1 - 1, x2 + 1, G - 1, z2 + 1, "deepslate_tiles")


def spiral(t):
    """A spiral staircase in a round tower of blackstone, from the lobby floor down to the lab level."""
    cx, cz = STAIR_C
    ring = [(-1, -1), (0, -1), (1, -1), (1, 0), (1, 1), (0, 1), (-1, 1), (-1, 0)]
    t.fill(cx - 2, F, cz - 2, cx + 2, G, cz + 2, WALL)
    t.clear(cx - 1, F + 1, cz - 1, cx + 1, G + 4, cz + 1)
    t.fill(cx, F, cz, cx, G + 3, cz, "polished_blackstone_wall")
    t.fill(cx - 1, F, cz - 1, cx + 1, F, cz + 1, FLOOR)
    names = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}
    steps = G - F - 1
    for i in range(steps + 1):
        dx, dz = ring[i % 8]
        y = G - i
        if i == 0:
            facing = "north"
        else:
            px, pz = ring[(i - 1) % 8]
            facing = names[(px - dx, pz - dz)]
        t.set(cx + dx, y, cz + dz, f"polished_blackstone_brick_stairs[facing={facing}]")
        if i % 8 == 4:
            t.set(cx, y + 2, cz, "lantern[hanging=false]")
    # top: a railing around the hole with an opening at the first step
    t.walls(cx - 2, G + 1, cz - 2, cx + 2, G + 1, cz + 2, "polished_blackstone_wall")
    t.clear(cx - 1, G + 1, cz - 2, cx - 1, G + 3, cz - 2)
    t.set(cx - 1, G, cz - 2, FLOOR)
    # bottom: a doorway on the south side, under the higher steps
    t.clear(cx - 1, F + 1, cz + 2, cx + 1, F + 3, cz + 2)


# ============================================================================================== lab level
def arrival(t):
    """The hall at the bottom of the elevator and the stairs, with a checkpoint into the lab."""
    room(t, 32, 6, 52, 20, height=5)
    # the elevator and staircase sit inside this hall; checkpoint desk and a warning in red light
    t.fill(38, F + 1, 18, 39, F + 1, 18, "polished_blackstone_slab[type=top]")
    t.fill(44, F + 1, 18, 45, F + 1, 18, "polished_blackstone_slab[type=top]")
    t.set(34, F + 1, 19, "red_banner[rotation=8]")
    t.set(50, F + 1, 19, "red_banner[rotation=8]")
    # corridor south into the main lab
    corridor(t, 41, 21, 42, 23)


def study(t):
    """Sinister's study: a Victorian room with books, his journals and a portrait wall."""
    room(t, 3, 4, 17, 18, height=5, floor="dark_oak_planks", wall="deepslate_bricks", lights=0)
    t.fill(4, F, 5, 16, F, 17, "red_carpet")
    t.fill(3, F + 1, 4, 3, F + 4, 18, "bookshelf")
    t.fill(3, F + 1, 4, 17, F + 4, 4, "bookshelf")
    t.clear(9, F + 1, 4, 11, F + 3, 4)
    t.fill(9, F + 1, 4, 11, F + 3, 4, "chiseled_bookshelf[facing=south]")
    t.fill(8, F + 1, 10, 12, F + 1, 11, "dark_oak_slab[type=top]")
    t.set(10, F + 1, 12, "dark_oak_stairs[facing=north]")
    t.set(9, F + 2, 10, "lectern[facing=south]")
    t.set(12, F + 2, 10, "brewing_stand")
    t.set(11, F + 2, 11, "candle[candles=3,lit=true]")
    t.chest(16, F + 1, 5, "west", "sinister_lab")
    t.chest(16, F + 1, 17, "west", "sinister_lab")
    for x, z in ((5, 16), (15, 16), (5, 6), (15, 8)):
        t.set(x, F + 1, z, "lantern[hanging=false]")
    t.set(17, F + 1, 11, "red_banner[rotation=12]")
    t.set(10, F + 6, 11, "lantern[hanging=true]")
    corridor(t, 18, 11, 31, 12)


def generator(t):
    """Power for the lab: furnaces, lava behind glass and lightning rods humming with red light."""
    room(t, 66, 4, 80, 18, height=6, wall="deepslate_tiles", light="shroomlight", lights=4)
    t.fill(70, F - 1, 8, 76, F - 1, 14, "lava")
    t.fill(70, F, 8, 76, F, 14, "orange_stained_glass")
    for x, z in ((68, 6), (78, 6), (68, 16), (78, 16)):
        t.fill(x, F + 1, z, x, F + 3, z, "blast_furnace[facing=south,lit=true]")
        t.set(x, F + 4, z, "lightning_rod")
    t.fill(67, F + 1, 11, 67, F + 2, 11, "redstone_block")
    t.fill(79, F + 1, 11, 79, F + 2, 11, "redstone_block")
    t.barrel(80, F + 1, 5, "sinister_lab", facing="west")
    corridor(t, 53, 11, 65, 12)


def cells(t):
    """Specimen cells: a long hall of barred cells where Sinister kept mutants he studied."""
    room(t, 4, 26, 15, 62, height=4, lights=6)
    for z in range(27, 62, 5):
        for x1, x2 in ((4, 7), (12, 15)):
            t.fill(x1, F + 1, z, x2, F + 4, z, WALL)
            t.fill(x1, F + 1, z + 4, x2, F + 4, z + 4, WALL)
            gate = x2 + 1 if x1 == 4 else x1 - 1
            t.fill(gate, F + 1, z + 1, gate, F + 4, z + 3, "iron_bars")
            t.set(x1 + 1 if x1 == 4 else x2 - 1, F + 1, z + 2, "chain[axis=y]")
            if t.rand.random() < 0.5:
                t.set(x1 + 2 if x1 == 4 else x2 - 2, F + 1, z + 1, "bone_block")
            t.scatter(x1, F + 1, z + 1, x2, F + 3, z + 3, "cobweb", 0.08, lambda s: s == "minecraft:air")
    t.chest(5, F + 1, 59, "east", "sinister_lab")
    # connections: north to the study, east to the main lab
    corridor(t, 9, 19, 10, 25)
    corridor(t, 16, 44, 20, 45)


def nursery(t):
    """The clone nursery: rows of small tanks with more sleeping clones."""
    room(t, 67, 26, 80, 62, height=5, light="sea_lantern", lights=6)
    for z in range(29, 60, 6):
        for x in (68, 78):
            t.fill(x, F + 1, z, x + 2, F + 1, z + 2, FLOOR)
            t.fill(x, F + 2, z, x + 2, F + 4, z + 2, "lime_stained_glass")
            t.clear(x + 1, F + 2, z + 1, x + 1, F + 4, z + 1)
            t.fill(x, F + 5, z, x + 2, F + 5, z + 2, "iron_block")
            t.set(x + 1, F + 1, z + 1, "sea_lantern")
    # only some tanks still hold a clone, the others are empty and dark
    for x, z in ((68, 35), (78, 41), (68, 47), (78, 53)):
        t.mob(x + 1, F + 2, z + 1, CLONE, extra=SLEEPING, yaw=-90.0 if x == 68 else 90.0)
    t.chest(73, F + 1, 61, "north", "sinister_lab")
    corridor(t, 73, 19, 74, 25)
    corridor(t, 63, 44, 66, 45)


def failed_experiments(t):
    """Failed experiments: bubbling vats, bones and soul fire."""
    room(t, 24, 68, 60, 80, height=5, wall="deepslate_bricks", light="soul_lantern[hanging=true]", lights=0)
    for x in range(27, 58, 6):
        t.set(x, F + 1, 70, "water_cauldron[level=3]")
        t.set(x, F, 70, "magma_block")
        t.set(x + 1, F + 1, 70, "bone_block")
        t.set(x, F + 1, 78, "soul_campfire[lit=true]")
        t.set(x + 3, F + 6, 74, "soul_lantern[hanging=true]")
    t.fill(38, F + 1, 74, 46, F + 1, 74, "smooth_stone_slab[type=top]")
    t.scatter(24, F + 1, 68, 60, F + 1, 80, "bone_block", 0.03, lambda s: s == "minecraft:air")
    t.chest(59, F + 1, 79, "west", "sinister_lab")
    corridor(t, 41, 66, 42, 67)


def old_tunnels(t):
    """Rough old tunnels that wind from the failed experiments around the cells and up to an old well."""
    rough_tunnel(t, [(23, F, 76), (14, F, 72), (8, F, 68), (6, F, 64)], radius=1.5)
    t.clear(5, F + 1, 63, 6, F + 3, 63)   # into the cells
    rough_tunnel(t, [(61, F, 76), (70, F, 72), (77, F + 2, 68), (79, F + 4, 66)], radius=1.5)
    # the well shaft: a ladder from the lab level all the way up to an old stone well on the surface
    wx, wz = 81, 66
    t.fill(wx - 1, F + 4, wz - 1, wx + 1, G, wz + 1, "cobblestone")
    t.scatter(wx - 1, F + 4, wz - 1, wx + 1, G, wz + 1, "mossy_cobblestone", 0.3)
    t.clear(wx, F + 5, wz, wx, G + 2, wz)
    t.fill(wx, F + 5, wz, wx, G, wz, "ladder[facing=north]")
    t.set(wx, F + 4, wz, "cobblestone")
    t.clear(wx - 1, F + 5, wz, wx - 1, F + 7, wz)
    # a small grassy patch around the well on the surface
    t.fill(wx - 2, G - 2, wz - 2, wx + 2, G - 1, wz + 2, "dirt")
    t.fill(wx - 2, G, wz - 2, wx + 2, G, wz + 2, "grass_block")
    t.set(wx, G, wz, "ladder[facing=north]")
    t.clear(wx - 2, G + 1, wz - 2, wx + 2, G + 5, wz + 2)
    # the well on the surface
    t.walls(wx - 1, G + 1, wz - 1, wx + 1, G + 1, wz + 1, "cobblestone_wall")
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        t.fill(wx + dx, G + 1, wz + dz, wx + dx, G + 3, wz + dz, "oak_fence")
    t.fill(wx - 1, G + 4, wz - 1, wx + 1, G + 4, wz + 1, "oak_slab[type=bottom]")
    t.clear(wx, G + 1, wz - 1, wx, G + 2, wz - 1)
    # a few rotten supports and cobwebs in the tunnels
    for x, z in ((14, 72), (70, 72)):
        t.fill(x, F + 1, z, x, F + 3, z, "oak_fence")
        t.set(x, F + 4, z, "oak_planks")


def main_lab(t):
    """The main lab (the room Sinister fights in): 42 x 42, with his dais and the eight clone tanks."""
    s = Shifted(t, LAB_X, F, LAB_Z)
    s.room(0, 0, 0, 41, 13, 41, WALL, FLOOR, WALL)
    s.clear(1, 1, 1, 40, 12, 40)
    s.scatter(0, 0, 0, 41, 13, 41, "cracked_polished_blackstone_bricks", 0.15,
              lambda st: st == "minecraft:" + WALL)
    s.fill(1, 0, 1, 40, 0, 40, FLOOR)
    for x in range(4, 39, 6):
        for z in range(4, 39, 6):
            s.set(x, 13, z, "shroomlight" if (x + z) % 12 == 8 else "redstone_lamp[lit=true]")
    for z in (2, 39):
        s.fill(1, 10, z, 40, 10, z, "iron_block")
        s.fill(1, 11, z, 40, 11, z, "polished_basalt[axis=x]")
    # clone tanks along the side walls; each holds a sleeping clone that smashes out when he calls it
    for x, z in [(3, 4), (3, 12), (3, 26), (3, 34), (36, 4), (36, 12), (36, 26), (36, 34)]:
        s.fill(x, 1, z, x + 2, 1, z + 2, FLOOR)
        s.set(x + 1, 1, z + 1, "sea_lantern")
        s.fill(x, 2, z, x + 2, 4, z + 2, "red_stained_glass")
        s.clear(x + 1, 2, z + 1, x + 1, 4, z + 1)
        s.fill(x, 5, z, x + 2, 5, z + 2, "iron_block")
        s.set(x + 1, 5, z + 1, "redstone_lamp[lit=true]")
        s.fill(x + 1, 6, z + 1, x + 1, 12, z + 1, "chain[axis=y]")
        s.mob(x + 1, 2, z + 1, CLONE, extra=SLEEPING, yaw=-90.0 if x < 20 else 90.0)
    # operating tables
    for x, z in ((10, 20), (31, 20)):
        s.fill(x - 1, 1, z, x + 1, 1, z, "smooth_stone_slab[type=top]")
        s.set(x - 2, 1, z, "anvil[facing=east]")
        s.set(x + 2, 1, z, "brewing_stand")
        s.fill(x, 8, z, x, 12, z, "chain[axis=y]")
        s.set(x, 7, z, "lantern[hanging=true]")
        s.set(x, 2, z, "red_carpet")
    # his dais with the red diamond
    s.disc(20.5, 0, 20.5, 8, "crimson_nylium")
    s.disc(20.5, 0, 20.5, 7, FLOOR)
    s.disc(20.5, 1, 20.5, 4, WALL)
    s.disc(20.5, 1, 20.5, 4, FLOOR, hollow=True)
    for i in range(4):
        for dx, dz in ((i, 3 - i), (-1 - i, 3 - i), (i, -4 + i), (-1 - i, -4 + i)):
            s.set(21 + dx, 1, 21 + dz, "red_concrete")
    for x, z in ((20, 20), (21, 20), (20, 21), (21, 21)):
        s.set(x, 1, z, "red_concrete")
    for x, z in ((15, 15), (26, 15), (15, 26), (26, 26)):
        s.fill(x, 1, z, x, 4, z, "polished_blackstone_wall")
        s.set(x, 5, z, "redstone_lamp[lit=true]")
    for i in range(6):
        s.set(20 - i, 0, 8 + i, "crying_obsidian")
        s.set(21 + i, 0, 8 + i, "crying_obsidian")
        s.set(20 - i, 0, 33 - i, "crying_obsidian")
        s.set(21 + i, 0, 33 - i, "crying_obsidian")
    # Sinister himself (Java: entity/SinisterBoss), on his dais, facing the door from the arrival hall
    s.mob(20, 2, 21, "xmen:mister_sinister", yaw=180.0)
    # desk and chests at the back
    s.fill(17, 1, 36, 24, 1, 37, "polished_blackstone_slab[type=top]")
    s.set(20, 1, 38, "polished_blackstone_stairs[facing=north]")
    s.set(18, 2, 36, "brewing_stand")
    s.set(23, 2, 36, "lectern[facing=north]")
    s.chest(17, 1, 39, "north", "sinister_lab")
    s.chest(24, 1, 39, "north", "sinister_lab")
    # gene archive along the north wall
    for x in range(9, 33):
        if x not in (19, 20, 21, 22):
            s.set(x, 1, 1, "polished_blackstone_slab[type=top]")
            s.set(x, 2, 1, "brewing_stand" if x % 2 else "red_stained_glass_pane")
    # doors: north (arrival hall), west (cells), east (nursery), south (failed experiments)
    s.clear(20, 1, 0, 21, 3, 0)
    s.clear(0, 1, 20, 0, 3, 21)
    s.clear(41, 1, 20, 41, 3, 21)
    s.clear(20, 1, 41, 21, 3, 41)


def build():
    t = Template("sinister_lab", N, TOP, N, seed=8)
    # lab level: first every room function runs once on a scratch template only to collect its rooms, the rooms
    # get carved, then the functions run for real to furnish them
    rooms = (arrival, study, generator, cells, nursery, failed_experiments)
    PLANNING[0] = True
    scratch = Template("scratch", N, TOP, N)
    for f in rooms:
        f(scratch)
    PLANNING[0] = False
    carve(t)
    for f in rooms:
        f(t)
    old_tunnels(t)
    main_lab(t)
    surface(t)
    lift(t)
    spiral(t)
    return t
