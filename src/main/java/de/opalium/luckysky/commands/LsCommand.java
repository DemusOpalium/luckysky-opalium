package de.opalium.luckysky.commands;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class LsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "start", "stop", "reset", "clean",
            "plat", "plat+", "bind",
            "mode5", "mode20", "mode60",
            "wither", "taunt_on", "taunt_off"
    );

    private final LuckySkyPlugin plugin;

    public LsCommand(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("luckysky.admin")) {
            sender.sendMessage("No permission.");
            return true;
        }

        GameManager manager = plugin.game();
        if (manager == null) {
            sender.sendMessage("Plugin not ready.");
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "help";
        switch (sub) {
            case "start":
                manager.start();
                sender.sendMessage(plugin.settings().prefix + "Start.");
                break;
            case "stop":
                manager.stop();
                sender.sendMessage(plugin.settings().prefix + "Stop.");
                break;
            case "reset":
                manager.softWipe();
                sender.sendMessage(plugin.settings().prefix + "Softwipe.");
                break;
            case "clean":
                manager.hardWipe();
                sender.sendMessage(plugin.settings().prefix + "Hardwipe.");
                break;
            case "plat":
                manager.placePlatform(false);
                sender.sendMessage(plugin.settings().prefix + "Plattform gesetzt.");
                break;
            case "plat+":
                manager.placePlatform(true);
                sender.sendMessage(plugin.settings().prefix + "Plattform 3x3 gesetzt.");
                break;
            case "bind":
                manager.bindAll();
                sender.sendMessage(plugin.settings().prefix + "Spawn gebunden.");
                break;
            case "mode5":
                manager.setDurationMinutes(5);
                sender.sendMessage(plugin.settings().prefix + "Dauer 5m.");
                break;
            case "mode20":
                manager.setDurationMinutes(20);
                sender.sendMessage(plugin.settings().prefix + "Dauer 20m.");
                break;
            case "mode60":
                manager.setDurationMinutes(60);
                sender.sendMessage(plugin.settings().prefix + "Dauer 60m.");
                break;
            case "wither":
                manager.spawnWitherNow();
                sender.sendMessage(plugin.settings().prefix + "Wither gespawnt.");
                break;
            case "taunt_on":
                manager.setTaunts(true);
                sender.sendMessage(plugin.settings().prefix + "Taunts AN.");
                break;
            case "taunt_off":
                manager.setTaunts(false);
                sender.sendMessage(plugin.settings().prefix + "Taunts AUS.");
                break;
            default:
                sender.sendMessage("/ls start|stop|reset|clean|plat|plat+|bind|mode5|mode20|mode60|wither|taunt_on|taunt_off");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String current = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream()
                    .filter(sub -> sub.startsWith(current))
                    .toList();
        }
        return Collections.emptyList();
    }
}
