# Simple Boat Engine · Forge 1.20.1

An original standalone Minecraft outboard-motor mod for vanilla boats. The Java source,
pixel art, cuboid model, and synthesized motor sound were produced independently for
this project. It does not require other vehicle mods or use their source/assets.

## Features

- Four accessories: **Boat Engine**, **Fuel Bucket**, **Auxiliary Fuel Canister**
  and **Automatic Boat Pole Light**, plus the **Fishing Boat** based on the
  blue 72-cuboid Blockbench model. The previous experimental Large Fishing
  Skiff is no longer registered in the game.
- The Fishing Boat retains vanilla buoyancy and steering, two seating positions
  (rear driver and port passenger) and the long blue bow deck. The original
  variant is `/give @s simpleboatengine:aggelos_blue_fishing_boat` (internal
  registry ID retained to preserve existing boats in saved worlds). Nine more
  items — oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry and
  bamboo — use the same hull, seats and functional accessories with recolored
  UV-compatible wood atlases.
- Right-click the **white lid on the forward blue deck**, with an empty hand,
  to open the boat's persistent **54-slot cargo inventory**. Breaking a boat
  spills its contents. Clicking elsewhere does not open storage. From the
  water, aim at the hull and right-click to get aboard; the mod does not
  automatically climb up for you. On foot, its bow deck and central/stern
  floor are maintained as walkable surfaces, without expanding the underlying
  vanilla buoyancy collision box.
- The fishing boat's item appears in its mod tab and in vanilla Tools &
  Utilities. Use only one current Simple Boat Engine JAR in `mods/` when
  testing; old and new JARs together cause duplicate registration errors.
- All three fitted accessories use separate attachment positions on the boat:
  motor at the transom, fuel canister on starboard aft, and automatic pole light
  at the port stern corner. Both custom boats have individually calibrated
  mount points: the motor hangs outside the transom, the fuel tank sits low on
  the interior starboard floor, and the pole light attaches to the aft rail.
  The hull visual offset and riders have been lowered closer to the waterline.
  On vanilla boats, the pole light sits further left of the motor. Engine
  targeting, exhaust particles and actual emitted light follow the mounts.
- Equip a vanilla boat with the engine by right-clicking the **boat** while holding
  the engine item. Installation is the only operation performed on the boat hull.
- After installing, point the crosshair at the visible **motor** at the stern and
  right-click it with an empty hand to turn it on/off.
- To refuel, turn the motor off and right-click **the motor itself** with the Fuel
  Bucket. The bucket is returned empty. Each bucket adds 3000 fuel units, up to
  the installed tank's capacity. The base 3000-unit tank supports 5000 blocks
  at full throttle 10, before counting slowing or steering.
- Install the small red Auxiliary Fuel Canister by right-clicking an
  **engine-equipped boat** with the canister in your hand. It appears on the
  **rear right (starboard)** side, and raises capacity from 3000 to **6000**.
  The new space can be filled with a second Fuel Bucket. The canister stays
  fitted to this boat.
- Right-click an engine-equipped boat with the **Automatic Boat Pole Light**
  to fit its custom model to the **rear left (port)** corner, alongside
  the wooden gunwale near the outboard motor. The mounting base is inset onto
  the wood rather than suspended outside the hull or below the waterline.
  The light uses its unlit model during daytime and switches
  to its lit model automatically at the start of dusk (world time 12000).
  It produces **real light level 14**, the same as a torch, around the boat,
  without fuel use or extra dependencies. It switches off at dawn (23000).
  An invisible non-solid temporary light block follows the boat and removes
  itself when the boat moves away, the sun rises, or the boat despawns.
- While riding a motorized boat, use the mouse wheel for throttle **0 through 10**;
  optional controls can be set in the key bindings. The setting acts as **cruise
  control**: throttle 1–10 moves the boat without holding W; throttle 0 stops
  motor acceleration and uses no fuel. Steering remains with the vanilla keys.
  The motor shuts off immediately and resets throttle when a player gets out
  of the boat. Cruising speed is slightly higher than in the previous version.
- Lower throttle settings consume proportionally less fuel per distance. The last
  20% of the tank limits throttle to 3, providing a low-fuel safety reserve.
- Compact HUD to the right of the hotbar shows live speed, throttle setting and
  remaining fuel; color shade changes with speed. Only visible while running.
- Motor-propeller blades turn even at throttle 0 while the engine is on. More bubbles
  at higher throttle. Small smoke wisps emit above the top intake only while ON.
- Original spatial motor hum, slightly louder; supports dedicated-server
  synchronized motor state.

## Build

The project requires Java 17 and Gradle 8.8. A compiled build is supplied
as the GitHub Actions artifact when the workflow succeeds.

1. Open a terminal in the folder containing `settings.gradle`.
2. Run `gradle build` using Gradle 8.8 with Java 17.
3. Take the built `.jar` from `build/libs/` and install it on the client and server.

Alternatively, upload these files to **a newly created GitHub repository** and run
the included **Build Simple Boat Engine** GitHub Actions workflow. Download the
artifact from that workflow's build page.

## Modeling and source assets

- Current editable Blockbench model: `models/outboard_engine_forge_1.20.1.bbmodel`
- Exact 256×256 UV texture: `src/main/resources/assets/simpleboatengine/textures/item/outboard_engine.png`
- Side canister's editable model: `models/small_red_fuel_canister.bbmodel`
- Side canister's 128×128 atlas: `src/main/resources/assets/simpleboatengine/textures/item/boat_accessories.png`
- Boat pole light's two editable models: `models/boat_pole_light_off.bbmodel`
  and `models/boat_pole_light_on.bbmodel`.
- Boat pole light's 128×128 atlas: `src/main/resources/assets/simpleboatengine/textures/item/pole_light.png`
- Legacy skiff reference (no longer registered): `models/large_motor_fishing_skiff.bbmodel`;
  actual fishing boat source: `models/aggelos_blue_fishing_boat.bbmodel`;
  its original 128px UV atlas and renderer mesh can be reproduced with
  `python3 tools/generate_skiff_mesh.py`.
- Produce the nine wood variants' compatible 128px atlases, inventory icons
  and item models with `python3 tools/generate_wood_boats.py`.
- Original engine sound: `assets/simpleboatengine/sounds/motor_loop.ogg`
- Regenerate the current game's 129-cuboid mesh after editing the Blockbench
  file: `python3 tools/generate_outboard_mesh.py`.
- Regenerate the side canister's mesh with: `python3 tools/generate_canister_mesh.py`.
- Regenerate both pole light variants with: `python3 tools/generate_pole_light_mesh.py`.
- `tools/make_assets.py` remains for rebuilding the original legacy textures,
  bucket and audio, and will not replace the updated engine's inventory model.

The in-world motor and inventory item share `MotorMesh.java` with the full UV
atlas. The propeller blades and hub animate together around their model pivot.

## Copyright

This repository contains the Simple Boat Engine implementation only and is not a
repackaging or renaming of any third-party mod. It does not modify or delete the
authorship/license obligations of other repositories. All original assets and
source code here are © 2026 AggelosDaldon, all rights reserved.
