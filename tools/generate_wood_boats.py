"""Produce nine wood palettes from Aggelos' exact 128x128 Blockbench UV layout.

Only blue/teal hull pixels change: white hatch, cream inner floor and detailing
are kept untouched. Generated assets are committed alongside the source script.
"""

from pathlib import Path
import json
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/simpleboatengine"
WOOD = {
    "oak": (159, 119, 67),
    "spruce": (107, 78, 53),
    "birch": (211, 195, 140),
    "jungle": (156, 107, 84),
    "acacia": (174, 92, 52),
    "dark_oak": (82, 59, 47),
    "mangrove": (137, 57, 62),
    "cherry": (217, 155, 161),
    "bamboo": (187, 172, 77),
}


def replace(pixel, base):
    r, g, b, alpha = pixel
    if not alpha:
        return pixel
    is_blue = b > r * 1.35 and b > g * 1.22
    is_teal = g > r * 1.38 and b > r * 1.33 and g < 155
    if not (is_blue or is_teal):
        return pixel
    # Preserve the original pixel-by-pixel shading and weathering.
    brightness = (0.31 * r + 0.53 * g + 0.16 * b)
    shade = min(1.36, max(0.43, 0.62 + brightness / 138.0))
    if is_teal:
        shade *= 0.66
    return tuple(min(255, round(component * shade)) for component in base) + (alpha,)


def generate():
    source_entity = Image.open(ASSETS / "textures/entity/aggelos_blue_fishing_boat.png").convert("RGBA")
    source_item = Image.open(ASSETS / "textures/item/aggelos_blue_fishing_boat.png").convert("RGBA")
    # White/neutral diffuse UV atlas gives accurate vanilla dye colors while
    # keeping each Blockbench pixel's shadowing. Accents render unpainted.
    neutral = Image.new("RGBA", source_entity.size)
    shades = []
    for red, green, blue, alpha in source_entity.getdata():
        luminance = red * 0.30 + green * 0.56 + blue * 0.14
        shade = min(255, round(95 + luminance * 1.1)) if luminance < 145 else min(255, round(25 + luminance * 0.88))
        shades.append((shade, shade, shade, alpha))
    neutral.putdata(shades)
    neutral.save(ASSETS / "textures/entity/paintable_fishing_boat.png", optimize=True)
    for wood, rgb in WOOD.items():
        for image, folder in ((source_entity, "entity"), (source_item, "item")):
            out = Image.new("RGBA", image.size)
            out.putdata([replace(pixel, rgb) for pixel in image.getdata()])
            out.save(ASSETS / f"textures/{folder}/{wood}_fishing_boat.png", optimize=True)
        # All boat item models must select the BEWLR 3D renderer, otherwise
        # their painted NBT colors never appear in inventory or the hotbar.
        model = {
            "parent": "minecraft:builtin/entity",
            "textures": {"particle": f"simpleboatengine:item/{wood}_fishing_boat"},
            "display": {
                "gui": {"rotation": [24, 145, 0], "translation": [0, 0, 0],
                        "scale": [1.35, 1.35, 1.35]},
                "ground": {"rotation": [0, 90, 0], "translation": [0, 1, 0],
                           "scale": [0.7, 0.7, 0.7]},
                "fixed": {"rotation": [0, 180, 0], "scale": [1.1, 1.1, 1.1]},
                "firstperson_righthand": {"rotation": [0, -45, 0],
                                          "scale": [0.65, 0.65, 0.65]},
                "firstperson_lefthand": {"rotation": [0, 45, 0],
                                         "scale": [0.65, 0.65, 0.65]},
            },
        }
        (ASSETS / f"models/item/{wood}_fishing_boat.json").write_text(
            json.dumps(model, indent=2) + "\n", encoding="utf-8")
        print(f"Generated {wood} fishing boat (3 assets)")


if __name__ == "__main__":
    generate()
