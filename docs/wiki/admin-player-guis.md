# Admin- und Player-GUIs

LuckySky stellt mehrere Inventar-basierte GUIs bereit, um Match- und Spieleraktionen ohne Chatbefehle zu verwalten. Dieser Artikel beschreibt Aufbau, Layout, Berechtigungen und Erweiterbarkeit.

## Admin-GUI
- **Aufruf:** `/luckysky admin` oder per Citizens-NPC-Interaktion.
- **Registerkarten:**
  - *Matchkontrolle:* Start/Stop, Welt-Reset, Wither-Toggle, Timer-Schnellwahl.
  - *Arena-Wartung:* Safe-Platform-Bau, Lucky-Block-Variante, Entity-Wipes.
  - *Belohnungen:* Vorschau der Erfolgs-/Fehlschlags-Befehle, Sofortauszahlung.
- **Hotbar-Aktionen:** Buttons mit `actionbar:`-Tag zeigen kontextabhängige Hinweise (z. B. Countdown-Restzeit).
- **Konfiguration:** `admin-gui.yml` definiert Layouts. Nutzen Sie `placeholder: true`, um künftige Buttons vorzubereiten.

## Player-GUIs
- **Queue-GUI:** Spieler wählen Spielmodus, Event-Varianten oder Training-Sessions aus. Das Interface ruft `/ls queue join <variante>` auf.
- **Loadout-GUI:** Ermöglicht kosmetische Einstellungen (z. B. Trails, Victory-Dances). Optional in `config.yml` deaktivierbar.
- **Duels-GUI:** Wird per `/luckysky duels` geöffnet und erlaubt das Binden von LuckySky-Varianten an Duels-Kits (siehe [Duels Crystal PvP Builder](duels-crystal-pvp-builder.md)).

## Permissions
- `luckysky.gui.admin`: Vollzugriff auf die Admin-GUI samt gefährlicher Aktionen (Wipes, Timer-Resets).
- `luckysky.gui.staff`: Zugriff auf Matchkontrolle, jedoch ohne Wipe/Belohnungs-Tabs.
- `luckysky.gui.players`: Öffnet die Player-GUI-Sammlung, inklusive Queue- und Loadout-GUI.
- `luckysky.gui.queue.bypass`: Überspringt Cooldowns beim erneuten Betreten der Queue.

## Erweiterung
1. **Neuer Button:** Fügen Sie in `admin-gui.yml` einen Slot-Eintrag hinzu, inklusive `commands:`-Liste. Unterstützt mehrere Befehle.
2. **API-Hooks:** Verwenden Sie `LuckySkyAdminGui#registerExtension`, um neue Tabs zur Laufzeit hinzuzufügen.
3. **Übersetzungen:** Texte werden über `messages.yml` geladen; passen Sie dort Lore und Titel an.

## Fehlerbehebung
- Wenn das GUI nicht öffnet, prüfen Sie den Permission-Pfad (LuckPerms `lp user <name> permission info`).
- Inventare schließen sofort, wenn die Aktion serverseitig fehlschlägt. Kontrollieren Sie dann die Server-Konsole auf Exceptions.
