package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GateConfig;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PortalService {
    private PortalService() {}

    /** Öffnet (oder aktualisiert) ein Backspawn-Portal. */
    public static void openBackspawn(LuckySkyPlugin plugin) {
        openBackspawn(plugin, Bukkit.getConsoleSender());
    }

    public static void openBackspawn(LuckySkyPlugin plugin, CommandSender sender) {
        run(plugin, sender, plugin.configs().gate().openCommands());
    }

    /** Entfernt das Backspawn-Portal. */
    public static void closeBackspawn(LuckySkyPlugin plugin) {
        closeBackspawn(plugin, Bukkit.getConsoleSender());
    }

    public static void closeBackspawn(LuckySkyPlugin plugin, CommandSender sender) {
        run(plugin, sender, plugin.configs().gate().closeCommands());
    }

    private static void run(LuckySkyPlugin plugin, CommandSender sender, List<String> commands) {
        GateConfig gate = plugin.configs().gate();
        if (commands == null || commands.isEmpty()) {
            plugin.getLogger().warning("[LuckySky] Gate-Konfiguration enthält keine Befehle.");
            return;
        }
        Player executor = resolveExecutor(sender, gate.executorPermission());
        if (executor == null) {
            Logger logger = plugin.getLogger();
            logger.warning("[LuckySky] Kein Spieler mit Berechtigung '" + gate.executorPermission()
                    + "' verfügbar, um Portalbefehle auszuführen.");
            return;
        }
        Map<String, String> placeholders = gate.placeholders();
        for (String command : commands) {
            executor.performCommand(apply(command, placeholders));
        }
    }

    private static Player resolveExecutor(CommandSender sender, String permission) {
        if (sender instanceof Player player) {
            if (permission == null || permission.isBlank() || player.hasPermission(permission)) {
                return player;
            }
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (permission == null || permission.isBlank() || online.hasPermission(permission)) {
                return online;
            }
        }
        return null;
    }

    private static String apply(String command, Map<String, String> placeholders) {
        String result = command;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
