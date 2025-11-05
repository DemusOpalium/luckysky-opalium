# LuckySky-Opalium Plugin Status (Paper 1.21.1+)

## Release: v0.2.0 Core
- ✅ Builds via `gradle build`, producing `build/libs/LuckySky-Opalium-0.2.0.jar`.
- ✅ Targets `api-version: "1.21"` and Java 21.
- ✅ Implements full `/ls` command suite with working platform, corridor, wipe, timer, bind, Wither, taunt, and sign features.
- ✅ Provides configurable `config.yml` and `messages.yml` with German defaults.
- ✅ Wither taunt scheduler (titles/actionbar/sounds) controlled via commands and config.

## Known Limitations / Next Steps
- ⏳ GUI & inventory control panel for operators.
- ⏳ Scoreboard / bossbar timer presentation for players.
- ⏳ Multiverse teleport helpers and automatic world loading.
- ⏳ LuckPerms example configuration and documentation snippets.
- ⏳ Automated tests and gameplay verification harnesses.

## Test Checklist (Manual)
1. Deploy on Paper 1.21.10 with Java 21.
2. `/ls plat`, `/ls plat+`, `/ls corridor` create the configured structure and corridor.
3. `/ls bind` moves and binds online players to the platform spawn.
4. `/ls mode5`, `/ls mode20`, `/ls mode60` adjust timer presets.
5. `/ls start` begins the timer and keeps the platform safe; `/ls stop` halts it.
6. `/ls clean` performs a soft wipe (items/clouds/armor stands). `/ls reset` performs hard wipe including Wither despawn.
7. `/ls wither` spawns the Wither (one at a time). `/ls taunt_on` & `/ls taunt_off` toggle taunts.
8. `/ls sign` places the info sign.

All deviations or edge cases should be documented in future changelog entries.
