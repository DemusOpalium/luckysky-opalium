# LuckySky-Opalium Plugin Status (Paper 1.21.1+)

## Build & Compatibility
- **Gradle project builds successfully** using the default `gradle build` task, producing `build/libs/LuckySky-Opalium-0.1.0-SNAPSHOT.jar`.
- **API target:** `api-version: "1.21"` in `plugin.yml`, aligned with Paper 1.21.1.
- **Java requirement:** Code uses only the Bukkit API and compiles under Java 21; no third-party dependencies.

## Implemented Functionality
- Plugin currently registers the `/ls` command and checks the `opalium.luckysky.admin` permission.
- Each subcommand responds with a localized "TODO implement" placeholder message.
- No gameplay logic, world manipulation, timers, GUI, or Wither event systems are implemented yet.

## Missing Features Before Production Use
- Start/stop/reset routines, platform builders, entity wipes, timers, and spawn binding still need full implementations.
- Planned systems (GUI, scoreboard, Wither taunts, Multiverse/LuckPerms hooks) are absent.
- Gradle wrapper (`./gradlew`) is not committed, so servers without global Gradle must install it manually.

## Recommendation
At this stage the repository produces a **loadable but placeholder plugin**. It is suitable for early command testing but **not yet ready** for production deployment on a Paper 1.21.1 server. Complete the TODO implementations and polish configuration/documentation before releasing to players.
