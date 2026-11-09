#!/usr/bin/env python3
"""Generate faithful 256x256 pixel-art inventory icons directly from game models.

Reads block element geometry and each visible UV region rather than painting
unrelated flat symbols. Renders in a 128px isometric pixel grid, enlarged to
256px with nearest-neighbor sampling and transparent backgrounds.
"""
import json,re
from pathlib import Path
from PIL import Image, ImageDraw, ImageStat
ROOT=Path("src/main/resources/assets/simpleboatengine")
OUT=ROOT/"textures"/"item"
OUT.mkdir(parents=True,exist_ok=True)

def read_model(path):
    data=json.loads((ROOT/"models"/"block"/(path+".json")).read_text())
    return data.get("elements",[]),data.get("textures",{})
def read_java_model(path):
    source=Path("src/main/java/gr/daldon/simpleboatengine/client")/path
    text=source.read_text()
    blocks=re.findall(r"\{([\d.,fF\-\s]+)\}",text)
    boxes=[]
    for block in blocks:
        nums=[]
        try:
            nums=[float(n.strip().rstrip("fF")) for n in block.split(",")]
        except ValueError:continue
        if len(nums)>=6:
            boxes.append({"from":nums[:3],"to":nums[3:6],"faces":{}})
    return boxes,{}
def tex_color(key,face,texture_map,default):
    value=texture_map.get(key.lstrip("#"),"")
    if not value.startswith("simpleboatengine:"):
        return default
    full=ROOT/"textures"/(value.split(":")[1]+".png")
    if not full.exists():return default
    im=Image.open(full).convert("RGBA")
    uv=face.get("uv")
    if uv and len(uv)==4:
        # The model coordinates are in normalized 0..16 UV.
        x0,x1=sorted((int(uv[0]*im.width/16),int(uv[2]*im.width/16)))
        y0,y1=sorted((int(uv[1]*im.height/16),int(uv[3]*im.height/16)))
        if x1>x0 and y1>y0:
            im=im.crop((max(0,x0),max(0,y0),min(im.width,x1),min(im.height,y1)))
    pixels=[p for p in im.getdata() if p[3]>=90]
    if not pixels:return default
    # Median approximates the real mapped texture while staying legible at 128px.
    return tuple(sorted(p[i] for p in pixels)[len(pixels)//2] for i in range(3))
def shade(rgb,factor):
    return tuple(max(0,min(255,int(v*factor))) for v in rgb)+(255,)
def draw_model(elements,texture_map,output,base=(165,118,73)):
    if not elements:return
    xs=[v for e in elements for v in [e["from"][0],e["to"][0]]]
    ys=[v for e in elements for v in [e["from"][1],e["to"][1]]]
    zs=[v for e in elements for v in [e["from"][2],e["to"][2]]]
    cx=(min(xs)+max(xs))/2;cy=(min(ys)+max(ys))/2;cz=(min(zs)+max(zs))/2
    import math
    def project(pt):
        x,y,z=pt
        return ((x-cx)*.86-(z-cz)*.86,(x-cx)*.45+(z-cz)*.45-(y-cy)*1.03)
    raw=[]
    for e in elements:
        x0,y0,z0=e["from"];x1,y1,z1=e["to"]
        if x1<=x0 or y1<=y0 or z1<=z0:continue
        face=e.get("faces",{})
        top=[(x0,y1,z0),(x1,y1,z0),(x1,y1,z1),(x0,y1,z1)]
        right=[(x1,y0,z0),(x1,y1,z0),(x1,y1,z1),(x1,y0,z1)]
        front=[(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)]
        for polygon,facename,brightness in [(top,"up",1.14),(right,"east",.83),(front,"south",.98)]:
            f=face.get(facename,{})
            col=tex_color(f.get("texture","#none"),f,texture_map,base)
            raw.append((sum(q[0]+q[2] for q in polygon)/4 + sum(q[1] for q in polygon)/8,
                        [project(q) for q in polygon],shade(col,brightness)))
    if not raw:return
    raw.sort(key=lambda q:q[0])
    coords=[q for _,face,_ in raw for q in face]
    minx,maxx=min(v[0] for v in coords),max(v[0] for v in coords)
    miny,maxy=min(v[1] for v in coords),max(v[1] for v in coords)
    scale=min(107/max(maxx-minx,1),107/max(maxy-miny,1))
    offsetx=64-(minx+maxx)*scale/2
    offsety=63-(miny+maxy)*scale/2
    image=Image.new("RGBA",(128,128),(0,0,0,0))
    brush=ImageDraw.Draw(image)
    for _,face,col in raw:
        points=[(round(px*scale+offsetx),round(py*scale+offsety)) for px,py in face]
        brush.polygon(points,fill=col)
        if scale>1.1:
            brush.line(points+[points[0]],fill=shade(col[:3],.76),width=1)
    image.resize((256,256),Image.Resampling.NEAREST).save(OUT/(output+"_icon.png"),optimize=True)

for kind,out,col in [
    ("boat_customizer","boat_customizer",(143,98,65)),
    ("boat_stand","boat_stand",(121,83,56)),
    ("boat_builder","boat_builder",(144,107,78)),
    ("lift_buoy_orange","lift_buoy",(213,70,42)),
    ("lift_buoy_yellow","yellow_lift_buoy",(235,181,42)),
    ("lift_buoy_white","white_lift_buoy",(220,218,210))
]:
    try:
        geometry,textures=read_model(kind)
        draw_model(geometry,textures,out,col)
    except Exception as error:
        print("Icon fallback:",kind,str(error))

for source,destination,color in [
    ("IceBreakerModelData.java","ice_breaker",(184,200,204)),
    ("RefuelCanisterMeshData.java","fuel_bucket",(206,86,57)),
    ("CanisterMeshData.java","fuel_canister",(207,59,43)),
    ("FishingMotorMeshData.java","engine",(114,124,131)),
    ("PoleLightMeshData.java","pole_light",(173,177,156))
]:
    try:
        geometry,texture=read_java_model(source)
        draw_model(geometry,texture,destination,color)
    except Exception as error:
        print("Mesh icon fallback:",source,str(error))
print("Model-derived 256x256 inventory sprite set generated.")
