# LuckySky-Opalium

Paper plugin providing the core LuckySky minigame controls for Opalium Haven.

> **Status:** ✅ READY – v0.2.0 Core with functional session control, safe-platform tooling, wipes, Wither events, taunts, and optional info sign.

## Requirements
- Paper 1.21.1 (tested on 1.21.10 runtime)
- Java 21 (Temurin recommended)
- Optional: Multiverse-Core for dedicated worlds, LuckPerms for permissions

## Installation
1. Download the latest `LuckySky-Opalium-0.2.0.jar` from the GitHub Actions artifacts.
2. Drop the jar into your server's `plugins/` directory.
3. Start or reload the server (`/plugman reload LuckySky-Opalium` or restart) and watch the console for `LuckySky-Opalium v0.2.0 enabled.`
4. Adjust `config.yml` and `messages.yml` under `plugins/LuckySky-Opalium/` if you need to change coordinates, materials, or texts.

## Commands
All commands require the `luckysky.admin` permission and can be triggered with `/ls` or `/luckysky`.

| Command | Description |
| --- | --- |
| `/ls start` | Starts a LuckySky session, rebuilds the safe platform, and launches the session timer. |
| `/ls stop` | Stops the running session and clears the timer. |
| `/ls reset` | Hard wipe: removes items, clouds, armor stands, and Wither entities in the configured radius. |
| `/ls clean` | Soft wipe: removes drops, clouds, and ghost armor stands. |
| `/ls plat` | Builds the 3+1 safe platform pattern. |
| `/ls plat+` | Expands the platform to the 3×3 layout. |
| `/ls corridor` | Clears the configurable corridor around the platform. |
| `/ls bind` | Teleports online players in the LuckySky world onto the platform and binds their spawn. |
| `/ls mode5`, `/ls mode20`, `/ls mode60` | Sets the session timer to 5 / 20 / 60 minutes. |
| `/ls wither` | Spawns the Abyssal Wither if enabled and not already active. |
| `/ls taunt_on`, `/ls taunt_off` | Enables or disables the Wither taunt scheduler (titles/actionbar/sounds). |
| `/ls sign` | Places the configurable info sign on the platform edge. |

## Permissions
| Permission | Default | Description |
| --- | --- | --- |
| `luckysky.admin` | `op` | Access to all LuckySky control commands. |

## Configuration Highlights (`config.yml`)
- `world`: Target world name (default `LuckySky`).
- `platform`: Y-level, center offsets, materials, and sizes for the safe platform.
- `corridor`: Radius and height for the air corridor around the start area.
- `wipes`: Entity filters and search distances for soft/hard wipes.
- `session`: Allowed timer presets and default mode.
- `wither`: Spawn location and taunt options.
- `sign`: Toggle and customize the info sign placement and text.

Messages are stored in `messages.yml` (German defaults) and can be localized freely.

## Building From Source
```bash
gradle build
```
The build produces `build/libs/LuckySky-Opalium-0.2.0.jar`.

## Status & Roadmap
The v0.2.0 core release provides working commands, session handling, platform utilities, and Wither management. Upcoming work (GUI, scoreboard integration, Multiverse teleports) and the manual acceptance checklist are tracked in [`docs/plugin-status.md`](docs/plugin-status.md).
