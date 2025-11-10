package de.opalium.luckysky.commands;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.duels.DuelsManager;
import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.game.WitherService;
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
            "reload", "duels", "countdown", "start", "stop", "reset", "plat", "plat+", "bind",
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
                if (!(sender instanceof Player player)) {
                    Msg.to(sender, "&cNur Spieler können die Duels-GUI öffnen.");
                    return true;
                }
                duels.openMenu(player);
            }
            case "start", "countdown" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.start();
                if (sub.equals("countdown")) {
                    Msg.to(sender, "&aCountdown gestartet.");
                } else {
                    Msg.to(sender, "&aGame started.");
                }
            }
            case "stop" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.stop();
                Msg.to(sender, "&eGame stopped.");
            }
            case "reset" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.stop();
                Msg.to(sender, "&eGame beendet – Spieler zur Lobby teleportiert.");
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
                game.bindAll(sender);
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
                handleOptionalReward(sender, game, args, 1);
                Msg.to(sender, "&aDauer auf 5 Minuten gesetzt.");
            }
            case "mode20" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.setDurationMinutes(20);
                handleOptionalReward(sender, game, args, 1);
                Msg.to(sender, "&aDauer auf 20 Minuten gesetzt.");
            }
            case "mode60" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                game.setDurationMinutes(60);
                handleOptionalReward(sender, game, args, 1);
                Msg.to(sender, "&aDauer auf 60 Minuten gesetzt.");
            }
            case "wither" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                WitherService.SpawnRequestResult result = game.spawnWitherNow();
                switch (result) {
                    case ACCEPTED -> Msg.to(sender, "&dWither-Spawn ausgelöst.");
                    case WITHER_DISABLED -> Msg.to(sender, "&cWither-Spawns sind deaktiviert.");
                    case GAME_NOT_RUNNING -> Msg.to(sender, "&eLuckySky läuft derzeit nicht.");
                    case SKIPPED_BY_MODE -> Msg.to(sender, "&eWither-Spawn ist für diesen Modus gesperrt.");
                }
            }
            case "taunt_on" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                plugin.configs().updateTraps(plugin.configs().traps().withTauntsEnabled(true));
                plugin.reloadSettings();
                game.setTauntsEnabled(true);
                Msg.to(sender, "&aTaunts aktiviert.");
            }
            case "taunt_off" -> {
                GameManager game = requireGame(sender);
                if (game == null || !requirePermission(sender, PERM_ADMIN)) {
                    return true;
                }
                plugin.configs().updateTraps(plugin.configs().traps().withTauntsEnabled(false));
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
                String menuId = args.length > 1 ? args[1] : null;
                if (menuId == null) {
                    plugin.adminGui().open(player);
                } else {
                    plugin.adminGui().open(player, menuId);
                }
            }
            default -> Msg.to(sender, "&7Unbekannt. /ls help");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        if (sender.hasPermission(PERM_ADMIN)) {
            Msg.to(sender, "&7/ls reload &8– Lädt die LuckySky-Konfiguration neu");
            Msg.to(sender, "&7/ls countdown &8– Startet das Spiel inkl. Countdown & Plattform-Teleport");
            Msg.to(sender, "&7/ls start &8– Startet das Spiel");
            Msg.to(sender, "&7/ls stop &8– Stoppt das Spiel");
            Msg.to(sender, "&7/ls reset &8– Stoppt das Spiel & bringt alle zur Lobby");
            Msg.to(sender, "&7/ls plat &8– Setzt Safe-Plattform");
            Msg.to(sender, "&7/ls plat+ &8– 3x3 Erweiterung (falls aktiv)");
            Msg.to(sender, "&7/ls bind &8– Bindet alle an Respawn");
            Msg.to(sender, "&7/ls clean &8– Soft-Wipe (Entities nahe Lucky)");
            Msg.to(sender, "&7/ls hardwipe &8– Hard-Wipe (inkl. ArmorStands)");
            Msg.to(sender, "&7/ls mode5|mode20|mode60 [win|fail] &8– Zeitvorgabe (optional Rewards)");
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

    private void handleOptionalReward(CommandSender sender, GameManager game, String[] args, int startIndex) {
        if (args.length <= startIndex) {
            return;
        }
        for (int i = startIndex; i < args.length; i++) {
            String token = args[i].toLowerCase(Locale.ROOT);
            if (token.startsWith("reward=")) {
                token = token.substring("reward=".length());
            }
            switch (token) {
                case "win", "success" -> {
                    Player player = sender instanceof Player ? (Player) sender : null;
                    game.triggerRewardsWin(player);
                    Msg.to(sender, "&aRewards (Win) ausgeführt.");
                    return;
                }
                case "fail", "lose" -> {
                    game.triggerRewardsFail();
                    Msg.to(sender, "&cRewards (Fail) ausgeführt.");
                    return;
                }
                default -> {
                }
            }
        }
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
        if (args.length == 2 && "gui".equalsIgnoreCase(args[0])) {
            String current = args[1].toLowerCase(Locale.ROOT);
            return plugin.adminGui().menuIds().stream()
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(current))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        return Collections.emptyList();
    }
}
