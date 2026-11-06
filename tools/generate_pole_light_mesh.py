"""Compile the two Blockbench pole-light states and their embedded UV atlas."""

import base64
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = ROOT / "models"
OUTPUT = ROOT / "src/main/java/gr/daldon/simpleboatengine/client/PoleLightMeshData.java"
TEXTURE = ROOT / "src/main/resources/assets/simpleboatengine/textures/item/pole_light.png"
SIDES = ("north", "south", "west", "east", "up", "down")


def jfloat(value):
    return (f"{float(value):.6f}".rstrip("0").rstrip(".") or "0") + "f"


def generate():
    off = json.loads((MODEL_DIR / "boat_pole_light_off.bbmodel").read_text(encoding="utf-8"))
    on = json.loads((MODEL_DIR / "boat_pole_light_on.bbmodel").read_text(encoding="utf-8"))
    assert off["resolution"] == on["resolution"] == {"width": 128, "height": 128}
    assert len(off["elements"]) == len(on["elements"])
    embedded = [base64.b64decode(x["textures"][0]["source"].split(",", 1)[1]) for x in (off, on)]
    assert embedded[0] == embedded[1], "On/off states must use the same UV atlas"
    TEXTURE.write_bytes(embedded[0])

    base, off_lens, on_lens = [], [], []

    def row(cube):
        origin = cube.get("origin", [8, 0, 8])
        rotation = (cube.get("rotation") or [0, 0, 0])[2]
        values = [*cube["from"], *cube["to"], rotation, *origin, 0]
        for side in SIDES:
            values.extend(cube["faces"][side]["uv"])
        assert len(values) == 35
        return "        {" + ", ".join(map(jfloat, values)) + "}, // " + cube["name"]

    for cold, bright in zip(off["elements"], on["elements"]):
        assert cold["name"] == bright["name"] and cold["from"] == bright["from"] and cold["to"] == bright["to"]
        if cold["faces"] == bright["faces"]:
            base.append(row(cold))
        else:
            off_lens.append(row(cold))
            on_lens.append(row(bright))

    OUTPUT.write_text(
        """package gr.daldon.simpleboatengine.client;

/** Generated from both models/boat_pole_light_*.bbmodel files. */
final class PoleLightMeshData {
    static final float[][] BASE = {
""" + "\n".join(base) + """
    };
    static final float[][] OFF_LENS = {
""" + "\n".join(off_lens) + """
    };
    static final float[][] ON_LENS = {
""" + "\n".join(on_lens) + """
    };
    private PoleLightMeshData() {}
}
""",
        encoding="utf-8",
    )
    print(f"Pole light: {len(base)} base pieces, {len(on_lens)} switchable lens pieces")


if __name__ == "__main__":
    generate()
