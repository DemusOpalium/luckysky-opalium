![LuckySky Hauptbanner](docs/images/luckysky/banner/Lucky-banner01.png)

# LuckySky-Opalium

Ein Paper-Plugin für **Minecraft 1.21.10**, entstanden aus Leidenschaft, Neugier und dem Wunsch, etwas Eigenes zu schaffen.  
LuckySky vereint Startplattform, automatische Lucky-Block-Spawns, Countdown- und Reward-Systeme, Wither-Events, Respawns, Scoreboards und grafische GUIs in einem modernen, modularen Minigame-System.

---

## 💙 Projekt & Idee

LuckySky-Opalium ist ein freies Community-Projekt, das auf dem Server **mcopalium.de** entwickelt und getestet wird.  
Die Idee entstand aus einem alten YouTube-Video über *Lucky Skyblock* – zwei Spieler auf einem schwebenden Block, ständig neue Lucky Blocks, voller Chaos und Spaß.  
Doch keine aktuelle Paper-Version bot so etwas. Also wurde beschlossen:  
**„Dann bauen wir es selbst.“**

Anfangs war es nur ein Versuch mit Command-Blöcken.  
Dann kam die Erkenntnis: das braucht ein richtiges Plugin.  
Heute ist LuckySky-Opalium ein wachsendes System mit klarer Struktur und offenem Code – entwickelt aus Freude am Lernen, nicht für Profit.

Ziele:
- ein einfaches, stabiles Minigame für Freunde und Gäste  
- modularer Aufbau, klar konfigurierbar  
- erweiterbar mit Duels, NPCs und Multiverse-Welten  
- Open Source und zugänglich für jeden, der basteln will  

> *„LuckySky ist kein Produkt – es ist ein Zuhause für Ideen.“*

---

![Banner Abschnitt](docs/images/luckysky/banner/Lucky-Banner02.png)

## ⚙ Konfiguration & Provisioning <img src="docs/images/luckysky/branding/icons/128x128/Command-Block.png" alt="cfg" width="22"/>

- **Hauptdatei `game.yml`** – Spawns (Lobby/Plattform), Lucky-Block-Position, Rundendauer (Bossbar / Actionbar), Rewards, Lives, Wither-Modus  
- **GUIs:** `admin-gui.yml` & `player-gui.yml` – anpassbar ohne Rebuild  
- **Setup:** Welt-Template bereitstellen → Server starten → `/ls reload`  
- **AccessGate:** Lobby offen, Countdown / Run nur Whitelist oder Admin  
- **Kompatibilität:** Multiverse-Core, LuckPerms, Duels, Citizens  
- **Welt-Lifecycle:** siehe [Wiki → LuckySky-Weltrotation](docs/wiki/luckysky-weltrotation.md)

---

## ✨ Hauptfunktionen

- **GameManager** – Steuert Start / Stop, lädt Welten, setzt Plattformen, verwaltet Spieler und Services  
- **LuckyService** – Platziert periodisch den konfigurierten Lucky Block  
- **CountdownService** – Tickgenauer Rundentimer mit Bossbar / Actionbar  
- **RewardService** – Führt Sieger-/Verlierer-Befehle aus und setzt nach Ablauf zurück  
- **RespawnService** – One-Life-Modus, Spectator-Übergänge, korrekte Respawns  
- **AccessGate** – Kontrolliert Join / Teleport / WorldChange  

**Weitere Systeme:**  
WitherService (Spawn + Taunts), ScoreboardService (Status / Timer / Spieler / Wither),  
WipeService (Entity-Cleanup), NPCService (Teleports / Menüs),  
Duels-Integration (Kits & Arena-Bindung), StateMachine (LOBBY → COUNTDOWN → RUN → ENDING → RESETTING)

---

![Banner Abschnitt](docs/images/luckysky/banner/Lucky-Banner03.png)

## 📚 Dokumentation · Wiki

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
- **Abhängigkeiten:** Nur Paper / Bukkit  
- **Empfohlene IDE:** IntelliJ IDEA oder VS Code mit Gradle-Plugin  

---

## 🧩 API & Erweiterbarkeit <img src="docs/images/luckysky/branding/icons/128x128/Icon-Herz.png" alt="api" width="20"/>

```java
GameManager game = LuckySkyPlugin.get().game();
game.start();                       // Startet die Runde
game.countdown().startMinutes(20);  // Setzt die Rundendauer
game.stop();                        // Stoppt die Runde
