![LuckySky Hauptbanner](docs/images/luckysky/banner/Lucky-banner01.png)

# 🟣 LuckySky-Opalium

Ein Paper-Plugin für **Minecraft 1.21.10**, entstanden aus Leidenschaft, Neugier und dem Wunsch, etwas Eigenes zu schaffen.  
LuckySky vereint Startplattform, automatische Lucky-Block-Spawns, Countdown- und Reward-Systeme, Wither-Events, Respawns, Scoreboards und grafische GUIs in einem modernen, modularen Minigame-System.

---

## ⚠ Worlds-Konfiguration (`worlds.yml`)

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
💙 Projekt & Idee

LuckySky-Opalium ist ein freies Community-Projekt, entwickelt und getestet auf mcopalium.de
.
Die Idee stammt aus einem alten Lucky Skyblock-Video – zwei Spieler auf einem schwebenden Block, ständig neue Lucky Blocks, pures Chaos.
Da keine aktuelle Paper-Version so etwas bot, hieß es: „Dann bauen wir es selbst.“

Ziele:

Einfaches, stabiles Minigame für Freunde und Gäste

Modularer Aufbau, klar konfigurierbar

Erweiterbar mit Duels, NPCs und Multiverse

Open Source – zum Lernen und Experimentieren

„LuckySky ist kein Produkt – es ist ein Zuhause für Ideen.“
```yaml
| System               | Beschreibung                                                                                                   |
| :------------------- | :------------------------------------------------------------------------------------------------------------- |
| **GameManager**      | Start/Stop, Weltverwaltung, Plattform, Spielersteuerung                                                        |
| **LuckyService**     | Periodischer Lucky-Block-Spawn                                                                                 |
| **CountdownService** | Tickgenauer Rundentimer mit Bossbar / Actionbar                                                                |
| **RewardService**    | Sieger- / Verlierer-Befehle                                                                                    |
| **RespawnService**   | One-Life-Modus, Spectator-Übergänge                                                                            |
| **AccessGate**       | Kontrolliert Join / Teleport / WorldChange                                                                     |
| **Weitere**          | WitherService, ScoreboardService, WipeService, NPCService, Duels-Integration, StateMachine (LOBBY → RUN → END) |
  ```


    🔹 Einfaches, stabiles Minigame für Freunde und Gäste
    🔹 Modularer Aufbau, klar konfigurierbar
    🔹 Erweiterbar mit Duels, NPCs und Multiverse
    🔹 Open Source für alle, die basteln wollen

    „LuckySky ist kein Produkt – es ist ein Zuhause für Ideen.“

✨ Hauptfunktionen
Feature	Beschreibung
GameManager	Steuert Start/Stop, lädt Welten, setzt Plattformen, verwaltet Spieler & Services
LuckyService	Platziert periodisch den konfigurierten Lucky Block
CountdownService	Tickgenauer Rundentimer mit Bossbar/Actionbar
RewardService	Führt Sieger-/Verlierer-Befehle aus
RespawnService	One-Life-Modus, Spectator-Übergänge
AccessGate	Kontrolliert Join/Teleport/WorldChange
Weitere	WitherService, ScoreboardService, WipeService, NPCService, Duels-Integration, StateMachine (LOBBY → COUNTDOWN → RUN → ENDING → RESETTING)
⚙️ Konfiguration & Setup
📁 Hauptdateien

    game.yml – Spawns, Lucky-Block-Position, Rundendauer, Rewards, Lives, Wither-Modus
    admin-gui.yml & player-gui.yml – Anpassbar ohne Rebuild

🚀 Schneller Setup

    Welt-Template bereitstellen
    Server starten
    /ls reload

AccessGate: Lobby offen, Countdown/Run nur Whitelist oder Admin
Kompatibilität: Multiverse-Core, LuckPerms, Duels, Citizens
Welt-Lifecycle: Siehe Wiki → LuckySky-Weltrotation

🧱 Build & Entwicklung
```yaml
# Build ohne Tests
gradle clean build -x test

# Ausgabe:
build/libs/LuckySky-Opalium-<version>.jar

  ```
| Komponente         | Version                    |
| :----------------- | :------------------------- |
| **Java**           | 21                         |
| **Gradle**         | 8.10 +                     |
| **Server**         | Paper 1.21.10              |
| **Abhängigkeiten** | Nur Paper / Bukkit         |
| **IDE**            | IntelliJ IDEA oder VS Code |

```yaml
GameManager game = LuckySkyPlugin.get().game();
game.start();                       // Startet die Runde
game.countdown().startMinutes(20);  // Setzt die Rundendauer
game.stop();                        // Stoppt die Runde
  ```
📚 Dokumentation · Wiki

NPC-Depot

Admin- & Player-GUIs

LuckySky-Weltrotation

Duels Crystal PvP Builder

Fallen-Handbuch

Permissions & LuckPerms-Setups

🤝 Contributing

Fork das Repo

Branch erstellen → feature/NeuesFeature

Änderungen committen → git commit -m "Add feature"

Push → git push origin feature/NeuesFeature

Pull Request eröffnen

📄 License

Dieses Projekt steht unter der MIT License
.

⭐ Star das Repo, wenn es dir gefällt.
💬 Fragen? → Issues öffnen
 oder mcopalium.de

 
```yaml

Das ist die fertige Version:  
- **Interne Links** funktionieren (z. B. `docs/wiki/...`)  
- **Nur YAML- und Java-Abschnitte** sind als kopierbare Codeblöcke formatiert  
- Markdown rendert korrekt auf GitHub  
- Keine übergroße Schrift, keine Layout-Fehler.
  ```
