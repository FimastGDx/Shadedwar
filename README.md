# Shadedwar — Fabric 1.21.4 port

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.21.4-3C8527?style=for-the-badge" alt="Minecraft 1.21.4" />
  <img src="https://img.shields.io/badge/Fabric%20Loader-0.19.5%2B-DBD0B4?style=for-the-badge" alt="Fabric Loader 0.19.5+" />
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/GeckoLib-4.8.5-2D7FF9?style=for-the-badge" alt="GeckoLib 4.8.5" />
  <img src="https://img.shields.io/badge/Status-Beta-8A2BE2?style=for-the-badge" alt="Beta" />
  <img src="https://img.shields.io/badge/License-Custom%20non--commercial-lightgrey?style=for-the-badge" alt="Custom non-commercial license" />
</p>

**Fullfud Shaded Mod by fullfud.** This branch is an unofficial fork — a port of that mod to
Fabric 1.21.4 with gameplay additions on top. It is not affiliated with, endorsed by, or supported
by `fullfud`.

> **Read before downloading.** The mod ships under a custom **non-commercial** license, not an
> open-source one. Personal play and non-commercial servers are permitted; any other monetization
> requires prior written consent from `fullfud`, and forks, ports and translations inherit these
> terms verbatim. See [License](#license).

## Table of Contents

- [Original mod](#original-mod)
- [What this fork changes](#what-this-fork-changes)
- [Features](#features)
- [Requirements](#requirements)
- [Build from source](#build-from-source)
- [In-game usage](#in-game-usage)
- [Keybinds and config](#keybinds-and-config)
- [Project structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)

## Original mod

|  |  |
| --- | --- |
| Mod | Shadedwar — mod id `fullfud`, in-game name `Shadedwar` |
| Author and copyright holder | `fullfud`, © 2025–2026 |
| Upstream repository | <https://github.com/fullfud/Shadedwar> |
| Original platform | Minecraft 1.20.1, Forge 47+, Java 17, GeckoLib 4.3.x |
| Attribution required by the license | *Fullfud Shaded Mod by fullfud* |

Everything the mod is built around — the concept, the drone models, textures, sounds, flight physics,
HUD and shader work — is fullfud's. The original Forge 1.20.1 build stays on the `main` branch of
this repository; the port and the changes below live on `fabric-port`.

## What this fork changes

### Platform

|  | Upstream (`main`) | This fork (`fabric-port`) |
| --- | --- | --- |
| Minecraft | 1.20.1 | 1.21.4 |
| Loader | Forge 47+ | Fabric Loader 0.19.5+ with Fabric API |
| Java | 17 | 21 |
| GeckoLib | 4.3.1 | 4.8.5 |
| Build | ForgeGradle, `reobfJar` | Fabric Loom 1.11.8, `remapJar` |
| Metadata | `META-INF/mods.toml` | `fabric.mod.json` |
| Widened access | access transformer | `fullfud.accesswidener` |
| Networking | one `SimpleChannel`, implicit sequential ids | `CustomPacketPayload` + `StreamCodec`, named payload ids |
| Item link data | item NBT tags | data components (`core/FullfudDataComponents`) |
| Pilot session state | Forge `Entity.getPersistentData()` | `core/data/PersistentData` |
| Config | `ForgeConfigSpec` | own TOML spec (`core/config/ConfigSpec`) |
| Drone camera | bytecode-patching JS coremod | `mixin/client/GameRendererMixin` |
| Chunk-loading library | `lattice` subproject, shipped jar-in-jar | vendored source under `dev/lazurite/lattice` |

Notes on the port itself:

- **lattice** — viewpoint-based chunk loading, which is what lets a remotely piloted drone load and
  render terrain around itself — is no longer a Gradle subproject or a nested jar. Its public API is
  unchanged, `impl/mixin/fix/` was rewritten against 1.21.4 internals, and its mixin configs ride the
  mod's own refmap. MIT, © 2021 Lazurite; see [LICENSE-lattice](LICENSE-lattice).
- The drone camera moved from a coremod that located vanilla's rotation block structurally to an
  ordinary mixin, so a mapping change now fails at load instead of silently disabling the FPV camera.
- Item ↔ drone links are data components rather than raw NBT, so controllers, goggles and monitors
  linked in the 1.20.1 build do not carry their links across.
- Forge-only mechanisms that needed replacing: the event bus subscribers, `DeferredRegister`, the
  persistent-data store, and game-rule registration (through the access widener, since Fabric API has
  no game-rule API).

### Gameplay added or reworked here

- **Survival progression.** Lithium ore with world generation, steel ingots, plates and blocks,
  copper wire, synth powder and a flight controller, wired into 39 recipes with loot tables and
  mining tags. Upstream shipped no recipes at all, so every device is now craftable.
- **Drone service bay.** A screwdriver opens a bay on a placed drone to fit a warhead (four charge
  tiers), Shahed fuel or a battery; on a REB emitter the same tool cycles its mode.
- **Cargo airframes.** FPV variants that carry storage slots instead of a charge.
- **20 advancements**, from the first drone through electronics and a long flight to a Shahed strike.
- **REB rework.** Detect mode only warns and no longer jams, with an on-screen caution/warning
  overlay and its own audio; battery drain is per mode — roughly two in-game days of listening or ten
  minutes of jamming on a full charge.
- **Recall of a distant FPV drone.** A drone parked in unloaded chunks is remembered in saved data
  and pulled back under a chunk ticket when you press the controller, instead of being reported
  missing and unlinked.
- **Post-blast lighting refresh**, so a Shahed warhead no longer leaves terrain black until
  something is placed there.
- **Shahed collision fix.** The airframe no longer slips into a building and hangs inside it; a hull
  held against terrain detonates instead.
- **Keybind hygiene.** Defaults moved off keys vanilla already owns (`Q`, `E` and `F` used to take
  drop-item, inventory and swap-offhand out of service), with a one-time migration of existing
  `options.txt` profiles.
- **Localization.** Full Russian and pre-reform Russian (`rpr`); partial German, Spanish, Japanese,
  Korean, Polish, Ukrainian, Simplified and Traditional Chinese.

Status: **beta**. Verification is a compile plus in-game testing — the project has no automated tests.

## Features

### FPV

- Three airframe presets — `7 Inch 6S`, `65mm Whoop 1S`, `7 Inch 6S Strike` — each in three signal
  tiers (base, `x2`, `x4` range and penetration), plus cargo variants
- Goggles and controller linking, arm/disarm, and detonation of an installed charge in flight
- Betaflight-style rate curves, RK4 integration and a blade-element rotor model
- MAX7456 OSD fonts, FPV post-processing with signal noise, engine and cockpit audio
- Gamepad support polled through GLFW, with a calibration screen that opens on first connect

### Shahed and FP5

- Shahed-136 in white and black, each also as a slow variant
- Shahed launcher and FP5 Flamingo launcher entities with monitor-driven remote flight
- Long-range control: tracking ranges up to 4096 blocks backed by drone-owned chunk tickets
- Warheads in four tiers, shrapnel, and distance-banded blast audio

### Electronic warfare

- REB emitter with detect and jam modes, powered by a rechargeable battery
- Signal degradation, drone-side warnings and failsafe recovery

## Requirements

- Minecraft 1.21.4
- Fabric Loader 0.19.5 or newer, Java 21
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [GeckoLib](https://modrinth.com/mod/geckolib) 4.8 or newer

The mod adds entities and its own networking, so it belongs on both the client and the server.

## Build from source

Needs a JDK 21 and network access for Gradle. On Windows use `gradlew.bat` in place of `./gradlew`.

```bash
./gradlew compileJava   # compile check only
./gradlew build         # full build -> build/libs/shadedwar-<version>.jar
./gradlew runClient     # development client
./gradlew runServer     # development server
```

`build`, `assemble`, `jar` and `remapJar` increment the tracked `build_number.properties`; the version
string is `${mod_base_version}-${mod_channel}.${buildNumber}` from `gradle.properties`. Use
`compileJava` when you only need to know whether the code compiles. There is no data-generation task
and no test source set.

## In-game usage

### FPV

1. Craft an FPV drone, an FPV controller and FPV goggles.
2. Place the drone and right-click it with the controller to link them; the link cascades to goggles
   found on your head, in your hands, or in your inventory.
3. The first time a gamepad is connected the calibration screen opens by itself — set the stick order
   and the arm input there, and reopen it later with the calibration keybind.
4. Optionally right-click the drone with a screwdriver to fit a warhead, or fly a cargo airframe.
5. Wear the goggles, hold the controller and use it to take control. Arm with `V`, detonate with `M`,
   leave with `Esc`.

A drone left behind in unloaded chunks can be entered again the same way: the controller loads its
last known position and hands control over once the chunk is live.

### Shahed and FP5

1. Place a launcher, then right-click it with a drone item to mount the drone on it.
2. Right-click the mounted drone with a screwdriver to fit fuel and a warhead.
3. Right-click the launcher with a monitor to link and launch, then use the monitor to fly. Shift +
   empty hand takes an unlaunched drone back off the launcher.

### REB

1. Place the emitter and give it a charged battery.
2. Right-click it with a screwdriver to switch between detect (warnings only) and jam.

## Keybinds and config

| Action | Default |
| --- | --- |
| FPV yaw left / right | `←` / `→` |
| FPV arm | `V` |
| FPV detonate charge | `M` |
| FPV controller calibration | unbound |
| Shahed power up / down | `R` / `G` |

Files under the game directory:

- `config/fullfud-client.toml` — roughly three dozen values in sections `fpv.controller`,
  `fpv.rawMapping`, `fpv.keyboard`, `fpv.camera`, `fpv.render`, `fpv.audio`, `shahed.render`,
  `shahed.audio`, `shahed.monitor`, `droneAudio` and `warnings`
- `config/fullfud-server.toml` — sections `world`, `flight` and `debug`: explosion block damage, and
  the choice between arcade flight and the simulated quadcopter
- `config/fullfud/fpv_controller.snbt` — gamepad calibration profile

## Project structure

- `src/main/java/com/fullfud/fullfud/client` — HUD and OSD, camera hooks, screens, gamepad input,
  renderers
- `src/main/java/com/fullfud/fullfud/common/entity` — drone, launcher and emitter entities
- `src/main/java/com/fullfud/fullfud/common/entity/drone` — flight physics, presets, service bay
  (loader-agnostic: no Fabric imports, and the tuning surface for flight feel)
- `src/main/java/com/fullfud/fullfud/core` — registries, networking, config, chunk loading, advancements
- `src/main/java/com/fullfud/fullfud/mixin` — the mod's own mixins
- `src/main/java/dev/lazurite/lattice` — vendored viewpoint chunk-loading library
- `src/main/resources` — assets, lang files, shaders, `data/` recipes and advancements,
  `fabric.mod.json`

## Contributing

Pull requests are welcome, especially for FPV physics parity, controller handling, bug fixes,
localization, and HUD or shader work.

1. Fork the repository and branch off `fabric-port`.
2. Keep changes focused and commit messages descriptive.
3. Open a pull request explaining what changed and why, with testing notes for anything that touches
   flight, camera, audio or networking — those cannot be verified by a compile alone.

Anything contributed here is subject to the same license as the rest of the project.

## License

This project inherits the custom **Fullfud Shaded War License** verbatim, as that license requires of
ports and forks. See [LICENSE](LICENSE) for the full text.

Short version:

- personal gameplay is allowed
- non-commercial servers are allowed, where income does not exceed actual hosting costs
- redistribution must include the license unchanged, charge no fees, state the non-commercial
  limitation up front, and attribute the mod as *Fullfud Shaded Mod by fullfud*
- derivative works — including this port — must inherit the license verbatim and carry the same
  attribution
- all other monetization requires prior written permission from `fullfud`
- "fullfud", "Shaded War" and related marks are fullfud's; nothing here implies endorsement

Third-party material bundled in the repository is **not** covered by that license and stays under its
own upstream terms:

- `dev/lazurite/lattice` — MIT, © 2021 Lazurite ([LICENSE-lattice](LICENSE-lattice))
- Betaflight Configurator `.mcm` OSD glyph resources — GPL-3.0
- `vcr_osd_mono.ttf` — upstream font terms

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for the documented list.
