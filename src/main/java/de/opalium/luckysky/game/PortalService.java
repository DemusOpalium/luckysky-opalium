package de.opalium.luckysky.game;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PortalService {
    private static final String ADMIN_PERMISSION = "opalium.luckysky.admin";

    private PortalService() {}

    /** Öffnet (oder aktualisiert) ein Backspawn-Portal nach LuckySky (0/101/2). */
    public static void openBackspawn() {
        openBackspawn(Bukkit.getConsoleSender());
    }

    public static void openBackspawn(CommandSender sender) {
        run(sender, "mvp create backspawn", "mvp select backspawn", "mvp modify dest e:LuckySky:0,101,2");
    }

    /** Entfernt das Backspawn-Portal. */
    public static void closeBackspawn() {
        closeBackspawn(Bukkit.getConsoleSender());
    }

    public static void closeBackspawn(CommandSender sender) {
        run(sender, "mvp remove backspawn");
    }

    private static void run(CommandSender sender, String... commands) {
        Player executor = resolveExecutor(sender);
        if (executor == null) {
            return;
        }
        for (String command : commands) {
            executor.performCommand(command);
        }
    }

    private static Player resolveExecutor(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(ADMIN_PERMISSION)) {
                return online;
            }
        }
        return null;
    }
}
