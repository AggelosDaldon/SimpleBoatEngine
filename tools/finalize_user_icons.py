#!/usr/bin/env python3
"""Use the six exact 32px item sprites supplied by the user.
No interpolation or alpha fringes; upscale to 256px using nearest neighbor.
These are the final inventory textures and are not redrawn by other generators.
"""
from pathlib import Path
from PIL import Image
import base64,zlib
# Exact 32x32 RGBA pixels from the two source PNGs whose binary Git blobs
# were damaged in transport. Keep an independent zlib-checked recovery copy.
PIXEL_BACKUP={
    "fuel_bucket":"eNrtli2sq0AQhVcikUgkciVyJRJZiaysrKysrKxEIpHIlZWVlZVIJG7eHHb5vXBfAtw0ubkkJ0BL9ps5M/sjxN+1dLmuS77v06f4MpAUxwfyvc/E4HH+QRBQpOKP8T3PI6UighedL75Dh9BKCZYzkaBICtorBiklx6DIhx+eQ8VF0Osu6HGbV5UL0ldBKnB2iQE9iD6QgUv52bAvB4xvGGMJuiUmBsSCeDexufd8rkEYhk0vxKFP+WnZX/CO7P+T2WUmKFHr+fBehYqiKO6EOsShy7Xn3KXDcfRS0vjyZH/KlD3ITB0Sta4XPNdrfI+ZG8qw4TfvzH/z+HVuWF/EuaNH8FwXgo7ROg+Qk8T8Yy56MOJ5EPgB3U6Oqe/VeDwn/A/2Wj78xfh3ZiXKNYrc5v2VmvzqAQOiwXNt+w8erOGjb+Dj+27U5JTZMW+GRQPuUPitZa/Pn+f5WXRzHeO0eSOWJS70tOtArbfxMdcRg75YHo9Xsq/ZuY9hKrC1ZbdxbeHnA34bwzsX3ToETxrdjfQg7239N+aP6s3jvzj//DLg29pUxdee3DV/ey8L01+YZ1XWr7fV5Lu9+a3KvOc3a90Mf4/6F0v5/4dfFz/Tf/XAf6ztjf9W0/rTjv034mvDnqq4jvv/R/LXxudujuvxHXE8JjHsxQfjmRpGu75Me63S/d5Het/5/xqw62J576GH+Q7fr+Vj702PPf9t2ZWesIr5dbhdF1ALjLVm/8e5JT1Z/22P0zf7Tj3ZfxEvfNxy/sM5Lzst7zdzgld7nD1H530+D6COCaS+F7h7sX/79Q8pUSls",
    "fuel_canister":"eNrtVi10wjAQroyMrERWRlZWTiKRyElkZGRl5GQlsnISiUQiKyeRuOy+9LIF1nbpALH3yHvfI23v7rvfkCx7rudKXyrLXCruzW2kdMd8MYku2q+FuJsPGwHu3O3Jhykcov1cH6QUTpJeAN4VlEfEfUJM9PtGz3UCINfJ3OtpIZN8yCm+slSuqiqnlPI+gP+V0LLN5Uiti6v9itCw3jqxF4S4jF/Sc8y/mdFXkLMRf4qeJJkYmnywBMN24EdJeEmA4jpAD/rejpyug6V6fVC9ThH2vubC27KUjx3JdPkwjhEa+E56yMGW9t1i4c6LwtUjPizRY2XlPqju1+gIR/49qXIY5c93QQ/fzrBdKNdSL1YDtcC7N4qvIWxJBsDzrWhgi2KDXUNYUU7UCL/1PS4o55RDAny9niubgCALfdg5EGBX+7mY5rfcL/AXuvDFcv3r5NkXfR9I5LHnhd1Ufs3AfscxwE4KN+qGXnxn+djWXH7NuQA3ztV3QivlRZ8HbP1s9OdvwzHrO/DH+nvmN+xXjB2f+VP6t/Brrqvv4wH+VgjfJzqBv3wA/zaJX/w5fswx5qgZ6cPW90juZ/43/jnx19l3X4Wz9ED99oUw36I/a0OfmjvU33JMmEHD/396AhvOBeThm41yMYcfXIgBc99wTTcJ/Bf1yvrzK+QilR/1PfA9q57JqX/Urp9L2AtxLEf4Fd9ZDOuZgb25el9PfB+SCfeHuffrIuHO9RuKB93Jn+t/r092TqYt"
}
ROOT=Path(__file__).resolve().parents[1]
source=ROOT/"tools/user_icons"
dest=ROOT/"src/main/resources/assets/simpleboatengine/textures/item"
dest.mkdir(parents=True,exist_ok=True)
names=("boat_recovery_kit","fuel_bucket","ice_breaker","boat_stand",
       "pole_light","fuel_canister","engine")
for name in names:
    try:
        icon=Image.open(source/(name+".png")).convert("RGBA")
        icon.load()
    except (OSError, ValueError):
        if name not in PIXEL_BACKUP:raise
        pixels=zlib.decompress(base64.b64decode(PIXEL_BACKUP[name]))
        if len(pixels)!=32*32*4:raise ValueError("Recovery pixels invalid")
        icon=Image.frombytes("RGBA",(32,32),pixels)
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
