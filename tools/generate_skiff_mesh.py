"""Convert the supplied Blockbench fishing skiff into a textured cuboid mesh.

Keeps the original UV coordinates and per-cube Y-axis rotations; also extracts
the PNG atlas embedded in the editable .bbmodel so the two stay in sync.
"""

import base64
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "models/large_motor_fishing_skiff.bbmodel"
DEST = ROOT / "src/main/java/gr/daldon/simpleboatengine/client/SkiffMeshData.java"
TEXTURE = ROOT / "src/main/resources/assets/simpleboatengine/textures/entity/large_motor_fishing_skiff.png"
SIDES = ("north", "south", "west", "east", "up", "down")


def fmt(number):
    return (f"{float(number):.6f}".rstrip("0").rstrip(".") or "0") + "f"


def main():
    model = json.loads(SOURCE.read_text(encoding="utf-8"))
    by_id = {entry["uuid"]: entry for entry in model["elements"]}
    rows = []

    def visit(node):
        if isinstance(node, str):
            piece = by_id[node]
            origin = piece.get("origin", [0, 0, 0])
            rotation = (piece.get("rotation") or [0, 0, 0])[1]
            values = [*piece["from"], *piece["to"], rotation, *origin]
            for side in SIDES:
                values.extend(piece["faces"][side]["uv"])
            assert len(values) == 34, piece["name"]
            rows.append("        {" + ", ".join(map(fmt, values)) + "}, // " + piece["name"])
        else:
            for child in node.get("children", []):
                visit(child)

    for group in model["outliner"]:
        visit(group)

    assert len(rows) == len(model["elements"])
    DEST.write_text(
        """package gr.daldon.simpleboatengine.client;

/** Generated from models/large_motor_fishing_skiff.bbmodel; do not hand-edit. */
final class SkiffMeshData {
    // Bounds (6), Y rotation and pivot (4), six sets of 128px face UVs (24).
    static final float[][] PARTS = {
""" + "\n".join(rows) + """
    };
    private SkiffMeshData() {}
}
""",
        encoding="utf-8",
    )
    encoded = model["textures"][0]["source"].split(",", 1)[1]
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    TEXTURE.write_bytes(base64.b64decode(encoded))
    print(f"Generated {len(rows)} skiff cuboids and {TEXTURE.stat().st_size} byte texture")


if __name__ == "__main__":
    main()
