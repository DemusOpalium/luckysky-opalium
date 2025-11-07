package de.opalium.luckysky.commands;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.arena.ArenaService;
import de.opalium.luckysky.arena.ArenaService.OperationType;
import de.opalium.luckysky.arena.ArenaService.ArenaDefinition;
import de.opalium.luckysky.trap.TrapService;
import de.opalium.luckysky.util.Msg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class ArenaCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "opalium.luckysky.arena";
    private final LuckySkyPlugin plugin;

    public ArenaCommand(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            Msg.to(sender, "&cNo permission.");
            return true;
        }
        ArenaService arenas = plugin.arenaService();
        TrapService traps = plugin.trapService();
        if (args.length == 0) {
            if (sender instanceof Player player) {
                plugin.arenaEditorGui().openMain(player);
                return true;
            }
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> handleList(sender, arenas);
            case "build", "clear", "light", "crown", "floor", "ceiling" -> handleArenaOperation(sender, arenas, sub, args);
            case "traps" -> handleTrapCommand(sender, traps, args);
            case "editor" -> {
                if (!(sender instanceof Player player)) {
                    Msg.to(sender, "&cNur Spieler können den Editor nutzen.");
                } else {
                    plugin.arenaEditorGui().openMain(player);
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        Msg.to(sender, "&7/arena list &8– Zeigt alle Arenen und Varianten");
        Msg.to(sender, "&7/arena build <arena> [var] &8– Baut eine Arena-Variante");
        Msg.to(sender, "&7/arena clear <arena> [var] &8– Entfernt Arena-Blöcke");
        Msg.to(sender, "&7/arena light|crown|floor|ceiling <arena> [var] &8– Wendet Operation an");
        Msg.to(sender, "&7/arena traps <enable|disable|cycle|clear> [rotation] &8– Verwalte Fallen");
        Msg.to(sender, "&7/arena editor &8– Öffnet den Arena-Editor");
    }

    private void handleList(CommandSender sender, ArenaService arenas) {
        if (arenas.arenas().isEmpty()) {
            Msg.to(sender, "&cKeine Arenen definiert.");
            return;
        }
        Msg.to(sender, "&7Verfügbare Arenen:");
        List<String> arenaIds = new ArrayList<>(arenas.arenas().keySet());
        arenaIds.sort(String::compareToIgnoreCase);
        for (String arenaId : arenaIds) {
            ArenaDefinition definition = arenas.arena(arenaId).orElse(null);
            if (definition == null) {
                continue;
            }
            String variants = definition.variants().keySet().stream().sorted().collect(Collectors.joining(", "));
            Msg.to(sender, " &8- &f" + arenaId + " &7(" + variants + ")");
        }
    }

    private void handleArenaOperation(CommandSender sender, ArenaService arenas, String action, String[] args) {
        if (args.length < 2) {
            Msg.to(sender, "&cNutze: /arena " + action + " <arena> [var]");
            return;
        }
        String arenaId = args[1];
        String variantId = args.length >= 3 ? args[2] : plugin.settings().arenaDefaultVariant();
        Optional<ArenaDefinition> definition = arenas.arena(arenaId);
        if (definition.isEmpty()) {
            Msg.to(sender, "&cUnbekannte Arena: &f" + arenaId);
            return;
        }
        if (!definition.get().variants().containsKey(variantId)) {
            Msg.to(sender, "&cVariante &f" + variantId + "&c existiert nicht in Arena &f" + arenaId + "&c.");
            return;
        }
        OperationType type = OperationType.fromKey(action);
        if (type == null) {
            switch (action) {
                case "floor" -> type = OperationType.FLOOR;
                case "ceiling" -> type = OperationType.CEILING;
                default -> type = OperationType.BUILD;
            }
        }
        arenas.applyWithFeedback(arenaId, variantId, type);
    }

    private void handleTrapCommand(CommandSender sender, TrapService traps, String[] args) {
        if (args.length < 2) {
            Msg.to(sender, "&cNutze: /arena traps <enable|disable|cycle|clear> [rotation]");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        boolean success = false;
        switch (sub) {
            case "enable" -> {
                success = args.length >= 3 ? traps.enable(args[2]) : traps.enableDefault();
                if (success) {
                    Msg.to(sender, "&aFallen aktiviert.");
                }
            }
            case "disable" -> {
                success = traps.disable();
                if (success) {
                    Msg.to(sender, "&cFallen deaktiviert.");
                }
            }
            case "cycle" -> {
                success = traps.cycle();
                if (success) {
                    Msg.to(sender, "&eFallen weitergeschaltet.");
                }
            }
            case "clear" -> {
                success = traps.clear();
                if (success) {
                    Msg.to(sender, "&7Fallen zurückgesetzt.");
                }
            }
            default -> {
                Msg.to(sender, "&cUnbekannte Trap-Aktion: " + sub);
                return;
            }
        }
        if (!success) {
            Msg.to(sender, "&cFallen-Aktion fehlgeschlagen.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }
        ArenaService arenas = plugin.arenaService();
        TrapService traps = plugin.trapService();
        if (args.length == 1) {
            return tab(List.of("list", "build", "clear", "light", "crown", "floor", "ceiling", "traps", "editor"), args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("build", "clear", "light", "crown", "floor", "ceiling").contains(sub)) {
            return tab(new ArrayList<>(arenas.arenaIds()), args[1]);
        }
        if (args.length == 3 && List.of("build", "clear", "light", "crown", "floor", "ceiling").contains(sub)) {
            Optional<ArenaDefinition> definition = arenas.arena(args[1]);
            return definition.map(value -> tab(new ArrayList<>(value.variants().keySet()), args[2]))
                    .orElse(Collections.emptyList());
        }
        if (sub.equals("traps")) {
            if (args.length == 2) {
                return tab(List.of("enable", "disable", "cycle", "clear"), args[1]);
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("enable")) {
                return tab(new ArrayList<>(traps.rotations().keySet()), args[2]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> tab(List<String> options, String arg) {
        String lower = arg.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
