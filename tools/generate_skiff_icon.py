"""Create a small Minecraft-style top-view icon for the skiff item registry.

The in-world skiff continues to use the full Blockbench 3D mesh and texture.
"""

from pathlib import Path
from PIL import Image, ImageDraw


DEST = (Path(__file__).resolve().parents[1] /
        "src/main/resources/assets/simpleboatengine/textures/item/large_fishing_skiff.png")
BLUE_DEST = DEST.with_name("aggelos_blue_fishing_boat.png")


def main():
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.polygon([(12, 1), (19, 1), (24, 7), (27, 22), (25, 28),
                  (21, 30), (10, 30), (6, 27), (4, 22), (7, 7)],
                 fill=(36, 66, 85, 255))
    draw.polygon([(12, 2), (19, 2), (23, 8), (25, 22), (23, 27),
                  (20, 28), (11, 28), (8, 26), (6, 22), (9, 8)],
                 fill=(103, 120, 123, 255))
    draw.polygon([(12, 5), (19, 5), (21, 9), (23, 22), (21, 25),
                  (10, 25), (8, 22), (11, 9)],
                 fill=(44, 66, 72, 255))
    draw.polygon([(12, 3), (19, 3), (21, 6), (11, 6)],
                 fill=(158, 181, 185, 255))
    draw.rectangle([11, 11, 20, 13], fill=(152, 165, 165, 255))
    draw.rectangle([10, 22, 21, 24], fill=(177, 188, 187, 255))
    draw.rectangle([11, 28, 20, 30], fill=(27, 39, 46, 255))
    draw.rectangle([13, 27, 18, 29], fill=(44, 52, 60, 255))
    for y in (15, 18, 21):
        draw.line([(12, y), (19, y)], fill=(68, 87, 91, 255), width=1)
    draw.line([(7, 11), (5, 22)], fill=(73, 127, 155, 255), width=1)
    draw.line([(24, 11), (26, 22)], fill=(72, 123, 151, 255), width=1)
    DEST.parent.mkdir(parents=True, exist_ok=True)
    image.save(DEST, optimize=True)
    print(f"Generated {DEST}")

    # Distinct bright-blue / white icon for the second real-world fishing boat.
    blue = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    ink = ImageDraw.Draw(blue)
    ink.polygon([(12, 1), (19, 1), (24, 7), (27, 22), (25, 28),
                 (21, 30), (10, 30), (6, 27), (4, 22), (7, 7)],
                fill=(18, 47, 113, 255))
    ink.polygon([(12, 2), (19, 2), (23, 8), (25, 22), (23, 27),
                 (20, 28), (11, 28), (8, 26), (6, 22), (9, 8)],
                fill=(57, 101, 191, 255))
    ink.polygon([(12, 5), (19, 5), (21, 9), (23, 22), (21, 25),
                 (10, 25), (8, 22), (11, 9)],
                fill=(219, 227, 237, 255))
    ink.polygon([(12, 3), (19, 3), (21, 6), (11, 6)],
                fill=(30, 54, 107, 255))
    ink.polygon([(10, 6), (21, 6), (24, 12), (7, 12)],
                fill=(25, 49, 106, 255))
    ink.rectangle([9, 23, 22, 25], fill=(231, 236, 243, 255))
    ink.rectangle([11, 28, 20, 30], fill=(21, 36, 86, 255))
    ink.rectangle([12, 27, 19, 29], fill=(225, 231, 239, 255))
    for row in (16, 19, 22):
        ink.line([(11, row), (20, row)], fill=(177, 193, 218, 255), width=1)
    BLUE_DEST.parent.mkdir(parents=True, exist_ok=True)
    blue.save(BLUE_DEST, optimize=True)
    print(f"Generated {BLUE_DEST}")


if __name__ == "__main__":
    main()
