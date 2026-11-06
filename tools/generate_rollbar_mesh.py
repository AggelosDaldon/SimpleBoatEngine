"""Compile the editable roll bar's ON and OFF Blockbench models into Java mesh data."""

import base64
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = ROOT / "models"
OUTPUT = ROOT / "src/main/java/gr/daldon/simpleboatengine/client/RollbarMeshData.java"
TEXTURE = ROOT / "src/main/resources/assets/simpleboatengine/textures/item/rollbar_spotlights.png"
SIDES = ("north", "south", "west", "east", "up", "down")


def jfloat(value):
    return (f"{float(value):.6f}".rstrip("0").rstrip(".") or "0") + "f"


def generate():
    off = json.loads((MODEL_DIR / "boat_rollbar_spotlights_off.bbmodel").read_text(encoding="utf-8"))
    on = json.loads((MODEL_DIR / "boat_rollbar_spotlights_on.bbmodel").read_text(encoding="utf-8"))
    assert off["resolution"] == on["resolution"] == {"width": 128, "height": 128}
    assert len(off["elements"]) == len(on["elements"])
    embedded = [base64.b64decode(model["textures"][0]["source"].split(",", 1)[1])
                for model in (off, on)]
    assert embedded[0] == embedded[1], "Both variants must use the same texture atlas"
    TEXTURE.write_bytes(embedded[0])

    structure, off_lenses, on_lenses = [], [], []

    def row(cube):
        origin = cube.get("origin", [8, 0, 8])
        rotation = (cube.get("rotation") or [0, 0, 0])[2]
        values = [*cube["from"], *cube["to"], rotation, *origin, 0]
        for side in SIDES:
            values.extend(cube["faces"][side]["uv"])
        assert len(values) == 35
        return "        {" + ", ".join(map(jfloat, values)) + "}, // " + cube["name"]

    for cold, bright in zip(off["elements"], on["elements"]):
        assert cold["from"] == bright["from"] and cold["to"] == bright["to"]
        if cold["faces"] == bright["faces"]:
            structure.append(row(cold))
        else:
            off_lenses.append(row(cold))
            on_lenses.append(row(bright))

    OUTPUT.write_text(
        """package gr.daldon.simpleboatengine.client;

/** Generated from models/boat_rollbar_spotlights_{off,on}.bbmodel. */
final class RollbarMeshData {
    static final float[][] FRAME = {
""" + "\n".join(structure) + """
    };
    static final float[][] OFF_LENSES = {
""" + "\n".join(off_lenses) + """
    };
    static final float[][] ON_LENSES = {
""" + "\n".join(on_lenses) + """
    };
    private RollbarMeshData() {}
}
""",
        encoding="utf-8",
    )
    print(f"Rollbar: {len(structure)} frame pieces, {len(off_lenses)} switchable lamps")


if __name__ == "__main__":
    generate()
