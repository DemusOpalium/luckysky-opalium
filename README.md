![LuckySky Banner](docs/images/luckysky/branding/banner/Banner-001.png)

# LuckySky-Opalium

Paper 1.21.10 Plugin für das LuckySky-Minigame von Opalium.  
Bietet Spielsteuerung, Plattform-Utilities, automatische Lucky-Block-Spawns, Countdown-/Reward-Systeme, Wither-Events und vollständige In-Game-GUIs.

---

## ⚙ Konfiguration & Provisioning
- **Zentrale Blaupause:** `game.yml` bündelt Welt-Spawnpunkte, Lucky-Block-Position, Rundendauer (inkl. Bossbar / Actionbar), Rewards, Lives, Spawns und Wither-Mode.  
- **GUI-Layouts:** `admin-gui.yml` und `player-gui.yml` lassen sich ohne Rebuild ändern.  
- **Provisioning-Workflow:** Welt-Template bereitstellen, Server starten, dann `/ls reload` ausführen, damit LuckySky `game.yml`, GUIs und Weltdefinitionen lädt.  
- **AccessGate:** erlaubt Zutritt in *LOBBY*, sperrt COUNTDOWN / RUN für Nicht-Whitelist-Spieler.  
- **Weltrotation / Lifecycle:** siehe [LuckySky-Weltrotation](docs/wiki/luckysky-weltrotation.md).

---

## 💡 Hauptfunktionen

| Symbol | Komponente | Beschreibung |
|:--:|:--|:--|
| ![Gear](docs/images/luckysky/branding/icons/128x128/Icon-Rad.png) | **GameManager** | Koordiniert Start/Stop, lädt Welt, platziert Safe-Plattform, bindet Spieler, startet Lucky/Countdown/Wither-Services, verwaltet Teilnehmer. |
| ![CommandBlock](docs/images/luckysky/branding/icons/128x128/Command-Block.png) | **LuckyService** | Platziert periodisch den konfigurierten Lucky-Block-Typ an der Arena-Position. |
| ![Power](docs/images/luckysky/branding/icons/128x128/Icon-off.png) | **CountdownService** | Führt tick-genauen Rundentimer mit Bossbar / Actionbar aus und stoppt nach Ablauf. |
| ![Magic](docs/images/luckysky/branding/icons/128x128/Icon-Tool-Click-Magic.png) | **WitherService** | Verwaltet verzögerte Spawns, Taunt-Nachrichten und Aktivierungs-Toggles. |
| ![Heart](docs/images/luckysky/branding/icons/128x128/Icon-Herz.png) | **RewardService** | Führt Rewards/Fails aus, startet 60-Sekunden-Endtimer und setzt danach in LOBBY zurück. |
| ![Magic](docs/images/luckysky/branding/icons/128x128/Icon-Tool-Click-Magic.png) | **Admin GUI** | In-Game-Steuerung für Start/Stop, Zeitpresets, Wipes, Plattform, Toggles, Reload. |
| ![Heart](docs/images/luckysky/branding/icons/128x128/Icon-Herz.png) | **Duels Integration** | Verknüpft Lucky-Varianten mit Duels-Kits über GUI oder Commands. |

---

## 🗺 Struktur

