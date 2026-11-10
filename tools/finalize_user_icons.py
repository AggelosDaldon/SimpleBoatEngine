#!/usr/bin/env python3
"""Use the six exact 32px item sprites supplied by the user.
No interpolation or alpha fringes; upscale to 256px using nearest neighbor.
These are the final inventory textures and are not redrawn by other generators.
"""
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
source=ROOT/"tools/user_icons"
dest=ROOT/"src/main/resources/assets/simpleboatengine/textures/item"
dest.mkdir(parents=True,exist_ok=True)
names=("boat_recovery_kit","fuel_bucket","ice_breaker","boat_stand",
       "pole_light","fuel_canister","engine")
for name in names:
    icon=Image.open(source/(name+".png")).convert("RGBA")
    size=64 if name=="engine" else 32
    if icon.size!=(size,size):raise ValueError(f"{name}: unexpected sprite dimensions")
    # Opaque colored pixels, fully transparent background. This avoids MC
    # texture bleeding between semitransparent edge pixels.
    pix=icon.load()
    for y in range(size):
        for x in range(size):
            r,g,b,a=pix[x,y]
            pix[x,y]=(r,g,b,255 if a>=128 else 0)
    icon.resize((256,256),Image.Resampling.NEAREST).save(dest/(name+"_icon.png"),optimize=True)
print("Installed seven user-supplied 256x256 pixel icons with clean alpha.")
