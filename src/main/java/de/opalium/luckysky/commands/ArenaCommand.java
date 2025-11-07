package de.opalium.luckysky.commands;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.DuelsConfig;
import de.opalium.luckysky.duels.DuelsManager;
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

public class ArenaCommand implements CommandExecutor, TabCompleter {
    private final LuckySkyPlugin plugin;

    public ArenaCommand(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("opalium.luckysky.arena")) {
            Msg.to(sender, "&cNo permission.");
            return true;
        }
        DuelsManager duels = plugin.duels();
        if (duels == null) {
            Msg.to(sender, "&cDuels-System nicht initialisiert.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "select" -> {
                if (args.length < 2) {
                    Msg.to(sender, "&cNutze /arena select <arenaId>.");
                    return true;
                }
                duels.selectArena(args[1], sender);
            }
            case "reset" -> {
                if (args.length < 2) {
                    Msg.to(sender, "&cNutze /arena reset <preset>.");
                    return true;
                }
                duels.reset(args[1], sender);
            }
            case "floor" -> {
                if (args.length < 2) {
                    Msg.to(sender, "&cNutze /arena floor <preset>.");
                    return true;
                }
                duels.floor(args[1], sender);
            }
            case "lava" -> {
                if (args.length < 2) {
                    Msg.to(sender, "&cNutze /arena lava <preset|clear>.");
                    return true;
                }
                duels.lava(args[1], sender);
            }
            case "light" -> {
                if (args.length < 2) {
                    Msg.to(sender, "&cNutze /arena light <on|off>.");
                    return true;
                }
                boolean enable = args[1].equalsIgnoreCase("on");
                duels.light(enable, sender);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        Msg.to(sender, "&7/arena select <id> &8– Arena wählen");
        Msg.to(sender, "&7/arena reset <preset> &8– Reset-Preset ausführen");
        Msg.to(sender, "&7/arena floor <preset> &8– Boden-Preset anwenden");
        Msg.to(sender, "&7/arena lava <preset|clear> &8– Lava-Preset umschalten");
        Msg.to(sender, "&7/arena light <on|off> &8– Licht-Raster steuern");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("opalium.luckysky.arena")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(List.of("select", "reset", "floor", "lava", "light"), args[0]);
        }
        DuelsConfig duels = plugin.configs().duels();
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "select" -> {
                    return filter(new ArrayList<>(duels.arenas().keySet()), args[1]);
                }
                case "reset" -> {
                    DuelsConfig.Arena arena = duels.arenas().get(duels.activeArena());
                    if (arena != null) {
                        return filter(new ArrayList<>(arena.resets().keySet()), args[1]);
                    }
                }
                case "floor" -> {
                    DuelsConfig.Arena arena = duels.arenas().get(duels.activeArena());
                    if (arena != null) {
                        return filter(new ArrayList<>(arena.floor().presets().keySet()), args[1]);
                    }
                }
                case "lava" -> {
                    DuelsConfig.Arena arena = duels.arenas().get(duels.activeArena());
                    if (arena != null) {
                        List<String> options = new ArrayList<>(arena.hazards().presets().keySet());
                        options.add("clear");
                        return filter(options, args[1]);
                    }
                }
                case "light" -> {
                    return filter(List.of("on", "off"), args[1]);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String current) {
        String lower = current.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
