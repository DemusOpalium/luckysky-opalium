![LuckySky Hauptbanner](docs/images/luckysky/banner/Lucky-banner01.png)

# LuckySky-Opalium

Ein Paper-Plugin für Minecraft 1.21.10.  
LuckySky bündelt Startplattform, automatische Lucky-Block-Spawns, Countdown- und Reward-Systeme, Wither-Events, Respawns, Scoreboards und GUIs zu einem einzigen, modularen Minigame-Plugin.

---

## ⚙ Konfiguration & Provisioning <img src="docs/images/luckysky/branding/icons/128x128/Command-Block.png" alt="cfg" width="22"/>

- **Hauptdatei `game.yml`** – Spawns (Lobby/Plattform), Lucky-Block-Position, Rundendauer (Bossbar / Actionbar), Rewards, Lives, Wither-Modus  
- **GUIs:** `admin-gui.yml` und `player-gui.yml` anpassbar ohne Rebuild  
- **Setup:** Welt-Template bereitstellen → Server starten → `/ls reload`  
- **AccessGate:** LOBBY offen, COUNTDOWN/RUN nur Whitelist oder Admin  
- **Kompatibilität:** Multiverse-Core, LuckPerms, Duels, Citizens  
- **Welt-Lifecycle:** [Wiki → LuckySky-Weltrotation](docs/wiki/luckysky-weltrotation.md)

---

## 💡 Hauptfunktionen

- 🧭 **GameManager** – Steuert Start/Stop, Welt-Laden, Plattform, Player-Bind, Services  
- 🎲 **LuckyService** – Platziert periodisch den Lucky-Block  
- ⏱ **CountdownService** – Tickgenauer Rundentimer mit Bossbar/Actionbar  
- ❤️ **RewardService** – Gewinner/Fail-Befehle, Endtimer, Reset → Lobby  
- 👁 **RespawnService** – One-Life/Spectator, Respawn-Routing  
- 🚧 **AccessGate** – Join/WorldChange-Kontrolle, Zugangslogik  

**Weitere Systeme:** WitherService (Spawn + Taunts), ScoreboardService (State/Timer/Spieler/Wither), WipeService (Entity-Cleanup), NPCService (Teleports / Menüs), Duels-Integration (Kits per GUI), StateMachine (LOBBY → COUNTDOWN → RUN → ENDING → RESETTING)

---

![Abschnittsbanner](docs/images/luckysky/banner/Lucky-Banner02.png)

## 🎬 Gameplay-Vorschau

[![LuckySky-Catch Video](docs/images/luckysky/branding/logo/LuckySky-Logo4.png)](https://github.com/DemusOpalium/luckysky-opalium/blob/main/docs/images/luckysky/branding/docs/LuckySky-Catch.mp4?raw=true)  
*Klicke auf das Bild, um die MP4-Vorschau zu sehen (öffnet direkt auf GitHub).*

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

- **Java 21 · Gradle 8.10+ · Paper 1.21.10**  
- **Build:** `gradle build` → `build/libs/LuckySky-Opalium-0.1.0-SNAPSHOT.jar`  
- **Tests überspringen:** `gradle clean build -x test`  
- **Abhängigkeiten:** nur Paper/Bukkit  
- **IDE-Tipp:** IntelliJ IDEA oder VS Code mit Gradle-Plugin  

---

## 🧩 API & Erweiterbarkeit <img src="docs/images/luckysky/branding/icons/128x128/Icon-Herz.png" alt="api" width="20"/>

```java
GameManager game = LuckySkyPlugin.get().game();
game.start();                       // startet Runde
game.countdown().startMinutes(20);  // setzt Rundendauer
game.stop();                        // stoppt Runde
