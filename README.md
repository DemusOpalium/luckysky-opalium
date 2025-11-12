![LuckySky Hauptbanner](docs/images/luckysky/banner/Lucky-banner01.png)

# LuckySky-Opalium

Ein Paper-Plugin für Minecraft 1.21.10, das das LuckySky-Minigame steuert.  
Es bietet eine sichere Startplattform, automatische Lucky-Block-Spawns, Countdown- und Reward-Systeme, Wither-Events, Respawns, Scoreboards und vollständige In-Game-GUIs.  
Ideal für Minigame-Server – einfach zu konfigurieren und zu betreiben.

---

## ⚙ Konfiguration & Provisioning

- **Zentrale Konfigurationsdatei**: `game.yml` fasst alles zusammen: Spawnpunkte (Lobby, Plattform), Lucky-Block-Position, Rundendauer (mit Bossbar und Actionbar), Rewards, Lives, Spawn-Binding und Wither-Modus.
- **GUI-Anpassungen**: `admin-gui.yml` und `player-gui.yml` können ohne Neubau geändert werden.
- **Setup-Prozess**: Lege ein Welt-Template bereit, starte den Server und führe `/ls reload` aus – LuckySky lädt Konfigs, GUIs und Welt-Definitionen automatisch.
- **Zugangskontrolle (AccessGate)**: Erlaubt allen Zutritt zur Lobby; Countdown und Spielrunden nur für Whitelist-Spieler oder Admins.
- **Welt-Management**: Detaillierte Infos zur Rotation und Lifecycle in [LuckySky-Weltrotation](docs/wiki/luckysky-weltrotation.md).
- **Kompatibilität**: Unterstützt Multiverse-Core, LuckPerms, Duels und NPC-Plugins wie Citizens.

---

## 💡 Hauptfunktionen

| Icon | Komponente | Beschreibung |
|:--:|:--|:--|
| <img src="docs/images/luckysky/branding/icons/128x128/Command-Block.png" alt="Command Block Icon" width="64"> | **GameManager** | Koordiniert Spielstart/-ende, lädt Welten, platziert Plattformen, bindet Spieler und startet Services wie Lucky, Countdown und Wither. |
| <img src="docs/images/luckysky/branding/icons/128x128/Icon-Herz.png" alt="Herz Icon" width="64"> | **LuckyService** | Platziert regelmäßig den konfigurierten Lucky-Block an der Arena-Position. |
| <img src="docs/images/luckysky/branding/icons/128x128/Command-Block.png" alt="Command Block Icon" width="64"> | **RewardService** | Führt Gewinner-/Verlierer-Befehle aus, startet den 60-Sekunden-Endtimer und setzt alles auf Lobby zurück. |
| <img src="docs/images/luckysky/branding/icons/128x128/Icon-Herz.png" alt="Herz Icon" width="64"> | **CountdownService** | Präziser Timer mit optionaler Bossbar und Actionbar für Rundendauer. |
| <img src="docs/images/luckysky/branding/icons/128x128/Command-Block.png" alt="Command Block Icon" width="64"> | **RespawnService** | Handhabt One-Life-Modus, Spectator-Übergänge und korrekte Respawn-Pfade. |
| <img src="docs/images/luckysky/branding/icons/128x128/Icon-Herz.png" alt="Herz Icon" width="64"> | **AccessGate** | Überwacht Teleports, Weltwechsel und Join-Events, um unbefugte Spieler fernzuhalten. |

### 🧩 Weitere Systeme
- **WitherService:** Verzögerter Spawn, Taunts, Spawnmodi (Start, Timeout, Manuell).  
- **ScoreboardService:** Zeigt GameState, Timer, Spielerzahlen und Wither-Status in Echtzeit.  
- **WipeService:** Entfernt Resteinträge (ArmorStands, Clouds, FallingBlocks) nach jeder Runde.  
- **NPCService:** Bindet NPCs für Arena-Einstieg, Teleports oder Menüöffnungen.  
- **DuelsIntegration:** Erlaubt LuckySky-Duelle über `DuelsUI`, mit Kit-Zuordnung.  
- **StateMachine:** Kernlogik für Zustände (LOBBY, COUNTDOWN, RUN, ENDING, RESETTING).  

---

## 🖼 Galerie · Aurora Blue & Sunrise Gold

<div align="center">
  <img src="docs/images/luckysky/banner/Lucky-banner01.png" width="80%" alt="Banner 01" />
  <img src="docs/images/luckysky/banner/Lucky-Banner02.png" width="80%" alt="Banner 02" />
  <img src="docs/images/luckysky/banner/Lucky-banner03.png" width="80%" alt="Banner 03" />
  <img src="docs/images/luckysky/banner/Lucky.Banner04.png" width="80%" alt="Banner 04" />
  <img src="docs/images/luckysky/banner/Lucky.Banner05.png" width="80%" alt="Banner 05" />
</div>

---

## 🎬 Trailer · Aurora Blue & Sunrise Gold

<figure>
  <video 
    src="https://i.imgur.com/SczJEsW.mp4" 
    controls 
    poster="https://raw.githubusercontent.com/DEINUSERNAME/DEINREPO/main/docs/images/luckysky/branding/logo/LuckySky-Logo4.png" 
    width="100%" 
    preload="metadata">
    Dein Browser unterstützt keine Videos.
    <a href="https://i.imgur.com/SczJEsW.mp4">Download LuckySky-Catch.mp4</a>
  </video>
  <figcaption>
    <strong>Aurora Blue</strong> und <strong>Sunrise Gold</strong> definieren die visuelle Identität von LuckySky.
  </figcaption>
</figure>

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

- **Voraussetzungen:** Java 21, Gradle 8.10+, Paper 1.21.10.  
- **Build:** `gradle build` → `build/libs/LuckySky-Opalium-0.1.0-SNAPSHOT.jar`.  
- **Abhängigkeiten:** Nur Paper API und Bukkit (keine externen Libs).  
- **Testhinweis:** `gradle clean build -x test` falls Maven-Blocker auftreten.  
- **Empfohlene IDEs:** IntelliJ IDEA oder VSCode mit Gradle-Plugin.  

---

## 🧩 API & Erweiterbarkeit

LuckySky-Opalium kann durch eigene Addons erweitert werden.  
Entwickler können auf den zentralen `GameManager` und `StateMachine` zugreifen:

```java
GameManager game = LuckySkyPlugin.get().game();
game.start();
game.stop();
game.countdown().startMinutes(20);
