"""Draws the skins for Apocalypse, his Four Horsemen (Storm, Psylocke, Archangel, Magneto) and the Clan Akkaba
cultists, the celestial vein block, and writes their models, spawn eggs and names.

Run from the repo root: python3 tools/make_apocalypse.py (needs Pillow).
The cube layout matches apocalypse/client/ApocalypseModel.java and ServantModel.java.
Pass --preview to also write build/apocalypse_preview.png (flat front views to check the skins).
"""
import json
import os
import random
import sys

from PIL import Image

ASSETS = os.path.join("src", "main", "resources", "assets", "xmen")
DATA = os.path.join("src", "main", "resources", "data", "xmen")
rand = random.Random(16)
CLEAR = (0, 0, 0, 0)


def hexc(value):
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(color, amount):
    """amount > 0 lightens, < 0 darkens."""
    r, g, b, a = color
    if amount >= 0:
        return tuple(round(c + (255 - c) * amount) for c in (r, g, b)) + (a,)
    return tuple(round(c * (1 + amount)) for c in (r, g, b)) + (a,)


def noisy(color, strength=0.05):
    if color[3] == 0:
        return color
    return shade(color, rand.uniform(-strength, strength))


def box_faces(u, v, w, h, d):
    """Where each side of a model cube sits on its texture (Minecraft's box UV layout)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


LIGHT = {"top": 0.12, "bottom": -0.3, "front": 0.0, "back": -0.1, "left": -0.05, "right": -0.05}


def paint(img, box, painter, lit=True):
    """painter(face, x, y, w, h) -> color or None (= leave)."""
    u, v, w, h, d = box
    for face, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
        for x in range(fw):
            for y in range(fh):
                c = painter(face, x, y, fw, fh)
                if c is None:
                    continue
                if lit and c[3]:
                    c = shade(c, LIGHT[face])
                img.putpixel((fx + x, fy + y), noisy(c, 0.035))


HEAD, HAT, LEG, BODY, ARM = (0, 0, 8, 8, 8), (32, 0, 8, 8, 8), (0, 16, 4, 12, 4), (16, 16, 8, 12, 4), \
    (40, 16, 4, 12, 4)
# Apocalypse's extra cubes
CREST, FIN, PAD, CABLE, CAPE, TABARD = (0, 32, 2, 4, 10), (24, 32, 1, 6, 6), (38, 32, 6, 3, 6), (0, 46, 1, 8, 1), \
    (40, 41, 10, 20, 1), (4, 46, 6, 8, 1)
# the servants' extra cubes
MOHAWK, WING, BLADE = (0, 32, 2, 3, 10), (0, 45, 16, 18, 1), (34, 45, 1, 2, 10)


def face(skin, hair, eyes, mouth, hair_rows=2, beard=None, brows=None):
    """A head: skin with a face on the front and hair on top/back/the upper sides."""
    def p(f, x, y, w, h):
        if f == "top":
            return hair
        if f == "bottom":
            return skin
        if f == "back":
            return hair if y < 7 else skin
        if f in ("left", "right"):
            return hair if y < hair_rows + 1 or (y < 5 and ((f == "right" and x > 4) or (f == "left" and x < 3))) \
                else skin
        # front
        if y < hair_rows:
            return hair
        if y == 3 and x in (1, 2, 5, 6):
            return brows or shade(skin, -0.25)
        if y == 4 and x in (1, 6):
            return (240, 240, 240, 255)
        if y == 4 and x in (2, 5):
            return eyes
        if y == 6 and 3 <= x <= 4:
            return mouth
        if beard and y >= 6:
            return beard
        if y == 5 and x in (3, 4):
            return shade(skin, -0.12)
        return skin
    return p


def solid(color, edge=None):
    def p(f, x, y, w, h):
        if edge and (x in (0, w - 1) or y in (0, h - 1)) and w > 2 and h > 2:
            return edge
        return color
    return p


def clear(f, x, y, w, h):
    return CLEAR


# ============================================================================ Apocalypse
APOC_SKIN = hexc("566a99")
APOC_LIPS = hexc("3a2f5e")
ARMOR = hexc("2f3440")
ARMOR_LIGHT = hexc("4a5162")
BRONZE = hexc("8a6a3a")
GOLD = hexc("c9a227")
CAPE_BLUE = hexc("1b2036")
EYE = hexc("8fd8ff")


def apocalypse():
    img = Image.new("RGBA", (64, 64), CLEAR)
    paint(img, HEAD, face(APOC_SKIN, ARMOR, hexc("1a2a4a"), APOC_LIPS, hair_rows=0))

    def helmet(f, x, y, w, h):
        if f == "front":
            if y < 3 or x in (0, 7):
                return ARMOR_LIGHT if y == 2 and 0 < x < 7 else ARMOR
            return CLEAR
        if f == "bottom":
            return CLEAR
        if f == "top":
            return ARMOR_LIGHT if x in (3, 4) else ARMOR
        if f in ("left", "right") and y >= 6 and ((f == "right" and x < 2) or (f == "left" and x > 5)):
            return CLEAR   # open at the cheek in front
        return ARMOR_LIGHT if y % 3 == 0 else ARMOR
    paint(img, HAT, helmet)

    def body(f, x, y, w, h):
        if f in ("top", "bottom"):
            return ARMOR
        if y in (10, 11):
            return BRONZE if not (f == "front" and x in (3, 4)) else GOLD
        if f == "front":
            if x in (3, 4) and y < 10:
                return ARMOR_LIGHT
            if y in (2, 5, 8):
                return shade(ARMOR, 0.25)
            if y < 2 and x in (0, 7):
                return BRONZE
            return ARMOR
        if f == "back":
            return shade(ARMOR, -0.1)
        return ARMOR if y % 3 else ARMOR_LIGHT
    paint(img, BODY, body)

    def arm(f, x, y, w, h):
        if f == "bottom" or y >= 10:
            return APOC_SKIN   # bare blue hands
        if f == "top":
            return ARMOR
        if y in (7, 8):
            return BRONZE   # gauntlet cuffs
        return ARMOR_LIGHT if y % 3 == 0 else ARMOR
    paint(img, ARM, arm)

    def leg(f, x, y, w, h):
        if f in ("top", "bottom"):
            return shade(ARMOR, -0.3)
        if y >= 9:
            return shade(ARMOR, -0.35)  # boots
        if y in (4, 5) and f == "front":
            return ARMOR_LIGHT          # knee plates
        return ARMOR
    paint(img, LEG, leg)

    paint(img, CREST, lambda f, x, y, w, h: ARMOR_LIGHT if f == "top" or (f != "bottom" and y == 0) else ARMOR)
    paint(img, FIN, lambda f, x, y, w, h: shade(ARMOR, -0.15) if (x + y) % 3 else ARMOR_LIGHT)
    paint(img, PAD, lambda f, x, y, w, h: BRONZE if (f != "top" and y == h - 1) else
          (ARMOR_LIGHT if f == "top" and (x + y) % 2 == 0 else ARMOR))
    paint(img, CABLE, lambda f, x, y, w, h: hexc("1a1c22") if y % 2 else hexc("2c2f38"))
    paint(img, CAPE, lambda f, x, y, w, h: shade(CAPE_BLUE, 0.08) if x % 4 == 0 else CAPE_BLUE)
    paint(img, TABARD, lambda f, x, y, w, h: GOLD if f == "front" and (x in (0, w - 1) or y == h - 1)
          else hexc("23305a"))

    glow = Image.new("RGBA", (64, 64), CLEAR)
    fx, fy = 8, 8   # front of the head
    for x in (2, 5):
        glow.putpixel((fx + x, fy + 4), EYE)
    return img, glow


# ============================================================================ servants
def servant_base(skin, head_painter, top, sleeves, hands, pants, boots, belt=None, hat=clear):
    img = Image.new("RGBA", (64, 64), CLEAR)
    paint(img, HEAD, head_painter)
    paint(img, HAT, hat)

    def body(f, x, y, w, h):
        if belt and y == 10:
            return belt
        return top(f, x, y, w, h) if callable(top) else top
    paint(img, BODY, body)
    paint(img, ARM, lambda f, x, y, w, h: hands if (y >= 10 or f == "bottom") else
          (sleeves(f, x, y, w, h) if callable(sleeves) else sleeves))
    paint(img, LEG, lambda f, x, y, w, h: boots if (y >= 8 or f == "bottom") else pants)
    return img


def storm():
    skin = hexc("7a4b2e")
    white = hexc("f4f4f6")
    black = hexc("17171c")
    leather = hexc("24242b")
    img = servant_base(skin, face(skin, white, hexc("e8e8ff"), hexc("4a2a22"), hair_rows=1),
                       lambda f, x, y, w, h: hexc("8a8f9a") if f == "front" and x in (3, 4) and y < 8 else
                       (leather if (x + y) % 5 else black),
                       leather, skin, leather, black, belt=hexc("8a8f9a"))
    # shaved sides: only a strip of white hair on top, dark eye makeup
    for x in range(8):
        for y in range(8):
            if x not in (3, 4):
                img.putpixel((8 + x, y), shade(skin, -0.1))   # top of the head
    for x in (1, 2, 5, 6):
        img.putpixel((8 + x, 8 + 3), black)
    paint(img, MOHAWK, solid(white, shade(white, -0.15)))
    glow = Image.new("RGBA", (64, 64), CLEAR)
    for x in (1, 2, 5, 6):
        glow.putpixel((8 + x, 8 + 4), (235, 245, 255, 255))
    return img, glow


def psylocke():
    skin = hexc("d9b08c")
    hair = hexc("141018")
    purple = hexc("4b1a6e")
    dark = hexc("1d0f29")

    def suit(f, x, y, w, h):
        if f == "front" and (y + abs(x - 3.5)) % 6 < 1:
            return hexc("6e2a9a")
        return purple if (f == "front" and 2 <= x <= 5 and y < 9) else dark

    img = servant_base(skin, face(skin, hair, hexc("2a1a3a"), hexc("8a3a5a"), hair_rows=2), suit,
                       lambda f, x, y, w, h: dark if y < 3 else purple, dark, dark, hexc("0f0a14"),
                       belt=hexc("6e2a9a"))
    # long ponytail down the back of the head
    for y in range(8):
        for x in range(8):
            img.putpixel((24 + x, 8 + y), hair)
    paint(img, BLADE, solid(hexc("e070ff"), hexc("ff9cff")), lit=False)
    glow = Image.new("RGBA", (64, 64), CLEAR)
    paint(glow, BLADE, solid(hexc("e070ff"), hexc("ffc8ff")), lit=False)
    return img, glow


def archangel():
    skin = hexc("e2c2a8")
    hair = hexc("d8c070")
    leather = hexc("1c2232")
    steel = hexc("8e9aae")
    img = servant_base(skin, face(skin, hair, hexc("3a6ab0"), hexc("9a5a5a"), hair_rows=2),
                       lambda f, x, y, w, h: shade(leather, 0.2) if f == "front" and y in (3, 6) else leather,
                       leather, skin, leather, hexc("10131c"), belt=steel)
    # dark veins on his face from the transformation
    for (x, y) in ((1, 5), (1, 6), (6, 5), (6, 6), (0, 3)):
        img.putpixel((8 + x, 8 + y), hexc("3a3f5a"))

    def wing(f, x, y, w, h):
        # rows of metal blade-feathers, sharper toward the tip
        if f in ("front", "back"):
            if (x + y * 2) % 7 == 0:
                return shade(steel, -0.35)
            if y % 4 == 0:
                return shade(steel, 0.25)
            return shade(steel, -0.1 * (x % 3))
        return shade(steel, -0.2)
    paint(img, WING, wing)
    glow = Image.new("RGBA", (64, 64), CLEAR)
    for x in (2, 5):
        glow.putpixel((8 + x, 8 + 4), (120, 170, 255, 255))
    return img, glow


def horseman_magneto():
    skin = hexc("e0bfa0")
    armor = hexc("3a3e46")
    silver = hexc("a8adb6")
    img = servant_base(skin, face(skin, hexc("bfbfbf"), hexc("3a5a7a"), hexc("8a5a50"), hair_rows=1),
                       lambda f, x, y, w, h: silver if (f == "front" and (y in (1, 2) or x in (3, 4))) else armor,
                       lambda f, x, y, w, h: silver if y in (0, 1) else armor, hexc("26282e"), armor,
                       hexc("1c1e22"), belt=silver)

    def helmet(f, x, y, w, h):
        if f == "bottom":
            return CLEAR
        if f == "front":
            if y < 3 or (x in (0, 7) and y < 7):
                return silver if y == 0 else shade(silver, -0.25)
            return CLEAR
        if f in ("left", "right") and y > 6:
            return CLEAR
        return shade(silver, -0.2) if (x + y) % 4 else silver
    paint(img, HAT, helmet)
    return img, None


def cultist():
    skin = hexc("b07c55")
    robe = hexc("6b4a2a")
    dark = hexc("3e2a17")
    gold = hexc("c9a227")

    def hood(f, x, y, w, h):
        if f == "bottom":
            return CLEAR
        if f == "front":
            if y < 2 or x in (0, 7):
                return gold if y == 1 and 0 < x < 7 else robe
            return CLEAR
        return robe if (x + y) % 6 else dark
    img = servant_base(skin, face(skin, dark, hexc("1a1008"), hexc("5a3a2a"), hair_rows=2),
                       lambda f, x, y, w, h: gold if f == "front" and x in (3, 4) else robe,
                       robe, skin, robe, dark, belt=gold, hat=hood)
    # face in the shadow of the hood
    for x in range(8):
        img.putpixel((8 + x, 8 + 2), shade(skin, -0.45))
    return img, None


# ============================================================================ vein block
def vein_block(lit):
    img = Image.new("RGBA", (16, 16))
    base = hexc("d9c7a0") if not lit else hexc("e8d6aa")
    for x in range(16):
        for y in range(16):
            img.putpixel((x, y), noisy(base, 0.06))
    gold = hexc("c99a27") if not lit else hexc("ffe27a")
    core = hexc("e8b83a") if not lit else hexc("fffbe0")
    paths = [[(0, 7), (3, 7), (5, 5), (8, 5), (10, 7), (15, 7)], [(5, 5), (5, 0)], [(10, 7), (11, 10), (11, 15)],
             [(8, 5), (9, 2), (13, 1)], [(3, 7), (2, 11), (5, 13), (7, 15)]]
    for path in paths:
        for (x1, y1), (x2, y2) in zip(path, path[1:]):
            n = max(abs(x2 - x1), abs(y2 - y1))
            for i in range(n + 1):
                x = round(x1 + (x2 - x1) * i / n)
                y = round(y1 + (y2 - y1) * i / n)
                img.putpixel((x, y), core)
                for dx, dy in ((1, 0), (0, 1)):
                    if 0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy)) != core:
                        img.putpixel((x + dx, y + dy), gold)
    return img


# ============================================================================ json
NAMES = {
    "entity.xmen.apocalypse": ("Apocalypse", "Apocalypse"),
    "entity.xmen.storm": ("Storm", "Storm"),
    "entity.xmen.psylocke": ("Psylocke", "Psylocke"),
    "entity.xmen.archangel": ("Archangel", "Archangel"),
    "entity.xmen.horseman_magneto": ("Magneto", "Magneto"),
    "entity.xmen.akkaba_cultist": ("Clan Akkaba Cultist", "Kultist von Clan Akkaba"),
    "item.xmen.apocalypse_spawn_egg": ("Apocalypse Spawn Egg", "Apocalypse-Spawn-Ei"),
    "item.xmen.storm_spawn_egg": ("Storm Spawn Egg", "Storm-Spawn-Ei"),
    "item.xmen.psylocke_spawn_egg": ("Psylocke Spawn Egg", "Psylocke-Spawn-Ei"),
    "item.xmen.archangel_spawn_egg": ("Archangel Spawn Egg", "Archangel-Spawn-Ei"),
    "item.xmen.horseman_magneto_spawn_egg": ("Horseman Magneto Spawn Egg", "Reiter-Magneto-Spawn-Ei"),
    "item.xmen.akkaba_cultist_spawn_egg": ("Clan Akkaba Cultist Spawn Egg", "Akkaba-Kultist-Spawn-Ei"),
    "block.xmen.celestial_vein": ("Celestial Vein", "Himmlische Ader"),
}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def save(img, *path):
    full = os.path.join(ASSETS, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def main():
    for name, make in (("apocalypse", apocalypse), ("storm", storm), ("psylocke", psylocke),
                       ("archangel", archangel), ("horseman_magneto", horseman_magneto),
                       ("akkaba_cultist", cultist)):
        skin, glow = make()
        save(skin, "textures", "entity", "apocalypse", name + ".png")
        if glow:
            save(glow, "textures", "entity", "apocalypse", name + "_glow.png")
        write_json(os.path.join(ASSETS, "models", "item", name + "_spawn_egg.json"),
                   {"parent": "minecraft:item/template_spawn_egg"})
    save(vein_block(False), "textures", "block", "celestial_vein.png")
    save(vein_block(True), "textures", "block", "celestial_vein_lit.png")
    for suffix in ("", "_lit"):
        write_json(os.path.join(ASSETS, "models", "block", "celestial_vein" + suffix + ".json"),
                   {"parent": "minecraft:block/cube_all", "textures": {"all": "xmen:block/celestial_vein" + suffix}})
    write_json(os.path.join(ASSETS, "blockstates", "celestial_vein.json"),
               {"variants": {"lit=false": {"model": "xmen:block/celestial_vein"},
                             "lit=true": {"model": "xmen:block/celestial_vein_lit"}}})
    for index, file in ((0, "en_us.json"), (1, "de_de.json")):
        path = os.path.join(ASSETS, "lang", file)
        with open(path) as f:
            data = json.load(f)
        for key, names in NAMES.items():
            data[key] = names[index]
        write_json(path, data)
    if "--preview" in sys.argv:
        preview()
    print("apocalypse assets written")


def preview():
    """Front views of every skin side by side, 8x bigger."""
    names = ["apocalypse", "storm", "psylocke", "archangel", "horseman_magneto", "akkaba_cultist"]
    sheet = Image.new("RGBA", (len(names) * 20 * 8, 36 * 8), (90, 90, 90, 255))
    for i, name in enumerate(names):
        skin = Image.open(os.path.join(ASSETS, "textures", "entity", "apocalypse", name + ".png"))
        fig = Image.new("RGBA", (20, 36), CLEAR)
        parts = [((8, 8, 16, 16), (6, 2)), ((40, 8, 48, 16), (6, 2)), ((20, 20, 28, 32), (6, 10)),
                 ((44, 20, 48, 32), (2, 10)), ((44, 20, 48, 32), (14, 10)), ((4, 20, 8, 32), (6, 22)),
                 ((4, 20, 8, 32), (10, 22))]
        for src, at in parts:
            piece = skin.crop(src)
            fig.alpha_composite(piece, at)
        sheet.alpha_composite(fig.resize((160, 288), Image.NEAREST), (i * 160, 0))
    os.makedirs("build", exist_ok=True)
    sheet.save(os.path.join("build", "apocalypse_preview.png"))


if __name__ == "__main__":
    main()
