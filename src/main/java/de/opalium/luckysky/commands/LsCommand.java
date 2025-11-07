package de.opalium.luckysky.commands;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameManager;
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

public class LsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "start", "stop", "plat", "plat+", "bind",
            "clean", "hardwipe", "mode5", "mode20", "mode60",
            "wither", "taunt_on", "taunt_off", "gui"
    );

    private final LuckySkyPlugin plugin;

    public LsCommand(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("opalium.luckysky.admin") && !sender.hasPermission("luckysky.admin")) {
            Msg.to(sender, "&cNo permission.");
            return true;
        }

        GameManager game = plugin.game();
        if (game == null) {
            Msg.to(sender, "&cPlugin not ready.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "start" -> {
                game.start();
                Msg.to(sender, "&aGame started.");
            }
            case "stop" -> {
                game.stop();
                Msg.to(sender, "&eGame stopped.");
            }
            case "plat" -> {
                game.placePlatform();
                Msg.to(sender, "&bPlattform gesetzt.");
            }
            case "plat+" -> {
                game.placePlatformExtended();
                Msg.to(sender, "&bPlattform 3x3 angewendet.");
            }
            case "bind" -> {
                game.bindAll();
                Msg.to(sender, "&bAlle gebunden.");
            }
            case "clean" -> Msg.to(sender, "&7Soft-Wipe entfernt: &f" + game.softClear());
            case "hardwipe" -> Msg.to(sender, "&7Hard-Wipe entfernt: &f" + game.hardClear());
            case "mode5" -> {
                game.setDurationMinutes(5);
                Msg.to(sender, "&aDauer auf 5 Minuten gesetzt.");
            }
            case "mode20" -> {
                game.setDurationMinutes(20);
                Msg.to(sender, "&aDauer auf 20 Minuten gesetzt.");
            }
            case "mode60" -> {
                game.setDurationMinutes(60);
                Msg.to(sender, "&aDauer auf 60 Minuten gesetzt.");
            }
            case "wither" -> {
                game.spawnWitherNow();
                Msg.to(sender, "&dWither gespawnt.");
            }
            case "taunt_on" -> {
                plugin.getConfig().set("withers.taunts.enable", true);
                plugin.saveConfig();
                plugin.reloadSettings();
                game.setTauntsEnabled(true);
                Msg.to(sender, "&aTaunts aktiviert.");
            }
            case "taunt_off" -> {
                plugin.getConfig().set("withers.taunts.enable", false);
                plugin.saveConfig();
                plugin.reloadSettings();
                game.setTauntsEnabled(false);
                Msg.to(sender, "&cTaunts deaktiviert.");
            }
            case "gui" -> {
                if (!(sender instanceof Player player)) {
                    Msg.to(sender, "&cNur Spieler.");
                    return true;
                }
                plugin.adminGui().open(player);
            }
            default -> Msg.to(sender, "&7Unbekannt. /ls help");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        Msg.to(sender, "&7/ls start &8– Startet das Spiel");
        Msg.to(sender, "&7/ls stop &8– Stoppt das Spiel");
        Msg.to(sender, "&7/ls plat &8– Setzt Safe-Plattform");
        Msg.to(sender, "&7/ls plat+ &8– 3x3 Erweiterung (falls aktiv)");
        Msg.to(sender, "&7/ls bind &8– Bindet alle an Respawn");
        Msg.to(sender, "&7/ls clean &8– Soft-Wipe (Entities nahe Lucky)");
        Msg.to(sender, "&7/ls hardwipe &8– Hard-Wipe (inkl. ArmorStands)");
        Msg.to(sender, "&7/ls mode5|mode20|mode60 &8– Zeitvorgabe");
        Msg.to(sender, "&7/ls wither &8– Wither sofort spawnen");
        Msg.to(sender, "&7/ls taunt_on/off &8– Taunts toggeln");
        Msg.to(sender, "&7/ls gui &8– Öffnet das Admin-Menü");
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
