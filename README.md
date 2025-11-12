![LuckySky Hauptbanner](docs/images/luckysky/banner/Lucky-banner01.png)

# LuckySky-Opalium

⚠ **Worlds-Konfiguration (`worlds.yml`)**

Beim ersten Start legt LuckySky automatisch  
`/plugins/LuckySky/worlds.yml` an.  
In manchen Setups kann der Spawn zu niedrig (Y = 80) sein → Spieler fallen ins Void.  

**Fix:**  
Ersetze den Inhalt der Datei `worlds.yml` mit:

```yaml
luckySky:
  worldName: "LuckySky"
  spawn:
    x: 0.0
    y: 101.0
    z: 2.0
    yaw: 180.0
    pitch: 0.0
  # Lobby wird extern verwaltet (Multiverse/Essentials)
  # lobby:
  #   x: 0.0
  #   y: 101.0
  #   z: 2.0
  #   yaw: 180.0
  #   pitch: 0.0
  lucky:
    startBanner: "§aLuckySky läuft – break the blocks!"
    require_air_at_target: true

duels:
  worldName: "duels"
  lobby:
    x: 1.0
    y: -56.0
    z: 0.0
    yaw: 0.0
    pitch: 0.0
  protection_radius: 24

  ```

Ein Paper-Plugin für Minecraft 1.21.10, entstanden aus Leidenschaft, Neugier und dem Wunsch, etwas Eigenes zu schaffen.
LuckySky vereint Startplattform, automatische Lucky-Block-Spawns, Countdown- und Reward-Systeme, Wither-Events, Respawns, Scoreboards und grafische GUIs in einem modernen, modularen Minigame-System.

💙 Projekt & Idee

LuckySky-Opalium ist ein freies Community-Projekt, das auf dem Server mcopalium.de entwickelt und getestet wird.
Die Idee entstand aus einem alten YouTube-Video über Lucky Skyblock – zwei Spieler auf einem schwebenden Block, ständig neue Lucky Blocks, voller Chaos und Spaß.
Keine aktuelle Paper-Version bot so etwas. Also wurde beschlossen:
„Dann bauen wir es selbst.“

Ziele:

Einfaches, stabiles Minigame für Freunde und Gäste

Modularer Aufbau, klar konfigurierbar

Erweiterbar mit Duels, NPCs und Multiverse

Open Source für alle, die basteln wollen

„LuckySky ist kein Produkt – es ist ein Zuhause für Ideen.“

⚙ Konfiguration & Provisioning

Hauptdatei game.yml – Spawns, Lucky-Block-Position, Rundendauer, Rewards, Lives, Wither-Modus

GUIs: admin-gui.yml & player-gui.yml – anpassbar ohne Rebuild

Setup: Welt-Template bereitstellen → Server starten → /ls reload

AccessGate: Lobby offen, Countdown/Run nur Whitelist oder Admin

Kompatibilität: Multiverse-Core, LuckPerms, Duels, Citizens

Welt-Lifecycle: siehe Wiki → LuckySky-Weltrotation

✨ Hauptfunktionen

GameManager – Steuert Start/Stop, lädt Welten, setzt Plattformen, verwaltet Spieler und Services

LuckyService – Platziert periodisch den konfigurierten Lucky Block

CountdownService – Tickgenauer Rundentimer mit Bossbar/Actionbar

RewardService – Führt Sieger-/Verlierer-Befehle aus

RespawnService – One-Life-Modus, Spectator-Übergänge

AccessGate – Kontrolliert Join/Teleport/WorldChange

Weitere Systeme:
WitherService, ScoreboardService, WipeService, NPCService,
Duels-Integration, StateMachine (LOBBY → COUNTDOWN → RUN → ENDING → RESETTING)

📚 Dokumentation · Wiki

NPC-Depot

Admin- & Player-GUIs

LuckySky-Weltrotation

Duels Crystal PvP Builder

Fallen-Handbuch

Permissions & LuckPerms-Setups

🧱 Build & Entwicklung

Java 21 · Gradle 8.10+ · Paper 1.21.10

Build: gradle build → build/libs/LuckySky-Opalium-<version>.jar

Tests überspringen: gradle clean build -x test

Abhängigkeiten: Nur Paper / Bukkit

Empfohlene IDE: IntelliJ IDEA oder VS Code

🧩 API & Erweiterbarkeit


GameManager game = LuckySkyPlugin.get().game();
game.start();                       // Startet die Runde
game.countdown().startMinutes(20);  // Setzt die Rundendauer
game.stop();                        // Stoppt die Runde



---

✅ Kopiere das **komplett** so in dein GitHub-README.  
Dann wird **nur der YAML-Block und der Java-Block** formatiert, alles andere bleibt normal.
