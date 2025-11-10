package de.opalium.luckysky.util;

import de.opalium.luckysky.game.WitherService;
import org.bukkit.command.CommandSender;

public final class WitherSpawnMessages {
    private WitherSpawnMessages() {
    }

    public static void sendSpawnResult(CommandSender sender, WitherService.SpawnRequestResult result) {
        String text = switch (result) {
            case ACCEPTED -> "&dWither-Spawn ausgelöst.";
            case WITHER_DISABLED -> "&cWither-Spawns sind deaktiviert.";
            case GAME_NOT_RUNNING -> "&eLuckySky läuft derzeit nicht.";
            case SKIPPED_BY_MODE -> "&eWither-Spawn im aktuellen Modus übersprungen.";
            case FAILED -> "&cWither-Spawn fehlgeschlagen (Welt/Regeln prüfen).";
        };
        Msg.to(sender, text);
    }
}
