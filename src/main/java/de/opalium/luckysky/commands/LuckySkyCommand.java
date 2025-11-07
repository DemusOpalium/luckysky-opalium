package de.opalium.luckysky.commands;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.util.Msg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class LuckySkyCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "duels");

    private final LuckySkyPlugin plugin;

    public LuckySkyCommand(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!hasBase(sender)) {
            Msg.to(sender, "&cKeine Berechtigung.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "duels" -> handleDuels(sender);
            default -> sendHelp(sender);
        }
        return true;
    }

    private boolean hasBase(CommandSender sender) {
        return sender.hasPermission("opalium.luckysky.base") || sender.hasPermission("opalium.luckysky.admin");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("opalium.luckysky.admin")) {
            Msg.to(sender, "&cKeine Berechtigung.");
            return;
        }
        plugin.reloadSettings();
        Msg.to(sender, "&aLuckySky-Konfiguration neu geladen.");
    }

    private void handleDuels(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Msg.to(sender, "&cNur Spieler.");
            return;
        }
        if (!player.hasPermission("opalium.luckysky.duels.use") && !player.hasPermission("opalium.luckysky.admin")) {
            Msg.to(sender, "&cKeine Berechtigung.");
            return;
        }
        plugin.duelsMenu().open(player);
    }

    private void sendHelp(CommandSender sender) {
        Msg.to(sender, "&7/luckysky reload &8– Lädt die Konfiguration neu.");
        Msg.to(sender, "&7/luckysky duels &8– Öffnet das Duels-Menü.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String current = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream()
                    .filter(sub -> sub.startsWith(current))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return Collections.emptyList();
    }
}
