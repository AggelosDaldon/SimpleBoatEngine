#!/usr/bin/env python3
"""Rebuild Blockbench-derived marine parts from the supplied user-authored meshes.
Keep exact UVs, geometry and 64x64 textures; only translate into Minecraft block coordinates.
"""
import base64, zlib, json
from pathlib import Path

MODELS = {
    "orange": "eNq1lEGOwyAMRe+StUEYMIl6lSjbnqCzGs3dxx9TEmXZyhvkfAmeP/5k/12ey2MPOQpJ3AjFQctLNUhrzGTKjyp7yZSobsTtoG8/jj8a7BoTFbC1GGxIaMeUzk66k5sd83l9UkURDKpMqoxGxJMqFJLeKqqJld6JeF80/OmA891zb8jRNKapS71PuPfiOOICRouFUA1uefdSTq7u08FnO+jLj5POsVFYkTCtBr1r6MgkR3pVUsGo64SbtFL1ZotCNFMNOVtnzrrGZJKz8zDGfLXe/3Hu3hl3zglj12WOXcVNF5Ncwp7hmHvAeKbdRHTDfs/shFzIYXr2I1/u+mbZl2vZBgPV4CZ77pWSV8SOf4yFtHA=",
    "yellow": "eNq1lEGOwyAMRe+StUEYMIl6lSjbnqCzGs3dxx9TEmXZyhvkfAmeP/5k/12ey2MPOQpJ3AjFQctLNUhrzGTKjyp7yZSobsTtoG8/jj8a7BoTFbC1GGxIaMeUzk66k5sd83l9UkURDKpMqoxGxJMLeKqqJld6JeF80/OmA891zb8jRNKapS71PuPfiOOICRouFUA1uefdSTq7u08FnO+jLj5POsVFYkTCtBr1r6MgkR3pVUsGo64SbtFL1ZotCNFMNOVtnzrrGZJKz8zDGfLXe/3Hu3hl3zglj12WOXcVNF5Ncwp7hmHvAeKbdRHTDfs/shFzIYXr2I1/u+mbZl2vZBgPV4CZ77pWSV8SOf4yFtHA=",
    "white": "eNq1lEGOwyAMRe+StUEYMIl6lSjbnqCzGs3dxx9TEmXZyhvkfAmeP/5k/12ey2MPOQpJ3AjFQctLNUhrzGTKjyp7yZSobsTtoG8/jj8a7BoTFbC1GGxIaMeUzk66k5sd83l9UkURDKpMqoxGxJMLeKqqJld6JeF80/OmA891zb8jRNKapS71PuPfiOOICRouFUA1uefdSTq7u08FnO+jLj5POsVFYkTCtBr1r6MgkR3pVUsGo64SbtFL1ZotCNFMNOVtnzrrGZJKz8zDGfLXe/3Hu3hl3zglj12WOXcVNF5Ncwp7hmHvAeKbdRHTDfs/shFzIYXr2I1/u+mbZl2vZBgPV4CZ77pWSV8SOf4yFtHA=",
    "refuel": "eNq9l01ygzAMhe+StfHYxn/0Khm2PUG76vTufU+m2CTZEbHJRALzSU+yMPef2+ft4z5l48wUVnP7gpWNj4bGN4y7wyUPV17Nif/rr9lQxYSOAtjPneUZxxza4pPGTswjsOjzpkj9RjVzJ+JeZ2Jtq08anSgqdlHjJcjJD0hnE27SFdaDMUKzOlHSGpESgrOhc0UXkyOW4iHvsDo9UVTLytq6BUCXn22hz+nm7m01yWISCKhlX021aC2blXeQgMKARjTeeGedOpygI5vRXIKWvKOADnknuK9I+4iOUn99Mto8AHTIeoZrEVdSZc9P6HQRWbaXp+LTkDd3u1/2gDRewCjnEzdyc+liW76F8/NariODNUYEaeMGjDXEEuhbrpjkexAvYrgohMQQYst577eAN4nPjKQXAMsdV4uiJ40RX6mA4EvHu38NijY+7BUPHZ9b+zWfIl4SzagAOiH30T7TuRjXJ+zWv62QJ42H7CtPEhjzD9kvpvlUxXckBWa/jCM+UBd36H8FfCZpO7nt+MiTTYQk6vhC0oz2H/GJx6iMH3X8Qnxq2df9s4vOIuLX4TsBfYNnSB+9wxqDWBiEaPA/AFtkGRPI9XetTv8X1FnwryTwWrtv/QPZBm4O"
}
TEXTURES = {
    "orange": "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAtUlEQVR4nO3YPQrCQABE4aysooWFf9h5NK+eA1hYCQtB10NM8SD7vn6yw7ApktKf9z4lbvso3udPlC/nX5TfROkVGH6AOrWWPSGNh+fvmq9AxAHoAjQHoAvQHIAuQHMAugDNAegCtOEHqO/vNnrA6XKM8q95ifKP6yHKD38Dhh+gpt/j6f+E9PyllSg//A1wALoAzQHoAjQHoAvQHIAuQHMAugBt+AEkSZIkSZIkSZIkSdL6/QHV0B1cq7QYugAAAABJRU5ErkJggg==",
    "yellow": "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAs0lEQVR4nO3XPQrCQBiE4Sgp1Cj4cwHvfwGPI6RWSGkwiR5iihfyvU8/u8OwzW6ej/uvCXS3UxJvhv4V5XfnLspvo/QKlB+gnccpOmAav2Gevb/8C3AAugDNAegCNAegC9AcgC5AcwC6AK38AO0yL9EB+0v2Hx/6d5Q/XI9RvvwLKD9Am//nw/yHvb/8C3AAugDNAegCNAegC9AcgC5AcwC6AK38AJIkSZIkSZIkSZIkaf3+WAwjjg4PTF8AAAAASUVORK5CYII=",
    "white": "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAnklEQVR4nO3YOw4CMRRD0QFNm8/+twaizR5SBBbh4orknt6RZb0qj9f7870Cvfckfo0xonwpJco/o/QGjh/gnnNGD/x7/vgLcAC6AM0B6AI0B6AL0ByALkBzALoA7fgB7rVW9ECtNcqn/wGttSh//AUcP4D/AVF6Aw5AF2A5AF2A5gB0AZoD0AVoDkAXoB0/gCRJkiRJkiRJkiRJ2t8PqQskXKLSEWoAAAAASUVORK5CYII=",
    "refuel": "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAAuElEQVR4nO3XIQoCURRG4asY3iJswhRFBJPFNU12AabZnE3ENViMXkaj1sdDTpjzpQdy4edgmdm9W71LKZGZkZlR+74+ntU3v+/jft10311uTffzmDgD0ANoBqAH0AxAD6AZgB5AMwA9gGYAegDNAPQA2uK0OzR9Tw/Duel+udk23ffjGCVf398q35P/BxiAHkAzAD2AZgB6AM0A9ACaAegBNAPQA2gGoAdIkiRJkiRJkiRJkiT92wfJBgaBw+PSWAAAAABJRU5ErkJggg=="
}
# Both yellow and white meshes share exact geometry with orange.
MODELS["yellow"] = MODELS["orange"]
MODELS["white"] = MODELS["orange"]
root=Path("src/main/resources/assets/simpleboatengine")
faces=("north","south","east","west","up","down")
for col in ("orange","yellow","white"):
    voxels=json.loads(zlib.decompress(base64.b64decode(MODELS[col])))
    elements=[]
    for e in voxels:
        # Blockbench authored centered around zero; seawater surface is y=16.
        el={"from":[round(e["f"][0]+8,4),round(e["f"][1]+18,4),round(e["f"][2]+8,4)],
            "to":[round(e["t"][0]+8,4),round(e["t"][1]+18,4),round(e["t"][2]+8,4)],
            "faces":{side:{"uv":[round(v/4,4) for v in e["u"][i]],"texture":"#body"} for i,side in enumerate(faces)}}
        elements.append(el)
    model={"textures":{"body":f"simpleboatengine:block/user_buoy_{col}","particle":f"simpleboatengine:block/user_buoy_{col}"},"elements":elements}
    path=root/"models"/"block"/f"lift_buoy_{col}.json"
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(model,indent=2))
    image=root/"textures"/"block"/f"user_buoy_{col}.png"
    image.parent.mkdir(parents=True,exist_ok=True)
    image.write_bytes(base64.b64decode(TEXTURES[col]))
    if col=="white":
        # Original white PNG in the uploaded pack has a truncated IDAT stream.
        # Repair the physical texture, not just the inventory icon, by
        # recoloring the intact matching UV atlas from the orange version.
        from PIL import Image
        try:
            with Image.open(image) as check: check.load()
        except (OSError, ValueError):
            with Image.open(root/"textures"/"block"/"user_buoy_orange.png") as original:
                original=original.convert("RGBA")
                restored=Image.new("RGBA",original.size,(0,0,0,0))
                src=original.load();dest=restored.load()
                for yy in range(original.height):
                    for xx in range(original.width):
                        red,green,blue,alpha=src[xx,yy]
                        grey=max(140,min(252,int((red+green+blue)*.18+150)))
                        dest[xx,yy]=(grey,min(255,grey+2),min(255,grey+3),alpha)
                restored.save(image,optimize=True)
# The user's 32-cuboid 64px UV model, retained as a dedicated item mesh.
pieces=json.loads(zlib.decompress(base64.b64decode(MODELS["refuel"])))
lines=["package gr.daldon.simpleboatengine.client;","public final class RefuelCanisterMeshData {","  public static final float[][] PARTS = {"]
for e in pieces:
    # MotorMesh expects 11 parameters followed by 4 UV floats for each face,
    # where x/z values are offset by 8 internally.
    f=e["f"]; t=e["t"]
    # MotorMesh face sequence north/south/west/east/up/down
    uv=[e["u"][i] for i in [0,1,3,2,4,5]]
    a=[f[0]+8,f[1],f[2]+8,t[0]+8,t[1],t[2]+8,0,0,0,0,0]+[v for face in uv for v in face]
    lines.append("    {"+",".join(f"{v:.4f}f" for v in a)+"},")
lines.extend(["  };","  private RefuelCanisterMeshData() {}","}"])
out=Path("src/main/java/gr/daldon/simpleboatengine/client/RefuelCanisterMeshData.java")
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text("\n".join(lines)+"\n")
(root/"textures"/"item"/"refuel_canister_custom.png").write_bytes(base64.b64decode(TEXTURES["refuel"]))
