package de.opalium.luckysky.commands;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.core.SessionManager;
import de.opalium.luckysky.util.ConfigKeys;
import de.opalium.luckysky.util.Messages;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "start", "stop", "reset", "clean", "plat", "plat+", "corridor", "bind",
            "mode5", "mode20", "mode60", "wither", "taunt_on", "taunt_off", "sign"
    );

    private final LuckySkyPlugin plugin;
    private final Messages messages;

    public LsCommand(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("luckysky.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            messages.send(sender, "unknown-subcommand");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start" -> handleStart(sender);
            case "stop" -> handleStop(sender);
            case "reset" -> handleReset(sender);
            case "clean" -> handleClean(sender);
            case "plat" -> handlePlat(sender);
            case "plat+" -> handlePlatPlus(sender);
            case "corridor" -> handleCorridor(sender);
            case "bind" -> handleBind(sender);
            case "mode5" -> handleMode(sender, 5);
            case "mode20" -> handleMode(sender, 20);
            case "mode60" -> handleMode(sender, 60);
            case "wither" -> handleWither(sender);
            case "taunt_on" -> handleTaunt(sender, true);
            case "taunt_off" -> handleTaunt(sender, false);
            case "sign" -> handleSign(sender);
            default -> messages.send(sender, "unknown-subcommand");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, completions);
            return completions;
        }
        return List.of();
    }

    private void handleStart(CommandSender sender) {
        if (plugin.getGameWorld().isEmpty()) {
            messages.send(sender, "world-missing");
            return;
        }
        SessionManager sessionManager = plugin.getSessionManager();
        if (!sessionManager.startSession()) {
            messages.send(sender, "session-already-running");
            return;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("minutes", String.valueOf(sessionManager.getCurrentModeMinutes()));
        messages.send(sender, "session-started", placeholders);
    }

    private void handleStop(CommandSender sender) {
        SessionManager sessionManager = plugin.getSessionManager();
        if (!sessionManager.stopSession()) {
            messages.send(sender, "session-not-running");
            return;
        }
        messages.send(sender, "session-stopped");
    }

    private void handleReset(CommandSender sender) {
        Optional<World> world = plugin.getGameWorld();
        if (world.isEmpty()) {
            messages.send(sender, "world-missing");
            return;
        }
        int removed = plugin.getWipeService().performHardWipe();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(removed));
        messages.send(sender, "hard-wipe-done", placeholders);
    }

    private void handleClean(CommandSender sender) {
        Optional<World> world = plugin.getGameWorld();
        if (world.isEmpty()) {
            messages.send(sender, "world-missing");
            return;
        }
        int removed = plugin.getWipeService().performSoftWipe();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("count", String.valueOf(removed));
        messages.send(sender, "soft-wipe-done", placeholders);
    }

    private void handlePlat(CommandSender sender) {
        if (plugin.getGameWorld().isEmpty()) {
            messages.send(sender, "world-missing");
            return;
        }
        plugin.getPlatformBuilder().buildBase();
        messages.send(sender, "plat-built");
    }

    private void handlePlatPlus(CommandSender sender) {
        if (plugin.getGameWorld().isEmpty()) {
            messages.send(sender, "world-missing");
            return;
        }
        plugin.getPlatformBuilder().buildExpanded();
        messages.send(sender, "plat-expanded");
    }

    private void handleCorridor(CommandSender sender) {
        if (plugin.getGameWorld().isEmpty()) {
            messages.send(sender, "world-missing");
            return;
        }
        plugin.getCorridorCleaner().clearCorridor();
        messages.send(sender, "corridor-cleared");
    }

    private void handleBind(CommandSender sender) {
        Optional<World> worldOptional = plugin.getGameWorld();
        if (worldOptional.isEmpty()) {
            messages.send(sender, "world-missing");
            return;
        }
        World world = worldOptional.get();
        for (Player player : world.getPlayers()) {
            player.setBedSpawnLocation(plugin.getPlatformBuilder().getPlatformCenter(world), true);
            player.teleport(plugin.getPlatformBuilder().getPlatformCenter(world));
        }
        messages.send(sender, "bind-done");
    }

    private void handleMode(CommandSender sender, int minutes) {
        if (!plugin.getSessionManager().setModeMinutes(minutes)) {
            messages.send(sender, "invalid-mode");
            return;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("minutes", String.valueOf(minutes));
        messages.send(sender, "mode-set", placeholders);
    }

    private void handleWither(CommandSender sender) {
        if (!plugin.getConfigData().getBoolean(ConfigKeys.WITHER_ENABLED, true)) {
            messages.send(sender, "wither-disabled");
            return;
        }
        if (plugin.getGameWorld().isEmpty()) {
            messages.send(sender, "world-missing");
            return;
        }
        if (plugin.getWitherService().isWitherAlive()) {
            messages.send(sender, "wither-already-active");
            return;
        }
        boolean spawned = plugin.getWitherService().spawnWither();
        if (spawned) {
            messages.send(sender, "wither-spawned");
        } else {
            messages.send(sender, "wither-spawn-failed");
        }
    }

    private void handleTaunt(CommandSender sender, boolean enable) {
        boolean immediate = plugin.getWitherService().setTauntsEnabled(enable);
        if (enable) {
            messages.send(sender, "taunts-on");
            if (!immediate) {
                messages.send(sender, "taunts-pending");
            }
        } else {
            messages.send(sender, "taunts-off");
        }
    }

    private void handleSign(CommandSender sender) {
        if (plugin.getGameWorld().isEmpty()) {
            messages.send(sender, "world-missing");
            return;
        }
        plugin.getPlatformBuilder().placeInfoSign();
        messages.send(sender, "sign-placed");
    }
}
