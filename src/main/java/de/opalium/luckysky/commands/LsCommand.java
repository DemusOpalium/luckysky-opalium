package de.opalium.luckysky.commands;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.duels.DuelsManager;
import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.util.Msg;
import java.util.ArrayList;
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
    private static final List<String> SUBCOMMANDS = List.of(
            "reload", "duels", "start", "stop", "plat", "plat+", "bind",
            "clean", "hardwipe", "mode5", "mode20", "mode60",
            "wither", "taunt_on", "taunt_off", "gui"
    );

    private static final String PERM_BASE = "opalium.luckysky.base";
    private static final String PERM_ADMIN = "opalium.luckysky.admin";
    private static final String PERM_DUELS_USE = "opalium.luckysky.duels.use";

    private final LuckySkyPlugin plugin;

    public LsCommand(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERM_BASE)) {
            Msg.to(sender, "&cNo permission.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                plugin.reloadSettings();
                Msg.to(sender, "&aLuckySky-Konfiguration erfolgreich neu geladen.");
            }
            case "duels" -> {
                if (!requirePermission(sender, PERM_DUELS_USE)) {
                    return true;
                }
                DuelsManager duels = plugin.duels();
                if (duels == null) {
                    Msg.to(sender, "&cLuckySky-Duels ist nicht verfügbar.");
                    return true;
                }
                boolean adminBypass = sender.hasPermission(PERM_ADMIN);
                if (!duels.isEnabled() && !adminBypass) {
                    if (duels.isDependencyMissing()) {
                        Msg.to(sender, "&cLuckySky-Duels benötigt das Duels-Plugin.");
                    } else {
                        Msg.to(sender, "&cLuckySky-Duels ist momentan deaktiviert.");
                    }
                    return true;
                }
                if (args.length >= 2) {
                    duels.performMappedCommand(sender, args[1]);
                } else {
                    if (!(sender instanceof Player player)) {
                        Msg.to(sender, "&cNutze &e/ls duels <Variante>&c von der Konsole aus.");
                        return true;
                    }
                    duels.openMenu(player);
                }
            }
            case "start" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.start();
                Msg.to(sender, "&aGame started.");
            }
            case "stop" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.stop();
                Msg.to(sender, "&eGame stopped.");
            }
            case "plat" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.placePlatform();
                Msg.to(sender, "&bPlattform gesetzt.");
            }
            case "plat+" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.placePlatformExtended();
                Msg.to(sender, "&bPlattform 3x3 angewendet.");
            }
            case "bind" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.bindAll();
                Msg.to(sender, "&bAlle gebunden.");
            }
            case "clean" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                Msg.to(sender, "&7Soft-Wipe entfernt: &f" + game.softClear());
            }
            case "hardwipe" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                Msg.to(sender, "&7Hard-Wipe entfernt: &f" + game.hardClear());
            }
            case "mode5" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.setDurationMinutes(5);
                Msg.to(sender, "&aDauer auf 5 Minuten gesetzt.");
            }
            case "mode20" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.setDurationMinutes(20);
                Msg.to(sender, "&aDauer auf 20 Minuten gesetzt.");
            }
            case "mode60" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.setDurationMinutes(60);
                Msg.to(sender, "&aDauer auf 60 Minuten gesetzt.");
            }
            case "wither" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.spawnWitherNow();
                Msg.to(sender, "&dWither gespawnt.");
            }
            case "taunt_on" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                plugin.getConfig().set("withers.taunts.enable", true);
                plugin.saveConfig();
                plugin.reloadSettings();
                game.setTauntsEnabled(true);
                Msg.to(sender, "&aTaunts aktiviert.");
            }
            case "taunt_off" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                plugin.getConfig().set("withers.taunts.enable", false);
                plugin.saveConfig();
                plugin.reloadSettings();
                game.setTauntsEnabled(false);
                Msg.to(sender, "&cTaunts deaktiviert.");
            }
            case "gui" -> {
                if (!requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
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
        if (sender.hasPermission(PERM_ADMIN)) {
            Msg.to(sender, "&7/ls reload &8– Lädt die LuckySky-Konfiguration neu");
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
        if (sender.hasPermission(PERM_DUELS_USE)) {
            Msg.to(sender, "&7/ls duels [Variante] &8– Öffnet LuckySky-Duels oder wählt ein Kit");
        }
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        Msg.to(sender, "&cNo permission.");
        return false;
    }

    private GameManager requireGame(CommandSender sender) {
        GameManager game = plugin.game();
        if (game == null) {
            Msg.to(sender, "&cPlugin not ready.");
        }
        return game;
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
