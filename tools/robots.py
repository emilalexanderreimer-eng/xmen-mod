"""Textures and names for the Sentinel and the Danger Room's training robot and laser drone.
The texture offsets match the models in src/main/java/com/emil/xmen/robot/client.

Run:  python3 tools/robots.py   (needs Pillow)
"""
import json
import os
import random

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "xmen")
OUT = os.path.join(ASSETS, "textures", "entity", "robot")
rand = random.Random(7)


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (c[3],)


def jitter(c, amount=0.05):
    return shade(c, 1 + (rand.random() - 0.5) * 2 * amount)


def faces(u, v, w, h, d):
    """(face, x0, y0, width, height) of the six faces of a model box at texture offset (u, v)."""
    return [("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d), ("right", u, v + d, d, h),
            ("front", u + d, v + d, w, h), ("left", u + d + w, v + d, d, h), ("back", u + d + w + d, v + d, w, h)]


def paint(img, box, fn):
    u, v, w, h, d = box
    for face, x0, y0, fw, fh in faces(u, v, w, h, d):
        for y in range(fh):
            for x in range(fw):
                c = fn(face, x, y, fw, fh)
                if c is not None:
                    img.putpixel((x0 + x, y0 + y), c)


def metal(base, line=None, every=6, shine=True):
    """Metal plates with panel lines and a lighter top edge."""
    dark = line or shade(base, 0.7)

    def fn(face, x, y, w, h):
        if face == "top":
            return jitter(shade(base, 1.12), 0.03)
        if face == "bottom":
            return jitter(shade(base, 0.65), 0.03)
        if y % every == every - 1 or (w > 6 and x in (0, w - 1)):
            return dark
        c = shade(base, 1.1) if shine and y == 0 else base
        return jitter(c, 0.04)
    return fn


def solid(c):
    return lambda face, x, y, w, h: jitter(c, 0.03)


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name + ".png"))


def front_pixels(box, pixels):
    """Absolute texture positions of (x, y) pixels on a box's front face."""
    u, v, w, h, d = box
    return [(u + d + x, v + d + y) for x, y in pixels]


# ------------------------------------------------------------------ Sentinel (128x128)
PURPLE = hexc("6a2c7e")
MAGENTA = hexc("8e3a99")
SILVER = hexc("a9aeb7")
DARK = hexc("2d2b33")
EYE = hexc("ffb21e")

S = {
    "chest": (0, 0, 22, 18, 14), "plate": (38, 68, 14, 10, 1), "waist": (40, 32, 12, 8, 8),
    "neck": (68, 68, 6, 3, 6), "head": (72, 0, 12, 12, 13), "face": (0, 82, 8, 6, 1), "crest": (92, 68, 4, 2, 8),
    "shoulder": (0, 32, 10, 7, 10), "upper_arm": (60, 50, 6, 11, 6), "forearm": (80, 32, 7, 10, 7),
    "hand": (84, 50, 8, 6, 8), "thigh": (0, 50, 7, 11, 7), "shin": (28, 50, 8, 10, 8), "foot": (0, 68, 9, 3, 10),
}
SENTINEL_EYES = [(1, 2), (2, 2), (1, 3), (2, 3), (5, 2), (6, 2), (5, 3), (6, 3)]


def sentinel():
    img = Image.new("RGBA", (128, 128))
    paint(img, S["chest"], metal(PURPLE, every=6))
    paint(img, S["plate"], metal(MAGENTA, every=5))
    paint(img, S["waist"], metal(DARK, every=3))
    paint(img, S["neck"], metal(DARK, every=2))
    paint(img, S["head"], metal(PURPLE, every=13))
    paint(img, S["crest"], metal(SILVER, every=9))
    paint(img, S["shoulder"], metal(MAGENTA, every=7))
    paint(img, S["upper_arm"], metal(SILVER, every=3))
    paint(img, S["forearm"], metal(PURPLE, every=5))
    paint(img, S["hand"], metal(SILVER, every=3))
    paint(img, S["thigh"], metal(SILVER, every=3))
    paint(img, S["shin"], metal(PURPLE, every=5))
    paint(img, S["foot"], metal(DARK, every=4))

    def face(f, x, y, w, h):
        if f == "front" and (x, y) in SENTINEL_EYES:
            return EYE
        if f == "front" and y == 5 and 2 <= x <= 5:
            return DARK                               # the mouth slit
        return jitter(SILVER, 0.03)
    paint(img, S["face"], face)
    # a silver X-shaped vent on the chest plate and a dark belly vent on the chest
    u, v, w, h, d = S["plate"]
    for i in range(8):
        for x0 in (u + d + 3,):
            img.putpixel((x0 + i, v + d + 1 + i), SILVER)
            img.putpixel((x0 + 7 - i, v + d + 1 + i), SILVER)
    glow = Image.new("RGBA", (128, 128))
    for p in front_pixels(S["face"], SENTINEL_EYES):
        glow.putpixel(p, EYE)
    save(img, "sentinel")
    save(glow, "sentinel_glow")


# ------------------------------------------------------------------ training robot (64x64)
CHROME = hexc("b9c0c9")
STEEL = hexc("6f7782")
BLUE = hexc("3fb8ff")

R = {"head": (0, 0, 8, 8, 8), "visor": (32, 0, 7, 2, 1), "antenna": (48, 0, 1, 4, 1), "body": (0, 16, 8, 12, 5),
     "core": (26, 16, 4, 4, 1), "arm": (40, 16, 4, 12, 4), "shoulder": (36, 34, 5, 4, 5), "leg": (0, 34, 4, 12, 4)}


def training_robot():
    img = Image.new("RGBA", (64, 64))
    paint(img, R["head"], metal(CHROME, every=8))
    paint(img, R["visor"], solid(BLUE))
    paint(img, R["antenna"], lambda f, x, y, w, h: hexc("ff4040") if y == 0 else STEEL)
    paint(img, R["body"], metal(CHROME, every=4))
    paint(img, R["core"], solid(BLUE))
    paint(img, R["arm"], metal(STEEL, every=4))
    paint(img, R["shoulder"], metal(CHROME, every=4))
    paint(img, R["leg"], metal(STEEL, every=4))
    glow = Image.new("RGBA", (64, 64))
    for box in (R["visor"], R["core"]):
        u, v, w, h, d = box
        for f, x0, y0, fw, fh in faces(*box):
            if f == "front":
                for y in range(fh):
                    for x in range(fw):
                        glow.putpixel((x0 + x, y0 + y), BLUE)
    save(img, "training_robot")
    save(glow, "training_robot_glow")


# ------------------------------------------------------------------ laser drone (64x64)
GUNMETAL = hexc("3a3f47")
RED = hexc("ff2a2a")

D = {"pod": (0, 0, 8, 6, 8), "eye": (32, 0, 4, 3, 1), "gun": (32, 4, 2, 2, 4), "arm_x": (0, 14, 22, 1, 2),
     "arm_z": (0, 17, 2, 1, 22), "blades": (24, 40, 7, 0, 7), "hub": (48, 0, 1, 1, 1)}


def laser_drone():
    img = Image.new("RGBA", (64, 64))
    paint(img, D["pod"], metal(GUNMETAL, line=hexc("23262b"), every=3))
    paint(img, D["eye"], solid(RED))
    paint(img, D["gun"], solid(hexc("1c1e22")))
    paint(img, D["arm_x"], solid(hexc("8a919b")))
    paint(img, D["arm_z"], solid(hexc("8a919b")))
    paint(img, D["hub"], solid(hexc("8a919b")))

    # blurred spinning blades: a see-through grey disc
    def blades(f, x, y, w, h):
        r = ((x - 3) ** 2 + (y - 3) ** 2) ** 0.5
        return (200, 205, 212, 150) if r <= 3.5 else None
    paint(img, D["blades"], blades)
    glow = Image.new("RGBA", (64, 64))
    for f, x0, y0, fw, fh in faces(*D["eye"]):
        if f == "front":
            for y in range(fh):
                for x in range(fw):
                    glow.putpixel((x0 + x, y0 + y), RED)
    save(img, "laser_drone")
    save(glow, "laser_drone_glow")


NAMES = {
    "entity.xmen.sentinel": ("Sentinel", "Sentinel"),
    "entity.xmen.training_robot": ("Training Robot", "Trainingsroboter"),
    "entity.xmen.laser_drone": ("Laser Drone", "Laserdrohne"),
    "item.xmen.sentinel_spawn_egg": ("Sentinel Spawn Egg", "Sentinel-Spawn-Ei"),
    "item.xmen.training_robot_spawn_egg": ("Training Robot Spawn Egg", "Trainingsroboter-Spawn-Ei"),
    "item.xmen.laser_drone_spawn_egg": ("Laser Drone Spawn Egg", "Laserdrohnen-Spawn-Ei"),
}


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def lang_and_models():
    for index, file in ((0, "en_us.json"), (1, "de_de.json")):
        path = os.path.join(ASSETS, "lang", file)
        with open(path) as f:
            data = json.load(f)
        for key, names in NAMES.items():
            data[key] = names[index]
        write_json(path, data)
    for name in ("sentinel", "training_robot", "laser_drone"):
        write_json(os.path.join(ASSETS, "models", "item", name + "_spawn_egg.json"),
                   {"parent": "minecraft:item/template_spawn_egg"})


if __name__ == "__main__":
    sentinel()
    training_robot()
    laser_drone()
    lang_and_models()
    print("robot assets written")
