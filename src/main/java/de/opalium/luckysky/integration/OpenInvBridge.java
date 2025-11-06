package de.opalium.luckysky.integration;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public final class OpenInvBridge {
    private OpenInvBridge() {
    }

    public static boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin("OpenInv") != null;
    }

    public static boolean open(Player admin, String targetName) {
        if (!isPresent()) {
            return false;
        }
        return admin.performCommand("openinv " + targetName);
    }

    public static boolean openAsConsole(String targetName) {
        if (!isPresent()) {
            return false;
        }
        ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
        return Bukkit.dispatchCommand(console, "openinv " + targetName);
    }
}
