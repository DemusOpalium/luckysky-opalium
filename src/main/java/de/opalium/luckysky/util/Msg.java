package de.opalium.luckysky.util;

import de.opalium.luckysky.LuckySkyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Msg {
    private Msg() {
    }

    private static String prefix() {
        return ChatColor.translateAlternateColorCodes('&', LuckySkyPlugin.get().configs().messages().prefix());
    }

    public static void to(CommandSender sender, String text) {
        sender.sendMessage(prefix() + ChatColor.translateAlternateColorCodes('&', text));
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
