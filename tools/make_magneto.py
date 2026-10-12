"""Draws everything for Magneto: his skin, his cape, the texture of his worn helmet, plus the spawn egg model and
names. Run from the repo root: python3 tools/make_magneto.py (needs Pillow). Pass --preview to also write
build/magneto_preview.png, a flat front/side view of the worn helmet."""
import json
import os
import random
import sys

from PIL import Image

ASSETS = os.path.join("src", "main", "resources", "assets", "xmen")
rand = random.Random(7)


def hexc(value):
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def shade(color, amount):
    """amount > 0 lightens, < 0 darkens."""
    r, g, b, a = color
    if amount >= 0:
        return tuple(round(c + (255 - c) * amount) for c in (r, g, b)) + (a,)
    return tuple(round(c * (1 + amount)) for c in (r, g, b)) + (a,)


def noisy(color, strength=0.06):
    return shade(color, rand.uniform(-strength, strength))


def box_faces(u, v, w, h, d):
    """Where each side of a model cube sits on its texture (Minecraft's box UV layout)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


def save(img, *path):
    full = os.path.join(ASSETS, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


# ============================================================================ helmet
HELMET_RED = hexc("7d1626")
HELMET_DARK = hexc("4a0b16")
CREST = hexc("a8202f")

# (u, v, w, h, d, color) for each cube in client/MagnetoHelmetModel.java, in the same order
HELMET_CUBES = [
    ("dome", 0, 0, 10, 3, 10, HELMET_RED), ("back", 40, 0, 10, 7, 4, HELMET_RED),
    ("left cheek", 68, 0, 1, 7, 6, HELMET_RED), ("right cheek", 82, 0, 1, 7, 6, HELMET_RED),
    ("brow", 96, 0, 8, 1, 1, CREST), ("left jaw", 96, 4, 1, 4, 1, HELMET_RED),
    ("right jaw", 100, 4, 1, 4, 1, HELMET_RED), ("ridge", 0, 16, 2, 1, 11, CREST),
    ("forehead ridge", 26, 16, 2, 4, 1, CREST), ("left fin", 32, 16, 1, 3, 4, HELMET_DARK),
    ("right fin", 42, 16, 1, 3, 4, HELMET_DARK),
]
# where those cubes are in the model (x, y, z), for the preview
HELMET_BOXES = [(-5, -9, -5), (-5, -6, 1), (4, -6, -5), (-5, -6, -5), (-4, -6, -5), (3, -3, -5), (-4, -3, -5),
                (-1, -10, -6), (-1, -9, -6), (5, -8, -3), (-6, -8, -3)]


def helmet():
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    for name, u, v, w, h, d, base in HELMET_CUBES:
        for face, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
            light = {"top": 0.18, "bottom": -0.35, "front": 0.0, "back": -0.12, "left": -0.06, "right": -0.06}[face]
            for x in range(fw):
                for y in range(fh):
                    c = shade(base, light)
                    if face not in ("top", "bottom") and fh > 1:
                        c = shade(c, 0.12 - 0.3 * y / (fh - 1))   # polished metal: light from above
                    if face == "top" and fw > 2 and fh > 2:
                        # a soft shine across the dome
                        dist = abs((x - fw * 0.35) / fw) + abs((y - fh * 0.4) / fh)
                        c = shade(c, max(0.0, 0.22 - dist * 0.4))
                    edge = x in (0, fw - 1) or y in (0, fh - 1)
                    if edge and fw > 2 and fh > 2:
                        c = shade(c, -0.18)
                    img.putpixel((fx + x, fy + y), noisy(c, 0.04))
        if name in ("ridge", "forehead ridge"):
            # a bright line along the middle of the ridge
            fx, fy, fw, fh = box_faces(u, v, w, h, d)["top" if name == "ridge" else "front"]
            for y in range(fh):
                img.putpixel((fx, fy + y), shade(CREST, 0.35))
    # rivets on the cheek guards
    for u in (68, 82):
        fx, fy, fw, fh = box_faces(u, 0, 1, 7, 6)["left" if u == 68 else "right"]
        for x, y in ((1, 1), (fw - 2, 1), (1, fh - 2), (fw - 2, fh - 2)):
            img.putpixel((fx + x, fy + y), hexc("c9a24a"))
    save(img, "textures", "models", "armor", "magneto_layer_1.png")
    return img


# ============================================================================ Magneto himself
SUIT = hexc("6e1220")
SUIT_DARK = hexc("440a14")
GLOVE = hexc("472058")
SKIN = hexc("d6a184")
HAIR = hexc("b9b6b0")


def paint_box(img, u, v, w, h, d, color_at):
    for face, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
        for x in range(fw):
            for y in range(fh):
                img.putpixel((fx + x, fy + y), noisy(color_at(face, x, y, fw, fh), 0.05))


def skin():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def head(face, x, y, w, h):
        if face == "top" or (face != "front" and y < 4) or face == "back":
            return HAIR
        if face == "bottom":
            return shade(SKIN, -0.2)
        if face == "front":
            if y < 2:
                return HAIR
            if y == 2:
                return shade(HAIR, -0.25) if x in (1, 2, 5, 6) else SKIN
            if y == 3 and x in (1, 6):
                return hexc("f0f0f0")
            if y == 3 and x in (2, 5):
                return hexc("5d7a94")   # grey-blue eyes
            if y == 4 and x in (3, 4):
                return shade(SKIN, -0.15)
            if y == 6 and 2 <= x <= 5:
                return hexc("9a5a50")
            if y == 7:
                return shade(HAIR, -0.1) if x in (1, 2, 5, 6) else shade(SKIN, -0.08)
            return SKIN
        return shade(SKIN, -0.1) if y >= 4 else HAIR

    def body(face, x, y, w, h):
        if face == "front":
            if y == 9:
                return hexc("c9a24a") if x in (3, 4) else SUIT_DARK   # belt with a gold buckle
            if y <= 2 and abs(x - 3.5) < 3 - y:
                return SUIT_DARK   # V collar
            if x in (0, w - 1):
                return SUIT_DARK
            return SUIT
        if face == "back":
            return SUIT_DARK if y == 9 else shade(SUIT, -0.1)
        return SUIT_DARK

    def arm(face, x, y, w, h):
        if face == "bottom" or (face != "top" and y >= 9):
            return GLOVE
        if face != "top" and y == 8:
            return shade(GLOVE, 0.15)
        return SUIT if face in ("front", "top") else shade(SUIT, -0.12)

    def leg(face, x, y, w, h):
        if face == "bottom" or (face != "top" and y >= 8):
            return GLOVE
        return SUIT if face == "front" else shade(SUIT, -0.12)

    paint_box(img, 0, 0, 8, 8, 8, head)
    paint_box(img, 16, 16, 8, 12, 4, body)
    paint_box(img, 40, 16, 4, 12, 4, arm)
    paint_box(img, 32, 48, 4, 12, 4, arm)
    paint_box(img, 0, 16, 4, 12, 4, leg)
    paint_box(img, 16, 48, 4, 12, 4, leg)
    save(img, "textures", "entity", "magneto.png")


def cape():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    outside, inside = hexc("5b1f72"), hexc("4a0d18")

    def color(face, x, y, w, h):
        if face == "front":
            fold = (x % 3 == 0) * -0.15 + (x % 3 == 1) * 0.06
            return shade(outside, fold - 0.15 * y / h)
        if face == "back":
            return shade(inside, -0.1 * y / h)
        return shade(outside, -0.3)

    paint_box(img, 0, 0, 10, 16, 1, color)
    save(img, "textures", "entity", "magneto_cape.png")


# ============================================================================ preview of the worn helmet
def preview(tex):
    """Front and side view, 16x scale, with a plain head underneath, to check the helmet shape."""
    scale = 16
    out = Image.new("RGBA", (2 * 16 * scale, 16 * scale), hexc("2b2b2b"))

    def draw(px, py, color, ox):
        for dx in range(scale):
            for dy in range(scale):
                out.putpixel((ox + px * scale + dx, py * scale + dy), color)

    for view, ox in (("front", 0), ("side", 16 * scale)):
        depth = {}
        boxes = [((-4, -8, -4), (8, 8, 8), None)] + [(pos, cube[3:6], (cube[1], cube[2])) for pos, cube in
                                                      zip(HELMET_BOXES, HELMET_CUBES)]
        for (x0, y0, z0), (w, h, d), uv in boxes:
            for i in range(w if view == "front" else d):
                for j in range(h):
                    if view == "front":
                        sx, z = x0 + i, z0
                        face = "front"
                    else:
                        sx, z = z0 + i, -(x0 + w)   # looking from the left (+x) side
                        face = "left"
                    key = (sx + 8, y0 + j + 12)
                    if key in depth and depth[key] <= z:
                        continue
                    if uv is None:
                        color = SKIN
                    else:
                        fx, fy, fw, fh = box_faces(uv[0], uv[1], w, h, d)[face]
                        color = tex.getpixel((fx + (i if view == "front" else d - 1 - i), fy + j))
                    depth[key] = z
                    if 0 <= key[0] < 16 and 0 <= key[1] < 16:
                        draw(key[0], key[1], color, ox)
    os.makedirs("build", exist_ok=True)
    out.save(os.path.join("build", "magneto_preview.png"))


# ============================================================================ models and names
NAMES = {
    "entity.xmen.magneto": ("Magneto", "Magneto"),
    "entity.xmen.magneto_debris": ("Flying Metal", "Fliegendes Metall"),
    "item.xmen.magneto_spawn_egg": ("Magneto Spawn Egg", "Magneto-Spawn-Ei"),
}


def models_and_lang():
    path = os.path.join(ASSETS, "models", "item", "magneto_spawn_egg.json")
    with open(path, "w") as f:
        json.dump({"parent": "minecraft:item/template_spawn_egg"}, f, indent=2)
        f.write("\n")
    for index, file in ((0, "en_us.json"), (1, "de_de.json")):
        path = os.path.join(ASSETS, "lang", file)
        with open(path) as f:
            data = json.load(f)
        for key, names in NAMES.items():
            data[key] = names[index]
        with open(path, "w") as f:
            json.dump(data, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    tex = helmet()
    skin()
    cape()
    models_and_lang()
    if "--preview" in sys.argv:
        preview(tex)
    print("magneto assets written")
