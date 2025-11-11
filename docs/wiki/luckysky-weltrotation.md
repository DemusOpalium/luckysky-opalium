![LuckySky Banner](../images/luckysky/banner/Lucky-banner01.png)

# LuckySky-Weltrotation

Die LuckySky-Weltrotation organisiert, welche Arenen, Lobbywelten und Event-Instanzen aktiv sind. Diese Seite beschreibt die Planungszyklen, technische Umsetzung und Wartungsbefehle.

## Rotationsmodell
- **Standardrotation:** Drei Arenen (Tag, Dämmerung, Sonderereignis) wechseln alle 4 Stunden. Die Rotationsliste liegt in `world-rotation.yml`.
- **Saisonale Pools:** Spezielle Events (Halloween, Sommer) werden als separate Pools gepflegt und per Toggle in der Admin-GUI aktiviert.
- **Backup-Instanzen:** Halten Sie mindestens eine „kalte“ Weltkopie (`worldname_template`) bereit, um schnell auf Fehler zu reagieren.

## Ablaufplan
1. Scheduler prüft zur vollen Stunde, ob die aktuelle Arena älter als der Sollwert ist.
2. Bei Wechsel wird die Queue kurzzeitig gesperrt (`/ls queue lock`).
3. Multiverse importiert die nächste Arena (siehe unten), während Spieler in die Lobby teleportiert werden.
4. Nach erfolgreichem Import wird die Arena als aktiv markiert und die Queue wieder freigegeben.

## Citizens-Parking
Damit Admin- und Event-NPCs bei einem Weltwechsel nicht verloren gehen, parken Sie alle Citizens im Spawn-Chunk auf einer sicheren Plattform bei Y≈-58.
- Verwenden Sie `/npc tphere` und positionieren Sie sie gesammelt auf einer Bedrock-Plattform.
- Setzen Sie `/npc path` auf `stationary true`, damit sie nicht von Bewegungs-Triggern weggeschoben werden.
- Nutzen Sie Chunk-Loader oder die Spawn-Chunk-Garantie (Multiverse `alwaysLoaded: true`), damit die NPCs dauerhaft im Speicher bleiben.

## Multiverse-Befehle
- `mv import <welt> normal`: Importiert eine neue Arena. Nutzen Sie `normal`, `nether` oder `end` je nach Generator.
- `mv remove <welt>`: Entfernt die alte Arena aus der Rotationsliste, ohne die Dateien zu löschen.
- `mv confirm`: Bestätigt kritische Aktionen wie `mv remove`.
- `mvtp <welt>`: Teleportiert Operatoren in die Instanz zur Validierung.
- `mv set spawn`: Aktualisiert den Welten-Spawn, falls der Teleportationspunkt verschoben wird.

## Fehlerbehandlung
- Wenn Multiverse-Importe hängen bleiben, überprüfen Sie die Serverkonsole auf Chunk-Fehler und führen Sie `/mv regen <welt>` erst nach einer vollständigen Sicherung aus.
- Bei nicht geladenen NPCs kontrollieren Sie, ob sich die Park-Platform noch im Spawn-Chunk befindet (`/mvchunkinfo`).
