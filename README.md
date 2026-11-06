# Simple Boat Engine · Forge 1.20.1

An original standalone Minecraft outboard-motor mod for vanilla boats. The Java source,
pixel art, cuboid model, and synthesized motor sound were produced independently for
this project. It does not require other vehicle mods or use their source/assets.

## Features

- Two items only: **Boat Engine** and **Fuel Bucket**. Independent custom recipes.
- Equip a vanilla boat with the engine by right-clicking the **boat** while holding
  the engine item. Installation is the only operation performed on the boat hull.
- After installing, point the crosshair at the visible **motor** at the stern and
  right-click it with an empty hand to turn it on/off.
- To refuel, turn the motor off and right-click **the motor itself** with the Fuel
  Bucket. The bucket is returned empty. The 3000-unit tank supports a 5000-block
  journey at full throttle 10, before counting slowing or steering.
- While riding a motorized boat, use the mouse wheel for throttle **0 through 10**;
  optional controls can be set in the key bindings. Hold **W/forward** to apply
  thrust; releasing forward stops acceleration. Throttle 0 uses no fuel.
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

- Original editable Blockbench model: `simple_boat_engine_original.bbmodel`
- Six original textures: `src/main/resources/assets/simpleboatengine/textures/item/`
- Original engine sound: `assets/simpleboatengine/sounds/motor_loop.ogg`
- Rebuild all original assets with: `python tools/make_assets.py` (needs `ffmpeg`)

The editable Blockbench file is an art-source model. The in-world rotating model
is rendered by `MotorMesh.java`, with the matching item JSON model for inventory.

## Copyright

This repository contains the Simple Boat Engine implementation only and is not a
repackaging or renaming of any third-party mod. It does not modify or delete the
authorship/license obligations of other repositories. All original assets and
source code here are © 2026 AggelosDaldon, all rights reserved.
