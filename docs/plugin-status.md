# LuckySky-Opalium Plugin Status (Paper 1.21.1+)

## Build & Compatibility
- **Gradle project builds successfully** using the default `gradle build` task, producing `build/libs/LuckySky-Opalium-0.1.0-SNAPSHOT.jar`.
- **API target:** `api-version: "1.21"` in `plugin.yml`, aligned with Paper 1.21.1.
- **Java requirement:** Code uses only the Bukkit API and compiles under Java 21; no third-party dependencies.

## Implemented Functionality
- `GameManager` coordinates start/stop flows: ensures the arena world is loaded, places the safe platform, binds online players, switches them to Survival, launches Lucky/Duration/Wither services, and tracks active participants.
- `LuckyService` periodically places the configured Lucky Block variant at the arena coordinates, optionally requiring an air block before triggering the external command.
- `DurationService` provides the default/preset round timers and stops the match once the countdown finishes.
- `WitherService` manages delayed spawns, taunt broadcasts, and manual/automatic toggles for both features.
- `WipeService` exposes soft/hard clear routines that remove lingering entities (area-effect clouds, falling blocks, armor stands, etc.) near the Lucky Block.
- `RewardsService` executes configured reward/fail command lists for the winner or all participants when the Wither dies or the timer expires.
- Admin GUI (`AdminGui`) offers in-game controls for the above systems: start/stop, time presets, taunt/wither toggles, platform placement, wipes, spawn binding, Lucky variant cycling, teleport, and config save/reload.
- Duels integration (`DuelsManager` + `/duelsui`) loads menu templates from the config, opens a kit-selection GUI, and maps Lucky variants to Duels kit commands (with dependency checks/bypass for admins).
- Event listeners (`BossListener`, `PlayerListener`) convert Wither deaths, player deaths, and respawns into `GameManager` callbacks (reward triggers, spectator mode on one-life runs, participant reactivation).

## In Arbeit / Offene Punkte
- Arena helpers mentioned in legacy tooling (rig setup, corridor clean-up, warp signage) are not implemented yet.
- Automation for releases/CI (Gradle wrapper, GitHub Actions, etc.) is still missing.

## Recommendation
The repository now ships a playable LuckySky experience with functioning services, wipes, rewards, and GUIs. Deploy on a Paper 1.21.1+ server, then continue implementing the outstanding arena helper utilities and delivery automation to reach full parity with production workflows.
