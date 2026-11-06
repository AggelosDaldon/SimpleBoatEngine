"""Convert an editable Blockbench motor into exact per-face in-game mesh data.

Run: python3 tools/generate_outboard_mesh.py
"""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "models/outboard_engine_forge_1.20.1.bbmodel"
OUTPUT = ROOT / "src/main/java/gr/daldon/simpleboatengine/client/MotorMeshData.java"
FACE_ORDER = ("north", "south", "west", "east", "up", "down")


def number(value):
    formatted = f"{float(value):.6f}".rstrip("0").rstrip(".")
    return (formatted if formatted else "0") + "f"


def main():
    model = json.loads(SOURCE.read_text(encoding="utf-8"))
    elements = {item["uuid"]: item for item in model["elements"]}
    groups = {group["uuid"]: group for group in model["groups"]}
    ordered = []

    def visit(node, ancestry=()):
        if isinstance(node, str):
            ordered.append((elements[node], ancestry))
            return
        name = groups[node["uuid"]]["name"]
        for child in node["children"]:
            visit(child, ancestry + (name,))

    for root in model["outliner"]:
        visit(root)

    rows = []
    for element, ancestry in ordered:
        name = element["name"]
        if name == "smoke particle here":
            continue  # Blockbench placement marker, not visible geometry.
        rotation = (element.get("rotation") or [0, 0, 0])[2]
        pivot = element.get("origin", [8, 0, 8])
        propeller = "propeller" in ancestry and name != "Propeller rear axle"
        values = list(element["from"] + element["to"])
        values.extend((rotation, *pivot, int(propeller)))
        for side in FACE_ORDER:
            values.extend(element["faces"][side]["uv"])
        assert len(values) == 35, name
        rows.append("        {" + ", ".join(number(v) for v in values) + "}, // " + name)

    text = """package gr.daldon.simpleboatengine.client;

/** Generated from models/outboard_engine_forge_1.20.1.bbmodel; do not hand-edit. */
final class MotorMeshData {
    // x0,y0,z0,x1,y1,z1, rotationZ, originX,Y,Z, spins, then face UVs
    // north, south, west, east, up, down: (u0,v0,u1,v1) in 256px atlas.
    static final float[][] PARTS = {
""" + "\n".join(rows) + """
    };
    private MotorMeshData() {}
}
"""
    OUTPUT.write_text(text, encoding="utf-8")
    print(f"Wrote {len(rows)} engine cuboids to {OUTPUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
