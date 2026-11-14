"""Extract the mounted radio group from the supplied fishing-boat model."""

import base64
import json
from pathlib import Path

from generate_skiff_mesh import SIDES, fmt

ROOT = Path(__file__).resolve().parents[1]
MODEL = ROOT / "models/fishing_boat_with_radio.bbmodel"
CLIENT = ROOT / "src/main/java/gr/daldon/simpleboatengine/client"
ATLAS = ROOT / "src/main/resources/assets/simpleboatengine/textures/entity/mounted_boat_radio.png"


def find_group(nodes, uuid):
    for node in nodes:
        if isinstance(node, str):
            continue
        if node["uuid"] == uuid:
            return node["children"]
        children = find_group(node["children"], uuid)
        if children is not None:
            return children
    return None


model = json.loads(MODEL.read_text(encoding="utf-8"))
group = next(group for group in model["groups"] if group["name"].lower() == "radio")
elements = {element["uuid"]: element for element in model["elements"]}
pieces = [elements[key] for key in find_group(model["outliner"], group["uuid"])]
texture_id = pieces[0]["faces"]["north"]["texture"]
texture = model["textures"][texture_id]
assert texture["width"] == texture["height"] == 64

rows = []
for piece in pieces:
    rx, yaw, rz = piece.get("rotation", [0, 0, 0])
    assert rx == rz == 0
    values = [*piece["from"], *piece["to"], yaw, *piece["origin"]]
    for side in SIDES:
        face = piece["faces"][side]
        assert face["texture"] == texture_id and not face.get("rotation")
        # SkiffMesh expects UV coordinates in 128-unit atlas space.
        values.extend(2 * coordinate for coordinate in face["uv"])
    rows.append("        {" + ", ".join(map(fmt, values)) + "}, // " + piece["name"])

rx, yaw, rz = group["rotation"]
assert rx == rz == 0
(CLIENT / "BoatRadioMeshData.java").write_text(
    """package gr.daldon.simpleboatengine.client;

/** Generated from the radio group in models/fishing_boat_with_radio.bbmodel. */
final class BoatRadioMeshData {
    static final float X = %s;
    static final float Y = %s;
    static final float Z = %s;
    static final float YAW = %s;
    static final float[][] PARTS = {
%s
    };

    private BoatRadioMeshData() {}
}
""" % (*map(fmt, group["origin"]), fmt(yaw), "\n".join(rows)),
    encoding="utf-8",
)
ATLAS.write_bytes(base64.b64decode(texture["source"].split(",", 1)[1]))
