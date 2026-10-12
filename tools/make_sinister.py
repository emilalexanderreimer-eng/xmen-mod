"""Draws everything for Mister Sinister: his skin, his clones' skin, his cape, the glowing eyes and diamonds, plus
the spawn egg models and names. Run from the repo root: python3 tools/make_sinister.py (needs Pillow). Pass
--preview to also write build/sinister_preview.png, a flat front view of the skin."""
import json
import os
import sys

from PIL import Image

from make_magneto import ASSETS, box_faces, hexc, noisy, paint_box, save, shade

CLEAR = (0, 0, 0, 0)

# the classic look: chalk-white skin, black hair with a widow's peak, red diamond on the forehead and chest,
# a blue-black suit with steel shoulder plates and gauntlets
LOOKS = {
    "mister_sinister": {"skin": hexc("e4e2df"), "shadow": hexc("aeb2bd"), "hair": hexc("17161b"),
                        "suit": hexc("1b1e29"), "suit_dark": hexc("101219"), "steel": hexc("343d57"),
                        "trim": hexc("8c93a3"), "red": hexc("c3121d"), "lips": hexc("3b1822")},
    # the clones are grown copies: greyer suits, paler and a little sickly
    "sinister_clone": {"skin": hexc("d7dbd2"), "shadow": hexc("9fa89e"), "hair": hexc("1e1f22"),
                       "suit": hexc("2a2d2f"), "suit_dark": hexc("1a1c1d"), "steel": hexc("3f4446"),
                       "trim": hexc("6e7472"), "red": hexc("7d1a20"), "lips": hexc("3a2a2c")},
}


CHEST_DIAMOND = {1: (3, 4), 2: (2, 5), 3: (1, 6), 4: (2, 5), 5: (3, 4)}   # row: first and last column


def diamond(x, y):
    """True inside the red diamond on his chest (front of the body, 8 wide)."""
    span = CHEST_DIAMOND.get(y)
    return span is not None and span[0] <= x <= span[1]


def skin(name, look):
    img = Image.new("RGBA", (64, 64), CLEAR)
    SKIN, SHADOW, HAIR = look["skin"], look["shadow"], look["hair"]
    SUIT, SUIT_DARK, STEEL, TRIM, RED = look["suit"], look["suit_dark"], look["steel"], look["trim"], look["red"]

    def head(face, x, y, w, h):
        if face == "top" or face == "back":
            return HAIR
        if face == "bottom":
            return SHADOW
        if face == "front":
            if y == 0:
                return HAIR
            if y == 1:
                return HAIR if x in (0, 3, 4, 7) else SKIN          # widow's peak
            if y == 2:
                return HAIR if x in (0, 7) else (RED if x in (3, 4) else SKIN)
            if y == 3:
                if x in (3, 4):
                    return shade(RED, -0.25)                           # bottom of the forehead diamond
                return HAIR if x in (1, 2, 5, 6) else shade(SKIN, -0.05)   # sharp black brows
            if y == 4:
                if x in (2, 5):
                    return RED                                          # red eyes
                if x in (1, 6):
                    return hexc("3a3440")
                return SKIN
            if y == 5:
                if x in (0, 7):
                    return SHADOW                                       # hollow cheeks
                return shade(SKIN, -0.08) if x == 4 else SKIN
            if y == 6:
                if x in (3, 4):
                    return look["lips"]                                 # a thin, cruel smile
                if x in (2, 5):
                    return shade(look["lips"], 0.35)
                return SHADOW if x in (0, 7) else SKIN
            return shade(SKIN, -0.08) if 1 <= x <= 6 else SHADOW
        # sides: slicked-back hair over the top and back, an ear and a gaunt cheek
        back = x >= 4 if face == "right" else x <= 3
        if y < 3 or back:
            return HAIR
        return SHADOW if y >= 5 else SKIN

    def body(face, x, y, w, h):
        if face == "front":
            if diamond(x, y):
                edge = x == CHEST_DIAMOND[y][0] and y <= 3
                return shade(RED, 0.25) if edge else RED
            if y == 8:
                return RED if x in (3, 4) else TRIM                    # belt
            if x in (0, 7):
                return STEEL
            if y in (0, 1) and x in (1, 6):
                return STEEL
            return SUIT
        if face == "back":
            return TRIM if y == 8 else SUIT_DARK
        if face == "top":
            return STEEL
        return TRIM if y == 8 else SUIT_DARK

    def arm(face, x, y, w, h):
        if face == "bottom":
            return STEEL
        if face == "top":
            return SUIT
        if y == 6:
            return TRIM                                                 # edge of the gauntlet
        if y >= 7:
            ridge = face == "front" and x == 1
            return shade(STEEL, 0.15) if ridge else STEEL
        return SUIT if face in ("front", "left", "right") else SUIT_DARK

    def leg(face, x, y, w, h):
        if face == "bottom":
            return SUIT_DARK
        if face == "top":
            return SUIT
        if y == 6:
            return TRIM                                                 # top of the boots
        if y >= 7:
            return STEEL if face == "front" else shade(STEEL, -0.15)
        if face == "front" and 1 <= y <= 4 and x in (1, 2):
            return shade(SUIT, 0.1)                                     # thigh plates
        return SUIT if face == "front" else SUIT_DARK

    paint_box(img, 0, 0, 8, 8, 8, head)
    paint_box(img, 16, 16, 8, 12, 4, body)
    paint_box(img, 40, 16, 4, 12, 4, arm)
    paint_box(img, 32, 48, 4, 12, 4, arm)
    paint_box(img, 0, 16, 4, 12, 4, leg)
    paint_box(img, 16, 48, 4, 12, 4, leg)

    # outer layers: slicked-back hair that stands out over the head, steel shoulder plates
    def hair(face, x, y, w, h):
        if face == "top" or face == "back":
            return shade(HAIR, 0.06)
        if face in ("left", "right") and y < 4:
            back = x >= 4 if face == "right" else x <= 3
            return HAIR if back or y < 2 else CLEAR
        if face == "front" and y == 0 and x not in (3, 4):
            return HAIR
        return CLEAR

    def shoulder(face, x, y, w, h):
        if face == "top":
            return shade(TRIM, -0.1)
        if face == "bottom":
            return CLEAR
        if y < 3:
            return shade(STEEL, 0.12 - 0.08 * y) if y < 2 else TRIM
        return CLEAR

    paint_layer(img, 32, 0, 8, 8, 8, hair)
    paint_layer(img, 40, 32, 4, 12, 4, shoulder)
    paint_layer(img, 48, 48, 4, 12, 4, shoulder)
    save(img, "textures", "entity", f"{name}.png")
    return img


def paint_layer(img, u, v, w, h, d, color_at):
    """Like paint_box, but transparent pixels stay transparent (for the outer skin layers)."""
    for face, (fx, fy, fw, fh) in box_faces(u, v, w, h, d).items():
        for x in range(fw):
            for y in range(fh):
                c = color_at(face, x, y, fw, fh)
                img.putpixel((fx + x, fy + y), CLEAR if c[3] == 0 else noisy(c, 0.04))


def glow():
    """Only the parts that glow in the dark: his eyes and the two red diamonds."""
    base = skin("mister_sinister", LOOKS["mister_sinister"])
    img = Image.new("RGBA", (64, 64), CLEAR)
    red = LOOKS["mister_sinister"]["red"]
    head_front = box_faces(0, 0, 8, 8, 8)["front"]
    body_front = box_faces(16, 16, 8, 12, 4)["front"]
    for x, y in ((3, 2), (4, 2), (3, 3), (4, 3), (2, 4), (5, 4)):
        img.putpixel((head_front[0] + x, head_front[1] + y), shade(red, 0.3))
    for x in range(8):
        for y in range(12):
            if diamond(x, y):
                img.putpixel((body_front[0] + x, body_front[1] + y), base.getpixel((body_front[0] + x,
                                                                                     body_front[1] + y)))
    save(img, "textures", "entity", "mister_sinister_glow.png")


def cape():
    """A long black cape with a blood-red lining and a ragged bottom edge."""
    img = Image.new("RGBA", (64, 32), CLEAR)
    outside, inside = hexc("121218"), hexc("6e0c14")
    torn = {0: 2, 1: 0, 2: 1, 3: 3, 4: 0, 5: 2, 6: 1, 7: 0, 8: 3, 9: 1}   # how many rows each column is missing

    def color(face, x, y, w, h):
        col = x if face != "back" else w - 1 - x
        if face in ("front", "back") and y >= h - torn.get(col, 0):
            return CLEAR
        if face == "front":
            fold = (x % 3 == 0) * -0.25 + (x % 3 == 1) * 0.1
            return shade(outside, fold + 0.12 * (1 - y / h))
        if face == "back":
            return shade(inside, (x % 3 == 0) * -0.2 - 0.15 * y / h)
        return shade(outside, -0.2)

    for face, (fx, fy, fw, fh) in box_faces(0, 0, 10, 16, 1).items():
        for x in range(fw):
            for y in range(fh):
                c = color(face, x, y, fw, fh)
                img.putpixel((fx + x, fy + y), CLEAR if c[3] == 0 else noisy(c, 0.04))
    save(img, "textures", "entity", "mister_sinister_cape.png")


def preview(tex):
    """The front of the head and body, 16x scale."""
    scale = 16
    out = Image.new("RGBA", (8 * scale, 20 * scale), hexc("2b2b2b"))
    parts = [(box_faces(0, 0, 8, 8, 8)["front"], 0), (box_faces(16, 16, 8, 12, 4)["front"], 8)]
    for (fx, fy, fw, fh), oy in parts:
        for x in range(fw):
            for y in range(fh):
                c = tex.getpixel((fx + x, fy + y))
                for dx in range(scale):
                    for dy in range(scale):
                        out.putpixel((x * scale + dx, (oy + y) * scale + dy), c)
    os.makedirs("build", exist_ok=True)
    out.save(os.path.join("build", "sinister_preview.png"))


NAMES = {
    "entity.xmen.mister_sinister": ("Mister Sinister", "Mister Sinister"),
    "entity.xmen.sinister_clone": ("Sinister Clone", "Sinister-Klon"),
    "entity.xmen.sinister_blast": ("Sinister's Energy Blast", "Sinisters Energiestoß"),
    "item.xmen.mister_sinister_spawn_egg": ("Mister Sinister Spawn Egg", "Mister-Sinister-Spawn-Ei"),
    "item.xmen.sinister_clone_spawn_egg": ("Sinister Clone Spawn Egg", "Sinister-Klon-Spawn-Ei"),
}


def models_and_lang():
    for egg in ("mister_sinister_spawn_egg", "sinister_clone_spawn_egg"):
        with open(os.path.join(ASSETS, "models", "item", f"{egg}.json"), "w") as f:
            json.dump({"parent": "minecraft:item/template_spawn_egg"}, f, indent=2)
            f.write("\n")
    for index, file in ((0, "en_us.json"), (1, "de_de.json")):
        path = os.path.join(ASSETS, "lang", file)
        with open(path) as f:
            data = json.load(f)
        for key, names in NAMES.items():
            data[key] = names[index]
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    tex = skin("mister_sinister", LOOKS["mister_sinister"])
    skin("sinister_clone", LOOKS["sinister_clone"])
    glow()
    cape()
    models_and_lang()
    if "--preview" in sys.argv:
        preview(tex)
    print("mister sinister assets written")
