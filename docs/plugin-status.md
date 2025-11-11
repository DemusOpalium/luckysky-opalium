# LuckySky-Opalium Plugin Status (Paper 1.21.1+)

![LuckySky primary logo rendered in teal and purple gradients](./images/luckysky/branding/branding/logo/LuckySky-Logo2.png)

![LuckySky gradient divider banner in purple and blue](./images/luckysky/branding/branding/banner/Banner-001.png)

## Build & Compatibility
- **Gradle project builds successfully** using the default `gradle build` task, producing `build/libs/LuckySky-Opalium-0.1.0-SNAPSHOT.jar`.
- **API target:** `api-version: "1.21"` in `plugin.yml`, aligned with Paper 1.21.1.
- **Java requirement:** Code uses only the Bukkit API and compiles under Java 21; no third-party dependencies.

![LuckySky gradient divider banner in purple and blue](./images/luckysky/branding/branding/banner/Banner-001.png)

## Implemented Functionality

### ![Gear icon representing GameManager orchestration](./images/luckysky/branding/branding/icons/128x128/Icon-Rad.png) GameManager
Coordinates start/stop flows: ensures the arena world is loaded, places the safe platform, binds online players, switches them to Survival, launches Lucky/Countdown/Wither services, and tracks active participants.

### ![Command block icon representing Lucky block spawning](./images/luckysky/branding/branding/icons/128x128/Command-Block.png) LuckyService
Periodically places the configured Lucky Block variant at the arena coordinates, optionally requiring an air block before triggering the external command.

### ![Power symbol icon highlighting countdown control](./images/luckysky/branding/branding/icons/128x128/Icon-off.png) CountdownService
Provides tick-accurate round timers for the configured presets, optionally mirroring the countdown via bossbar/actionbar while halting the match once the timer finishes.

### ![Magic wand icon representing Wither event effects](./images/luckysky/branding/branding/icons/128x128/Icon-Tool-Click-Magic.png) WitherService
Manages delayed spawns, taunt broadcasts, and manual/automatic toggles for both features.

### ![Gear icon depicting arena cleanup automation](./images/luckysky/branding/branding/icons/128x128/Icon-Rad.png) WipeService
Exposes soft/hard clear routines that remove lingering entities (area-effect clouds, falling blocks, armor stands, etc.) near the Lucky Block.

### ![Heart icon showcasing player reward flows](./images/luckysky/branding/branding/icons/128x128/Icon-Herz.png) RewardService
Executes configured reward/fail command lists for the winner or all participants when the Wither dies or the timer expires, then manages a 60-second end timer before resetting the arena.

### ![Magic wand icon highlighting interactive admin controls](./images/luckysky/branding/branding/icons/128x128/Icon-Tool-Click-Magic.png) Admin GUI
Provides in-game controls for the above systems: start/stop, time presets, taunt/wither toggles, platform placement, wipes, spawn binding, Lucky variant cycling, teleport, and config save/reload.

### ![Heart icon representing PvP duel matchmaking](./images/luckysky/branding/branding/icons/128x128/Icon-Herz.png) Duels Integration
Loads menu templates from the config, opens a kit-selection GUI, and maps Lucky variants to Duels kit commands (with dependency checks/bypass for admins).

### ![Power symbol icon emphasizing reactive listeners](./images/luckysky/branding/branding/icons/128x128/Icon-off.png) Event Listeners
Convert Wither deaths, player deaths, and respawns into `GameManager`/service callbacks (reward triggers, AccessGate state updates, spectator mode on one-life runs, participant reactivation).

![LuckySky gradient divider banner in purple and blue](./images/luckysky/branding/branding/banner/Banner-001.png)

## In Arbeit / Offene Punkte
- Arena helpers mentioned in legacy tooling (rig setup, corridor clean-up, warp signage) are not implemented yet.
- Automation for releases/CI (Gradle wrapper, GitHub Actions, etc.) is still missing.

![LuckySky gradient divider banner in purple and blue](./images/luckysky/branding/branding/banner/Banner-001.png)

## Recommendation
The repository now ships a playable LuckySky experience with functioning services, wipes, rewards, and GUIs. Deploy on a Paper 1.21.1+ server, then continue implementing the outstanding arena helper utilities and delivery automation to reach full parity with production workflows.
