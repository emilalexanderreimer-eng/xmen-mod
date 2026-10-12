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


# ============================================================================ mansion lower level (movie style)
def box_faces(u, v, w, h, d):
    """(face, x0, y0, width, height) of the six faces of a model box at texture offset (u, v)."""
    return [("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d), ("right", u, v + d, d, h),
            ("front", u + d, v + d, w, h), ("left", u + d + w, v + d, d, h), ("back", u + d + w + d, v + d, w, h)]


def paint_box(img, u, v, w, h, d, fn):
    for face, x0, y0, fw, fh in box_faces(u, v, w, h, d):
        for y in range(fh):
            for x in range(fw):
                c = fn(face, x, y, fw, fh)
                if c is not None and 0 <= x0 + x < img.size[0] and 0 <= y0 + y < img.size[1]:
                    img.putpixel((x0 + x, y0 + y), c)


def jitter(c, amount=0.06):
    return shade(c, 1 + (rand.random() - 0.5) * 2 * amount)


SILVER = hexc("b9c0c9")


def silver_panel():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        row = 1 + (rand.random() - 0.5) * 0.06
        for x in range(16):
            c = shade(SILVER, row * (1 + (rand.random() - 0.5) * 0.04))
            if y in (0, 15) or x in (0, 15):
                c = shade(SILVER, 0.72)
            elif y in (1,) or x in (1,):
                c = shade(SILVER, 1.12)
            elif y in (5, 10):
                c = shade(SILVER, 0.86)
            img.putpixel((x, y), c)
    return img


def x_door_tiles():
    """A 48x48 round door with a big X, cut into nine 16x16 tiles (0 = top left, 8 = bottom right)."""
    big = Image.new("RGBA", (48, 48))
    panel = silver_panel()
    for y in range(48):
        for x in range(48):
            dx, dy = x - 23.5, y - 23.5
            r = math.hypot(dx, dy)
            if r > 23.2:
                c = panel.getpixel((x % 16, y % 16))
            elif r > 21.5:
                c = hexc("3a3f47")
            elif r < 4.5:
                c = hexc("dfe5ec") if r < 3.4 else hexc("4a5059")
            elif abs(abs(dx) - abs(dy)) < 1.6:
                c = hexc("2a2e35")           # the X: the seams between the four parts
            elif abs(abs(dx) - abs(dy)) < 3.2:
                c = hexc("e8edf2")           # bright edge along the X
            else:
                light = 1.05 - (dx + dy) / 120 - r / 160
                c = jitter(shade(hexc("aab2bc"), light), 0.03)
                if 17.5 < r < 18.5:
                    c = shade(c, 0.8)
            big.putpixel((x, y), c)
    tiles = []
    for row in range(3):
        for col in range(3):
            tiles.append(big.crop((col * 16, row * 16, col * 16 + 16, row * 16 + 16)))
    return tiles


def lower_level_textures():
    save(silver_panel(), "block", "silver_panel.png")
    for i, tile in enumerate(x_door_tiles()):
        save(tile, "block", f"x_door_{i}.png")
    floor = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = jitter(hexc("5f656e"), 0.05)
            if (x + y) % 8 in (0, 1) and (x - y) % 8 in (0, 1):
                c = hexc("7d848e")         # diamond plate bumps
            if x in (0, 15) or y in (0, 15):
                c = hexc("454a52")
            floor.putpixel((x, y), c)
    save(floor, "block", "silver_floor.png")
    light = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = shade(SILVER, 0.9)
            if 2 <= x <= 13 and 2 <= y <= 13:
                c = hexc("eaf6ff") if 4 <= x <= 11 and 4 <= y <= 11 else hexc("bfe3ff")
            light.putpixel((x, y), jitter(c, 0.02))
    save(light, "block", "silver_light.png")
    danger = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = jitter(hexc("3b414b"), 0.04)
            if x in (0, 15) or y in (0, 15):
                c = hexc("7fd1ff")
            elif x in (1, 14) or y in (1, 14):
                c = hexc("262a31")
            r = math.hypot(x - 7.5, y - 7.5)
            if 3.0 < r < 4.2:
                c = hexc("5a6573")
            elif r <= 1.6:
                c = hexc("9fe0ff")
            danger.putpixel((x, y), c)
    save(danger, "block", "danger_room_wall.png")
    for lit in (False, True):
        panel = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                base = hexc("dcefff") if lit else hexc("2c3139")
                c = jitter(base, 0.03)
                if x == y or x + y == 15 or x in (0, 15) or y in (0, 15):
                    c = hexc("ffffff") if lit else hexc("1d2026")
                panel.putpixel((x, y), c)
        save(panel, "block", "cerebro_panel_lit.png" if lit else "cerebro_panel.png")
    court = Image.new("RGBA", (16, 16))
    for y in range(16):
        plank = (y // 4)
        for x in range(16):
            c = jitter(shade(hexc("c99a5f"), 1 + (plank % 2) * 0.05), 0.04)
            if y % 4 == 3 or (x + plank * 5) % 16 == 0:
                c = hexc("a87a45")
            court.putpixel((x, y), c)
    save(court, "block", "court_floor.png")
    control = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = jitter(hexc("30343b"), 0.04)
            if x in (0, 15) or y in (0, 15):
                c = hexc("f2c230") if (x + y) % 4 < 2 else hexc("202020")
            control.putpixel((x, y), c)
    for x, y, col in ((4, 5, "e53935"), (5, 5, "e53935"), (4, 6, "e53935"), (5, 6, "e53935"),
                      (10, 5, "43d16a"), (11, 5, "43d16a"), (10, 6, "43d16a"), (11, 6, "43d16a")):
        control.putpixel((x, y), hexc(col))
    for x in range(3, 13):
        control.putpixel((x, 10), hexc("7fd1ff"))
        control.putpixel((x, 11), hexc("4a8fb5"))
    save(control, "block", "hangar_control.png")


def entity_textures():
    # Professor X's wheelchair: frame at the top, wheels lower down (see WheelchairLayer)
    chair = Image.new("RGBA", (64, 64))
    for y in range(64):
        for x in range(64):
            if y < 26 and x < 40:
                c = jitter(hexc("2a2a30"), 0.05)        # black leather seat and back
            elif y < 26:
                c = jitter(hexc("b8bec6"), 0.04)        # chrome tubes
            elif x < 40:
                c = jitter(hexc("1a1a1a"), 0.05)        # big wheels
            else:
                c = jitter(hexc("2a2a2a"), 0.05)
            chair.putpixel((x, y), c)
    # spokes on the wheel sides (1x18x18 box at (0, 26): side faces at (0, 44) and (19, 44))
    for ox in (0, 19):
        for y in range(18):
            for x in range(18):
                r = math.hypot(x - 8.5, y - 8.5)
                if 1.5 < r < 7.0:
                    c = hexc("c9ced6") if (x == y or x + y == 17 or x in (8, 9) or y in (8, 9)) else hexc("3a3a40")
                elif r <= 1.5:
                    c = hexc("dfe5ec")
                else:
                    continue
                chair.putpixel((ox + x, 44 + y), c)
    save(chair, "entity", "wheelchair.png")

    # the X-Jet
    jet = Image.new("RGBA", (128, 128))
    for y in range(128):
        for x in range(128):
            c = jitter(hexc("1b1c20"), 0.05)
            if x % 12 == 0 or y % 9 == 0:
                c = hexc("26282e")                      # panel lines
            jet.putpixel((x, y), c)
    for y in range(100, 116):                           # cockpit glass
        for x in range(0, 34):
            jet.putpixel((x, y), jitter(hexc("3a5068") if (x + y) % 7 else hexc("8fb3d1"), 0.05))
    for y in range(100, 106):                           # glowing engines
        for x in range(60, 70):
            jet.putpixel((x, y), hexc("7fd1ff") if (x + y) % 3 else hexc("d8f4ff"))
    # a big X on the wing tops (wing box 54x1x6 at (0, 56): top face at (6, 56) 54x6)
    for i in range(6):
        for x0 in (12, 44):
            jet.putpixel((x0 + i, 56 + i), hexc("8a8f99"))
            jet.putpixel((x0 + 5 - i, 56 + i), hexc("8a8f99"))
    save(jet, "entity", "blackbird.png")


def blackbird_icon():
    icon = draw(blank(), [
        "................",
        ".......HH.......",
        ".......LL.......",
        "......LGGL......",
        "......LGGL......",
        ".....LLLLLL.....",
        "....LLLLLLLL....",
        "...LLLLLLLLLL...",
        "..LLLLLLLLLLLL..",
        ".LLLLLLLLLLLLLL.",
        "LLLLL.LLLL.LLLLL",
        "......LLLL......",
        ".....D.LL.D.....",
        "....DD.LL.DD....",
        "......BBBB......",
        "................"], {"H": hexc("5a5f69"), "L": hexc("23252b"), "G": hexc("7fa8c9"), "D": hexc("3a3d45"),
                              "B": hexc("7fd1ff")})
    save(icon, "item", "blackbird.png")


def movie_suit():
    """Black leather suits like in the first movies: padded grey panels, an X on the belt buckle."""
    black = hexc("1b1b1f")
    pad = hexc("3d4352")
    belt = hexc("2c2c30")
    clear = (0, 0, 0, 0)
    l1 = Image.new("RGBA", (64, 32), clear)

    def visor(face, x, y, w, h):          # helmet slot: only a tinted visor band and the earpieces
        if face == "front" and y in (3, 4) and 0 < x < 7:
            return hexc("8fb3d1") if (y == 3 and x in (2, 5)) else hexc("2e3a4a")
        if face in ("left", "right") and y in (3, 4, 5) and x in (3, 4):
            return hexc("2a2a2e")
        if face in ("left", "right") and y in (3, 4) and x > 4:
            return hexc("2e3a4a")
        return None
    paint_box(l1, 0, 0, 8, 8, 8, visor)

    def chest(face, x, y, w, h):
        if face in ("top", "bottom"):
            return jitter(black)
        if y in (10, 11):
            if face == "front" and x in (3, 4):
                return hexc("d0d4da") if (x == 3) == (y == 10) else hexc("9aa0a8")
            return jitter(belt)
        if face in ("front", "back") and (x in (1, 6)) and y >= 2:
            return jitter(pad) if y % 3 else shade(pad, 0.8)
        if face == "front" and y < 2:
            return jitter(pad)
        if face in ("front", "back") and x in (3, 4) and face == "front" and y < 10:
            return shade(black, 1.25) if y % 2 else jitter(black)
        return jitter(black)
    paint_box(l1, 16, 16, 8, 12, 4, chest)

    def arm(face, x, y, w, h):
        if y < 3:
            return jitter(pad) if face != "bottom" else jitter(black)
        if y >= 9:
            return jitter(hexc("141416"))
        if face == "right" or face == "front" and y in (5, 6):
            return jitter(shade(pad, 0.85))
        return jitter(black)
    paint_box(l1, 40, 16, 4, 12, 4, arm)

    def boot(face, x, y, w, h):
        if face == "top":
            return None
        if face == "bottom" or y == 11:
            return hexc("0e0e10")
        if y < 6:
            return None
        if y == 6:
            return jitter(pad)
        return jitter(hexc("151517"))
    paint_box(l1, 0, 16, 4, 12, 4, boot)
    save(l1, "models", "armor", "xmen_suit_layer_1.png")

    l2 = Image.new("RGBA", (64, 32), clear)

    def waist(face, x, y, w, h):
        if face in ("top", "bottom") or y >= 7:
            return jitter(black)
        return None
    paint_box(l2, 16, 16, 8, 12, 4, waist)

    def leg(face, x, y, w, h):
        if face == "front" and y in (3, 4, 5):
            return jitter(pad) if y != 4 else shade(pad, 0.8)
        if face in ("left", "right") and x in (1, 2) and y < 9:
            return jitter(shade(pad, 0.9))
        return jitter(black)
    paint_box(l2, 0, 16, 4, 12, 4, leg)
    save(l2, "models", "armor", "xmen_suit_layer_2.png")

    # item icons
    pal = {"H": hexc("4a4a52"), "L": black, "M": shade(black, 0.85), "D": hexc("0c0c0e"), "G": pad,
           "S": hexc("6d4c2f")}
    mask = blank()
    for x in range(2, 14):
        for y in (6, 7, 8):
            mask.putpixel((x, y), hexc("2e3a4a") if y != 7 or x in (2, 13) else hexc("8fb3d1"))
    for y in (5, 6, 7, 8, 9):
        mask.putpixel((1, y), hexc("2a2a2e"))
        mask.putpixel((14, y), hexc("2a2a2e"))
    save(mask, "item", "xmen_mask.png")
    suit = draw(blank(), CHEST, pal)
    for y in range(3, 13):
        for x in (4, 11):
            if 0 <= x < 16 and suit.getpixel((x, y))[3]:
                suit.putpixel((x, y), pad)
    for x, y in ((7, 11), (8, 12), (8, 11), (7, 12)):
        suit.putpixel((x, y), hexc("d0d4da"))
    save(suit, "item", "xmen_suit.png")
    pants = draw(blank(), LEGS, pal)
    for y in range(6, 9):
        for x in range(16):
            if pants.getpixel((x, y))[3] and x not in (7, 8):
                pants.putpixel((x, y), pad)
    save(pants, "item", "xmen_pants.png")
    save(draw(blank(), BOOTS, pal), "item", "xmen_boots.png")


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
    "silver_panel": {"parent": "minecraft:block/cube_all", "textures": {"all": "xmen:block/silver_panel"}},
    "silver_floor": {"parent": "minecraft:block/cube_all", "textures": {"all": "xmen:block/silver_floor"}},
    "silver_light": {"parent": "minecraft:block/cube_all", "textures": {"all": "xmen:block/silver_light"}},
    "danger_room_wall": {"parent": "minecraft:block/cube_all", "textures": {"all": "xmen:block/danger_room_wall"}},
    "hangar_control": {"parent": "minecraft:block/cube_all", "textures": {"all": "xmen:block/hangar_control"}},
}

EMPTY = {"textures": {"particle": "xmen:block/silver_panel"}, "elements": []}


def glowing_cube(texture):
    """A full cube that is drawn at full brightness (NeoForge reads neoforge_data)."""
    faces = {f: {"texture": "#all", "cullface": f} for f in ("north", "south", "east", "west", "up", "down")}
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"all": texture, "particle": texture},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces, "shade": False,
                          "neoforge_data": {"block_light": 15, "sky_light": 15}}]}


def special_models():
    """Blocks with several looks: X-door pieces, Cerebro panels, the court floor."""
    block = os.path.join(ASSETS, "models", "block")
    write_json(os.path.join(block, "empty.json"), EMPTY)
    variants = {}
    for part in range(9):
        side = "xmen:block/silver_panel"
        write_json(os.path.join(block, f"x_door_{part}.json"), {
            "parent": "minecraft:block/cube",
            "textures": {"north": f"xmen:block/x_door_{part}", "south": f"xmen:block/x_door_{2 - part % 3 + part // 3 * 3}",
                         "east": side, "west": side, "up": side, "down": side, "particle": side}})
        for axis, rot in (("x", 0), ("z", 90)):
            variants[f"axis={axis},open=false,part={part}"] = {"model": f"xmen:block/x_door_{part}", "y": rot}
            variants[f"axis={axis},open=true,part={part}"] = {"model": "xmen:block/empty"}
    write_json(os.path.join(ASSETS, "blockstates", "x_door.json"), {"variants": variants})
    write_json(os.path.join(ASSETS, "models", "item", "x_door.json"), {"parent": "xmen:block/x_door_4"})

    write_json(os.path.join(block, "cerebro_panel.json"),
               {"parent": "minecraft:block/cube_all", "textures": {"all": "xmen:block/cerebro_panel"}})
    write_json(os.path.join(block, "cerebro_panel_lit.json"), glowing_cube("xmen:block/cerebro_panel_lit"))
    write_json(os.path.join(ASSETS, "blockstates", "cerebro_panel.json"), {"variants": {
        "lit=false": {"model": "xmen:block/cerebro_panel"}, "lit=true": {"model": "xmen:block/cerebro_panel_lit"}}})
    write_json(os.path.join(ASSETS, "models", "item", "cerebro_panel.json"), {"parent": "xmen:block/cerebro_panel"})

    write_json(os.path.join(block, "court_floor.json"),
               {"parent": "minecraft:block/cube_all", "textures": {"all": "xmen:block/court_floor"}})
    write_json(os.path.join(ASSETS, "blockstates", "court_floor.json"), {"variants": {
        "open=false": {"model": "xmen:block/court_floor"}, "open=true": {"model": "xmen:block/empty"}}})
    write_json(os.path.join(ASSETS, "models", "item", "court_floor.json"), {"parent": "xmen:block/court_floor"})
    for name in ("x_door", "cerebro_panel", "court_floor"):
        write_json(os.path.join(DATA, "xmen", "loot_table", "blocks", name + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "xmen:" + name}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
ITEMS = ["mutant_serum", "celestial_orb", "adamantium_ingot", "sentinel_part", "adamantium_helmet",
         "adamantium_chestplate", "adamantium_leggings", "adamantium_boots", "xmen_mask", "xmen_suit", "xmen_pants",
         "xmen_boots", "magneto_helmet", "blackbird"]
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
    "item.xmen.xmen_mask": ("X-Men Visor", "X-Men-Visier"),
    "item.xmen.xmen_suit": ("X-Men Suit", "X-Men-Anzug"),
    "item.xmen.xmen_pants": ("X-Men Pants", "X-Men-Hose"),
    "item.xmen.xmen_boots": ("X-Men Boots", "X-Men-Stiefel"),
    "item.xmen.magneto_helmet": ("Magneto's Helmet", "Magnetos Helm"),
    "itemGroup.xmen": ("X-Men", "X-Men"),
    "block.xmen.x_door": ("X-Door", "X-Tür"),
    "block.xmen.cerebro_panel": ("Cerebro Panel", "Cerebro-Platte"),
    "block.xmen.silver_panel": ("Silver Wall Panel", "Silberne Wandplatte"),
    "block.xmen.silver_floor": ("Silver Floor", "Silberner Boden"),
    "block.xmen.silver_light": ("Silver Light", "Silberne Lampe"),
    "block.xmen.danger_room_wall": ("Danger Room Wall", "Danger-Room-Wand"),
    "block.xmen.court_floor": ("Basketball Court Floor", "Basketballplatz-Boden"),
    "block.xmen.hangar_control": ("Hangar Control", "Hangar-Steuerung"),
    "item.xmen.blackbird": ("X-Jet (Blackbird)", "X-Jet (Blackbird)"),
    "entity.xmen.mutant": ("Mutant", "Mutant"),
    "entity.xmen.blackbird": ("X-Jet", "X-Jet"),
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
        "xmen_mask": shaped("xmen_mask", ["LGL", "B B"], {"L": "leather", "G": "glass_pane", "B": "black_dye"}),
        "xmen_suit": shaped("xmen_suit", ["L L", "LBL", "LIL"], {"L": "leather", "B": "black_dye",
                                                                 "I": "iron_ingot"}),
        "xmen_pants": shaped("xmen_pants", ["LBL", "L L", "L L"], {"L": "leather", "B": "black_dye"}),
        "xmen_boots": shaped("xmen_boots", ["B B", "L L"], {"L": "leather", "B": "black_dye"}),
        "blackbird": shaped("blackbird", ["IEI", "SNS", "BFB"], {"I": "iron_block", "E": "elytra",
                                                               "S": "xmen:sentinel_part", "N": "nether_star",
                                                               "B": "black_dye", "F": "firework_rocket"}),
        "silver_panel": shaped("silver_panel", ["II", "II"], {"I": "iron_ingot"}, 8),
        "silver_floor": shaped("silver_floor", ["IS", "SI"], {"I": "iron_ingot", "S": "smooth_stone"}, 8),
        "silver_light": shaped("silver_light", [" I ", "IGI", " I "], {"I": "iron_ingot", "G": "glowstone"}, 4),
        "x_door": shaped("x_door", ["III", "IPI", "III"], {"I": "iron_ingot", "P": "piston"}, 3),
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
               {"replace": False, "values": ["xmen:elevator", "xmen:cerebro", "xmen:danger_room_console",
                                             "xmen:x_door", "xmen:cerebro_panel", "xmen:silver_panel",
                                             "xmen:silver_floor", "xmen:silver_light", "xmen:danger_room_wall",
                                             "xmen:hangar_control"]})
    write_json(os.path.join(DATA, "minecraft", "tags", "block", "mineable", "axe.json"),
               {"replace": False, "values": ["xmen:court_floor"]})
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
    lower_level_textures()
    entity_textures()
    blackbird_icon()
    movie_suit()
    models()
    special_models()
    recipes()
    lang()
    print("assets written")
