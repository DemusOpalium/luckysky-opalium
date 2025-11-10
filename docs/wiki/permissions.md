# Permissions & LuckPerms-Setups

Dieses Dokument listet alle relevanten LuckySky-Berechtigungen sowie empfohlene LuckPerms-Gruppen-Setups. Ergänzen oder modifizieren Sie die Aufteilung je nach Serverstruktur.

## Kernberechtigungen
| Kategorie | Permission | Beschreibung |
| --- | --- | --- |
| NPC-Verwaltung | `luckysky.npc.admin` | Legt NPCs an, verschiebt Depot-NPCs, führt Citizens-Befehle aus |
| NPC-Verwaltung | `luckysky.npc.inspect` | Prüft Depot-Statusanzeigen, kann Traits lesen |
| Admin-GUIs | `luckysky.gui.admin` | Vollzugriff auf sämtliche Admin-Tabs |
| Admin-GUIs | `luckysky.gui.staff` | Stark eingeschränktes Panel (Matchkontrolle) |
| Player-GUIs | `luckysky.gui.players` | Öffnet Queue-/Loadout-GUIs |
| Queue | `luckysky.gui.queue.bypass` | Umgeht Cooldowns und Mindestspielerzahlen |
| Duels | `luckysky.duels.builder` | Startet den Crystal PvP Builder |
| Duels | `luckysky.duels.builder.apply` | Speichert Kit-Bindungen |
| Duels | `luckysky.duels.builder.test` | Aktiviert Test-Teleports |
| Fallen | `luckysky.trap.edit` | Bearbeitet Fallen-Konfigurationen |
| Fallen | `luckysky.trap.preview` | Darf Fallen-Vorschau aktivieren |
| Fallen | `luckysky.trap.bypass` | Spielt nicht in Fallen hinein |
| Weltrotation | `luckysky.rotation.manage` | Startet manuellen Weltenwechsel, lädt Arenen |
| Wartung | `luckysky.maintenance.wipe` | Führt Entity-/Block-Wipes aus |
| Wartung | `luckysky.maintenance.rewards` | Triggert Belohnungsbefehle |

## Empfohlenes LuckPerms-Gruppenmodell

### Gruppenstruktur
- **default**
  - Standardspieler, keine LuckySky-Sonderrechte.
  - Enthält nur `luckysky.gui.players` falls die Player-GUIs öffentlich sein sollen.
- **luckysky_player**
  - Erhält `luckysky.gui.players` und `luckysky.gui.queue.bypass` (optional Event-Server).
  - Kontext `server=luckysky` um andere Server nicht zu beeinflussen.
- **luckysky_builder**
  - Enthält `luckysky.duels.builder`, `luckysky.duels.builder.test`, `luckysky.trap.preview`.
  - Optionale temporäre Rechte (`lp user <name> permission settemp ... 7d`).
- **luckysky_staff**
  - `luckysky.gui.staff`, `luckysky.rotation.manage`, `luckysky.maintenance.wipe`.
  - Aktiviert `weight 50`, damit sie über Player-Rollen priorisiert sind.
- **luckysky_admin**
  - Alle Rechte aus `luckysky_staff` plus `luckysky.gui.admin`, `luckysky.maintenance.rewards`, `luckysky.npc.admin`.
  - `meta`-Eintrag `prefix=&c[LS-Admin]&r` zur klaren Kennzeichnung.

### Setup-Beispiele
```bash
# Gruppen anlegen
lp creategroup luckysky_player
lp creategroup luckysky_builder
lp creategroup luckysky_staff
lp creategroup luckysky_admin

# Rechte vergeben
lp group luckysky_player permission set luckysky.gui.players true server=luckysky
lp group luckysky_player permission set luckysky.gui.queue.bypass true server=luckysky
lp group luckysky_builder permission set luckysky.duels.builder true server=luckysky
lp group luckysky_builder permission set luckysky.duels.builder.test true server=luckysky
lp group luckysky_staff permission set luckysky.gui.staff true server=luckysky
lp group luckysky_staff permission set luckysky.rotation.manage true server=luckysky
lp group luckysky_admin permission set luckysky.gui.admin true server=luckysky
lp group luckysky_admin permission set luckysky.npc.admin true server=luckysky
```

> **Hinweis:** Ergänzen Sie kontextsensitive Rechte (z. B. `world=<arena>`) falls bestimmte Aktionen nur in der Arena gelten sollen.

## Wartung
- Führen Sie regelmäßig `lp sync` aus, damit der LuckPerms-Netzwerkcache aktualisiert wird.
- Dokumentieren Sie Sonderrechte (temporäre Promotions) im internen Staff-Wiki.
