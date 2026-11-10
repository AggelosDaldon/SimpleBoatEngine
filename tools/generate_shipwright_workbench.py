#!/usr/bin/env python3
"""Convert the provided 67-element shipwright_workbench_2_blocks.bbmodel.
Keep the exact 128x128 painted texture and UVs, with four correctly anchored
orientations for the 2-block-wide placed workbench.
"""
import base64,json,zlib
from pathlib import Path
ROOT=Path("src/main/resources/assets/simpleboatengine")
GEOMETRY="eNq9msmO3CAQht9lzhixY+dVornmGilKTlHePbXYYNNADp1CM4c27eXrvxaKwl9/f3z7+PJ1s1E5teVP9fGTDr2yRie1RRj5ASNGwR98/k7fBpV0VFuCgV8wQN/aBP+f6o3Pn39UpdmMdvFJ5MY4Ru/5CUS3VN7RE946eGA5FVuN8kQieYXiQ6A81UdWHgBojMZO1bPZKpORKLHiDA0mbK+iTbxLk2fKyPsyiRH1fpPHDeSBW1j4KulwUcH9DNyRf/GbB69UW9K3yIcBIuPBjn9rz3xGlI5BGrxLtT7bKjSMca/zAyyooPcBl4HvwBsLFNwOhnZ+wJsHNyivIMKDLjkKZFKOHO7FiAdEX9SU1fZ4YcENjUqBH/HmQcWKTyo3hrLLoBqlrBlCpXVCWYo78KqSImAAHAdI+znC63BP6UJedVGFhsrMoJzYTHzimIZmJpFbo1Ar0D5BkqxU4AlYFehUUnrAR8LwgcNukNSzo3MynhPkZ0OEBJjYgQT0+C9IYIyykFBAvEhptJ8LabTJy2QshDcdK+FAxRuhuIYwKbYKgg3jXEKMnWUaXoQ3BW+EAwnvhPJ+yHUQVjab44f9pHkJKp2osbDlwZdogcuiOkOmVB47nnKc+ePNg0flwYChAsKzcR7QvscW8HzAA0ifpafVE45RCloYyHahLSHToVj2phsWkjzwSochTw5aTAp3NWo3/Jw3D+4VLrKBheqKhda8FtjioHzDeLA7XGeEdQMBkMRiAe4KHa4LHFhZpcEc51E3uC6Li4cp5SkefFL2AG3OsrMTrjtck7Hcw7AW9zyIioOLR04nTjmDcL4PhykRFw0PODH1cLkFgoF5bdHvQHWobHfjdJe1bRilMp4+s0hsDTyIjmrfdeYNJeWd9k1z4660bSiRW0zL1p5ZVtywEadf8J+LDbEgpe3odra7PHTERnGdZM1KbKFEREGL3YCoZOJgmUS7lj9HkczpowO2V8n8rXiSI2OMB1qYcoljHSSY02XFaItmtpuAramiWelGBNHZkjoqHANP2OTRKOuyTq7hg4zXX08YTXhGvAyGFEWzfqjdVMhtGLYac3G3m5p0dPSbjihdNhFeolZOA4gzv+8a9+LbcVkJ1xnh+aEw+hIcBXGuIBEuAjxo7RJLiFyI1OD3Y0g4wzrx0t1gGf5wQ0OtlYkXMtcSLyS6xguJb+KEJ94qJyyI1Qkvwrl+i3yQ+FofPAnHLsiMS1wwNP5HfYqJ/0EVnde4X3jxPe6hDH3vRFvkeqH1u4tuqtsatwuvPnfSDX2O+dZkPZjm/bldkurcsWPvDI7PvdTOPlggx/PCO4YUsg2c02HGZmlZJo9G+6X0HPDv/dYEPevNbk3lsZwCeB+vwJGrqyz2dCh7bKVHS8tIagfY7nIbN2SudooX3kanxtPVxkuVkD3rgLI5DyTkOdcqIx27B7cNsQlqy046VjIB+Hy3IQW/ylIv8toZlazqHevQ0kXKhRO6CA56o5PTju34pMOt4SkdNV2EtavvcLSvSGz9RtT5bskm/XIJv0GWm5eBRq8nCb/gZsB8CfvFpu6KYkpOlCX6badIr3MEnLl4c0jSiAy4vwBmyiJjwMxT6w3w/8bA519r4ROb"
PNG="iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAYAAADDPmHLAAAFP0lEQVR4nO3bTU8bRxzHcT+MsdcEYmKUNIU4KBgTIiL1lEvPfSU99w31NVTKu2gqISWXqk0IBAPGAgNBGIIxu4Ze+f9WmtEqt+73c/vv7MN4mJ2HnaH468/tu8I9nefz98NCnEwkvjXx4cmFTZ+Y2xUSieNbG/+0Yp83mdj7J/K8RNLfrDfs/eOJN9b7tRZmbbqc3375g/d+7951bbrmV2JN//v1L3K+5DdQHoOjoYknt/7y0vRSAblGBcg5KkDOuahaMQe+9L6aWPsQHSN0T69NvPK4buJYrncyJqhNOe/zymW93sb1yOY/rpRtHBgTRPUp+3xJ724f+9MPbB+s+W+3GiZ20ofXpPyTsn0nU2OAsn1+tRIov5J/DOCac9OFLK7GiYmfzVVNfC0FpEpFG4+lQFSxZAvESVxw9g9e0bhmC1hVH9Rs7D07bX7v3Jt+eZ1400ffRpmeV9YDib2/k2Qn5V2QCuZOz76ZAzrqD7UA+2djE4daAJ0lVF2gxusoVuJCYJYSagEe1uUNCs0iJD45vfLmV1sAzV80Hdnrg7MAecGcvwXQN55ZAAxXlyYyTkoS2xqjY4SoYs/fObFNWiLzfm0h+gPbh7Z+nLPXB96IuvThqTdeWhgdI4TGAC7QAkQ1eQOlvEJjhF7zzJu+tNi06dKE6xgg1Ofr/WkBco4KkHNUgJxzffmWn9VwFGc6/5++fd56y36L/9S18+6QmX/906yQ8Thb/tXRWbZpnLq6vPKmb34+8KYPjofe9BC3vPjIHEhNA1OLGTZ9Io2ITvtSi0ESN2QeHpr26SDxQc3+AVc7j+3zAtO6xWcN+/yM08D+4aVN12lvYHHoUfOhXO8vf71/rW5/vw76FpoN7/V0ATlHBcg5t1ldNwfisu1T9dtz+9VzE79ZXDLxi7ZNj2O5n3y6XDz/w3t+fCPXSxO8+XbLxAcb9tNsfCdNsnyXGD2V7wC6n0HOn0j857E04dJFLi21JN3+nt/X/jLxzY10QRrL7//trR0zTST/u7e6P4MuAPdQAXKOCpBzLorsNMwl2ufauLvbM/HyyqqJ97o2XftEHSPsbPRN3Fm3fWZckTGB5Kcu38bjO92DaNdD46JNv3W63OwfA+gYoVaTaayUX693IOm2PLY+2rUV7ePXXts9iTpGiGRtw8lagOY/lvV4F0V2BTzKuCI+M5ttP8HFhf0Q9GTpiYnPL+3ycpr9AWNdHha6fl4u2uuvZL28VLYHKhIXZHvB7OyM9/khzYU7b/rZhf9D140uDwvNflk2CLjRyBZ4HGgBNP1iaPcT6PmhFuDow5GJUy3Ajb8FqAZbgDtvuiSnRvmhFmA4tBVaW4B0bMvj9MCuBmZtAaZkA0zqQ5z+fsk/Y4CcS48B9A12suVIarReHzv/G6BjhKfTtsv58kn6THkjtIXYHtk9iS+nZUdSUb4DSAuQyH4GfWPK8sboGKFWs/lPEt0f4C+P+rR/P8PWx2Nv+u5X//6L5XlbHjpGoAXIOSpAzlEBcs7tdPe+6wa9/X74JF8GTi7DJ93zfmPbxCMZVX84z7a/YTTwbxsPOTwafNf1+/1sv1+NA9PAzYH//u7VWsccCC3eaHq788Ken/H6har8M2pg2qfpzSnZ1KrTQIl1kPh+yq6nt5qyTTswrepH/sWe0DRwtSOD1IyLQY1IpvGBaavmny4g56gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADgf+8/0AkDmaMXkZ0AAAAASUVORK5CYII="
def point(x,z,d):
    return {"north":(x,z),"east":(16-z,x),
            "south":(16-x,16-z),"west":(z,16-x)}[d]
def orient(elements,facing):
    mapping={"north":{"north":"north","south":"south","west":"west","east":"east"},
             "east":{"north":"east","east":"south","south":"west","west":"north"},
             "south":{"north":"south","east":"west","south":"north","west":"east"},
             "west":{"north":"west","east":"north","south":"east","west":"south"}}[facing]
    output=[]
    for e in elements:
        f=e["f"];t=e["t"]
        corners=[point(x,z,facing) for x in (f[0]+16,t[0]+16)
                                   for z in (f[2]+8,t[2]+8)]
        faces={mapping.get(d,d):{"texture":"#wood","uv":[v/8 for v in uv]}
               for d,uv in zip(("north","south","west","east","up","down"),e["u"])}
        output.append({"from":[min(x for x,z in corners),max(0,f[1]),min(z for x,z in corners)],
                       "to":[max(x for x,z in corners),t[1],max(z for x,z in corners)],
                       "faces":faces})
    return {"textures":{"wood":"simpleboatengine:block/shipwright_workbench","particle":"simpleboatengine:block/shipwright_workbench"},
            "elements":output}
geometry=json.loads(zlib.decompress(base64.b64decode(GEOMETRY)))
assert len(geometry)==67
texture=ROOT/"textures/block/shipwright_workbench.png"
texture.parent.mkdir(parents=True,exist_ok=True)
texture.write_bytes(base64.b64decode(PNG))
folder=ROOT/"models/block"
folder.mkdir(parents=True,exist_ok=True)
for facing in ("north","east","south","west"):
    (folder/("boat_builder_"+facing+".json")).write_text(json.dumps(orient(geometry,facing),indent=2)+"\n")
print("Generated exact 67-element Shipwright Workbench with four placement orientations")
