"""Draws everything for the Sentinel Mark X: the robot textures (the boss and your own reprogrammed one) with their
glow maps, Sentinel armor, the item icons, item models, recipes and names. (The normal Mark I Sentinel is drawn by
tools/robots.py.)
Run from the repo root: python3 tools/make_sentinel.py (needs Pillow). Pass --preview to also write
build/sentinel_preview.png with the front of every robot side by side.

The texture layout must match the boxes in src/main/java/com/emil/xmen/sentinel/client/SentinelModel.java."""
import json
import os
import random
import sys

from PIL import Image

ASSETS = os.path.join("src", "main", "resources", "assets", "xmen")
DATA = os.path.join("src", "main", "resources", "data")
rand = random.Random(73)
CLEAR = (0, 0, 0, 0)


def hexc(value, alpha=255):
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (alpha,)


def shade(color, amount):
    r, g, b, a = color
    if amount >= 0:
        return tuple(round(c + (255 - c) * amount) for c in (r, g, b)) + (a,)
    return tuple(round(c * (1 + amount)) for c in (r, g, b)) + (a,)


def noisy(color, strength=0.05):
    if color[3] == 0:
        return color
    return shade(color, rand.uniform(-strength, strength))


def save(img, *path):
    full = os.path.join(ASSETS, *path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def box_faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}


# ============================================================================ the robot
# (name, u, v, w, h, d) for every box in SentinelModel
PARTS = [("head", 0, 0, 8, 8, 8), ("crest", 32, 0, 2, 2, 8), ("ear", 56, 0, 1, 3, 3),
         ("chest", 0, 16, 10, 7, 6), ("waist", 32, 16, 7, 5, 4), ("core", 56, 16, 3, 3, 1),
         ("hips", 64, 16, 8, 2, 5),
         ("shoulder", 0, 32, 6, 5, 7), ("upper_arm", 26, 32, 4, 6, 4), ("forearm", 42, 32, 5, 7, 5),
         ("hand", 62, 32, 4, 2, 4),
         ("thigh", 0, 48, 5, 6, 5), ("knee", 20, 48, 4, 3, 1), ("shin", 30, 48, 4, 5, 4), ("foot", 46, 48, 5, 1, 6)]

STYLES = {
    # the future Sentinel: black overlapping scale plates with orange heat glowing between them
    "mark_x": dict(base="2a2c30", dark="17181b", light="44474d", face="34363b", joint="111214", eye="ff8a1a",
                   core="ffa040", seams="ff7a14", logo=None),
    # reprogrammed to protect you: the same plates, glowing blue, with a yellow X on the chest
    "ally": dict(base="2a2c30", dark="17181b", light="44474d", face="34363b", joint="111214", eye="3aa8ff",
                 core="4fc3ff", seams="3a9cff", logo="f2c230"),
}


def robot(style):
    s = {k: (hexc(v) if isinstance(v, str) else v) for k, v in STYLES[style].items()}
    img = Image.new("RGBA", (128, 64), CLEAR)
    glow = Image.new("RGBA", (128, 64), CLEAR)
    for name, u, v, w, h, d in PARTS:
        for face, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
            for x in range(fw):
                for y in range(fh):
                    color = surface(s, name, face, x, y, fw, fh)
                    img.putpixel((fx + x, fy + y), noisy(color, 0.04))
                    lit = glowing(s, name, face, x, y, fw, fh)
                    if lit:
                        glow.putpixel((fx + x, fy + y), lit)
                        img.putpixel((fx + x, fy + y), shade(lit, -0.1))
    return img, glow


def surface(s, name, face, x, y, fw, fh):
    edge = x == 0 or y == 0 or x == fw - 1 or y == fh - 1
    color = s["base"]
    if name in ("upper_arm", "shin", "waist"):
        color = s["joint"] if face != "front" else s["dark"]
    if s["seams"] and name not in ("core",):
        # overlapping scale plates: lighter top edge, dark bottom edge, rows of 3
        color = s["light"] if y % 3 == 0 else s["base"]
        if (x + (y // 3)) % 4 == 0:
            color = shade(color, -0.12)
    elif edge and fw > 2 and fh > 2:
        color = s["dark"]
    if name == "head":
        if face == "front":
            color = s["face"]
            if y in (3, 4) and x in (1, 2, 5, 6):
                color = s["joint"]  # eye sockets
            if y == 6 and 2 <= x <= 5:
                color = s["dark"]   # mouth slit
            if x == 0 or x == 7:
                color = shade(s["face"], -0.2)
        elif face == "top":
            color = s["light"] if not s["seams"] else color
    if name == "chest" and face == "front":
        if s["logo"] and 1 <= y <= 5 and x in (1 + y, 8 - y):
            color = s["logo"]
        elif y == fh - 1:
            color = s["dark"]
    if name == "foot" and face == "top":
        color = s["dark"]
    if name == "shoulder" and face == "top" and not s["seams"]:
        color = s["light"] if 1 <= x <= fw - 2 and 1 <= y <= fh - 2 else s["dark"]
    if name == "hand" and face == "bottom":
        color = s["joint"]
    return color


def glowing(s, name, face, x, y, fw, fh):
    if name == "head" and face == "front" and y in (3, 4) and x in (1, 2, 5, 6):
        return s["eye"] if y == 3 else shade(s["eye"], -0.25)
    if name == "core" and face == "front":
        return s["core"] if x == 1 and y == 1 else shade(s["core"], -0.2)
    if name == "hand" and face == "bottom" and 1 <= x <= 2 and 1 <= y <= 2:
        return s["core"]
    if s["seams"]:
        # heat glowing out between the plates of the Mark X
        if name in ("chest", "waist", "forearm", "thigh", "shoulder") and face in ("front", "back") and y % 3 == 2 \
                and (x + y) % 5 == 0:
            return shade(s["seams"], -0.3)
        if name == "head" and face == "front" and y == 6 and 2 <= x <= 5:
            return s["seams"]
    return None


# ============================================================================ armor and icons
def armor_layers():
    base, dark, light = hexc("6f747a"), hexc("3a3e43"), hexc("a3a9af")

    def layer():
        img = Image.new("RGBA", (64, 32), CLEAR)
        for x in range(64):
            for y in range(32):
                img.putpixel((x, y), noisy(base if (x + y // 4) % 8 else light, 0.05))
        return img

    l1 = layer()
    # helmet: silver face mask with the red eye slit
    for x in range(8, 16):
        for y in range(8, 16):
            l1.putpixel((x, y), noisy(hexc("b9bfc5"), 0.03))
    for x in (9, 10, 13, 14):
        l1.putpixel((x, 11), hexc("ff2a1a"))
    for x in range(10, 14):
        l1.putpixel((x, 14), dark)
    # chest: dark plate lines and the glowing core
    for x in range(20, 28):
        l1.putpixel((x, 26), dark)
    for x in range(23, 25):
        for y in range(22, 24):
            l1.putpixel((x, y), hexc("ff3b26"))
    save(l1, "textures", "models", "armor", "sentinel_layer_1.png")
    l2 = layer()
    for x in range(0, 64):
        l2.putpixel((x, 22), dark)
    save(l2, "textures", "models", "armor", "sentinel_layer_2.png")


def icon(rows, palette):
    img = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in palette:
                img.putpixel((x, y), palette[ch])
    return img


METAL = {"D": hexc("2d3034"), "G": hexc("6f747a"), "L": hexc("a3a9af"), "W": hexc("dfe3e6"),
         "R": hexc("e8231a"), "r": hexc("8e1610"), "O": hexc("ff9a2a"), "o": hexc("b85a0e"), "B": hexc("3aa8ff"),
         "b": hexc("1f5f9a"), "C": hexc("1f6b3a"), "c": hexc("39a35f"), "Y": hexc("f2c230")}

ICONS = {
    "sentinel_core": [
        "................",
        ".....DDDDDD.....",
        "....DGLLLLGD....",
        "...DGooooooGD...",
        "..DGoOOOOOOoGD..",
        "..DLoOOWWOOoLD..",
        "..DLoOWWWWOoLD..",
        "..DLoOWWWWOoLD..",
        "..DLoOOWWOOoLD..",
        "..DGoOOOOOOoGD..",
        "...DGooooooGD...",
        "....DGLLLLGD....",
        ".....DDDDDD.....",
        "......D..D......",
        ".....DD..DD.....",
        "................"],
    "sentinel_blaster": [
        "................",
        "................",
        "..........DD....",
        ".........DLLD...",
        "........DLGGLD..",
        ".......DLGGGRRD.",
        "......DLGGGGRWR.",
        ".....DLGGGGGRRD.",
        "....DLGGGGGGLD..",
        "...DDGGGGGGLD...",
        "..DGDDGGGGLD....",
        ".DGGGDDGGLD.....",
        ".DGGGGDDDD......",
        "..DGGGGD........",
        "...DDDD.........",
        "................"],
    "sentinel_control_chip": [
        "................",
        "...L.L.L.L.L....",
        "..DDDDDDDDDDD...",
        "..DCCCCCCCCCD...",
        ".LDCcccCcccCDL..",
        "..DCcDDDDDcCD...",
        ".LDCcDBBBDcCDL..",
        "..DCCDBWBDCCD...",
        ".LDCcDBBBDcCDL..",
        "..DCcDDDDDcCD...",
        ".LDCcccCcccCDL..",
        "..DCCCCCCCCCD...",
        "..DDDDDDDDDDD...",
        "...L.L.L.L.L....",
        "................",
        "................"],
    "sentinel_helmet": [
        "................",
        "................",
        "................",
        "....DDDDDDDD....",
        "...DLLLLLLLLD...",
        "..DLGGGGGGGGLD..",
        "..DGWWWWWWWWGD..",
        "..DGWRRWWRRWGD..",
        "..DGWWWWWWWWGD..",
        "..DGDWWDDWWDGD..",
        "..DDD.DDDD.DDD..",
        "................",
        "................",
        "................",
        "................",
        "................"],
    "sentinel_chestplate": [
        "................",
        "..DDDD....DDDD..",
        ".DLLGDDDDDDGLLD.",
        ".DGGGGLLLLGGGGD.",
        ".DGGGGGGGGGGGGD.",
        "..DDGGGRRGGGDD..",
        "...DGGRWWRGGD...",
        "...DGGGRRGGGD...",
        "...DGGGGGGGGD...",
        "...DDDDDDDDDD...",
        "...DGGGGGGGGD...",
        "...DGGGGGGGGD...",
        "...DLLLLLLLLD...",
        "...DDDDDDDDDD...",
        "................",
        "................"],
    "sentinel_leggings": [
        "................",
        "...DDDDDDDDDD...",
        "...DLLLLLLLLD...",
        "...DGGGGGGGGD...",
        "...DGGGDDGGGD...",
        "...DGGGDDGGGD...",
        "...DLLD..DLLD...",
        "...DGGD..DGGD...",
        "...DGGD..DGGD...",
        "...DGGD..DGGD...",
        "...DLLD..DLLD...",
        "...DGGD..DGGD...",
        "...DGGD..DGGD...",
        "...DDDD..DDDD...",
        "................",
        "................"],
    "sentinel_boots": [
        "................",
        "................",
        "................",
        "................",
        "...DDDD..DDDD...",
        "...DGGD..DGGD...",
        "...DLLD..DLLD...",
        "...DGGD..DGGD...",
        "...DGGD..DGGD...",
        "..DDGGD..DGGDD..",
        ".DGGGGD..DGGGGD.",
        ".DLLLLD..DLLLLD.",
        ".DDDDDD..DDDDDD.",
        "................",
        "................",
        "................"],
}


# ============================================================================ models, recipes, names
P = {"item": "xmen:sentinel_part"}
RECIPES = {
    "sentinel_helmet": ["PPP", "P P"],
    "sentinel_chestplate": ["P P", "PPP", "PPP"],
    "sentinel_leggings": ["PPP", "P P", "P P"],
    "sentinel_boots": ["P P", "P P"],
}
SHAPELESS = {
    "sentinel_blaster": [P, P, P, {"item": "minecraft:diamond"}, {"item": "minecraft:redstone"}],
    "sentinel_control_chip": [{"item": "xmen:sentinel_core"}, P, P, P, P, {"item": "minecraft:redstone_block"}],
}
NAMES = {
    "entity.xmen.sentinel_mark_x": ("Sentinel Mark X", "Sentinel Mark X"),
    "item.xmen.sentinel_mark_x_spawn_egg": ("Sentinel Mark X Spawn Egg", "Sentinel-Mark-X-Spawn-Ei"),
    "item.xmen.sentinel_core": ("Sentinel Core", "Sentinel-Kern"),
    "item.xmen.sentinel_blaster": ("Sentinel Blaster", "Sentinel-Blaster"),
    "item.xmen.sentinel_control_chip": ("Sentinel Control Chip", "Sentinel-Steuerchip"),
    "item.xmen.sentinel_helmet": ("Sentinel Helmet", "Sentinel-Helm"),
    "item.xmen.sentinel_chestplate": ("Sentinel Chestplate", "Sentinel-Brustplatte"),
    "item.xmen.sentinel_leggings": ("Sentinel Leggings", "Sentinel-Beinschutz"),
    "item.xmen.sentinel_boots": ("Sentinel Boots", "Sentinel-Stiefel"),
}


def models_recipes_lang():
    models = os.path.join(ASSETS, "models", "item")
    for name in ("sentinel_mark_x_spawn_egg",):
        write_json(os.path.join(models, name + ".json"), {"parent": "minecraft:item/template_spawn_egg"})
    for name in ICONS:
        parent = "minecraft:item/handheld" if name == "sentinel_blaster" else "minecraft:item/generated"
        write_json(os.path.join(models, name + ".json"), {"parent": parent, "textures": {"layer0": "xmen:item/" + name}})
    recipes = os.path.join(DATA, "xmen", "recipe")
    for name, pattern in RECIPES.items():
        write_json(os.path.join(recipes, name + ".json"),
                   {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern,
                    "key": {"P": P}, "result": {"id": "xmen:" + name, "count": 1}})
    for name, ingredients in SHAPELESS.items():
        write_json(os.path.join(recipes, name + ".json"),
                   {"type": "minecraft:crafting_shapeless", "category": "equipment", "ingredients": ingredients,
                    "result": {"id": "xmen:" + name, "count": 1}})
    for index, file in ((0, "en_us.json"), (1, "de_de.json")):
        path = os.path.join(ASSETS, "lang", file)
        with open(path) as f:
            data = json.load(f)
        for key, pair in NAMES.items():
            data[key] = pair[index]
        write_json(path, data)


def preview(images):
    """Front view of every robot at 8x scale, laid out roughly like the model."""
    scale = 8
    out = Image.new("RGBA", (len(images) * 30 * scale, 34 * scale), hexc("303030"))
    # (u, v, w, h) of a front face -> (x, y) on the preview
    parts = [((8, 8, 8, 8), (10, 0)), ((6, 22, 10, 7), (9, 8)), ((36, 20, 7, 5), (10, 15)),
             ((7, 39, 6, 5), (3, 6)), ((30, 36, 4, 6), (4, 11)), ((47, 37, 5, 7), (3, 17)),
             ((7, 39, 6, 5), (19, 6)), ((30, 36, 4, 6), (20, 11)), ((47, 37, 5, 7), (20, 17)),
             ((69, 21, 8, 2), (10, 20)), ((5, 53, 5, 6), (8, 22)), ((34, 52, 4, 5), (8, 28)),
             ((5, 53, 5, 6), (13, 22)), ((34, 52, 4, 5), (13, 28)), ((21, 49, 4, 3), (8, 27)),
             ((21, 49, 4, 3), (13, 27)), ((57, 17, 3, 3), (13, 10))]
    for n, img in enumerate(images):
        for (u, v, w, h), (ox, oy) in parts:
            for x in range(w):
                for y in range(h):
                    c = img.getpixel((u + x, v + y))
                    if c[3]:
                        for dx in range(scale):
                            for dy in range(scale):
                                out.putpixel(((n * 30 + ox + x) * scale + dx, (1 + oy + y) * scale + dy), c)
    os.makedirs("build", exist_ok=True)
    out.save(os.path.join("build", "sentinel_preview.png"))


if __name__ == "__main__":
    images = []
    for style in STYLES:
        img, glow = robot(style)
        save(img, "textures", "entity", "sentinel", style + ".png")
        save(glow, "textures", "entity", "sentinel", style + "_glow.png")
        images.append(img)
    armor_layers()
    for name, rows in ICONS.items():
        save(icon(rows, METAL), "textures", "item", name + ".png")
    models_recipes_lang()
    if "--preview" in sys.argv:
        preview(images)
    print("sentinel assets written")
