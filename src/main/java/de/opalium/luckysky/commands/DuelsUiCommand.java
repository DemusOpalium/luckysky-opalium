package de.opalium.luckysky.commands;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelsUiCommand implements CommandExecutor {
    private final LuckySkyPlugin plugin;

    public DuelsUiCommand(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.to(sender, "&cNur Spieler.");
            return true;
        }
        if (!player.hasPermission("opalium.luckysky.duels.use") && !player.hasPermission("opalium.luckysky.admin")) {
            Msg.to(sender, "&cKeine Berechtigung.");
            return true;
        }
        plugin.duelsMenu().open(player);
        return true;
    }
}
