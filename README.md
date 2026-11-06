# Simple Boat Engine · Forge 1.20.1

An original standalone Minecraft outboard-motor mod for vanilla boats. The Java source,
pixel art, cuboid model, and synthesized motor sound were produced independently for
this project. It does not require other vehicle mods or use their source/assets.

## Features

- Three items: **Boat Engine**, **Fuel Bucket** and **Auxiliary Fuel Canister**.
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
- While riding a motorized boat, use the mouse wheel for throttle **0 through 10**;
  optional controls can be set in the key bindings. The setting acts as **cruise
  control**: throttle 1–10 moves the boat without holding W; throttle 0 stops
  motor acceleration and uses no fuel. Steering remains with the vanilla keys.
- Lower throttle settings consume proportionally less fuel per distance. The last
  20% of the tank limits throttle to 3, providing a low-fuel safety reserve.
- Compact HUD to the right of the hotbar shows live speed, throttle setting and
  remaining fuel; color shade changes with speed. Only visible while running.
- Motor-propeller blades turn even at throttle 0 while the engine is on. More bubbles
  at higher throttle. Small smoke wisps emit above the top intake only while ON.
- Original spatial motor hum; supports dedicated-server synchronized motor state.

## Build

This is a fresh, uncompiled **source project**. Java 17 and Gradle 8.8 are required.
No existing mod's Gradle wrapper or compiled JAR is included.

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
- Original engine sound: `assets/simpleboatengine/sounds/motor_loop.ogg`
- Regenerate the current game's 129-cuboid mesh after editing the Blockbench
  file: `python3 tools/generate_outboard_mesh.py`.
- Regenerate the side canister's mesh with: `python3 tools/generate_canister_mesh.py`.
- `tools/make_assets.py` remains for rebuilding the original legacy textures,
  bucket and audio, and will not replace the updated engine's inventory model.

The in-world motor and inventory item share `MotorMesh.java` with the full UV
atlas. The propeller blades and hub animate together around their model pivot.

## Copyright

This repository contains the Simple Boat Engine implementation only and is not a
repackaging or renaming of any third-party mod. It does not modify or delete the
authorship/license obligations of other repositories. All original assets and
source code here are © 2026 AggelosDaldon, all rights reserved.
