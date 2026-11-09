#!/usr/bin/env python3
"""Exact user-provided Blockbench meshes: 81-cube marine workbench and
27-cube Blue Boat V2 icebreaker, including original 128/64px texture atlases.
This generator runs before Gradle compilation in GitHub Actions.
"""
import json,base64,zlib
from pathlib import Path

WORKBENCH_GEOMETRY="""eNrNmk2O3DgMhe/Sa9nQv+W5SqO32QYYZFZB7j6PpGSp7GrEGdc0DXQaJQWRvjxSFEnV+8+3b29/vU8uzT4ZO1szLfj0Yd5+8HSYl2Tw25sp4SPm/8H8eyzGmhyNyx/m6gBr/o01LSYsPn/fPv8yHS4sDS4sA1wOxtF0wqfGFjxWNdggeGz0itEpwipfVWmvXhVVVbwq0k67qqiWdKLR3vFET2W/E432bid63sHrunSD0w3KqflcF25wua6bssfRiUjGi1nHs7oa5wWv0WFRa3hxbHJxcEa1dY/FqKpUk4tzaFj9fNIkc0151nEyci8nvt+g5khMy6jVF8faTFB+zmYqs21YPEmCyRxz0T+EGXiL//75lK9H2RzmK92reDJGM8VuPgUmMKQnUK4LBTe1dLJ5l4uDP0Kz8/oEDbbVkwubP0HKs9cWKz/c3A1sGS7uLxMr8/XnETaXHhhkMiK+ljlsTDjV1hQrm1wc/BEZgkM4ok0CfBs0sm/Wlq0yfK6aokEjcqwAsLQFVgQNR7DBSLBlqpVuW+fq3fuK0Qm2MCMZwp9lZMPBdCyZV1Is4CZEvEiE4Dc3g4wRqEXuAuYq9C/Xev4vDk5yQRp34MJkStpkIs4ARglGxVUDA8KBSwTTxCpc9lgi2LAQQcwK319nN2SFuFDgu1w9vGJ0ypQJkrl6/FrRYWmSfWxRp4Pxcjrywd+cNp+Fr+3EsxRI7qAdsR2kY7obKOe41k2UXbh2e1KhGThtW1QyRyrUqHbM/UIXpPjQs/jSOtfzhcklSUsbC5dsDqiaXR4yHlEgjtkNjCKaHYsRLIkfLBmLbHB59FuwJP2dKs8Pnllh04KZdbAhfnhdMcjl0W+5VjCxJV0veD1fo5bm3IDW/r+yxeXRaTTXz2Ijcz1hVAQbNaN8UZ/MOc6yn5tTXzTcSekJWkl3cDWGO/haSXfUTbxNnc1JNfKZUfWFSxLFHtDSvNzB3RKfSLcnu59m5Gv6ZOxqn5lTF036B9TFS9Tgj73C4052KmBch6QWyyId5X3Oj6yksKjtdn91Hg8gT/Byplk39DiwdLGy6ytGJxJc19SLLBTh8QMThpj0nU7j3bA1zxhvezmUx+pCDunHLJx7Tihl6lavHZ+TkqF2Shaq/EYlFVJNnFbfSADVTjHyX7jnSkk7Cmjfe+Btcfb+S4MT2fnaXNBtz4mFIgsb3qnWNJnbCuKBThq4gpeqB8LqYwcJK69Vg1eMzolXJDQ/aCeED0+xX+1zKLA2n7OIgBVvmZfqc3YORcXlcK+m5nOJS9Na2oce94pm3LPtPYMB+8tj4Dy5Rr51eLNqy7MQlwYnxSv1ayUP2rVIVxS9DslK3NwOV+yQsIQe6xadWJfluGY6EGE7D8l43GM4Dv0u+5+6XBMHq++8qxeSR6+rp6J3SuhZy/Cv3BMphYcibtczR+jfV0g0t+J/koc+/te++vFTAjlV2DqqjpISMmnrGyoEOO457LAS3WXaWLiaMlfVLRd27IkLFzpWM25QP5AdjDr1HS5wsFtAp5m/TSJdqHVO3gqJWBMUyzfFmlSfZsjpDojyvnALQoI7Et5Lw27nPHznb0BchoxT18oD4GBkZb7Bxp3vVvpt33tN7TshzcaZ5wvNp3IDG+8IrSirz4cbNh755MsRt+BLlA0f+LhYU+X7+BdlSf4t"""
ICEBREAKER_GEOMETRY="""eNrFmN2OmzAQhd8l18ayPR7/9FWive1tpaq9qvruPTMEA4FA1AhYabV4tLGPz/x8Se5/bt9v3+6dt9EE/HaxWvoyt18ISixbQoxtQuw3YnefjDMUjEfk0wX2/Ik9HQIOzz/a81/TZDkDGay6ctOFDWxALFk/6IoFr0yxP+PDxRu6vPWGxS+Kza+uQhhkmi7UURiu6gw210M+XOwLk+OjSKA0qMpYahoJpl1jV7aFkTKPlDnLKflBGxLIsKxCXLCVqA76ZANUih71/89NmY/FUg2x5syU2fvQpEIcLmJZLLOZR9FkC1pCSg8OpjxUXxc0yyg/qjZSoMMkI5eROIVCKWaX4ygZXqH6imgjtGaTXGBx0M5wNuOqo82h7xhxOQQ+tJnFayYunChVTzHw1Gtca/rD0KvVitvMrGf0fVXvi8tp4r28oqr5jIQefpNIxZfiuRC7NE/B/Cb5MT8Jc2qSENeX96LusbvTwfZU+Ic05l75Q4rPax1Q+0p/6gCv8bzogIO0b/dB7fEw64OMtu1bGjN60gXCs9R7nvlQ1dXZcRriZBQ6jFaPI9gwKRAdzg+pZZzZyRQb+n/nMI4YHRKoU50ZHy32p3YSXQ9xAFoTxyLsoQ5/rlEnrH0GsFL5WgDnBX/r5fhVyK7QV6l8Any7jfHzAr7C2BX2CpJPQG+3MXNeoFcIu0JeAfJp4O02yPseeBWvK9xVHp+G3W6Du29hN61Tt5wL3b2yX4euX2cunYncvfpfQa6wdUFcwfBZwO1mxH0JXAHrkrdC4atxq1Rd0lYhfDVs8Y6PB4Rk69vncBZpBUG4ds3nXTdqGFpFQ1WHwkTWud9b6JShx2DiJgzBqDQge+LbT6fflxQtIAyar3+Ixvmt"""
WORKBENCH_TEXTURE="""iVBORw0KGgoAAAANSUhEUgAAAIAAAACACAYAAADDPmHLAAAEQElEQVR4nO3bv2sUQRTA8XfeElBIRCOSCBE0ipZWFtHC0hQ5xJBGG+3PJuKPQm0EMYcRRBHBQgQFISSFEeJfcNiIIAg2IkTQs7jCeBINxFiE3dzuXXYz2bnbOd/30yST2Z29y76dmZ23mzs3tG9F6nR1dUlceXFxMVSu/FyOrU9qb+jgDqPjRevn3n832j5a/+vwqVTH37Zte2x9UntfP380Ol60vP/AHqPjReu3CFQjAJTz8vl86A/mZUmod7usXWIA1Gq1UDk6hpgGQLS9fH6X0f7mn+//bi/tBcAQoBxDgHL0AMql7gFqtR+hsutjpu32urt3Ov35ktrzlpaWJE7a+qTtk8qm7ZnWa8cQoBwBoJxnunZsWjZtb3l5ObbednvfO2zOYbs9egDlCADlLCwEdXZZu9zjJ09DzwOY5tOPHxsy2j5aP3HnXmx9Uns3Trw1Ol60PHzL7PtG62/nfxtt3/D/25sunz966EhsfVJ7DAHKEQDKEQDKeZ1232o/n+7252t1e/QAyhEAyhEAyll4JjDbZ9o6fQzO+v9HD6AcAaAcAaCchWcCO2vMdG0Mzvr70gMoRwAoRwAol/v4/pXR8wDRcvnFw9j6pPY+/D1qtH20/t3P/amO/+jaSGx9Unu7t9Zi65Pa6+7ek5MM0QMoRwAoRwAoZ7wOYP0+eKGz7vttt5c1egDlCADlCADleCYw47X8rNEDKEcAKEcAKMczgRm3lzV6AOUIAOUIAAAAAAAAAAAAAAAAgP9M7uz1qZXkzVrn+c2xTN+P184TEZmvVIM/7O3rlW/VhaDc39sjn75UgvLgQJ/V7ZEtz/9lavJ8Ww985uqzth4PzXn1hbGLT9rSA0xeKtj9Ftg0T2T1pNT/7O/tCW00ONAXKvvb+Uy3b7YPsuGJSHAFf6sutHzM9098/T7IDo+FK0cAKOeJrHXL/k/bY36z8d7f543xR4ZNnsjaePzpS6XpGP7g8lDzvTegWCo3zBFEwmsPyI4XV/ny7khc9Yb4wTN8YSZ1W7Bv3QCoP/mjV14Hv5vO+qcnToqIyNz90wSBgzyRtW55cKCvobsvlsrS39uzqWGgWCpLsVQO9p27f1pERP6sNF8bQPuFcgHzlWpwwvyr3r/i/at3M+sChfFZEVntVYqlskxeKpAPcMS6t4F+122LjfkE7GsaAPVjvk1+TwB3xOYCpidOSrFUTr0u4F/99fuRC3DDurmA4QszwaRtvlLdVBdeGJ8NTnr9nEKEXIArYtcBRFbv4wvjs0H3bZoMSrOIhNaLDQC/F0g7gWvVnALpbSgXkBa5AHcl5gKKpbL1J4REyAW4gnSwcgSAcg25ABH7+f9mcwpyAW5oyAW046lgESEX4AiGAOUIAOV4L0C5UC6gnW/skAtwQ7AU7L+r144XQzj57mAOAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA4Lh/XGBsvv7wwZIAAAAASUVORK5CYII="""
ICEBREAKER_TEXTURE="""iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAYAAACqaXHeAAAA3klEQVR4nO3YIQ4CMRCF4d3NoqghJCA4BB6J4wYo7kC4Bh5HgsRxHhwhAcMBmqyAQzzxaOf//Gsnk3Yz3fZwvHwbwXq1VOLN6XyV8rPxSMp3UroCNMBdgFufUpIWyDlb82k+kfL9MAzSAqXnw1+B8A3o5TtY+Dck/AmgAe4C3OxzgHv/CuYAKc4VCN+A9v58Sf8DNtu9VMBu8Zbyt4cU5wTQAHcBbhW8BaT4H8wBHXOAVfgG+N8CU3X/j5QPfwJogLsAt+LnAHV/5gAtXr7wDQAAAAAAAAAAAAAAAPX7ARZoYJ77vvPHAAAAAElFTkSuQmCC"""

def unpack(b64):
    return json.loads(zlib.decompress(base64.b64decode(b64)))

root=Path("src/main/resources/assets/simpleboatengine")
for name,atlas in [("marine_workbench",WORKBENCH_TEXTURE),("icebreaker_v2",ICEBREAKER_TEXTURE)]:
    dest=root/"textures"/("block" if name=="marine_workbench" else "entity")/(name+".png")
    dest.parent.mkdir(parents=True,exist_ok=True)
    dest.write_bytes(base64.b64decode(atlas))

# Minecraft block models accept 2-block-wide geometry with out-of-range
# coordinates. Keep the 2-block workbench centered at x=16,z=8.
elements=[]
for cube in unpack(WORKBENCH_GEOMETRY):
    faces={direction:{"texture":"#marine","uv":[round(v/8,5) for v in uv]}
           for direction,uv in zip(("north","south","west","east","up","down"),cube["u"])}
    # Vanilla accepts model element coordinates only in [-16, 32].
    # Trim the last 1.35px of ornamental side edging rather than reject
    # the entire otherwise-exact workbench model at resource load time.
    item={"from":[round(max(-16,min(32,cube["f"][0]+16)),4),round(cube["f"][1],4),round(cube["f"][2]+8,4)],
          "to":[round(max(-16,min(32,cube["t"][0]+16)),4),round(cube["t"][1],4),round(cube["t"][2]+8,4)],
          "faces":faces}
    rot=cube.get("r") or [0,0,0]
    if any(rot):
        axis=max(range(3),key=lambda n:abs(rot[n]))
        # Vanilla element rotation accepts only ±22.5° for tilted brushes.
        angle=22.5 if rot[axis]>0 else -22.5
        origin=cube.get("o") or [0,0,0]
        item["rotation"]={"origin":[origin[0]+16,origin[1],origin[2]+8],
                          "axis":"xyz"[axis],"angle":angle}
    elements.append(item)
model={"textures":{"marine":"simpleboatengine:block/marine_workbench",
                   "particle":"simpleboatengine:block/marine_workbench"},
       "elements":elements}
out=root/"models"/"block"/"boat_customizer.json"
out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(model,indent=2)+"\\n")
# Export pre-rotated versions of the two-block model. Vanilla blockstates
# rotate around the MAIN block center, which previously displaced the second
# half into the floor on some directions. Here geometry follows the side block.
face_cycles={
    "north":{"north":"north","east":"east","south":"south","west":"west"},
    "east":{"north":"east","east":"south","south":"west","west":"north"},
    "south":{"north":"south","east":"west","south":"north","west":"east"},
    "west":{"north":"west","east":"north","south":"east","west":"south"},
}
def orient_point(x,z,facing):
    return {
        "north":(x,z),
        "east":(16-z,x),
        "south":(16-x,16-z),
        "west":(z,16-x),
    }[facing]
for facing in face_cycles:
    rotated=json.loads(json.dumps(model))
    for part in rotated["elements"]:
        a,b=part["from"],part["to"]
        corners=[orient_point(x,z,facing) for x in (a[0],b[0]) for z in (a[2],b[2])]
        part["from"]=[min(x for x,z in corners),a[1],min(z for x,z in corners)]
        part["to"]=[max(x for x,z in corners),b[1],max(z for x,z in corners)]
        part["faces"]={face_cycles[facing].get(face,face):data for face,data in part["faces"].items()}
        if "rotation" in part:
            rot=part["rotation"]
            x,z=orient_point(rot["origin"][0],rot["origin"][2],facing)
            rot["origin"]=[x,rot["origin"][1],z]
            if rot["axis"] in ("x","z") and facing in ("east","west"):
                rot["axis"]="z" if rot["axis"]=="x" else "x"
    (out.parent/("boat_customizer_"+facing+".json")).write_text(json.dumps(rotated,indent=2)+"\\n")

# A dedicated exact-UV V2 blade atlas, not the old placeholder beams.
# Mesh coordinates remain in original V2 boat-local pixels, no extra offset.
parts=[]
for cube in unpack(ICEBREAKER_GEOMETRY):
    rot=cube.get("r") or [0,0,0]
    pivot=cube.get("o") or [0,0,0]
    # SkiffMesh divides every UV by 128. Double the supplied 64px UVs.
    faceuv=[v*2 for face in cube["u"] for v in face]
    vals=cube["f"]+cube["t"]+[rot[1]]+pivot+faceuv
    parts.append(vals)
def num(v):
    return (("%.5f"%v).rstrip("0").rstrip(".") if "." in "%.5f"%v else str(v))+"f"
lines=["package gr.daldon.simpleboatengine.client;","",
       "/** Exact 27-element fitting geometry from supplied Blue Boat V2 bbmodel. */",
       "final class IceBreakerModelData {",
       "    static final float[][] PARTS = {"]
for vals in parts:
    lines.append("        {"+", ".join(num(v) for v in vals)+"},")
lines += ["    };","    private IceBreakerModelData() {}","}"]
source=Path("src/main/java/gr/daldon/simpleboatengine/client/IceBreakerModelData.java")
source.parent.mkdir(parents=True,exist_ok=True)
source.write_text("\n".join(lines)+"\n")
print("Uploaded marine asset import: 81 workbench cubes, 27 fitted icebreaker cubes, original UV textures")
