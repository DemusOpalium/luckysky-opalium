![LuckySky Hauptbanner](docs/images/luckysky/banner/Lucky-banner01.png)

# LuckySky-Opalium

Ein Paper-Plugin für Minecraft 1.21.10, das das LuckySky-Minigame steuert.  
Sichere Startplattform, automatische Lucky-Block-Spawns, Countdown-/Reward-Systeme, Wither-Events, Respawns, Scoreboards und vollständige In-Game-GUIs.

---

## ⚙ Konfiguration & Provisioning <img src="docs/images/luckysky/branding/icons/128x128/Command-Block.png" alt="cfg" width="24" />

- **Zentrale Datei**: `game.yml` (Lobby/Plattform-Spawns, Lucky-Block-Position, Rundendauer inkl. Bossbar/Actionbar, Rewards, One-Life, Spawn-Binding, Wither-Modus).
- **GUIs ohne Rebuild**: `admin-gui.yml`, `player-gui.yml`.
- **Setup**: Welt-Template bereitstellen → Server starten → `/ls reload`.
- **AccessGate**: LOBBY offen; COUNTDOWN/RUN nur Whitelist oder Admin-Bypass.
- **Kompatibilität**: Multiverse-Core, LuckPerms, Duels, Citizens.
- **Welt-Lifecycle**: Details im Wiki → [LuckySky-Weltrotation](docs/wiki/luckysky-weltrotation.md).

---

## 💡 Hauptfunktionen

- 🧭 **GameManager** – Start/Stop, Welt laden, Plattform setzen, Spieler binden, Lucky/Countdown/Wither starten.
- 🎲 **LuckyService** – platziert periodisch die konfigurierte Lucky-Variante am Arena-Punkt.
- ⏱ **CountdownService** – tickgenauer Rundentimer, optional Bossbar/Actionbar.
- ❤️ **RewardService** – Gewinner-/Fail-Befehle, 60-Sekunden Endtimer, Rückkehr in LOBBY.
- 👁 **RespawnService** – One-Life/Spectator-Übergänge, saubere Respawns.
- 🚧 **AccessGate** – Teleports/Join/WorldChange prüfen und unbefugte Spieler umlenken.

**Weitere Systeme:** WitherService (Start/Timeout/Manuell + Taunts), ScoreboardService (State/Timer/Spieler/Wither-Status), WipeService (Clouds/FallingBlocks/ArmorStands), NPCService (Einstieg/Teleports/Menüs), Duels-Integration (Kits per GUI), StateMachine (LOBBY/COUNTDOWN/RUN/ENDING/RESETTING).

---

![Abschnittsbanner](docs/images/luckysky/banner/Lucky-Banner02.png)

## 🎬
<video src="https://<DEIN_USERNAME>.github.io/<DEIN_REPO>/media/LuckySky-Catch.mp4"
       controls width="100%" preload="metadata">
  <a href="https://<DEIN_USERNAME>.github.io/<DEIN_REPO>/media/LuckySky-Catch.mp4">Download</a>
</video>


---

## 🧭 Dokumentation / Wiki

- [NPC-Depot](docs/wiki/npc-depot.md)  
- [Admin- & Player-GUIs](docs/wiki/admin-player-guis.md)  
- [LuckySky-Weltrotation](docs/wiki/luckysky-weltrotation.md)  
- [Duels Crystal PvP Builder](docs/wiki/duels-crystal-pvp-builder.md)  
- [Fallen-Handbuch](docs/wiki/fallen-handbuch.md)  
- [Permissions & LuckPerms-Setups](docs/wiki/permissions.md)

---

## 🧱 Build & Entwicklung

- **Voraussetzungen:** Java 21, Gradle 8.10+, Paper 1.21.10  
- **Build:** `gradle build` → `build/libs/LuckySky-Opalium-0.1.0-SNAPSHOT.jar`  
- **Tests umgehen bei Repo-Blockern:** `gradle clean build -x test`  
- **Abhängigkeiten:** nur Paper/Bukkit

---

## 🧩 API & Erweiterbarkeit <img src="docs/images/luckysky/branding/icons/128x128/Icon-Herz.png" alt="api" width="20" />

```java
GameManager game = LuckySkyPlugin.get().game();
game.start();                       // Startet Runde
game.countdown().startMinutes(20);  // Setzt Rundendauer
game.stop();                        // Stoppt Runde
