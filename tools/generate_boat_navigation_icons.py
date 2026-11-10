"""Generate original vanilla-scale pixel art and the tracker compass frames."""
from math import cos, sin, tau
from pathlib import Path
import json
from PIL import Image, ImageDraw

root = Path("src/main/resources/assets/simpleboatengine")
textures = root / "textures/item"
models = root / "models/item"
namespace = "simpleboatengine"


def model(texture):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"{namespace}:item/{texture}"}}


def write_model(name, content):
    (models / f"{name}.json").write_text(json.dumps(content, indent=2) + "\n")


body = Image.new("RGBA", (16, 16))
draw = ImageDraw.Draw(body)
draw.polygon([(5, 2), (10, 2), (14, 5), (15, 7), (15, 10), (12, 13),
              (10, 14), (5, 14), (2, 12), (0, 10), (0, 7), (2, 5)], fill="#152634")
draw.ellipse((1, 3, 14, 12), fill="#487887")
draw.ellipse((2, 3, 13, 11), fill="#95b9a9")
draw.ellipse((3, 4, 12, 10), fill="#285058")
draw.line((5, 3, 10, 3), fill="#c1cf9e")
draw.line((4, 12, 10, 12), fill="#2c5664")
for point in [(7, 4), (11, 7), (7, 10), (3, 7)]:
    draw.point(point, fill="#d8e2bd")

overrides = []
for frame in range(32):
    sprite = body.copy()
    draw = ImageDraw.Draw(sprite)
    angle = frame * tau / 32
    x, y = sin(angle), -cos(angle)
    tip = (round(7.5 + 4.4 * x), round(7 + 3.7 * y))
    tail = (round(7.5 - 3.8 * x), round(7 - 3 * y))
    draw.line((tail, (8, 7)), fill="#70c9ce", width=1)
    draw.line(((8, 7), tip), fill="#ffd36a", width=1)
    draw.point(tip, fill="#fff1a0")
    draw.point((8, 7), fill="#e2f2dc")
    key = f"boat_tracker_{frame:02}"
    sprite.save(textures / f"{key}.png")
    if frame:
        write_model(key, model(key))
        overrides.append({"predicate": {"angle": (frame - .5) / 32}, "model": f"{namespace}:item/{key}"})
write_model("boat_tracker", {**model("boat_tracker_00"), "overrides": [
    *overrides, {"predicate": {"angle": 31.5 / 32}, "model": f"{namespace}:item/boat_tracker"}
]})

book = Image.new("RGBA", (16, 16))
draw = ImageDraw.Draw(book)
draw.polygon([(1, 5), (9, 1), (14, 5), (15, 10), (6, 15), (1, 10)], fill="#172536")
draw.polygon([(2, 7), (9, 4), (14, 7), (14, 10), (6, 14), (2, 10)], fill="#ddc99b")
draw.line([(3, 10), (6, 13), (13, 10)], fill="#ae956e")
draw.polygon([(1, 5), (9, 1), (14, 5), (14, 7), (6, 12), (1, 8)], fill="#2e536c")
draw.polygon([(3, 5), (9, 2), (13, 5), (12, 7), (6, 10), (3, 8)], fill="#426a83")
draw.line([(2, 6), (2, 8), (6, 11)], fill="#1b3b53")
draw.line([(8, 4), (8, 7)], fill="#d9b96d")
draw.point((9, 5), fill="#f1d397")
draw.line([(6, 7), (10, 7)], fill="#e9cb83")
draw.line([(6, 7), (7, 8), (9, 8), (10, 7)], fill="#c49b51")
draw.point((4, 5), fill="#bc9959")
draw.point((11, 4), fill="#bc9959")
book.save(textures / "boat_logbook.png")
write_model("boat_logbook", model("boat_logbook"))
