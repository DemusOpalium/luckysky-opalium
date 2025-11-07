package de.opalium.luckysky.commands;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.duels.DuelsManager;
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
            Msg.to(sender, "&cNur Spieler können diese Aktion ausführen.");
            return true;
        }

        if (!player.hasPermission("opalium.luckysky.duels.mod")) {
            Msg.to(player, "&cDazu fehlt dir die Berechtigung.");
            return true;
        }

        DuelsManager duels = plugin.duels();
        if (duels == null) {
            Msg.to(player, "&cLuckySky-Duels ist nicht initialisiert.");
            return true;
        }

        duels.openMenu(player);
        return true;
    }
}
