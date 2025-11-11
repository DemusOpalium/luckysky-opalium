![LuckySky Banner](../images/luckysky/banner/Lucky-banner01.png)

# Duels Crystal PvP Builder

Der Duels Crystal PvP Builder verbindet LuckySky-Varianten mit vorgefertigten Duels-Kits, damit Trainingsmatches dieselben Ressourcen wie Events verwenden.

## Zielsetzung
- Einheitliche Gear-Sets für LuckySky-Duels.
- Schnelles Umschalten zwischen Crystal-Presets ohne Server-Neustart.
- Abgleich mit LuckySky-Loot-Tabellen, um falsche Item-Level zu verhindern.

## Einrichtung
1. **Template-Dateien:** Legen Sie JSON-/YAML-Kits in `plugins/Duels/kits/` an (`luckysky_crystal_default`, `luckysky_glass_cannon`).
2. **Builder-Aufruf:** Öffnen Sie die Duels-GUI (`/luckysky duels`) und wählen Sie „Crystal Builder“.
3. **Variant-Bindung:** Nutzen Sie die Buttons „Arena-Variante“ und „Kit-Auswahl“, um `worldRotationId` und `kitId` zu verknüpfen.
4. **Sync:** Speichern Sie die Bindung per `Synchronisieren`-Knopf; der Builder ruft intern `/duels reload` und `LuckyKitService#refresh()` auf.

## Erweiterung
- **Custom Slots:** Hinterlegen Sie zusätzliche Slots (z. B. Totems) in der `builder-layout.yml`, um sie per Drag-and-Drop zuzuweisen.
- **Skripting:** Verwenden Sie die `KitMappingUpdateEvent`, um externe Plugins (Stats, Leaderboards) über Änderungen zu informieren.

## Testing
- Führen Sie `/duels debug kit <kit>` aus, um Attribute zu überprüfen.
- Nutzen Sie einen privaten Testserver, um Crystal-Explosionen auf Latenz zu prüfen.

## Permissions
- `luckysky.duels.builder`: Öffnet den Builder-Dialog.
- `luckysky.duels.builder.apply`: Erlaubt das Speichern und Aktivieren von Bindungen.
- `luckysky.duels.builder.test`: Aktiviert den Teleport zu Testarenen.
