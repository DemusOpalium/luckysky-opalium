![LuckySky banner in Aurora Blue and Sunrise Gold](docs/images/luckysky/banner/Lucky-banner01.png)

# LuckySky-Opalium
 Paper plugin for Opalium's LuckySky minigame. Provides match control, safe-platform utilities, Lucky Block automation, timers, Wither events, wipes, rewards, and in-game administration GUIs.

## Features
- **Match lifecycle:** `/ls start` prepares the world, builds the safe platform, binds online players to the configured respawn, switches them to Survival, and launches the running timers/services. `/ls stop` (or defeating the boss/timeout) cleanly halts the round while keeping protection blocks intact.
- **Safe platform builder:** Platform presets from `config.yml` are placed via commands or the admin GUI, including optional 3×3 extensions for the landing area.
- **Lucky Block spawner:** `LuckyService` periodically places the configured Lucky Block variant at the arena coordinates, respecting air-only placement when desired.
- **Round timer:** `DurationService` tracks the configured duration or preset minute modes and stops the game when the countdown elapses.
- **Wither encounter:** `WitherService` handles delayed spawns, broadcast taunts, manual spawn triggers, and toggles for both features.
- **Arena wipes:** Soft and hard wipe routines remove lingering entities around the Lucky Block radius, including armor stand limits.
- **Rewards engine:** Command lists run on boss victory or failure, supporting winner-only or everyone payout modes.
- **Admin & Duels GUIs:** The LuckySky Admin GUI exposes all match controls in-game, while the optional Duels GUI/command maps Lucky variants to configured Duels kits.

## 🎬 Trailer · Aurora Blue & Sunrise Gold
<figure>
  <video src="docs/images/luckysky/branding/branding/docs/LuckySky-Catch.mp4" controls poster="docs/images/luckysky/banner/Lucky-banner03.png">
    Your browser does not support the video tag. You can download it directly via
    <a href="docs/images/luckysky/branding/branding/docs/LuckySky-Catch.mp4">LuckySky-Catch.mp4</a>.
  </video>
  <figcaption>
    <strong>Aurora Blue</strong> gradients and <strong>Sunrise Gold</strong> flares frame the LuckySky catch sequence.
  </figcaption>
</figure>

## Configuration Notes
- `admin-gui.yml` command buttons must declare a non-empty `commands:` list. Buttons without runnable commands are ignored when the layout is loaded.

## In Arbeit
- Arena quality-of-life helpers such as rig/corridor/sign automation remain TODO and require implementation before parity with legacy tooling.
- Continuous integration/packaging automation is not yet part of the repository (no Gradle wrapper or CI workflows provided).

## Build
Requires Java 21 and Gradle. Run `gradle build` to produce `build/libs/LuckySky-Opalium-0.1.0-SNAPSHOT.jar`.

> _#in work  Start 5.11.2025  +  (GPT Test CODEX)_
