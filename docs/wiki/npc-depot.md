![LuckySky Banner](../images/luckysky/banner/Lucky-banner01.png)

# NPC-Depot

Das NPC-Depot bündelt alle Citizens-NPCs, die für LuckySky relevante Händler-, Tutorial- oder Queue-Funktionen bereitstellen. Die Depot-Instanz liegt in einem dedizierten Verwaltungsbereich außerhalb der aktiven Spielwelten, damit Spieler-NPCs nicht versehentlich mitgeladen werden.

## Aufbau
- **Standort:** Verwenden Sie einen separaten Sektor in der Lobby-Welt oder einem Verwaltungsplot. Platzieren Sie die NPCs innerhalb eines 3×3-Bereichs für einfache Skript-Verkabelung.
- **Metadaten:** Jeder NPC erhält im Citizens-Editor (`/npc edit`) die Metadaten `luckysky-role` (z. B. `depot-supply`, `queue`, `event-host`) sowie optionale `luckysky-kit` Verweise für das Duels-Setup.
- **Parking:** Parken Sie inaktive NPCs dauerhaft im Spawn-Chunk auf Höhe Y≈-58, um Chunk-Unloads zu vermeiden. Nutzen Sie hierzu `/npc tp <id> <x> -58 <z>` und markieren Sie den Chunk mit `/chunkload` oder einem Keep-Alive-Plugin.

## Pflege-Workflow
1. **Duplizieren statt Neuaufsetzen:** Verwenden Sie `/npc copy` um bestehende Layouts zu übernehmen und reduzieren Sie so Skin-Downloads.
2. **Trait-Export:** Sichern Sie kritische Traits per `/citizens save` bevor Sie Depots aktualisieren.
3. **Status-Kontrolle:** Die LuckSky-Admin-GUI blendet Depot-Warnungen ein, wenn Trait-Checks (z. B. fehlende Kommandos) fehlschlagen.

## Automatisierung
- Skripte können über das Citizens API oder Skript-Addons regelmäßig prüfen, ob Depot-NPCs online sind und bei Bedarf nachteleportieren.
- Lag-Monitoring: Führen Sie `/npc path` im Wartungsfenster aus, um defekte Wegfindung zu erkennen.
