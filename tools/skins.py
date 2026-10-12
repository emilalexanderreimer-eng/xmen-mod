"""Paints the 64x64 skins of the X-Men characters (same layout as a Minecraft player skin).

Run:  python3 tools/skins.py   (needs Pillow)
Every character is described by a few colors and options in CHARACTERS below.
"""
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "xmen", "textures", "entity", "mutant")


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


CLEAR = (0, 0, 0, 0)

# box layout: name -> (u, v, width, height, depth)
BOXES = {
    "head": (0, 0, 8, 8, 8), "hat": (32, 0, 8, 8, 8),
    "body": (16, 16, 8, 12, 4), "jacket": (16, 32, 8, 12, 4),
    "rarm": (40, 16, 4, 12, 4), "rsleeve": (40, 32, 4, 12, 4),
    "larm": (32, 48, 4, 12, 4), "lsleeve": (48, 48, 4, 12, 4),
    "rleg": (0, 16, 4, 12, 4), "rpants": (0, 32, 4, 12, 4),
    "lleg": (16, 48, 4, 12, 4), "lpants": (0, 48, 4, 12, 4),
}


def faces(box):
    """Yields (face, x0, y0, w, h) for the six faces of a box."""
    u, v, w, h, d = BOXES[box]
    yield "top", u + d, v, w, d
    yield "bottom", u + d + w, v, w, d
    yield "right", u, v + d, d, h
    yield "front", u + d, v + d, w, h
    yield "left", u + d + w, v + d, d, h
    yield "back", u + d + w + d, v + d, w, h


class Skin:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), CLEAR)
        self.rand = random.Random(seed)

    def paint(self, box, fn):
        """fn(face, x, y, w, h) -> color or None (leave)."""
        for face, x0, y0, w, h in faces(box):
            for y in range(h):
                for x in range(w):
                    c = fn(face, x, y, w, h)
                    if c is not None:
                        self.img.putpixel((x0 + x, y0 + y), c)

    def px(self, box, face, x, y, c):
        for f, x0, y0, w, h in faces(box):
            if f == face and 0 <= x < w and 0 <= y < h:
                self.img.putpixel((x0 + x, y0 + y), c)

    def noise(self, c, amount=0.06):
        return shade(c, 1 + (self.rand.random() - 0.5) * 2 * amount)


def make(spec, seed):
    s = Skin(seed)
    skin = hexc(spec.get("skin", "e0ac8a"))
    hair = hexc(spec.get("hair", "3b2a1e"))
    style = spec.get("style", "short")
    eye = hexc(spec.get("eyes", "3a5f9c"))
    suit = hexc(spec.get("suit", "1e1e22"))
    suit2 = hexc(spec.get("panel", "3a3a42"))
    sleeves = hexc(spec["sleeves"]) if "sleeves" in spec else suit
    gloves = hexc(spec.get("gloves", spec.get("suit", "1e1e22")))
    legs = hexc(spec.get("legs", spec.get("suit", "1e1e22")))
    boots = hexc(spec.get("boots", "111111"))
    belt = hexc(spec.get("belt", "2a2a2a"))
    buckle = hexc(spec.get("buckle", "c0c0c0"))
    bare_arms = spec.get("bare_arms", False)
    n = s.noise

    # ------------------------------------------------------------ head
    def head(face, x, y, w, h):
        c = n(skin, 0.03)
        if style == "bald":
            return c
        if face == "top":
            return n(hair)
        if face == "back":
            if style in ("long", "white_long", "streak", "red_long"):
                return n(hair)
            return n(hair) if y < 6 else c
        if face in ("left", "right"):
            if style in ("long", "white_long", "streak", "red_long"):
                return n(hair) if (y < 3 or x >= 3 or y >= 0 and (face == "right" and x < 2 or face == "left" and x > 5)) and not (2 <= y <= 5 and 2 <= x <= 4) else c
            if style == "spiky":
                return n(hair) if y < 3 or (x >= 4 and y < 6) or (y < 7 and x in (1, 6)) else c
            return n(hair) if y < 2 or (x >= 5 and y < 5) else c
        if face == "front":
            if y == 0:
                return n(hair)
            if y == 1:
                return n(hair) if style != "short_receding" else c
            if style in ("long", "white_long", "streak", "red_long") and (x == 0 or x == 7) and y < 7:
                return n(hair)
            if style == "spiky" and (x in (0, 7) and y < 6):
                return n(hair)
            return c
        return c

    s.paint("head", head)
    # face
    s.px("head", "front", 1, 4, hexc("ffffff"))
    s.px("head", "front", 2, 4, eye)
    s.px("head", "front", 5, 4, eye)
    s.px("head", "front", 6, 4, hexc("ffffff"))
    s.px("head", "front", 1, 3, shade(hair, 0.9) if style != "bald" else shade(skin, 0.75))
    s.px("head", "front", 2, 3, shade(hair, 0.9) if style != "bald" else shade(skin, 0.75))
    s.px("head", "front", 5, 3, shade(hair, 0.9) if style != "bald" else shade(skin, 0.75))
    s.px("head", "front", 6, 3, shade(hair, 0.9) if style != "bald" else shade(skin, 0.75))
    s.px("head", "front", 3, 5, shade(skin, 0.85))
    s.px("head", "front", 4, 5, shade(skin, 0.85))
    s.px("head", "front", 3, 6, hexc(spec.get("lips", "a0524a")))
    s.px("head", "front", 4, 6, hexc(spec.get("lips", "a0524a")))
    if spec.get("streak"):
        for y in range(0, 3):
            s.px("head", "front", 3, y, hexc("f0f0f0"))
            s.px("head", "front", 4, y, hexc("f0f0f0"))
        for x in range(2, 6):
            s.px("head", "top", x, 7, hexc("f0f0f0"))
    if spec.get("sideburns"):
        for y in range(2, 7):
            s.px("head", "front", 0, y, n(hair))
            s.px("head", "front", 7, y, n(hair))
        for y in range(3, 7):
            s.px("head", "right", 7, y, n(hair))
            s.px("head", "left", 0, y, n(hair))
    if spec.get("beard"):
        for x in range(1, 7):
            s.px("head", "front", x, 7, n(hexc(spec["beard"])))
        for x in (1, 2, 5, 6):
            s.px("head", "front", x, 6, n(hexc(spec["beard"])))
    if spec.get("visor"):
        v = hexc("c8102e")
        for x in range(8):
            for face in ("front", "left", "right", "back"):
                s.px("head", face, x, 4, v)
        s.px("head", "front", 1, 3, hexc("5a5a5a"))
        s.px("head", "front", 6, 3, hexc("5a5a5a"))
        for x in range(1, 7):
            s.px("head", "front", x, 4, hexc("e53950") if x in (2, 5) else v)
    if spec.get("glowing_eyes"):
        s.px("head", "front", 1, 4, hexc(spec["glowing_eyes"]))
        s.px("head", "front", 2, 4, hexc(spec["glowing_eyes"]))
        s.px("head", "front", 5, 4, hexc(spec["glowing_eyes"]))
        s.px("head", "front", 6, 4, hexc(spec["glowing_eyes"]))
    if spec.get("diamond"):
        s.px("head", "front", 3, 2, hexc("c8102e"))
        s.px("head", "front", 4, 2, hexc("c8102e"))
        s.px("head", "front", 3, 1, hexc("c8102e"))
        s.px("head", "front", 4, 1, hexc("8b0000"))
    if spec.get("helmet"):
        # big helmet on the hat layer (Magneto, Apocalypse, Avalanche)
        hc = hexc(spec["helmet"])
        hc2 = hexc(spec.get("helmet2", spec["helmet"]))

        def helmet(face, x, y, w, h):
            if face == "front":
                if y <= 2 or x in (0, 7) or (y == 7 and x not in (2, 3, 4, 5)):
                    return n(hc)
                if y == 3 and x in (0, 1, 6, 7):
                    return n(hc)
                return None
            if face == "bottom":
                return None
            if face == "top" and spec.get("helmet_crest"):
                return n(hc2) if x in (3, 4) else n(hc)
            return n(hc) if not (face in ("left", "right") and y >= 6 and spec.get("open_sides")) else None

        s.paint("hat", helmet)
        if spec.get("helmet_crest"):
            for y in range(0, 3):
                s.px("hat", "front", 3, y, hc2)
                s.px("hat", "front", 4, y, hc2)
    if spec.get("cowl"):
        # Wolverine's comic mask is not in the films; this is the movie haircut instead
        pass

    # ------------------------------------------------------------ body
    emblem = spec.get("emblem", "x")

    def body(face, x, y, w, h):
        if face == "top":
            return n(suit)
        if spec.get("open_collar") and face == "front" and y < 2 and 2 <= x <= 5:
            return n(skin)
        if spec.get("tank_top") and face in ("front", "back") and y < 2 and (x < 2 or x > 5):
            return n(skin)
        if y in (10, 11):
            return n(belt)
        c = suit
        # padded side panels like the movie suits
        if face in ("front", "back") and (x in (0, 7)) and spec.get("panels", True):
            c = suit2
        if face == "front" and spec.get("chest_v") and y < 6 and abs(x - 3.5) <= (5 - y) * 0.6:
            c = hexc(spec["chest_v"])
        if face == "front" and spec.get("stripe") and x in (3, 4):
            c = hexc(spec["stripe"])
        return n(c)

    s.paint("body", body)
    if emblem == "x":
        s.px("body", "front", 3, 10, buckle)
        s.px("body", "front", 4, 10, buckle)
        s.px("body", "front", 3, 11, buckle)
        s.px("body", "front", 4, 11, buckle)
        s.px("body", "front", 3, 10, shade(buckle, 0.6))
        s.px("body", "front", 4, 11, shade(buckle, 0.6))
    if spec.get("coat"):
        coat = hexc(spec["coat"])

        def jacket(face, x, y, w, h):
            if face == "front" and 3 <= x <= 4 and y < 10:
                return None
            if face in ("top", "bottom"):
                return None
            return n(coat)

        s.paint("jacket", jacket)
        if spec.get("long_coat"):
            for leg in ("rpants", "lpants"):
                s.paint(leg, lambda face, x, y, w, h: n(coat) if face not in ("top", "bottom") and y < 9
                        and not (face == "front" and ((leg == "rpants" and x == 3) or (leg == "lpants" and x == 0)))
                        else None)

    # ------------------------------------------------------------ arms
    def arm(face, x, y, w, h):
        if face == "top":
            return n(sleeves if not bare_arms else skin)
        if face == "bottom":
            return n(gloves)
        if y >= 9:
            return n(gloves)
        if bare_arms and y >= 2:
            return n(skin, 0.03)
        if spec.get("shoulder") and y < 2:
            return n(hexc(spec["shoulder"]))
        return n(sleeves)

    s.paint("rarm", arm)
    s.paint("larm", arm)

    # ------------------------------------------------------------ legs
    def leg(face, x, y, w, h):
        if face == "top":
            return n(legs)
        if face == "bottom" or y >= spec.get("boot_top", 8):
            return n(boots)
        if face == "front" and y in (4, 5) and spec.get("knee"):
            return n(hexc(spec["knee"]))
        return n(legs)

    s.paint("rleg", leg)
    s.paint("lleg", leg)
    if spec.get("extra"):
        spec["extra"](s, n)
    return s.img


def fur(color):
    """Covers every part with the same color (Beast, Nightcrawler's tail-less look...)."""
    def extra(s, n):
        c = hexc(color)
        for box in ("head",):
            s.paint(box, lambda face, x, y, w, h: n(c, 0.1) if not (face == "front" and 1 <= y <= 7 and 1 <= x <= 6)
                    else None)
    return extra


def metal_skin(s, n):
    """Colossus: steel skin with lines."""
    for box in ("head", "rarm", "larm"):
        def fn(face, x, y, w, h, box=box):
            if box == "head" and face == "front" and y in (3, 4, 6):
                return None
            if box != "head" and y >= 9:
                return None
            if box != "head" and y < 2:
                return None
            return n(hexc("aab4be") if (x + y) % 4 else hexc("7d8790"), 0.04)
        s.paint(box, fn)


CHARACTERS = {
    # ---- the X-Men, in their black movie suits
    "wolverine": dict(skin="d9a07a", hair="2b1d14", style="spiky", sideburns=True, eyes="3b2a1e",
                      suit="18181b", panel="3b3b42", sleeves="18181b", gloves="1a1a1a", belt="2b2b2b",
                      buckle="c9c9c9", knee="2d2d33", bare_arms=False),
    "cyclops": dict(skin="e2b08e", hair="6b4a2f", visor=True, suit="141418", panel="2f3a58", chest_v="2f3a58",
                    belt="2b2b2b", buckle="d0d0d0"),
    "storm": dict(skin="7a4e33", hair="f2f2f2", style="white_long", eyes="5a6f8f", suit="141416", panel="3a3a44",
                  coat="1b1b20", lips="6e3a35"),
    "jean_grey": dict(skin="f0c9a8", hair="b8401f", style="red_long", eyes="3f8a4a", suit="151518",
                      panel="2f4a3a", belt="2b2b2b", lips="b44b4b"),
    "beast": dict(skin="3a56a8", hair="2a3f85", style="short", eyes="f2c230", suit="151518", panel="3a3a44",
                  glasses=True, lips="263a75", extra=fur("2f4ca0"), sleeves="151518"),
    "rogue": dict(skin="efc7a5", hair="6e4428", style="streak", streak=True, eyes="3f8a4a", suit="1a2a1c",
                  panel="2f4a30", gloves="2a3a2a", coat="2b3a2b", lips="b44b4b"),
    "iceman": dict(skin="e8c3a2", hair="d9b26b", eyes="4aa3df", suit="18181b", panel="3a4a5a",
                   sleeves="18181b", knee="2a3a4a"),
    "nightcrawler": dict(skin="1f2f6b", hair="141e45", style="short", glowing_eyes="f5d742", suit="1a1a1e",
                         panel="8b1a1a", lips="141e45", gloves="1f2f6b", boots="1f2f6b"),
    "colossus": dict(skin="aab4be", hair="2b1d14", eyes="3a5f9c", suit="1a1a1e", panel="8b1a1a",
                     extra=metal_skin, chest_v="8b1a1a"),
    "kitty_pryde": dict(skin="efc7a5", hair="3b2416", style="long", eyes="5b3a1e", suit="18181b",
                        panel="4a3a5a", lips="c06060"),
    "jubilee": dict(skin="f0c9a8", hair="111111", style="long", eyes="3b2a1e", suit="18181b", coat="f2c230",
                    lips="c06060"),
    "gambit": dict(skin="e2b08e", hair="5a3a24", style="long", glowing_eyes="ff2a2a", suit="1a1a1e",
                   panel="5a2a5a", coat="6b4a2f", long_coat=True),
    "professor_x": dict(skin="e8bfa0", style="bald", eyes="3a5f9c", suit="2d2d33", panel="2d2d33",
                        sleeves="2d2d33", gloves="e8bfa0", legs="2d2d33", boots="1a1a1a", belt="2d2d33",
                        emblem="tie", open_collar=False, stripe="7a1f1f", panels=False),
    # ---- other friendly mutants
    "moira": dict(skin="f0c9a8", hair="8a5a2a", style="long", eyes="3f8a4a", suit="f2f2f2", panel="e0e0e0",
                  sleeves="f2f2f2", gloves="f0c9a8", legs="3a3a44", belt="f2f2f2", emblem="none", panels=False),
    "scientist": dict(skin="e2b08e", hair="4a4a4a", eyes="3a5f9c", suit="f2f2f2", panel="e0e0e0",
                      sleeves="f2f2f2", gloves="e2b08e", legs="2a3a5a", belt="f2f2f2", emblem="none",
                      panels=False),
    "morlock": dict(skin="9aa48a", hair="2a2a1a", style="long", eyes="c9c96a", suit="4a3a2a", panel="3a2e22",
                    sleeves="5a4a35", gloves="9aa48a", legs="3a3226", boots="2a2218", belt="2a2218",
                    emblem="none"),
    "krakoa_mutant": dict(skin="c99a74", hair="2e5a2a", style="long", eyes="3f8a4a", suit="2e6b3a",
                          panel="f2c230", legs="1f4a28", boots="3a2a1a", belt="f2c230", emblem="none"),
    "refugee": dict(skin="e2b08e", hair="6b4a2f", eyes="3a5f9c", suit="5a6b7a", panel="4a5a6a", legs="3a4a5a",
                    boots="3a2a1a", belt="3a2a1a", emblem="none", gloves="e2b08e"),
    # ---- villains
    "mystique": dict(skin="1f4fb0", hair="c8102e", style="short", glowing_eyes="f5d742", suit="1f4fb0",
                     panel="163c8a", sleeves="1f4fb0", gloves="1f4fb0", legs="1f4fb0", boots="1f4fb0",
                     belt="1f4fb0", emblem="none", lips="163c8a"),
    "pyro": dict(skin="e8c3a8", hair="d9b26b", eyes="3a5f9c", suit="c84a12", panel="7a2a0a", sleeves="c84a12",
                 gloves="3a2a1a", legs="5a2a12", boots="2a1a10", belt="2a1a10", emblem="none"),
    "toad": dict(skin="9ab06a", hair="3a4a1a", eyes="c9c96a", suit="3a5a2a", panel="2a4a1a", legs="3a4a2a",
                 belt="2a2a1a", emblem="none", lips="5a6a3a"),
    "avalanche": dict(skin="e2b08e", hair="3b2416", suit="6b4a2f", panel="4a3220", sleeves="6b4a2f",
                      gloves="4a3220", legs="4a3220", boots="2a1a10", belt="2a1a10", emblem="none",
                      helmet="8a6a3a", open_sides=True),
    "blob": dict(skin="e8b896", hair="c9a06a", eyes="3a5f9c", suit="f2f2f2", tank_top=True, bare_arms=True,
                 legs="3a4a6a", boots="2a2a2a", belt="2a2a2a", emblem="none", panels=False, gloves="e8b896"),
    "white_queen": dict(skin="f5d8c0", hair="f5e6a8", style="long", eyes="4aa3df", suit="f5f5f5",
                        panel="e0e0e8", sleeves="f5f5f5", gloves="f5f5f5", legs="f5f5f5", boots="f5f5f5",
                        belt="f5f5f5", emblem="none", lips="c06070", coat="f5f5f5"),
}


# ---------------------------------------------------------------- extras for the characters added in 0.6.2
def eyepatch(s, n):
    """Callisto's black eyepatch over her right eye, with the strap around the head."""
    black = hexc("0e0e10")
    for x in (1, 2):
        for y in (3, 4, 5):
            s.px("head", "front", x, y, black)
    for x in range(8):
        s.px("head", "right", x, 3, black)
    for x in range(8):
        s.px("head", "back", x, 3, black)
    s.px("head", "front", 0, 3, black)
    s.px("head", "front", 3, 3, black)


def bone_spikes(s, n):
    """Marrow: white bone spikes growing out of her shoulders, arms and back."""
    bone = hexc("ece4cf")
    for box in ("rarm", "larm"):
        for (x, y) in ((1, 1), (2, 4), (1, 7)):
            for face in ("front", "back", "left", "right"):
                s.px(box, face, x, y, n(bone, 0.05))
    for (x, y) in ((1, 1), (6, 1), (3, 4), (5, 7), (2, 8)):
        s.px("body", "back", x, y, n(bone, 0.05))
    for x in (2, 5):
        s.px("head", "front", x, 6, bone)


def masque_face(s, n):
    """Masque reshapes faces, and his own is a patchwork of scars under a hood."""
    scar = hexc("b58a8a")
    pale = hexc("d8c0a8")
    for (x, y) in ((1, 2), (2, 2), (5, 5), (6, 5), (6, 6), (1, 6), (2, 7), (4, 3)):
        s.px("head", "front", x, y, scar)
    for (x, y) in ((3, 4), (4, 4), (0, 5), (7, 3)):
        s.px("head", "front", x, y, pale)
    hood = hexc("3a2f2a")

    def hat(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "front":
            return n(hood) if y == 0 or x in (0, 7) else None
        return n(hood)
    s.paint("hat", hat)


def leech_look(s, n):
    """Leech: a small green boy with big black eyes."""
    black = hexc("08080a")
    for x in (1, 2, 5, 6):
        for y in (3, 4):
            s.px("head", "front", x, y, black)
    s.px("head", "front", 2, 3, hexc("5a6a5a"))
    s.px("head", "front", 5, 3, hexc("5a6a5a"))


def healer_look(s, n):
    """Healer: a big bald man in a long grey robe with a glowing hand-print on the chest."""
    glow = hexc("9ef0c0")
    for (x, y) in ((3, 3), (4, 3), (2, 4), (3, 4), (4, 4), (5, 4), (3, 5), (4, 5)):
        s.px("body", "front", x, y, glow)


def caliban_look(s, n):
    """Caliban: chalk-white skin, no hair, dark round goggles."""
    dark = hexc("141416")
    for x in (1, 2, 5, 6):
        s.px("head", "front", x, 4, dark)
    s.px("head", "front", 3, 4, hexc("3a3a3a"))
    s.px("head", "front", 4, 4, hexc("3a3a3a"))
    for x in (0, 7):
        s.px("head", "front", x, 4, hexc("3a3a3a"))


def sabretooth_look(s, n):
    """Sabretooth: wild blond mane, fur collar and claws."""
    fur_c = hexc("6b4a2a")
    for x in range(8):
        for y in (0, 1):
            s.px("body", "front", x, y, n(fur_c, 0.12))
            s.px("body", "back", x, y, n(fur_c, 0.12))
    for box in ("rarm", "larm"):
        for y in (0, 1):
            for face in ("front", "back", "left", "right", "top"):
                for x in range(4):
                    s.px(box, face, x, y, n(fur_c, 0.12))
        for x in range(4):
            s.px(box, "bottom", x, 0, hexc("e8e0c8"))
    s.px("head", "front", 2, 6, hexc("f2f2e0"))
    s.px("head", "front", 5, 6, hexc("f2f2e0"))


def juggernaut_look(s, n):
    """Juggernaut: his round rust-red helmet that covers everything but the face."""
    helm = hexc("8a3a24")
    dark = hexc("5a2416")

    def hat(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "front":
            return n(helm) if y <= 2 or x in (0, 7) or y == 7 else None
        if face == "top":
            return n(dark) if x in (3, 4) else n(helm)
        return n(helm)
    s.paint("hat", hat)


def jumpsuit_number(s, n):
    """Multiple Man in his orange prison jumpsuit with a number on the chest."""
    for (x, y) in ((1, 2), (2, 2), (5, 2), (6, 2)):
        s.px("body", "front", x, y, hexc("1a1a1a"))


def azazel_look(s, n):
    """Azazel: red skin, black hair and beard, yellow eyes."""
    s.px("head", "front", 2, 4, hexc("f5d742"))
    s.px("head", "front", 5, 4, hexc("f5d742"))


def tuxedo(s, n):
    """A Hellfire Club tuxedo: white shirt, black bow tie."""
    white = hexc("f2f2f2")
    for y in range(0, 8):
        for x in (3, 4):
            s.px("body", "front", x, y, white)
    s.px("body", "front", 3, 0, hexc("101010"))
    s.px("body", "front", 4, 0, hexc("101010"))
    s.px("body", "front", 2, 0, hexc("101010"))
    s.px("body", "front", 5, 0, hexc("101010"))


def soldier_helmet(s, n):
    """Anti-mutant soldier: olive helmet and a dark visor."""
    olive = hexc("4a5a34")

    def hat(face, x, y, w, h):
        if face == "bottom":
            return None
        if face == "front":
            return n(olive) if y <= 2 or x in (0, 7) else (hexc("1a1a1e") if y == 4 else None)
        return n(olive) if y < 5 or face == "top" else None
    s.paint("hat", hat)


def selene_look(s, n):
    """Selene: black hair, pale skin, a black gown and red lips."""
    s.px("head", "front", 2, 4, hexc("8a1a1a"))
    s.px("head", "front", 5, 4, hexc("8a1a1a"))


CHARACTERS.update({
    # ---- the Morlocks (0.6.2)
    "callisto": dict(skin="e8c3a2", hair="121214", style="long", eyes="3b2a1e", suit="1a1a1c", panel="2a2622",
                     sleeves="1a1a1c", gloves="1a1a1c", legs="1f1c1a", boots="0e0e10", belt="3a2a1a",
                     buckle="8a8a8a", emblem="none", lips="7a3a3a", coat="2a2420", extra=eyepatch),
    "caliban": dict(skin="ece9e4", style="bald", eyes="141416", suit="161618", panel="161618", sleeves="161618",
                    gloves="ece9e4", legs="161618", boots="0e0e10", belt="161618", emblem="none", panels=False,
                    coat="1c1c20", long_coat=True, lips="b0a8a0", extra=caliban_look),
    "healer": dict(skin="c99a74", style="bald", eyes="3b2a1e", suit="6a6a6e", panel="5a5a5e", sleeves="6a6a6e",
                   gloves="c99a74", legs="5a5a5e", boots="3a2a1a", belt="4a3a2a", emblem="none", panels=False,
                   coat="7a7a7e", long_coat=True, extra=healer_look),
    "masque": dict(skin="c8a080", hair="2a1e1a", eyes="7a2a2a", suit="3a2f2a", panel="2a221e", sleeves="3a2f2a",
                   gloves="2a221e", legs="2a221e", boots="1a1410", belt="1a1410", emblem="none",
                   extra=masque_face),
    "leech": dict(skin="8fbf7a", style="bald", eyes="08080a", suit="6b5a3a", panel="5a4a2e", sleeves="6b5a3a",
                  gloves="8fbf7a", legs="4a3e2a", boots="8fbf7a", belt="4a3e2a", emblem="none", panels=False,
                  lips="5a7a4a", extra=leech_look),
    "marrow": dict(skin="e2b08e", hair="e04a8a", style="spiky", eyes="3f8a4a", suit="3a2a2a", panel="5a3a3a",
                   sleeves="3a2a2a", gloves="e2b08e", legs="2a2222", boots="1a1414", belt="1a1414", emblem="none",
                   bare_arms=True, tank_top=True, extra=bone_spikes),
    # ---- the Brotherhood's heavy hitters
    "sabretooth": dict(skin="d9a07a", hair="d9b26b", style="long", eyes="c9a032", suit="3a2a1a", panel="2a1e12",
                       sleeves="3a2a1a", gloves="d9a07a", legs="2a2218", boots="1a120a", belt="1a120a",
                       emblem="none", coat="4a3220", long_coat=True, beard="b8924a", extra=sabretooth_look),
    "juggernaut": dict(skin="e2b08e", eyes="3a5f9c", suit="8a3a24", panel="5a2416", sleeves="5a2416",
                       gloves="3a3a3a", legs="3a2a24", boots="2a1a14", belt="3a3a3a", buckle="8a8a8a",
                       emblem="none", chest_v="5a2416", extra=juggernaut_look),
    "multiple_man": dict(skin="e8c3a2", hair="4a3020", eyes="3a5f9c", suit="e0601a", panel="c8501a",
                         sleeves="e0601a", gloves="e8c3a2", legs="e0601a", boots="2a2a2a", belt="e0601a",
                         emblem="none", panels=False, extra=jumpsuit_number),
    # ---- the Hellfire Club (X-Men: First Class)
    "sebastian_shaw": dict(skin="e8c3a2", hair="8a8a8a", style="short_receding", eyes="3a5f9c", suit="1a1a1e",
                           panel="1a1a1e", sleeves="1a1a1e", gloves="e8c3a2", legs="1a1a1e", boots="0e0e10",
                           belt="1a1a1e", emblem="none", panels=False, coat="20202a", extra=tuxedo),
    "azazel": dict(skin="b8241a", hair="121212", style="short", eyes="f5d742", suit="141416", panel="141416",
                   sleeves="141416", gloves="b8241a", legs="141416", boots="0e0e10", belt="141416",
                   emblem="none", beard="121212", lips="7a1410", coat="1a1a1e", long_coat=True, extra=azazel_look),
    "riptide": dict(skin="d9a07a", hair="2a1a12", style="long", eyes="3b2a1e", suit="2a2a30", panel="2a2a30",
                    sleeves="2a2a30", gloves="d9a07a", legs="2a2a30", boots="141416", belt="2a2a30",
                    emblem="none", beard="2a1a12", panels=False, extra=tuxedo),
    "hellfire_guard": dict(skin="e2b08e", hair="2a1e14", eyes="3b2a1e", suit="121214", panel="121214",
                           sleeves="121214", gloves="f2f2f2", legs="121214", boots="0a0a0a", belt="121214",
                           emblem="none", panels=False, extra=tuxedo),
    # ---- Genosha (X-Men: Dark Phoenix)
    "selene": dict(skin="f0d4c0", hair="0e0e10", style="long", eyes="8a1a1a", suit="141016", panel="221a26",
                   sleeves="141016", gloves="f0d4c0", legs="141016", boots="0e0e10", belt="141016",
                   emblem="none", lips="a01a2a", panels=False, coat="1a141e", long_coat=True, extra=selene_look),
    "ariki": dict(skin="9a6a48", hair="141414", style="long", eyes="3b2a1e", suit="3a3428", panel="2a261e",
                  sleeves="3a3428", gloves="9a6a48", legs="2a261e", boots="1a1610", belt="1a1610", emblem="none",
                  bare_arms=True),
    "red_lotus": dict(skin="e8c3a2", hair="b8241a", style="spiky", eyes="3b2a1e", suit="8a1a1a", panel="5a1212",
                      sleeves="8a1a1a", gloves="1a1a1a", legs="1a1a1a", boots="0e0e10", belt="1a1a1a",
                      emblem="none"),
    "genoshan": dict(skin="c99a74", hair="3b2416", style="short", eyes="3b2a1e", suit="8a7a5a", panel="7a6a4a",
                     sleeves="8a7a5a", gloves="c99a74", legs="4a4a5a", boots="3a2a1a", belt="3a2a1a",
                     emblem="none", panels=False),
    # ---- anti-mutant soldiers at the Detection Tower
    "anti_mutant_soldier": dict(skin="e2b08e", hair="2a1e14", eyes="3b2a1e", suit="4a5a34", panel="3a4a28",
                                sleeves="4a5a34", gloves="1a1a1a", legs="3a4a28", boots="1a1a12", belt="2a2a1a",
                                buckle="6a6a5a", emblem="none", knee="2f3a20", extra=soldier_helmet),
})

# Characters with slim (Alex-style) arms
SLIM = {"storm", "jean_grey", "rogue", "kitty_pryde", "jubilee", "moira", "mystique", "white_queen",
        "krakoa_mutant", "callisto", "marrow", "selene"}


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, (name, spec) in enumerate(CHARACTERS.items()):
        img = make(spec, i + 7)
        if spec.get("glasses"):
            img.putpixel((8 + 1, 8 + 4), hexc("dddddd"))
        img.save(os.path.join(OUT, name + ".png"))
    print(f"{len(CHARACTERS)} skins written to {OUT}")


if __name__ == "__main__":
    main()
