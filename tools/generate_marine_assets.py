#!/usr/bin/env python3
"""Deterministic pixel-art textures for maritime utilities. Executed by GitHub Actions."""
from PIL import Image, ImageDraw
from pathlib import Path
from random import Random

root = Path("src/main/resources/assets/simpleboatengine/textures")
(root / "block").mkdir(parents=True,exist_ok=True)
(root / "item").mkdir(parents=True,exist_ok=True)

colors = {
    "buoy_red": (218,60,42),
    "buoy_yellow": (233,181,37),
    "buoy_seam": (102,45,35),
    "buoy_metal": (83,79,73),
    "marine_fuel": (72,102,117)
}
for key, base in colors.items():
    rng = Random(key)
    im = Image.new("RGBA",(16,16))
    pix = im.load()
    for y in range(16):
        for x in range(16):
            d = rng.randint(-7,7)+int(4*(7.5-y)/8)
            if x in (0,15) or y in (0,15): d-=11
            elif x in (3,4) and y in range(3,13): d+=8
            pix[x,y]=tuple(max(0,min(255,z+d)) for z in base)+(255,)
    dest = root / ("item" if key == "marine_fuel" else "block") / (key+".png")
    im.save(dest,optimize=True)
