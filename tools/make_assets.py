"""Makes the textures, models, recipes, block drops and translations for the X-Men blocks and items.

Run:  python3 tools/make_assets.py   (needs Pillow: pip install pillow)
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "xmen")
DATA = os.path.join(ROOT, "src", "main", "resources", "data")
rand = random.Random(42)


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def save(img, *parts):
    path = os.path.join(ASSETS, "textures", *parts)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), c[3])


def noisy(size, base, amount=0.08):
    img = Image.new("RGBA", size)
    for x in range(size[0]):
        for y in range(size[1]):
            img.putpixel((x, y), shade(base, 1 + (rand.random() - 0.5) * 2 * amount))
    return img


def draw(img, pixels, palette):
    """pixels: list of strings, one char per pixel, '.' = leave as is."""
    for y, row in enumerate(pixels):
        for x, ch in enumerate(row):
            if ch != "." and ch in palette:
                img.putpixel((x, y), palette[ch])
    return img


def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


# ============================================================================ blocks
def block_textures():
    metal = hexc("6b6f78")
    # elevator: metal frame with yellow arrows
    img = noisy((16, 16), metal, 0.05)
    for i in range(16):
        for j in (0, 15):
            img.putpixel((i, j), shade(metal, 0.6))
            img.putpixel((j, i), shade(metal, 0.6))
    draw(img, [
        "................",
        "................",
        ".......YY.......",
        "......YYYY......",
        ".....YYYYYY.....",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....YYYYYY.....",
        "......YYYY......",
        ".......YY.......",
        "................",
        "................"], {"Y": hexc("f2c230")})
    save(img, "block", "elevator.png")

    # cerebro: dark metal, glowing blue ring on top
    side = noisy((16, 16), hexc("2a2f3a"), 0.06)
    for x in range(16):
        side.putpixel((x, 3), hexc("4fc3f7"))
        side.putpixel((x, 12), hexc("4fc3f7"))
    for x in range(2, 14, 4):
        for y in range(6, 10):
            side.putpixel((x, y), hexc("81d4fa"))
    save(side, "block", "cerebro_side.png")
    top = noisy((16, 16), hexc("1c2029"), 0.05)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 5 <= d <= 6.5:
                top.putpixel((x, y), hexc("4fc3f7"))
            elif d < 2.5:
                top.putpixel((x, y), hexc("e1f5fe"))
    save(top, "block", "cerebro_top.png")

    # danger room console: panel with screen and buttons
    side = noisy((16, 16), hexc("8a8f99"), 0.05)
    for x in range(16):
        side.putpixel((x, 0), shade(hexc("8a8f99"), 0.6))
        side.putpixel((x, 15), shade(hexc("8a8f99"), 0.6))
    for x in range(3, 13):
        for y in range(3, 9):
            side.putpixel((x, y), hexc("1b5e20") if (x + y) % 3 else hexc("66bb6a"))
    for x, c in ((4, "e53935"), (7, "fdd835"), (10, "1e88e5")):
        side.putpixel((x, 11), hexc(c))
        side.putpixel((x + 1, 11), hexc(c))
    save(side, "block", "danger_room_console_side.png")
    top = noisy((16, 16), hexc("5f6470"), 0.05)
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                top.putpixel((x, y), hexc("f2c230") if (x + y) % 4 < 2 else hexc("222222"))
    draw(top, [
        "................",
        "................",
        "................",
        "....RR....RR....",
        "...R..R..R..R...",
        "....RR....RR....",
        "................",
        "................",
        "......XXXX......",
        ".....X....X.....",
        "......XXXX......",
        "................",
        "................",
        "................",
        "................",
        "................"], {"R": hexc("e53935"), "X": hexc("fdd835")})
    save(top, "block", "danger_room_console_top.png")

    # krakoa gate: moss with a big pink flower
    img = noisy((16, 16), hexc("4e8a3a"), 0.12)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            ang = math.atan2(y - 7.5, x - 7.5)
            petal = 4.5 + 2.0 * math.cos(ang * 5)
            if d < 2:
                img.putpixel((x, y), hexc("ffeb3b"))
            elif d < petal:
                img.putpixel((x, y), shade(hexc("f06292"), 0.8 + 0.4 * (1 - d / 7)))
    save(img, "block", "krakoa_gate.png")


# ============================================================================ items
INGOT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......HHHHHH....",
    ".....HLLLLLLD...",
    "....HLLLLLLLD...",
    "...HLLLLLLLD....",
    "..HMMMMMMMD.....",
    "..DMMMMMMD......",
    "..DDDDDDD.......",
    "................",
    "................",
    "................",
    "................"]
SWORD = [
    "...............H",
    "..............HL",
    ".............HLD",
    "............HLD.",
    "...........HLD..",
    "..........HLD...",
    ".........HLD....",
    "........HLD.....",
    "...G...HLD......",
    "...GG.HLD.......",
    "....GGLD........",
    ".....GG.........",
    "....SGGG........",
    "...S..GG........",
    "..S.............",
    ".S.............."]
PICK = [
    "................",
    "....HHHHHHH.....",
    "...HLLLLLLLD....",
    "..HLD...SSLLD...",
    "..LD....S..LD...",
    "..D....S....D...",
    "......S.........",
    ".....S..........",
    "....S...........",
    "...S............",
    "..S.............",
    ".S..............",
    "S...............",
    "................",
    "................",
    "................"]
HELMET = [
    "................",
    "................",
    "................",
    "....HHHHHHHH....",
    "...HLLLLLLLLD...",
    "...LLLLLLLLLD...",
    "...LLDDDDDDLD...",
    "...LD......LD...",
    "...LD......LD...",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................"]
CHEST = [
    "................",
    "...HHH....HHH...",
    "..HLLLHHHHLLLD..",
    "..LLLLLLLLLLLD..",
    "..LLLLLLLLLLLD..",
    "...DLLLLLLLLD...",
    "....LLLLLLLLD...",
    "....LLLLLLLLD...",
    "....LLLLLLLLD...",
    "....LLLLLLLLD...",
    "....LLLLLLLLD...",
    "....DDDDDDDDD...",
    "................",
    "................",
    "................",
    "................"]
LEGS = [
    "................",
    "................",
    "....HHHHHHHH....",
    "....LLLLLLLLD...",
    "....LLLLLLLLD...",
    "....LLLD.LLLD...",
    "....LLLD.LLLD...",
    "....LLLD.LLLD...",
    "....LLLD.LLLD...",
    "....LLLD.LLLD...",
    "....LLLD.LLLD...",
    "....DDDD.DDDD...",
    "................",
    "................",
    "................",
    "................"]
BOOTS = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "...HHH....HHH...",
    "...LLD....LLD...",
    "...LLD....LLD...",
    "...LLD....LLD...",
    "..LLLD...LLLD...",
    "..DDDD...DDDD...",
    "................",
    "................",
    "................",
    "................"]


def metal_palette(base):
    c = hexc(base)
    return {"H": shade(c, 1.35), "L": c, "M": shade(c, 0.85), "D": shade(c, 0.6), "G": hexc("3c3c3c"),
            "S": hexc("6d4c2f")}


def item_textures():
    adam = metal_palette("9fb4c8")
    save(draw(blank(), INGOT, adam), "item", "adamantium_ingot.png")
    save(draw(blank(), SWORD, adam), "item", "adamantium_sword.png")
    save(draw(blank(), PICK, adam), "item", "adamantium_pickaxe.png")
    for name, shape in (("helmet", HELMET), ("chestplate", CHEST), ("leggings", LEGS), ("boots", BOOTS)):
        save(draw(blank(), shape, adam), "item", f"adamantium_{name}.png")

    suit_blue = metal_palette("2d4fa3")
    suit_yellow = metal_palette("f2c230")
    mask = draw(blank(), HELMET, suit_blue)
    draw(mask, ["", "", "", "", "", "", "....YYYYYYYY...."], {"Y": hexc("f2c230")})
    save(mask, "item", "xmen_mask.png")
    suit = draw(blank(), CHEST, suit_blue)
    for i in range(6):   # X on the chest
        for (x, y) in ((5 + i, 4 + i), (10 - i, 4 + i)):
            suit.putpixel((x, y), hexc("f2c230"))
    save(suit, "item", "xmen_suit.png")
    pants = draw(blank(), LEGS, suit_blue)
    for y in range(3, 11):
        pants.putpixel((4, y), hexc("f2c230"))
        pants.putpixel((11, y), hexc("f2c230"))
    save(pants, "item", "xmen_pants.png")
    save(draw(blank(), BOOTS, suit_yellow), "item", "xmen_boots.png")

    mag = metal_palette("b3262e")
    helm = draw(blank(), [
        "................",
        ".....HHHHHH.....",
        "....HLLLLLLD....",
        "...HLLPPPPLLD...",
        "...LLPPPPPPLD...",
        "...LLD....DLD...",
        "...LLD....DLD...",
        "...LLLD..DLLD...",
        "....LLD..DLD....",
        "....DD....DD....",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................"], dict(mag, P=hexc("7b2d8e")))
    save(helm, "item", "magneto_helmet.png")

    serum = draw(blank(), [
        "................",
        "......GGGG......",
        ".......GG.......",
        "......WGGW......",
        ".....W....W.....",
        ".....W....W.....",
        ".....WAAAAW.....",
        ".....WAAAAW.....",
        ".....WALAAW.....",
        ".....WAAAAW.....",
        ".....WAAAAW.....",
        ".....WAAAAW.....",
        "......WWWW......",
        ".......SS.......",
        ".......S........",
        "................"], {"G": hexc("616161"), "W": hexc("e0f7fa"), "A": hexc("43d16a"),
                              "L": hexc("b9f6ca"), "S": hexc("bdbdbd")})
    save(serum, "item", "mutant_serum.png")

    orb = blank()
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 6.5:
                light = 1.2 - d / 8 - (x + y - 15) / 40
                orb.putpixel((x, y), shade(hexc("3b5bdb"), light))
            if 6.5 <= d < 7.4:
                orb.putpixel((x, y), hexc("f2c230"))
    for x, y in ((5, 5), (6, 4), (5, 6), (10, 10)):
        orb.putpixel((x, y), hexc("ffffff"))
    save(orb, "item", "celestial_orb.png")

    part = draw(blank(), [
        "................",
        "................",
        "...HHHHHHHHH....",
        "..HPPPPPPPPPD...",
        "..PPPPPPPPPPD...",
        "..PPPGGGGPPPD...",
        "..PPGRRRRGPPD...",
        "..PPGRWRRGPPD...",
        "..PPGRRRRGPPD...",
        "..PPPGGGGPPPD...",
        "..PPPPPPPPPPD...",
        "..DDDDDDDDDDD...",
        "....S.S.S.......",
        "....S.S.S.......",
        "................",
        "................"], {"H": hexc("b06ad0"), "P": hexc("8e44ad"), "D": hexc("5b2c6f"), "G": hexc("9e9e9e"),
                              "R": hexc("e53935"), "W": hexc("ffcdd2"), "S": hexc("bdbdbd")})
    save(part, "item", "sentinel_part.png")


def armor_layers():
    def layer(base, accent=None, pattern=None):
        img = noisy((64, 32), hexc(base), 0.07)
        if accent:
            for x in range(64):
                for y in range(32):
                    if pattern(x, y):
                        img.putpixel((x, y), hexc(accent))
        return img

    # Magneto's helmet has its own 3D model and texture, see make_magneto.py
    for name, base in (("adamantium", "9fb4c8"),):
        save(layer(base), "models", "armor", f"{name}_layer_1.png")
        save(layer(base), "models", "armor", f"{name}_layer_2.png")
    # X-Men suit: blue with yellow belt, shoulders and boots
    l1 = layer("2d4fa3", "f2c230", lambda x, y: (y < 4 and 40 <= x < 56) or (16 <= x < 40 and 20 <= y < 22)
               or (y >= 26 and x < 16) or (8 <= y < 10 and x < 32))
    save(l1, "models", "armor", "xmen_suit_layer_1.png")
    l2 = layer("2d4fa3", "f2c230", lambda x, y: (16 <= x < 40 and 16 <= y < 18))
    save(l2, "models", "armor", "xmen_suit_layer_2.png")


# ============================================================================ json
BLOCKS = {
    "elevator": {"parent": "minecraft:block/cube_all", "textures": {"all": "xmen:block/elevator"}},
    "cerebro": {"parent": "minecraft:block/cube_bottom_top",
                "textures": {"top": "xmen:block/cerebro_top", "bottom": "xmen:block/cerebro_side",
                             "side": "xmen:block/cerebro_side"}},
    "danger_room_console": {"parent": "minecraft:block/cube_bottom_top",
                            "textures": {"top": "xmen:block/danger_room_console_top",
                                         "bottom": "xmen:block/danger_room_console_side",
                                         "side": "xmen:block/danger_room_console_side"}},
    "krakoa_gate": {"parent": "minecraft:block/cube_all", "textures": {"all": "xmen:block/krakoa_gate"}},
}
ITEMS = ["mutant_serum", "celestial_orb", "adamantium_ingot", "sentinel_part", "adamantium_helmet",
         "adamantium_chestplate", "adamantium_leggings", "adamantium_boots", "xmen_mask", "xmen_suit", "xmen_pants",
         "xmen_boots", "magneto_helmet"]
TOOLS = ["adamantium_sword", "adamantium_pickaxe"]

NAMES = {
    "block.xmen.elevator": ("Elevator", "Aufzug"),
    "block.xmen.cerebro": ("Cerebro", "Cerebro"),
    "block.xmen.danger_room_console": ("Danger Room Console", "Danger-Room-Konsole"),
    "block.xmen.krakoa_gate": ("Krakoa Gate", "Krakoa-Tor"),
    "item.xmen.mutant_serum": ("Mutant Serum", "Mutanten-Serum"),
    "item.xmen.celestial_orb": ("Celestial Orb", "Himmlische Kugel"),
    "item.xmen.adamantium_ingot": ("Adamantium Ingot", "Adamantiumbarren"),
    "item.xmen.sentinel_part": ("Sentinel Part", "Sentinel-Teil"),
    "item.xmen.adamantium_sword": ("Adamantium Sword", "Adamantiumschwert"),
    "item.xmen.adamantium_pickaxe": ("Adamantium Pickaxe", "Adamantiumspitzhacke"),
    "item.xmen.adamantium_helmet": ("Adamantium Helmet", "Adamantiumhelm"),
    "item.xmen.adamantium_chestplate": ("Adamantium Chestplate", "Adamantiumbrustpanzer"),
    "item.xmen.adamantium_leggings": ("Adamantium Leggings", "Adamantiumbeinschutz"),
    "item.xmen.adamantium_boots": ("Adamantium Boots", "Adamantiumstiefel"),
    "item.xmen.xmen_mask": ("X-Men Mask", "X-Men-Maske"),
    "item.xmen.xmen_suit": ("X-Men Suit", "X-Men-Anzug"),
    "item.xmen.xmen_pants": ("X-Men Pants", "X-Men-Hose"),
    "item.xmen.xmen_boots": ("X-Men Boots", "X-Men-Stiefel"),
    "item.xmen.magneto_helmet": ("Magneto's Helmet", "Magnetos Helm"),
    "itemGroup.xmen": ("X-Men", "X-Men"),
}


def shaped(name, pattern, key, count=1):
    return {"type": "minecraft:crafting_shaped", "category": "equipment" if "_" in name else "misc",
            "pattern": pattern, "key": {k: {"item": v if ":" in v else "minecraft:" + v} for k, v in key.items()},
            "result": {"id": "xmen:" + name, "count": count}}


def recipes():
    a = {"#": "xmen:adamantium_ingot"}
    r = {
        "elevator": shaped("elevator", ["III", "IPI", "III"], {"I": "iron_ingot", "P": "ender_pearl"}, 2),
        "adamantium_ingot": shaped("adamantium_ingot", ["SIS", "IDI", "SIS"],
                                   {"S": "xmen:sentinel_part", "I": "iron_ingot", "D": "diamond"}),
        "adamantium_sword": shaped("adamantium_sword", ["#", "#", "S"], dict(a, S="stick")),
        "adamantium_pickaxe": shaped("adamantium_pickaxe", ["###", " S ", " S "], dict(a, S="stick")),
        "adamantium_helmet": shaped("adamantium_helmet", ["###", "# #"], a),
        "adamantium_chestplate": shaped("adamantium_chestplate", ["# #", "###", "###"], a),
        "adamantium_leggings": shaped("adamantium_leggings", ["###", "# #", "# #"], a),
        "adamantium_boots": shaped("adamantium_boots", ["# #", "# #"], a),
        "xmen_mask": shaped("xmen_mask", ["LYL", "L L"], {"L": "leather", "Y": "yellow_dye"}),
        "xmen_suit": shaped("xmen_suit", ["L L", "LYL", "LBL"], {"L": "leather", "Y": "yellow_dye",
                                                                 "B": "blue_dye"}),
        "xmen_pants": shaped("xmen_pants", ["LBL", "L L", "L L"], {"L": "leather", "B": "blue_dye"}),
        "xmen_boots": shaped("xmen_boots", ["Y Y", "L L"], {"L": "leather", "Y": "yellow_dye"}),
    }
    for name, recipe in r.items():
        write_json(os.path.join(DATA, "xmen", "recipe", name + ".json"), recipe)


def models():
    for name, model in BLOCKS.items():
        write_json(os.path.join(ASSETS, "models", "block", name + ".json"), model)
        write_json(os.path.join(ASSETS, "blockstates", name + ".json"),
                   {"variants": {"": {"model": "xmen:block/" + name}}})
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"), {"parent": "xmen:block/" + name})
        write_json(os.path.join(DATA, "xmen", "loot_table", "blocks", name + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "xmen:" + name}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for name in ITEMS:
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"),
                   {"parent": "minecraft:item/generated", "textures": {"layer0": "xmen:item/" + name}})
    for name in TOOLS:
        write_json(os.path.join(ASSETS, "models", "item", name + ".json"),
                   {"parent": "minecraft:item/handheld", "textures": {"layer0": "xmen:item/" + name}})
    write_json(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "pickaxe.json"),
               {"replace": False, "values": ["xmen:elevator", "xmen:cerebro", "xmen:danger_room_console"]})
    write_json(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "hoe.json"),
               {"replace": False, "values": ["xmen:krakoa_gate"]})
    write_json(os.path.join(DATA, "minecraft", "tags", "item", "swords.json"),
               {"replace": False, "values": ["xmen:adamantium_sword"]})
    write_json(os.path.join(DATA, "minecraft", "tags", "item", "pickaxes.json"),
               {"replace": False, "values": ["xmen:adamantium_pickaxe"]})


def lang():
    for index, file in ((0, "en_us.json"), (1, "de_de.json")):
        path = os.path.join(ASSETS, "lang", file)
        with open(path) as f:
            data = json.load(f)
        for key, names in NAMES.items():
            data[key] = names[index]
        write_json(path, data)


if __name__ == "__main__":
    block_textures()
    item_textures()
    armor_layers()
    models()
    recipes()
    lang()
    print("assets written")
