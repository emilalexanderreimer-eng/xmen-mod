"""Draws everything for the Weapon X facility's people: their skins, the claws, the soldiers' rifle, the tank
console, spawn egg models, names, and the console's loot table and mining tag.
Run from the repo root: python3 tools/make_weapon_x.py (needs Pillow). Pass --preview to also write
build/weapon_x_preview.png with the front of every skin side by side."""
import json
import os
import random
import sys

from PIL import Image

ASSETS = os.path.join("src", "main", "resources", "assets", "xmen")
DATA = os.path.join("src", "main", "resources", "data")
rand = random.Random(11)


def hexc(value):
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(color, amount):
    r, g, b, a = color
    if amount >= 0:
        return tuple(round(c + (255 - c) * amount) for c in (r, g, b)) + (a,)
    return tuple(round(c * (1 + amount)) for c in (r, g, b)) + (a,)


def noisy(color, strength=0.05):
    if color[3] == 0:
        return color
    return shade(color, rand.uniform(-strength, strength))


CLEAR = (0, 0, 0, 0)


def box_faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def paint_box(img, u, v, w, h, d, color_at, strength=0.05):
    for face, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
        for x in range(fw):
            for y in range(fh):
                img.putpixel((fx + x, fy + y), noisy(color_at(face, x, y), strength))


def save(img, *path):
    full = os.path.join(ASSETS, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


# ============================================================================ the people
# Every skin is described by a few colors and some details; skin() turns that into a 64x64 player skin.
SKINS = {
    "soldier": dict(skin="c99a7a", hair="1a1a1a", eyes="2b2b2b", balaclava="1c1c1c", goggles="3d6d7a",
                    helmet="2f3428", shirt="1f221f", vest="3a3f33", sleeve="1f221f", glove="111111",
                    pants="1f221f", boots="111111", belt="4a4a3a", pouches=True),
    "scientist": dict(skin="e2b796", hair="6b4a2b", eyes="3b6ea5", glasses=True, shirt="9cc3e6", tie="34495e",
                      coat="eef0f2", sleeve="eef0f2", glove="e2b796", pants="4d4f55", boots="2b2b2b",
                      belt=None, coat_long=True),
    "stryker": dict(skin="d9a888", hair="a9a9a4", hair_style="short", eyes="4a6680", shirt="c9bf9a", tie="2e3222",
                    jacket="4b5320", sleeve="4b5320", glove="d9a888", pants="4b5320", boots="1b1b1b",
                    belt="2a2a1a", ribbons=True, wrinkles=True),
    "deathstrike": dict(skin="ead1c3", hair="0d0d10", hair_style="bob", eyes="49d3ff", lips="8e1b2b",
                        shirt="141416", jacket="1c1c22", sleeve="1c1c22", glove="ead1c3", pants="141416",
                        boots="0b0b0b", belt="2c2c34"),
    "jason": dict(skin="e6d2c4", hair="c9c4bc", hair_style="buzz", eyes="cfd8dc", shirt="b7bcc1", sleeve="b7bcc1",
                  glove="e6d2c4", pants="9aa1a8", boots="d8d8d8", belt=None, pale=True),
    "x24": dict(skin="c7946d", hair="1e1712", hair_style="logan", eyes="5a3a22", beard="1e1712",
                shirt="2b2b2b", sleeve="c7946d", glove="c7946d", pants="2b3a55", boots="4a3020",
                belt="3a2615", feral=True),
    "weapon_xi": dict(skin="d8b49a", hair=None, hair_style="bald", eyes="d01010", stitched=True,
                      shirt="d8b49a", sleeve="d8b49a", glove="d8b49a", pants="1a1a1a", boots="0d0d0d",
                      belt="333333", markings="8a1010"),
    "logan": dict(skin="c99670", hair="1f1813", hair_style="logan", eyes="6b4a2b", beard="1f1813",
                  shirt="c99670", sleeve="c99670", glove="c99670", pants="8d9096", boots="c99670",
                  belt=None, chest_hair=True, wx_helmet=True),
}


def skin(spec):
    img = Image.new("RGBA", (64, 64), CLEAR)
    S = hexc(spec["skin"])
    hair = hexc(spec["hair"]) if spec.get("hair") else None
    style = spec.get("hair_style", "short")
    eyes = hexc(spec["eyes"])

    def head(face, x, y):
        if spec.get("balaclava"):
            bal = hexc(spec["balaclava"])
            if face == "front" and y in (3, 4) and 1 <= x <= 6:
                return S if y == 4 and x not in (2, 5) else (eyes if x in (2, 5) and y == 4 else S)
            return bal
        if face == "top":
            return hair if hair and style not in ("bald", "buzz") else (shade(hair, 0.2) if style == "buzz" else S)
        if face == "bottom":
            return shade(S, -0.2)
        if face == "back":
            if style == "bald":
                return S
            if style == "buzz":
                return shade(hair, 0.2) if y < 3 else S
            if style == "bob":
                return hair
            return hair if y < 6 else S
        if face in ("left", "right"):
            if style == "bald":
                return S
            if style == "buzz":
                return shade(hair, 0.2) if y < 2 else S
            if style == "bob":
                return hair if y < 7 else S
            if style == "logan" and y >= 2 and ((face == "right" and x >= 5) or (face == "left" and x <= 2)):
                return spec.get("beard") and hexc(spec["beard"]) or hair
            return hair if y < 2 or (y < 4 and ((face == "right" and x < 3) or (face == "left" and x > 4))) else S
        # the face itself
        if style == "bob":
            if y < 2 or x in (0, 7):
                return hair
        elif style == "logan":
            if y < 2:
                return hair
            if y == 2 and x in (0, 7):
                return hair
            if x in (0, 7) and y <= 6:
                return hexc(spec["beard"])    # mutton chops
            if y == 6 and x in (1, 6):
                return hexc(spec["beard"])
        elif style == "short":
            if y < 2:
                return hair
            if y == 2 and x in (0, 7):
                return hair
        elif style == "buzz":
            if y < 1:
                return shade(hair, 0.2)
        if y == 2 and x in (1, 2, 5, 6):
            brow = hair if hair and style not in ("bald", "buzz") else shade(S, -0.25)
            if spec.get("feral"):
                brow = shade(brow, -0.3)
            return brow
        if y == 3 and x in (1, 6):
            return hexc("f0f0f0") if not spec.get("pale") else hexc("e8e8f0")
        if y == 3 and x in (2, 5):
            return eyes
        if spec.get("glasses") and y == 3 and x in (0, 3, 4, 7):
            return hexc("2a2a2a")
        if y == 4 and x in (3, 4):
            return shade(S, -0.15)
        if spec.get("wrinkles") and y == 4 and x in (1, 6):
            return shade(S, -0.12)
        if y == 6 and 2 <= x <= 5:
            if spec.get("stitched"):
                return hexc("3a1a1a") if x % 2 == 0 else shade(S, -0.35)
            if spec.get("lips"):
                return hexc(spec["lips"]) if x in (3, 4) else S
            return shade(S, -0.3)
        if spec.get("markings") and (x, y) in ((1, 5), (6, 5), (0, 4), (7, 4)):
            return hexc(spec["markings"])
        return S

    def hat(face, x, y):
        if spec.get("helmet"):
            h = hexc(spec["helmet"])
            if face == "front":
                if y < 2:
                    return h
                if y == 3 and 1 <= x <= 6:
                    return hexc(spec["goggles"])
                if y == 2 and 1 <= x <= 6:
                    return hexc("222222")
                return CLEAR
            if face == "bottom":
                return CLEAR
            if face in ("left", "right", "back") and y >= 3:
                return CLEAR
            return h
        if spec.get("wx_helmet"):
            # the Weapon X helmet: a metal band with cables sticking out
            metal, dark = hexc("9aa0a6"), hexc("50555a")
            if face == "top":
                return metal if (x + y) % 3 else dark
            if face == "bottom":
                return CLEAR
            if face == "front":
                return metal if y < 2 else (dark if y == 2 else CLEAR)
            if y < 2:
                return metal
            if y == 2:
                return dark
            if face == "back" and y < 6 and x in (2, 5):
                return hexc("2a2a2a")   # cables
            return CLEAR
        return CLEAR

    def torso(face, x, y):
        base = hexc(spec["shirt"])
        if spec.get("markings") and face in ("front", "back") and (x + y) % 7 == 0 and y < 6:
            return hexc(spec["markings"])
        if spec.get("chest_hair") and face == "front" and ((2 <= x <= 5 and y == 2) or (x in (3, 4) and 3 <= y <= 7)) and (x + y) % 3:
            return shade(hexc("4a3020"), 0.1)
        if spec.get("vest") and face in ("front", "back") and 1 <= y <= 9:
            vest = hexc(spec["vest"])
            if spec.get("pouches") and face == "front" and y in (6, 7) and x in (1, 2, 5, 6):
                return shade(vest, -0.25)
            return vest if face == "back" or x not in (3, 4) or y > 2 else base
        if spec.get("tie") and face == "front" and x in (3, 4) and y <= 7:
            if spec.get("jacket") or spec.get("coat"):
                if y <= 1 or x == 3:
                    return hexc(spec["tie"])
            else:
                return hexc(spec["tie"])
        if spec.get("tie") and face == "front" and x in (2, 5) and y <= 1:
            return base
        if spec.get("ribbons") and face == "front" and y == 2 and x in (1, 2):
            return hexc("b8322a") if x == 1 else hexc("2c5aa0")
        if spec.get("belt") and y == 10:
            return hexc(spec["belt"]) if not (face == "front" and x in (3, 4)) else hexc("a08a4a")
        if y >= 11 and spec.get("pants"):
            return hexc(spec["pants"])
        if face in ("left", "right"):
            return shade(base, -0.1)
        return base

    def jacket(face, x, y):
        col = spec.get("jacket") or spec.get("coat")
        if not col:
            return CLEAR
        c = hexc(col)
        if face == "front" and 2 <= x <= 5 and y <= 4:
            return CLEAR if abs(x - 3.5) < 2.5 - y * 0.5 or y < 2 else c
        if face == "front" and x in (3, 4):
            return CLEAR if spec.get("coat") else shade(c, -0.2)
        if face in ("top", "bottom"):
            return CLEAR
        return c if face != "back" else shade(c, -0.05)

    def arm(face, x, y):
        sleeve = hexc(spec["sleeve"])
        if y >= 10 or face == "bottom":
            return hexc(spec["glove"])
        if spec.get("markings") and sleeve == S and (x + y) % 4 == 0 and y < 6:
            return hexc(spec["markings"])
        if spec.get("jacket") and y == 9:
            return shade(sleeve, -0.25)
        return sleeve if face != "back" else shade(sleeve, -0.08)

    def sleeve_overlay(face, x, y):
        if not spec.get("coat"):
            return CLEAR
        if face in ("top", "bottom") or y > 9:
            return CLEAR
        return hexc(spec["coat"])

    def leg(face, x, y):
        if face == "bottom" or y >= 9:
            return hexc(spec["boots"])
        if spec.get("pouches") and face in ("right", "left") and y in (3, 4):
            return shade(hexc(spec["pants"]), -0.3)
        return hexc(spec["pants"]) if face != "back" else shade(hexc(spec["pants"]), -0.08)

    def coat_leg(face, x, y):
        if not spec.get("coat_long") or face in ("top", "bottom") or y > 5:
            return CLEAR
        return hexc(spec["coat"])

    paint_box(img, 0, 0, 8, 8, 8, head)
    paint_box(img, 32, 0, 8, 8, 8, hat, 0.03)
    paint_box(img, 16, 16, 8, 12, 4, torso)
    paint_box(img, 16, 32, 8, 12, 4, jacket, 0.03)
    paint_box(img, 40, 16, 4, 12, 4, arm)
    paint_box(img, 32, 48, 4, 12, 4, arm)
    paint_box(img, 40, 32, 4, 12, 4, sleeve_overlay, 0.03)
    paint_box(img, 48, 48, 4, 12, 4, sleeve_overlay, 0.03)
    paint_box(img, 0, 16, 4, 12, 4, leg)
    paint_box(img, 16, 48, 4, 12, 4, leg)
    paint_box(img, 0, 32, 4, 12, 4, coat_leg, 0.03)
    paint_box(img, 0, 48, 4, 12, 4, coat_leg, 0.03)
    return img


# ============================================================================ claws, rifle, console
def claws():
    img = Image.new("RGBA", (16, 16), CLEAR)
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), noisy(shade(hexc("c8ced6"), 0.25 - 0.3 * ((x + y) % 4) / 4), 0.04))
    save(img, "textures", "entity", "weapon_x", "claws.png")


def rifle():
    img = Image.new("RGBA", (16, 16), CLEAR)
    body, dark, wood = hexc("2b2d2f"), hexc("161718"), hexc("3b3f35")
    pixels = []
    # a rifle lying diagonally: stock bottom-left, barrel top-right
    for i in range(14):
        x, y = 1 + i, 14 - i
        pixels.append((x, y, body))
        pixels.append((x, y - 1, dark if i > 8 else body))
    for i in range(4):   # stock
        pixels += [(1 + i, 13 - i + 2, wood), (2 + i, 15 - i, wood)]
    pixels += [(6, 12, dark), (7, 12, dark), (7, 13, dark), (8, 10, dark), (9, 10, dark)]   # grip and magazine
    pixels += [(6, 8, dark), (7, 8, dark), (7, 7, body)]    # scope
    for x, y, c in pixels:
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), noisy(c, 0.06))
    save(img, "textures", "item", "weapon_x_rifle.png")


def console():
    side = Image.new("RGBA", (16, 16), CLEAR)
    for x in range(16):
        for y in range(16):
            side.putpixel((x, y), noisy(hexc("5f6469") if 0 < x < 15 and 0 < y < 15 else hexc("3a3d40"), 0.04))
    save(side, "textures", "block", "weapon_x_tank_console_side.png")
    top = side.copy()
    for x in range(3, 13):
        for y in range(3, 13):
            top.putpixel((x, y), noisy(hexc("2a2d30"), 0.04))
    for x, y in ((5, 5), (8, 5), (11, 5)):
        top.putpixel((x, y), hexc("e03030"))
    save(top, "textures", "block", "weapon_x_tank_console_top.png")
    front = side.copy()
    for x in range(2, 14):
        for y in range(2, 9):
            front.putpixel((x, y), noisy(hexc("2f8fb0") if (x + y) % 5 else hexc("7fe3ff"), 0.05))
    for x in range(3, 13):   # heartbeat line on the screen
        front.putpixel((x, 6 if x not in (7, 8) else (3 if x == 7 else 8)), hexc("b8ff9a"))
    for x in range(3, 13):
        front.putpixel((x, 11), hexc("1a1a1a"))
    for x, c in ((4, "e8c020"), (7, "e03030"), (10, "40c040")):
        front.putpixel((x, 12), hexc(c))
        front.putpixel((x + 1, 12), hexc(c))
    save(front, "textures", "block", "weapon_x_tank_console_front.png")


def item_icon(name, pixels):
    img = Image.new("RGBA", (16, 16), CLEAR)
    for (x, y), c in pixels.items():
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), noisy(c, 0.05))
    save(img, "textures", "item", name + ".png")


def mutant_items():
    silver, light, dark = hexc("c8ced6"), hexc("f2f5f8"), hexc("6e757d")
    skin, knuckle = hexc("c99670"), hexc("9a6e4e")
    # Adamantium Claws: a fist with three long blades
    px = {}
    for i in range(3):
        for j in range(9):
            x, y = 5 + i * 3 + j // 3, 9 - j
            px[(x, y)] = light if j % 3 == 0 else silver
            px[(x + 1, y)] = dark
    for x in range(3, 13):
        for y in range(10, 15):
            px[(x, y)] = skin if y > 10 else knuckle
    item_icon("adamantium_claws", px)
    # Adamantium Skeleton: a syringe full of liquid silver
    px = {}
    for i in range(10):
        x, y = 3 + i, 12 - i
        px[(x, y)] = hexc("e8eef4")
        px[(x + 1, y)] = silver if i < 7 else hexc("e8eef4")
        px[(x, y - 1)] = dark if i in (0, 9) else (light if i < 7 else hexc("e8eef4"))
    for i in range(3):
        px[(13 + i, 2 - i)] = dark
    px[(1, 13)] = dark
    px[(2, 14)] = dark
    px[(2, 12)] = dark
    px[(0, 14)] = dark
    item_icon("adamantium_skeleton", px)
    # Healing Factor Serum: a vial of glowing green
    px = {}
    for x in range(5, 11):
        for y in range(5, 15):
            edge = x in (5, 10) or y == 14
            px[(x, y)] = hexc("d8e4e8") if edge else (hexc("6cf04a") if y > 7 else hexc("e8f4f8"))
    for x in range(6, 10):
        px[(x, 3)] = hexc("7a5230")
        px[(x, 4)] = hexc("7a5230")
    px[(7, 9)] = hexc("c8ffb0")
    item_icon("healing_factor_serum", px)
    # Inhibitor Collar: a metal ring with a red light
    px = {}
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + ((y - 8) * 1.3) ** 2) ** 0.5
            if 4.5 <= d <= 7:
                px[(x, y)] = silver if y < 8 else dark
    px[(7, 13)] = hexc("ff2020")
    px[(8, 13)] = hexc("ff2020")
    px[(7, 12)] = hexc("801010")
    px[(8, 12)] = hexc("801010")
    item_icon("inhibitor_collar", px)


# ============================================================================ models, names, data
PEOPLE = {
    "weapon_x_soldier": ("Weapon X Soldier", "Weapon-X-Soldat"),
    "weapon_x_scientist": ("Weapon X Scientist", "Weapon-X-Wissenschaftler"),
    "william_stryker": ("Colonel William Stryker", "Colonel William Stryker"),
    "lady_deathstrike": ("Lady Deathstrike", "Lady Deathstrike"),
    "jason_stryker": ("Jason Stryker", "Jason Stryker"),
    "x24": ("X-24", "X-24"),
    "weapon_xi": ("Weapon XI", "Weapon XI"),
    "weapon_x_logan": ("Logan (Weapon X)", "Logan (Weapon X)"),
}
NAMES = {
    "item.xmen.weapon_x_rifle": ("Weapon X Rifle", "Weapon-X-Gewehr"),
    "block.xmen.weapon_x_tank_console": ("Weapon X Tank Console", "Weapon-X-Tankkonsole"),
    "item.xmen.adamantium_claws": ("Adamantium Claws", "Adamantium-Krallen"),
    "item.xmen.adamantium_skeleton": ("Adamantium Skeleton", "Adamantium-Skelett"),
    "item.xmen.healing_factor_serum": ("Healing Factor Serum", "Heilfaktor-Serum"),
    "item.xmen.inhibitor_collar": ("Inhibitor Collar", "Hemmhalsband"),
}
RECIPES = {
    "adamantium_claws": {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["III", " L "],
                         "key": {"I": {"item": "xmen:adamantium_ingot"}, "L": {"item": "minecraft:leather"}},
                         "result": {"id": "xmen:adamantium_claws", "count": 1}},
    "adamantium_skeleton": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["III", "ISI", "III"],
                            "key": {"I": {"item": "xmen:adamantium_ingot"},
                                    "S": {"item": "xmen:healing_factor_serum"}},
                            "result": {"id": "xmen:adamantium_skeleton", "count": 1}},
}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def models_and_lang():
    models = os.path.join(ASSETS, "models")
    for name in PEOPLE:
        write_json(os.path.join(models, "item", name + "_spawn_egg.json"),
                   {"parent": "minecraft:item/template_spawn_egg"})
    for name in ("adamantium_skeleton", "healing_factor_serum", "inhibitor_collar"):
        write_json(os.path.join(models, "item", name + ".json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": "xmen:item/" + name}})
    write_json(os.path.join(models, "item", "adamantium_claws.json"),
               {"parent": "minecraft:item/handheld", "textures": {"layer0": "xmen:item/adamantium_claws"}})
    for name, recipe in RECIPES.items():
        write_json(os.path.join(DATA, "xmen", "recipe", name + ".json"), recipe)
    write_json(os.path.join(models, "item", "weapon_x_rifle.json"),
               {"parent": "minecraft:item/handheld", "textures": {"layer0": "xmen:item/weapon_x_rifle"}})
    write_json(os.path.join(models, "block", "weapon_x_tank_console.json"),
               {"parent": "minecraft:block/orientable",
                "textures": {"top": "xmen:block/weapon_x_tank_console_top",
                             "front": "xmen:block/weapon_x_tank_console_front",
                             "side": "xmen:block/weapon_x_tank_console_side"}})
    write_json(os.path.join(models, "item", "weapon_x_tank_console.json"),
               {"parent": "xmen:block/weapon_x_tank_console"})
    write_json(os.path.join(ASSETS, "blockstates", "weapon_x_tank_console.json"),
               {"variants": {f"facing={f}": dict({"model": "xmen:block/weapon_x_tank_console"},
                                                 **({"y": r} if r else {}))
                             for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    names = dict(NAMES)
    for name, (en, de) in PEOPLE.items():
        names["entity.xmen." + name] = (en, de)
        names["item.xmen." + name + "_spawn_egg"] = (en + " Spawn Egg", de + " Spawn-Ei")
    for index, file in ((0, "en_us.json"), (1, "de_de.json")):
        path = os.path.join(ASSETS, "lang", file)
        with open(path) as f:
            data = json.load(f)
        for key, pair in names.items():
            data[key] = pair[index]
        with open(path, "w") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
    # the console drops itself and needs a pickaxe
    write_json(os.path.join(DATA, "xmen", "loot_table", "blocks", "weapon_x_tank_console.json"),
               {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
                   {"type": "minecraft:item", "name": "xmen:weapon_x_tank_console"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    tag = os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json")
    with open(tag) as f:
        data = json.load(f)
    if "xmen:weapon_x_tank_console" not in data["values"]:
        data["values"].append("xmen:weapon_x_tank_console")
    write_json(tag, data)


def preview(images):
    """Front view of every skin (head, body, arms, legs) at 8x scale."""
    scale = 8
    out = Image.new("RGBA", (len(images) * 18 * scale, 34 * scale), hexc("303030"))
    parts = [((8, 8, 8, 8), (4, 0)), ((20, 20, 8, 12), (4, 8)), ((44, 20, 4, 12), (0, 8)),
             ((36, 52, 4, 12), (12, 8)), ((4, 20, 4, 12), (4, 20)), ((20, 52, 4, 12), (8, 20))]
    overlays = [((40, 8, 8, 8), (4, 0)), ((20, 36, 8, 12), (4, 8)), ((44, 36, 4, 12), (0, 8)),
                ((52, 52, 4, 12), (12, 8)), ((4, 36, 4, 12), (4, 20)), ((4, 52, 4, 12), (8, 20))]
    for n, img in enumerate(images):
        for layer in (parts, overlays):
            for (u, v, w, h), (ox, oy) in layer:
                for x in range(w):
                    for y in range(h):
                        c = img.getpixel((u + x, v + y))
                        if c[3]:
                            for dx in range(scale):
                                for dy in range(scale):
                                    out.putpixel(((n * 18 + 1 + ox + x) * scale + dx, (1 + oy + y) * scale + dy), c)
    os.makedirs("build", exist_ok=True)
    out.save(os.path.join("build", "weapon_x_preview.png"))


if __name__ == "__main__":
    images = []
    for name, spec in SKINS.items():
        img = skin(spec)
        save(img, "textures", "entity", "weapon_x", name + ".png")
        images.append(img)
    claws()
    rifle()
    console()
    mutant_items()
    models_and_lang()
    if "--preview" in sys.argv:
        preview(images)
    print("weapon x assets written")
