"""Helpers to build structure templates block by block in Python.

Coordinates are template coordinates: x goes east, y goes up, z goes south, starting at 0.
Positions that are never set keep whatever the world had there (like structure void).
"""
import math
import os
import random

import nbt

DATA_VERSION = 3955  # Minecraft 1.21.1
AIR = "air"

_SPECIAL = ("torch", "lantern", "door", "bed", "carpet", "flower", "sign", "rail", "button", "pressure_plate",
            "ladder", "vine", "banner", "poppy", "tulip", "dandelion", "orchid", "allium", "bluet", "daisy",
            "cornflower", "lily", "fern", "short_grass", "tall_grass", "sapling", "candle", "chain", "bars",
            "pane", "trapdoor", "lever", "head", "skull", "pot", "rose", "bush", "mushroom", "dripleaf", "azalea",
            "sea_pickle", "kelp", "coral", "amethyst_cluster", "end_rod", "lightning_rod", "chest", "barrel",
            "anvil", "bell", "lectern", "brewing_stand", "cauldron", "spawner", "campfire", "item_frame")


def parse_state(state):
    name, props = state, {}
    if "[" in state:
        name, rest = state.split("[", 1)
        for part in rest.rstrip("]").split(","):
            if part:
                k, v = part.split("=")
                props[k.strip()] = v.strip()
    if ":" not in name:
        name = "minecraft:" + name
    return name, props


def state_key(state):
    name, props = parse_state(state)
    return name + ("[" + ",".join(f"{k}={props[k]}" for k in sorted(props)) + "]" if props else "")


FACING = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
OPPOSITE = {"north": "south", "south": "north", "east": "west", "west": "east"}


class Template:
    def __init__(self, name, sx, sy, sz, seed=1):
        self.name = name
        self.sx, self.sy, self.sz = sx, sy, sz
        self.blocks = {}
        self.entities = []
        self.rand = random.Random(seed)

    # ------------------------------------------------------------------ basics
    def inside(self, x, y, z):
        return 0 <= x < self.sx and 0 <= y < self.sy and 0 <= z < self.sz

    def set(self, x, y, z, state, data=None):
        if self.inside(x, y, z):
            self.blocks[(x, y, z)] = (state_key(state), data)

    def get(self, x, y, z):
        entry = self.blocks.get((x, y, z))
        return entry[0] if entry else None

    def fill(self, x1, y1, z1, x2, y2, z2, state):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, state)

    def fill_if(self, x1, y1, z1, x2, y2, z2, state, test):
        """Fills only where test(current_state_or_None) is true."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    if test(self.get(x, y, z)):
                        self.set(x, y, z, state)

    def mix(self, x1, y1, z1, x2, y2, z2, states):
        """Fills with a random pick from states (a list, repeat entries to weight them)."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, self.rand.choice(states))

    def clear(self, x1, y1, z1, x2, y2, z2):
        self.fill(x1, y1, z1, x2, y2, z2, AIR)

    def walls(self, x1, y1, z1, x2, y2, z2, state):
        x1, x2 = sorted((x1, x2))
        z1, z2 = sorted((z1, z2))
        self.fill(x1, y1, z1, x2, y2, z1, state)
        self.fill(x1, y1, z2, x2, y2, z2, state)
        self.fill(x1, y1, z1, x1, y2, z2, state)
        self.fill(x2, y1, z1, x2, y2, z2, state)

    def room(self, x1, y1, z1, x2, y2, z2, wall, floor=None, ceiling=None):
        """A closed box: floor at y1, ceiling at y2, air inside."""
        x1, x2 = sorted((x1, x2))
        y1, y2 = sorted((y1, y2))
        z1, z2 = sorted((z1, z2))
        self.walls(x1, y1, z1, x2, y2, z2, wall)
        self.fill(x1, y1, z1, x2, y1, z2, floor or wall)
        self.fill(x1, y2, z1, x2, y2, z2, ceiling or wall)
        self.clear(x1 + 1, y1 + 1, z1 + 1, x2 - 1, y2 - 1, z2 - 1)

    def ellipsoid(self, cx, cy, cz, rx, ry, rz, state, hollow=False, only_y=None):
        for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
            for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
                if only_y and not only_y(y):
                    continue
                for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d <= 1.0:
                        if hollow:
                            d2 = ((x - cx) / max(rx - 1, 0.5)) ** 2 + ((y - cy) / max(ry - 1, 0.5)) ** 2 \
                                + ((z - cz) / max(rz - 1, 0.5)) ** 2
                            self.set(x, y, z, state if d2 > 1.0 else AIR)
                        else:
                            self.set(x, y, z, state)

    def disc(self, cx, y, cz, r, state, hollow=False):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for z in range(int(cz - r) - 1, int(cz + r) + 2):
                d = math.hypot(x - cx, z - cz)
                if d <= r + 0.3 and (not hollow or d > r - 0.7):
                    self.set(x, y, z, state)

    def cylinder(self, cx, y1, cz, r, y2, state, hollow=False):
        for y in range(y1, y2 + 1):
            self.disc(cx, y, cz, r, state, hollow)

    def line(self, x1, y1, z1, x2, y2, z2, state):
        n = max(abs(x2 - x1), abs(y2 - y1), abs(z2 - z1), 1)
        for i in range(n + 1):
            t = i / n
            self.set(round(x1 + (x2 - x1) * t), round(y1 + (y2 - y1) * t), round(z1 + (z2 - z1) * t), state)

    def scatter(self, x1, y1, z1, x2, y2, z2, state, chance, test=None):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    if self.rand.random() < chance and (test is None or test(self.get(x, y, z))):
                        self.set(x, y, z, state)

    def ruin(self, x1, y1, z1, x2, y2, z2, chance):
        """Knocks random holes into whatever was built (turns blocks into air)."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    cur = self.get(x, y, z)
                    if cur and cur != "minecraft:air" and self.rand.random() < chance:
                        self.set(x, y, z, AIR)

    # ------------------------------------------------------------------ furniture
    def chest(self, x, y, z, facing, loot, kind="chest"):
        self.set(x, y, z, f"{kind}[facing={facing}]", {"LootTable": f"xmen:chests/{loot}"})

    def barrel(self, x, y, z, loot, facing="up"):
        self.set(x, y, z, f"barrel[facing={facing}]", {"LootTable": f"xmen:chests/{loot}"})

    def door(self, x, y, z, facing, wood="oak", hinge="left"):
        self.set(x, y, z, f"{wood}_door[facing={facing},half=lower,hinge={hinge},open=false]")
        self.set(x, y + 1, z, f"{wood}_door[facing={facing},half=upper,hinge={hinge},open=false]")

    def double_door(self, x, y, z, facing, wood="oak"):
        """Two doors side by side; the second one is to the right when looking out (facing)."""
        dx, dz = FACING[facing]
        rx, rz = -dz, dx  # right of facing... (left-hand rotation)
        self.door(x, y, z, facing, wood, "left")
        self.door(x - rx, y, z - rz, facing, wood, "right")

    def bed(self, x, y, z, facing, color="red"):
        """Foot at (x, y, z), head one block in the facing direction."""
        dx, dz = FACING[facing]
        self.set(x, y, z, f"{color}_bed[facing={facing},part=foot]")
        self.set(x + dx, y, z + dz, f"{color}_bed[facing={facing},part=head]")

    def spawner(self, x, y, z, entity):
        self.set(x, y, z, "spawner", {"SpawnData": {"entity": {"id": entity}}, "Delay": nbt.Short(20)})

    def table(self, x, y, z, top="oak_pressure_plate", leg="oak_fence"):
        self.set(x, y, z, leg)
        self.set(x, y + 1, z, top)

    def lamp_post(self, x, y, z, height=3):
        for i in range(height):
            self.set(x, y + i, z, "dark_oak_fence")
        self.set(x, y + height, z, "lantern[hanging=false]")

    # ------------------------------------------------------------------ mobs
    def mob(self, x, y, z, entity, kind=None, extra=None, yaw=0.0):
        """Places a mob. 'kind' is read by the mod's Java code (MutantMobs) to give it a name, gear and powers."""
        data = {"id": entity if ":" in entity else "minecraft:" + entity,
                "Pos": [nbt.Double(x + 0.5), nbt.Double(y), nbt.Double(z + 0.5)],
                "Rotation": [nbt.Float(yaw), nbt.Float(0.0)],
                "PersistenceRequired": nbt.Byte(1)}
        if kind:
            data["Tags"] = ["xmen." + kind]
        if extra:
            data.update(extra)
        self.entities.append({"pos": [nbt.Double(x + 0.5), nbt.Double(y), nbt.Double(z + 0.5)],
                              "blockPos": [x, y, z], "nbt": data})

    # ------------------------------------------------------------------ saving
    def save(self, out_dir):
        palette, index = [], {}
        normal, special = [], []
        for pos, (state, data) in sorted(self.blocks.items(), key=lambda e: (e[0][1], e[0][0], e[0][2])):
            if state not in index:
                name, props = parse_state(state)
                entry = {"Name": name}
                if props:
                    entry["Properties"] = props
                index[state] = len(palette)
                palette.append(entry)
            block = {"pos": list(pos), "state": index[state]}
            if data:
                block["nbt"] = data
            is_special = data is not None or any(s in state for s in _SPECIAL)
            (special if is_special else normal).append(block)
        root = {
            "DataVersion": DATA_VERSION,
            "size": [self.sx, self.sy, self.sz],
            "palette": palette,
            "blocks": normal + special,
            "entities": self.entities,
        }
        path = os.path.join(out_dir, self.name + ".nbt")
        nbt.write(path, root)
        return path, len(self.blocks), len(palette), len(self.entities)
