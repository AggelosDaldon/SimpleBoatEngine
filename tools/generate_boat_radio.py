#!/usr/bin/env python3
"""Import the user's 12-element warm boat radio and the final 32px inventory icon.
The source was authored with 17.5 degree rotation; Minecraft block-model JSON
permits 22.5 degree steps, so use the nearest supported angle for the static model.
"""
import base64,json,zlib
from pathlib import Path
from PIL import Image
from io import BytesIO
ROOT=Path("src/main/resources/assets/simpleboatengine")
G="eNrNlG9vgjAQh78Lr0sD5c+YX8UsDUrVRUMJlGzL4nfftaX0Dn0/E028x4Y89+N6+9+kT3bJQXc/8tAer3IaVHtV4ySN+jbzqBKWnJLdPq1ZxlLBiw+WGKhr9sZ8NSc7eIYezQVwBqcKwfIa/lDtZE9CmbGy8WzSsztH4Jd6PDgPQFxhn5cB6fRXT9n9zrx9145XafQgj7o3o77JyYyfQzR/25jzp+7O2woUgtgjiPQRjf4IhgaAhkBiBytcWxg/zxcjT1qbRbvgDUszXjH4LuYVtwnlvNma+0DgUzbRnMJgTuliTqEzd4VPOppjuJrf1ImIp1ZzY57abl5P/TAbo3uZL+Y5L9xspDnPgrngJWt4XrkRJOal7aguyawTFrwJXLQJc9ausI5o1hHbOouQdsbrB2kb/nNpeFxIjMSNIIob0Rg3giFuP/l4xjHcqhdBXYDmVh1+v1jgl7bvbkr6CzroKcx5yYXXz3kV7Ev+zvIMxkhw8bgaw4KI+oQFfQIXfcKc/uJJhhyxrb67pcg+taob+9S19Jr+druf0VJfk35M/716pv+vi6btjer7Vk76eFVoUTZhgor1HVR29hu4BMJfild4B0F/1F10r70m5F9H9wLmx15r2PT1f8t//AEJIUpS"
ATLAS="iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAACfklEQVR4nO2awWsTQRTGv3TG/6AQKLZ4qE2EQQg0AVELQZBeRAUPHiyU0qMHDwUPXj168CB4K4HivaJoEUQJgqiFXgaJ0kNp04VisddACfWQnSGT3TTdzZq3sO8HgczOfMl7b9+bvB2SUzMXT0DI9Ut5ZyylPHXcarWc8dXLk0PpJQC8eHTjrPYmysPnH0m+txsbnlfvfwIAhBDOgna77YyTmr9/sxDH3sSRgxbUtw/t+7np8TN/8KdfB7F0o8YGwNwZc8eM40oV7eK6bgBwHeq948bxMF21kA9kROSaHbA+sh4hLN6Zxd+NHQDA9x9b9nqlXOrMz19AbX0Tt69N2bm3X/excKuEP+f66xZ8XZoYC7u49GwDQMeJifMT9mWcMvMAoNRMLF1aCASgvn0IpYrWiW6MM0oVnb0B6KR+HB01tgR6a/N/0r1vjLzme8ahJZAlAgGoFvLQuoFKuQSv6TlzXtNDpVyC1g1UC24HNzc9HktHTWgGrK7MA4B1xrzMbm7mAUDr37F0aSG0D6itb0L7m5UxHgC0/3tea3XmXn/ZtTohBNbebEH7fUCYbu34KLDXRK3Z3vXD6vt2gqbZEcdH9lpYI9NPBz9IQojUpX03NgBp6c1HjQTS8VRGhWyPuftg1BpafXJ3KP3Tlx+G0j++MuWMo+q5D6A2gJrMB0CSP48T6zOfARwAagOoCewBaa9ZPg9IGA4AtQHUSOrncWp95jOAA0BtADW55Qf3nP8HRK2hd5+/nbp+kN5r7uUi2Js4mc8ADgC1AdSQnwdQk/kM4ABQG0AN+XkANZnPAA4AtQHUkJ8HUJP5DOAAUBvAMAzDMAzDMAzDMAwzav4BN9hLNAPlQigAAAAASUVORK5CYII="
ICON="iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAC90lEQVR42u2Wr3bjSgzGf+1ZICg4cKDhQMM8QmFhYeE+QuHCC+8jXLiwMNDQcOBAwYFiXmDHThznT7v3dMnqHCc+lpyRPn36FPhrX2AppSGlNGz5Hr8oAVJKm75vX5FArfWi7/FPt+ciAjHG4f84oJTy8OEEQgiDiKCqv3W4A8F9+BQCh965H/onq5+Wo282fKCqZ967EhAgxUB1532fHz7F/KYZUox0Vwh4lYQhBMJvtEAFYgw34/74FDzeItHnTSYklagyXhuT9e3W8U0MAxeotH4q0wP3iaLuyExmHx0rOyFYcxg/ARU5Q6CJyutzwh1+/Nvx/BRpm0jXGz+7zNtLCyK8/bOn+vmkFBsTyWYPmwiIQFRBVc6xcKjV6bqCyBjbdYaZU6vjFX6+5xkW4VDA8lsxCNn8MgJtE4eoOmW/boajyFLUBZ4cYpkQP/NfRWCCOZdKNiMGRUTGGkTmexGZ+y0yVnvI7dR3iB/vAV7e3u+RYgEJfH9OmDngM3/86MOXYqeKnYpjdfEHBJ/ef3lqriuhTxAdzKzy3hVwMCDXI6R0FJsDx2RivznYUd+VShQuDdI5Au4rfXcoDhUhhkUZc61Ed1JUmqh0vZGrw0r/HSHXShM+tIx8ZoU5VBRRIZcyIxCbBrNxIlIT+K8roIoD1Rb916CIKqXWRSg+IsXFx61mpaCq44ZTJfc9ITRj1YBVR0PAzBCV+TIzQghY9dtSLIwCtKl6q+ciuqmMgmy8dySR9yBwFien5Jy5IlvvruKO17HcQMA3uBAFqhmxaahm41UrqU2UnGmmld0Gpc+Z1KYlzox2t6PvO9og95FwrYJhqt5KIbXtDFHue1IQ3J0+2/gnRKHve9rdbq6o2+/ZRb1vDKs71SeyOvOAR4HgYCXP6vfU6MyLUiohCBGhjVBqmQ98e2qOsJfrCeRiD8CQogJCjMprSJNATdrmR/czX3zRfYego9eB3uqslvtcrq/jZe2GYb1w5EwhTjlzAppzsnD+2jX7BYtcfmPhC4fgAAAAAElFTkSuQmCC"
elements=json.loads(zlib.decompress(base64.b64decode(G)))
out=[]
for e in elements:
    faces={side:{"uv":[round(v/4,3) for v in uv],"texture":"#0"}
           for side,uv in e["u"].items()}
    item={"name":e["n"],"from":[e["f"][0]+8,e["f"][1]+1,e["f"][2]+8],
          "to":[e["t"][0]+8,e["t"][1]+1,e["t"][2]+8],"faces":faces,
          "rotation":{"origin":[8,1,8],"axis":"y","angle":22.5}}
    out.append(item)
block={"textures":{"0":"simpleboatengine:block/boat_radio","particle":"simpleboatengine:block/boat_radio"},
       "elements":out}
blockpath=ROOT/"models/block/boat_radio_decoration.json"
blockpath.parent.mkdir(parents=True,exist_ok=True)
blockpath.write_text(json.dumps(block,indent=2)+"\n")
atlaspath=ROOT/"textures/block/boat_radio.png"
atlaspath.parent.mkdir(parents=True,exist_ok=True)
atlaspath.write_bytes(base64.b64decode(ATLAS))
icon=Image.open(BytesIO(base64.b64decode(ICON))).convert("RGBA")
assert icon.size==(32,32)
icon.resize((256,256),Image.Resampling.NEAREST).save(ROOT/"textures/item/boat_radio_icon.png")
(ROOT/"models/item/boat_radio.json").write_text(
    json.dumps({"parent":"minecraft:item/generated","textures":{"layer0":"simpleboatengine:item/boat_radio_icon"}},indent=2)+"\n")
(ROOT/"blockstates/boat_radio_decoration.json").write_text(
    json.dumps({"variants":{"":{"model":"simpleboatengine:block/boat_radio_decoration"}}},indent=2)+"\n")
print("Generated 12-element boat radio and exact user supplied radio inventory icon.")
