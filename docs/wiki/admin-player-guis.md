![LuckySky Banner](../images/luckysky/banner/Lucky-banner01.png)

# Admin- und Player-GUIs

LuckySky stellt mehrere Inventar-basierte GUIs bereit, um Match- und Spieleraktionen ohne Chatbefehle zu verwalten. Dieser Artikel beschreibt Aufbau, Layout, Berechtigungen und Erweiterbarkeit. Alle Layouts liegen jetzt unter `config/gui/` und können ohne Rebuild angepasst werden.

| GUI | Konfigurationsdatei | Beschreibung |
| --- | --- | --- |
| LuckySky Admin | `config/gui/luckysky-admin.yml` | Vollständige Staff-Steuerung inkl. Rewards, Timer und Portalaktionen. |
| LuckySky Player | `config/gui/luckysky-player.yml` | Lobby-Menü für Spieler mit Join, Start-Votes und Scoreboard-Toggle. |
| Duels Admin | `config/gui/duels-admin.yml` | Schnelle Arena-, Reset- und Lichtsteuerung für LuckySky-Duels. |
| Duels Player | `config/gui/duels-player.yml` | Platzhalter für zukünftige spielerorientierte Duels-Menüs (aktuell leer, kann mit Layouts befüllt werden). |

## Admin-GUI

![Admin-GUI-Übersicht](../images/luckysky/admin/LuckySky%20--%20Admin.png)

- **Aufruf:** `/luckysky admin` oder per Citizens-NPC-Interaktion.
- **Hotbar-Aktionen:** Buttons mit `actionbar:`-Tag zeigen kontextabhängige Hinweise (z. B. Countdown-Restzeit).
- **Konfiguration:** `config/gui/luckysky-admin.yml` definiert Layouts. Nutzen Sie `placeholder: true`, um künftige Buttons vorzubereiten.

### Registerkarten & Icon-Legende

| Symbol | Bereich | Schlüsselaktionen |
| --- | --- | --- |
| ![Icon für Matchkontrolle](../images/luckysky/branding/branding/icons/128x128/Icon-Rad.png) | Matchkontrolle | Start/Stop, Welt-Reset, Timer-Schnellwahl |
| ![Icon für Gameplay-Optionen](../images/luckysky/branding/branding/icons/128x128/Icon-Tool-Click-Magic.png) | Gameplay-Optionen | Lucky-Block-Variante, Scoreboard, Taunts |
| ![Icon für Belohnungen](../images/luckysky/branding/branding/icons/128x128/Icon-Herz.png) | Belohnungen | Erfolgs-/Fehlschlagsbefehle, Sofortauszahlung |
| ![Icon für Weltwerkzeuge](../images/luckysky/branding/branding/icons/128x128/Icon-off.png) | Welt- & Wartungstools | Plattformbau, Portale, Entity-Wipes |
| ![Icon für Vorlagenverwaltung](../images/luckysky/branding/branding/icons/128x128/Command-Block.png) | Vorlagenverwaltung | Speichern/Laden von Arenavorlagen |

### Matchkontrolle

| Aktion | Screenshot |
| --- | --- |
| Countdown-Auswahl (5 Minuten) | ![Start-Timer für 5 Minuten im Matchkontroll-Tab](../images/luckysky/admin/Start%205min.png) |
| Countdown-Auswahl (20 Minuten) | ![Start-Timer für 20 Minuten im Matchkontroll-Tab](../images/luckysky/admin/Start%2020min.png) |
| Sofortiger Stop & Lobby-Transfer | ![Stop- und Lobby-Schaltfläche für laufende Matches](../images/luckysky/admin/Stop%20und%20Lobby.png) |

### Gameplay-Optionen

| Funktion | Screenshot |
| --- | --- |
| Gameplay-Layout | ![Gameplay-Reiter mit zentralen Einstellungen](../images/luckysky/gameplay/LuckySky%20--%20Gameplay.png) |
| Wither-Status toggeln | ![Schaltfläche zum Aktivieren des Withers](../images/luckysky/admin/Wither%20AN.png) |
| Sofort-Spawn des Withers | ![Schaltfläche für sofortigen Wither-Spawn](../images/luckysky/admin/Wither%20sofort.png) |
| Scoreboard-Umschalter | ![Button zum Aktivieren des Scoreboards](../images/luckysky/admin/Scoreboard%20An.png) |
| Taunt-Auswahl | ![Taunt-Schaltfläche im Gameplay-Reiter](../images/luckysky/admin/Taunts%20AN.png) |
| Variantenumschalter | ![Variante-Auswahl für Spielmodi](../images/luckysky/gameplay/Variante.png) |

### Vorlagenverwaltung

| Aktion | Screenshot |
| --- | --- |
| Vorlagenübersicht | ![Vorlagen-Tab mit gespeicherten Layouts](../images/luckysky/admin/Vorlagen.png) |
| Vorlage sichern | ![Schaltfläche zum Sichern einer Basisvorlage](../images/luckysky/vorlagen/Base%20sichern.png) |
| Vorlage laden | ![Schaltfläche zum Laden einer Basisvorlage](../images/luckysky/vorlagen/base%20laden.png) |

### Weltwerkzeuge

| Werkzeug | Screenshot |
| --- | --- |
| Weltübersicht | ![Welt-Tab mit zentralen Wartungswerkzeugen](../images/luckysky/welt/LuckySky%20--%20Welt.png) |
| Portal öffnen | ![Schaltfläche zum Öffnen des Portals](../images/luckysky/welt/Portal%20%C3%B6ffnen.png) |
| Portal schließen | ![Schaltfläche zum Schließen des Portals](../images/luckysky/welt/Portal%20schlie%C3%9Fen.png) |
| Teleport zur Arena | ![Teleport-Schaltfläche für LuckySky-Arenen](../images/luckysky/welt/Teleport%20LuckySky.png) |

### Belohnungen & Ergebnisse

| Status | Screenshot |
| --- | --- |
| Erfolgreiche Reward-Auszahlung | ![Admin-Reward-Tab mit Erfolgsausgabe](../images/luckysky/admin/Reward%20Win.png) |
| Fehlgeschlagene Reward-Auszahlung | ![Admin-Reward-Tab mit Fehlerausgabe](../images/luckysky/admin/Reward%20Fail.png) |

## Player-GUIs

![Player-GUI Menüansicht](../images/luckysky/admin/Spieler%20-%20Men%C3%BC.png)

- **Queue-GUI:** Spieler wählen Spielmodus, Event-Varianten oder Training-Sessions aus. Das Interface ruft `/ls queue join <variante>` auf.
- **Loadout-GUI:** Ermöglicht kosmetische Einstellungen (z. B. Trails, Victory-Dances). Optional in `config.yml` deaktivierbar.
- **Duels-GUI:** Wird per `/luckysky duels` geöffnet und erlaubt das Binden von LuckySky-Varianten an Duels-Kits (siehe [Duels Crystal PvP Builder](duels-crystal-pvp-builder.md)).
- **Konfiguration:** Spielerbezogene Layouts liegen in `config/gui/luckysky-player.yml` (LuckySky) und `config/gui/duels-player.yml` (Duels, derzeit leer). Admin-spezifische Duels-Knöpfe werden separat in `config/gui/duels-admin.yml` gepflegt.

## Permissions

- `luckysky.gui.admin`: Vollzugriff auf die Admin-GUI samt gefährlicher Aktionen (Wipes, Timer-Resets).
- `luckysky.gui.staff`: Zugriff auf Matchkontrolle, jedoch ohne Wipe/Belohnungs-Tabs.
- `luckysky.gui.players`: Öffnet die Player-GUI-Sammlung, inklusive Queue- und Loadout-GUI.
- `luckysky.gui.queue.bypass`: Überspringt Cooldowns beim erneuten Betreten der Queue.
- `opalium.luckysky.duels.mod`: Öffnet das Duels-Admin-Menü `/duelsui`.

## Erweiterung

1. **Neuer Button:** Fügen Sie in `config/gui/luckysky-admin.yml` oder dem jeweiligen Player-/Duels-Layout einen Slot-Eintrag hinzu, inklusive `commands:`-Liste. Unterstützt mehrere Befehle.
2. **API-Hooks:** Verwenden Sie `LuckySkyAdminGui#registerExtension`, um neue Tabs zur Laufzeit hinzuzufügen.
3. **Übersetzungen:** Texte werden über `messages.yml` geladen; passen Sie dort Lore und Titel an.

## Fehlerbehebung

- Wenn das GUI nicht öffnet, prüfen Sie den Permission-Pfad (LuckPerms `lp user <name> permission info`).
- Inventare schließen sofort, wenn die Aktion serverseitig fehlschlägt. Kontrollieren Sie dann die Server-Konsole auf Exceptions.
