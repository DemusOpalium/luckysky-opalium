![LuckySky Hauptbanner](docs/images/luckysky/banner/Lucky-banner01.png)

# LuckySky-Opalium

Paper-Plugin für Minecraft **1.21.10**.  
Steuert das LuckySky-Minigame: sichere Startplattform, automatische Lucky-Block-Spawns, Countdown/Rewards, Wither-Event, Wipes und vollständige In-Game-GUIs.

---

![Section Banner](docs/images/luckysky/banner/Lucky-Banner02.png)

## ⚙ Konfiguration & Provisioning
- **Zentrale Blaupause:** `game.yml` bündelt Spawnpunkte (Lobby/Plattform), Lucky-Block-Position, Rundendauer inkl. **Bossbar/Actionbar**, Rewards, One-Life, Spawn-Binding und **Wither-Mode**.
- **GUIs ohne Rebuild änderbar:** `admin-gui.yml`, `player-gui.yml`.
- **Provisioning-Workflow:** Template platzieren → Server starten → `/ls reload` lädt `game.yml`, GUIs und Weltdefinitionen.
- **AccessGate:** Lässt alle in **LOBBY** rein. **COUNTDOWN/RUN** nur für Whitelist oder Admin-Bypass.
- **Weltrotation/Lifecycle:** siehe [LuckySky-Weltrotation](docs/wiki/luckysky-weltrotation.md).

---

![Section Banner](docs/images/luckysky/banner/Lucky-banner03.png)

## 💡 Hauptfunktionen

| Symbol | Komponente | Kurzbeschreibung |
|:--:|:--|:--|
| ![Cmd](docs/images/luckysky/branding/icons/128x128/Command-Block.png) | **LuckyService** | Platziert periodisch den konfigurierten Lucky-Block an der Arena-Position. |
| ![Herz](docs/images/luckysky/branding/icons/128x128/Icon-Herz.png) | **RewardService** | Führt Winner/Fail-Befehle aus, startet 60-Sekunden-Endtimer, kehrt danach in **LOBBY** zurück. |
| ![Cmd](docs/images/luckysky/branding/icons/128x128/Command-Block.png) | **CountdownService** | Tickgenauer Rundentimer mit optionaler Bossbar und Actionbar. |
| ![Herz](docs/images/luckysky/branding/icons/128x128/Icon-Herz.png) | **AccessGate** | Regelt Eintritt ins LuckySky-World basierend auf **GameState** + Whitelist/Berechtigungen. |
| ![Cmd](docs/images/luckysky/branding/icons/128x128/Command-Block.png) | **Admin/Player-GUIs** | Start/Stop, Presets, Wipes, Plattform, Toggles, Reload, Teleports, Duels-Mapping. |

> Zusätzliche Systeme: **RespawnService** (One-Life/Spectator & korrekte Respawn-Routen), **WitherService** (verzögerter Spawn, Taunts), **ScoreboardService** (State/Timer/Players).

---

![Section Banner](docs/images/luckysky/banner/Lucky.Banner04.png)

## 🖼 Galerie · Aurora Blue & Sunrise Gold
<div align="center">
  <img src="docs/images/luckysky/banner/Lucky-banner01.png" width="85%" alt="Banner 01"/>
  <img src="docs/images/luckysky/banner/Lucky-Banner02.png" width="85%" alt="Banner 02"/>
  <img src="docs/images/luckysky/banner/Lucky-banner03.png" width="85%" alt="Banner 03"/>
  <img src="docs/images/luckysky/banner/Lucky.Banner04.png" width="85%" alt="Banner 04"/>
  <img src="docs/images/luckysky/banner/Lucky.Banner05.png" width="85%" alt="Banner 05"/>
</div>

---

![Section Banner](docs/images/luckysky/branding/banner/Banner-001.png)

## 🎬 Trailer · Aurora Blue & Sunrise Gold

<!--
Video-Hinweis:
- GitHub rendert Videos in READMEs zuverlässig über absolute .mp4-Links.
- Hier: Imgur-Direktlink (stabil) + optionaler Raw-GitHub-Fallback, falls die Datei im Repo <~10 MB ist.
-->

<figure>
  <video controls width="100%" preload="metadata"
         src="https://i.imgur.com/SczJEsW.mp4"
         poster="docs/images/luckysky/branding/logo/LuckySky-Logo4.png">
    <a href="https://i.imgur.com/SczJEsW.mp4">Download LuckySky-Catch.mp4</a>
  </video>
  <figcaption>
    <strong>Aurora Blue</strong> und <strong>Sunrise Gold</strong> prägen die visuelle Identität von LuckySky.
  </figcaption>
</figure>

<!-- Optionaler Fallback über Raw GitHub (nur aktiv lassen, wenn die Datei im Repo < 10 MB ist)
<details>
<summary>Alternativer Raw-GitHub-Stream</summary>

<video controls width="100%" preload="metadata"
       src="https://raw.githubusercontent.com/DEINUSERNAME/DEINREPO/main/docs/images/luckysky/branding/docs/LuckySky-Catch.mp4"
       poster="docs/images/luckysky/branding/logo/LuckySky-Logo4.png">
  <a href="https://raw.githubusercontent.com/DEINUSERNAME/DEINREPO/main/docs/images/luckysky/branding/docs/LuckySky-Catch.mp4">Download LuckySky-Catch.mp4</a>
</video>
</details>
-->

---

![Section Banner](docs/images/luckysky/banner/Lucky.Banner05.png)

## 🧭 Dokumentation / Wiki
- [NPC-Depot](docs/wiki/npc-depot.md)  
- [Admin- & Player-GUIs](docs/wiki/admin-player-guis.md)  
- [LuckySky-Weltrotation](docs/wiki/luckysky-weltrotation.md)  
- [Duels Crystal PvP Builder](docs/wiki/duels-crystal-pvp-builder.md)  
- [Fallen-Handbuch](docs/wiki/fallen-handbuch.md)  
- [Permissions & LuckPerms-Setups](docs/wiki/permissions.md)

---

![Section Banner](docs/images/luckysky/branding/banner/Banner-001.png)

## 🧱 Build
- **Java 21**, **Gradle 8.10+**, **Paper 1.21.10**  
- Build: `gradle build` → Artefakt: `build/libs/LuckySky-Opalium-0.1.0-SNAPSHOT.jar`  
- Keine externen Dependencies außer Paper/Bukkit.

---

![Section Banner](docs/images/luckysky/banner/Lucky-banner01.png)

## 📦 Status
Siehe `docs/plugin-status.md`.  
Spielbar: Services, Rewards, GUIs, AccessGate aktiv.  
Offen: Arena-Helper (Rig/Corridor-Automation, Warp-Signage), CI/Release-Automation.

---

<div align="center">
  <img src="docs/images/luckysky/branding/logo/LuckySky-Logo4.png" width="240" alt="LuckySky Logo 4"/>
  <p><strong>LuckySky · Opalium Haven Project</strong></p>
</div>
