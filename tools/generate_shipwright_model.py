#!/usr/bin/env python3
"""Original shipwright_workbench_2_blocks Blockbench geometry and UV sheet.
The main block is x 0..16 and auxiliary block is x 16..32.
"""
import base64,zlib,json
from pathlib import Path

CUBES="""eNq9mUuS3CAMhu8ya0zxENjOVbp6mxMkq1Tunl/CFpienSNXzaKhxv74JfQAv/58/fz68Vpicckt69t9/ZJhdjH46paCmd+YeQUXXKz4e7sbv99/XQcuwadyhaYLUZ5xOckrbg0u3OTKrHM1l1kuKldTkUBMtm3uLdbUblkATQ2rEsuocDXfOyKq+G2QmUaZeCTgobbum4NP8FL9EC2YEHibtMM30sQ/lVvDl0gu+/WCJkd+G8l4Jjja2ltuDgZydggh8hq6UOuS+F6o+P/gKrU33Bx0arlC0wPMSWcMT+iMsnvhWI0lTJBHmiJjt55gmsDBrAgcxDABHxI669wMix1yBZcdXzVNkSeZ3nk62SdoXgBA5ZsFYFnG3QVK0ocJgs+PGUD5gwU631w/MvOsHuYvT8k/8YP4AW/v/VYoyaM3Sdh0xxIqxrF4blDapKxg4wDaj2C6ObiUrManzof8wgvKxvn8QDeQgmnQbAX2pFYfRHOP0CYEjseC20J70c3B2J0wHPbtPaF0+BHwYl5CsbkZFbk7SorntizBDa72eDNSz9F2VY9fLu4wytFQ2Doeew55pWqkJZcCk7O5bu5SIRWGj6p8d3H10kol6zD3R3CV2ez2m+6wOmmcH2avT9mcdK+ryZsX7CxeONdjl59gZsaN/U+dbGJvIZNuMgWXvsdMuKsoPlvHXfUmv9tzG+QCJlPqLmpRn8+2IargaFw4BR01mDq5rcYQLDmkSUwTHRFuncNWlK0oyVKvNBDOvN09J5fVNosJvcopcOJzAcnd9DbJTPFZt5zSB+2G8B3dMGe0POHlesu4fAYIv7o+yBntEc8LfPK84B9xvNK740+4ud+FPfv9oD/gdpp8LkeiR3xOHw5vx7EHHE6zt0+ytbfp09UH+okIj8Hn4zav9gy38fEYY1e71U0ujWWnT/Tk6Rk4amjlwiqn8G24h+BWFeci27oa+eAnUbXoHYh0znIyif10YKM9DTcgtfNJWo0dPY3trt9ZabuFiPqhgssdAZ6ND6QCZ9oML5IBir3yZuIrvMLv2fwofn6gmj8PLcV6x+Xxw7Wuw+67dYCNK1+5hH7JzfkGaR5w49Pgid8+8KvEWMf/3032/geyJzM8"""
ATLAS="""iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAYAAADDPmHLAAAFP0lEQVR4nO3bTU8bRxzHcT+MsdcEYmKUNIU4KBgTIiL1lEvPfSU99w31NVTKu2gqISWXqk0IBAPGAgNBGIIxu4Ze+f9WmtEqt+73c/vv7MN4mJ2HnaH468/tu8I9nefz98NCnEwkvjXx4cmFTZ+Y2xUSieNbG/+0Yp83mdj7J/K8RNLfrDfs/eOJN9b7tRZmbbqc3375g/d+7951bbrmV2JN//v1L3K+5DdQHoOjoYknt/7y0vRSAblGBcg5KkDOuahaMQe+9L6aWPsQHSN0T69NvPK4buJYrncyJqhNOe/zymW93sb1yOY/rpRtHBgTRPUp+3xJ724f+9MPbB+s+W+3GiZ20ofXpPyTsn0nU2OAsn1+tRIov5J/DOCac9OFLK7GiYmfzVVNfC0FpEpFG4+lQFSxZAvESVxw9g9e0bhmC1hVH9Rs7D07bX7v3Jt+eZ1400ffRpmeV9YDib2/k2Qn5V2QCuZOz76ZAzrqD7UA+2djE4daAJ0lVF2gxusoVuJCYJYSagEe1uUNCs0iJD45vfLmV1sAzV80Hdnrg7MAecGcvwXQN55ZAAxXlyYyTkoS2xqjY4SoYs/fObFNWiLzfm0h+gPbh7Z+nLPXB96IuvThqTdeWhgdI4TGAC7QAkQ1eQOlvEJjhF7zzJu+tNi06dKE6xgg1Ofr/WkBco4KkHNUgJxzffmWn9VwFGc6/5++fd56y36L/9S18+6QmX/906yQ8Thb/tXRWbZpnLq6vPKmb34+8KYPjofe9BC3vPjIHEhNA1OLGTZ9Io2ITvtSi0ESN2QeHpr26SDxQc3+AVc7j+3zAtO6xWcN+/yM08D+4aVN12lvYHHoUfOhXO8vf71/rW5/vw76FpoN7/V0ATlHBcg5t1ldNwfisu1T9dtz+9VzE79ZXDLxi7ZNj2O5n3y6XDz/w3t+fCPXSxO8+XbLxAcb9tNsfCdNsnyXGD2V7wC6n0HOn0j857E04dJFLi21JN3+nt/X/jLxzY10QRrL7//trR0zTST/u7e6P4MuAPdQAXKOCpBzLorsNMwl2ufauLvbM/HyyqqJ97o2XftEHSPsbPRN3Fm3fWZckTGB5Kcu38bjO92DaNdD46JNv3W63OwfA+gYoVaTaayUX693IOm2PLY+2rUV7ePXXts9iTpGiGRtw8lagOY/lvV4F0V2BTzKuCI+M5ttP8HFhf0Q9GTpiYnPL+3ycpr9AWNdHha6fl4u2uuvZL28VLYHKhIXZHvB7OyM9/khzYU7b/rZhf9D140uDwvNflk2CLjRyBZ4HGgBNP1iaPcT6PmhFuDow5GJUy3Ajb8FqAZbgDtvuiSnRvmhFmA4tBVaW4B0bMvj9MCuBmZtAaZkA0zqQ5z+fsk/Y4CcS48B9A12suVIarReHzv/G6BjhKfTtsv58kn6THkjtIXYHtk9iS+nZUdSUb4DSAuQyH4GfWPK8sboGKFWs/lPEt0f4C+P+rR/P8PWx2Nv+u5X//6L5XlbHjpGoAXIOSpAzlEBcs7tdPe+6wa9/X74JF8GTi7DJ93zfmPbxCMZVX84z7a/YTTwbxsPOTwafNf1+/1sv1+NA9PAzYH//u7VWsccCC3eaHq788Ken/H6har8M2pg2qfpzSnZ1KrTQIl1kPh+yq6nt5qyTTswrepH/sWe0DRwtSOD1IyLQY1IpvGBaavmny4g56gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADgf+8/0AkDmaMXkZ0AAAAASUVORK5CYII="""

ROOT=Path("src/main/resources/assets/simpleboatengine")
texture=ROOT/"textures"/"block"/"shipwright_workbench.png"
texture.parent.mkdir(parents=True,exist_ok=True)
texture.write_bytes(base64.b64decode(ATLAS))
elements=[]
faces=("north","south","west","east","up","down")
for e in json.loads(zlib.decompress(base64.b64decode(CUBES))):
    obj={"from":[round(max(0,min(32,e["f"][0]+16)),4),round(max(0,e["f"][1]),4),round(e["f"][2]+8,4)],
         "to":[round(max(0,min(32,e["t"][0]+16)),4),round(e["t"][1],4),round(e["t"][2]+8,4)],
         "faces":{d:{"uv":[v/8 for v in uv],"texture":"#all"} for d,uv in zip(faces,e["u"])}}
    elements.append(obj)
dest=ROOT/"models"/"block"/"boat_builder.json"
dest.write_text(json.dumps({"textures":{"all":"simpleboatengine:block/shipwright_workbench",
    "particle":"simpleboatengine:block/shipwright_workbench"},"elements":elements},indent=2)+"\n")
print("Generated original 67-cube 2-block Shipwright Workbench and 128x128 texture")
