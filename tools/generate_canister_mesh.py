"""Compile the user's side-canister Blockbench model to UV-accurate mesh data."""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "models/small_red_fuel_canister.bbmodel"
OUTPUT = ROOT / "src/main/java/gr/daldon/simpleboatengine/client/CanisterMeshData.java"
SIDES = ("north", "south", "west", "east", "up", "down")


def as_float(value):
    return (f"{float(value):.6f}".rstrip("0").rstrip(".") or "0") + "f"


def main():
    model = json.loads(SOURCE.read_text(encoding="utf-8"))
    elements = {cube["uuid"]: cube for cube in model["elements"]}
    rows = []

    def walk(node):
        if isinstance(node, str):
            element = elements[node]
            origin = element.get("origin", [8, 0, 8])
            rotation = (element.get("rotation") or [0, 0, 0])[2]
            values = [*element["from"], *element["to"], rotation, *origin, 0]
            for face in SIDES:
                values.extend(element["faces"][face]["uv"])
            assert len(values) == 35
            rows.append("        {" + ", ".join(map(as_float, values)) + "}, // " + element["name"])
        else:
            for child in node["children"]:
                walk(child)

    for root in model["outliner"]:
        walk(root)

    OUTPUT.write_text(
        """package gr.daldon.simpleboatengine.client;

/** Generated from models/small_red_fuel_canister.bbmodel; do not hand-edit. */
final class CanisterMeshData {
    // Bounds, Z rotation + pivot, spin flag (=0), and six 128px face UVs.
    static final float[][] PARTS = {
""" + "\n".join(rows) + """
    };
    private CanisterMeshData() {}
}
""",
        encoding="utf-8",
    )
    print(f"Generated {len(rows)} canister cuboids")


if __name__ == "__main__":
    main()
