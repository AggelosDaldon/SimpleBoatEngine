"""Convert the supplied Blockbench fishing skiff into a textured cuboid mesh.

Keeps the original UV coordinates and per-cube Y-axis rotations; also extracts
the PNG atlas embedded in the editable .bbmodel so the two stay in sync.
"""

import base64
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SPECS = (
    ("large_motor_fishing_skiff.bbmodel", "SkiffMeshData", "large_motor_fishing_skiff.png"),
    ("aggelos_blue_fishing_boat.bbmodel", "BlueBoatMeshData", "aggelos_blue_fishing_boat.png"),
)
SIDES = ("north", "south", "west", "east", "up", "down")


def fmt(number):
    return (f"{float(number):.6f}".rstrip("0").rstrip(".") or "0") + "f"


def generate(source_name, class_name, texture_name):
    source = ROOT / "models" / source_name
    dest = ROOT / "src/main/java/gr/daldon/simpleboatengine/client" / (class_name + ".java")
    texture = ROOT / "src/main/resources/assets/simpleboatengine/textures/entity" / texture_name
    model = json.loads(source.read_text(encoding="utf-8"))
    by_id = {entry["uuid"]: entry for entry in model["elements"]}
    rows = []
    channels = []

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
            name = piece["name"].lower()
            # 1 hull, 2 floor, 3 stripe, 4 hatch, 5 seats, 6 antifoul,
            # 7 interior sidewalls. White stern lining remains unpainted.
            if "central keel" in name or "underwater hull" in name or "lower dark edge" in name:
                channels.append(6)
            elif "stripe" in name:
                channels.append(3)
            elif "hatch" in name:
                channels.append(4)
            elif "seat" in name:
                channels.append(5)
            elif "interior sidewall" in name:
                channels.append(7)
            elif "floor" in name:
                channels.append(2)
            elif "white" in name:
                channels.append(0)
            else:
                channels.append(1)
        else:
            for child in node.get("children", []):
                visit(child)

    for group in model["outliner"]:
        visit(group)

    assert len(rows) == len(model["elements"])
    dest.write_text(
        """package gr.daldon.simpleboatengine.client;

/** Generated from the source Blockbench model; do not hand-edit. */
final class """ + class_name + """ {
    // Bounds (6), Y rotation and pivot (4), six sets of 128px face UVs (24).
    static final float[][] PARTS = {
""" + "\n".join(rows) + """
    };
    static final byte[] CHANNELS = {
        """ + ", ".join(map(str, channels)) + """
    };
    private """ + class_name + """() {}
}
""",
        encoding="utf-8",
    )
    encoded = model["textures"][0]["source"].split(",", 1)[1]
    texture.parent.mkdir(parents=True, exist_ok=True)
    texture.write_bytes(base64.b64decode(encoded))
    print(f"Generated {len(rows)} {class_name} cuboids and {texture.stat().st_size} byte texture")


def main():
    for spec in SPECS:
        generate(*spec)


if __name__ == "__main__":
    main()
