![LuckySky Banner](../images/luckysky/banner/Lucky-banner01.png)

# Fallen-Handbuch

LuckySky nutzt ein modulares Fallensystem, das Admins und Bauteams für Events konfigurieren können. Dieses Handbuch listet die wichtigsten Fallentypen, Sicherheitschecks und Automationen.

## Architektur
- **Fallen-Registry:** Alle Fallen werden in `traps.yml` definiert und zur Laufzeit über `TrapService` geladen.
- **Auslöser:** Unterstützt Druckplatten, unsichtbare Tripwire-Haken und Event-basierte Trigger (`TrapTriggerEvent`).
- **Effekte:** Standardmäßig Explosion, Void-Drop, Giftwolke, Levitation und Custom-Commands.

## Fallentypen
| Typ | Beschreibung | Konfiguration |
| --- | --- | --- |
| Explosion | Kristall-Explosion mit Blockschutz | `type: CRYSTAL`, `power: 6`, `protectBlocks: true` |
| Void Drop | Entfernt Bodenblöcke temporär | `type: VOID_DROP`, `restoreAfterTicks: 120` |
| Giftkammer | Spawnt Lingering Potion Wolke | `type: GAS`, `potion: POISON`, `duration: 200` |
| Launcher | Wirft Spieler vertikal hoch | `type: LAUNCH`, `velocity: 2.4` |
| Command | Führt Serverbefehle aus | `type: COMMAND`, `commands: [...]` |

## Sicherheitsrichtlinien
1. **Fallback-Bereich:** Setzen Sie eine unsichtbare Barriere im Untergrund, damit Spieler nicht unendlich fallen.
2. **Friendly Fire:** Legen Sie `affectsAllies: false` fest, um Teammitglieder zu schützen.
3. **Cooldowns:** Aktivieren Sie `cooldownTicks`, um wiederholte Auslösung zu verhindern.
4. **Logging:** Aktivieren Sie `logActivations: true`, damit das Staff-Team Missbrauch erkennen kann.

## Automation
- **GUI-Verknüpfung:** Das Admin-GUI-Fallenpanel ruft `TrapService#toggle` auf und zeigt Status-Lampen pro Falle.
- **Skript-Integration:** Mit Skript/Denizen können Sie `on trap activate:`-Handler definieren.
- **Rollback:** Verwenden Sie `/ls traps restore <name>`, um manuelle Wiederherstellung zu erzwingen.

## Permissions
- `luckysky.trap.edit`: Erlaubt das Bearbeiten und Aktivieren von Fallen.
- `luckysky.trap.preview`: Schaltet Vorschau-Trigger frei (Marker-Armorstands, Partikel).
- `luckysky.trap.bypass`: Spieler werden von Fallen nicht erfasst (nützlich für Moderatoren).
