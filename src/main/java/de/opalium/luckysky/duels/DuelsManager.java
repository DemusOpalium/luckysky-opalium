package de.opalium.luckysky.duels;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.DuelsConfig;
import de.opalium.luckysky.util.Msg;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

public class DuelsManager implements Listener {
    private final LuckySkyPlugin plugin;
    private final ArenaService arenaService;
    private final DuelsGui gui;
    private boolean enabled;
    private boolean dependencyMissing;

    public DuelsManager(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.arenaService = new ArenaService(plugin);
        this.gui = new DuelsGui(plugin);
        reload();
    }

    public void reload() {
        DuelsConfig config = plugin.configs().duels();
        Plugin dependency = Bukkit.getPluginManager().getPlugin("Duels");
        dependencyMissing = dependency == null || !dependency.isEnabled();
        enabled = !config.arenas().isEmpty() && !dependencyMissing;
        gui.reload();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDependencyMissing() {
        return dependencyMissing;
    }

    public void openMenu(Player player) {
        if (!enabled) {
            if (dependencyMissing) {
                Msg.to(player, "&cLuckySky-Duels benötigt das Duels-Plugin.");
            } else {
                Msg.to(player, "&cKeine Duels-Arena konfiguriert.");
            }
            return;
        }
        gui.open(player);
    }

    public boolean selectArena(String id, CommandSender sender) {
        if (!arenaService.selectArena(id)) {
            Msg.to(sender, "&cUnbekannte Arena: &f" + id);
            return false;
        }
        Msg.to(sender, "&aAktive Arena: &f" + id);
        return true;
    }

    public boolean reset(String preset, CommandSender sender) {
        return arenaService.applyReset(preset, sender);
    }

    public boolean floor(String preset, CommandSender sender) {
        return arenaService.applyFloorPreset(preset, sender);
    }

    public boolean lava(String preset, CommandSender sender) {
        return arenaService.applyHazardPreset(preset, sender);
    }

    public boolean trap(String preset, CommandSender sender) {
        return arenaService.applyTrapPreset(preset, sender);
    }

    public void light(boolean enabled, CommandSender sender) {
        arenaService.setLight(enabled, sender);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!event.getView().getTitle().equals(gui.title())) {
            return;
        }
        if (event.getRawSlot() >= gui.size()) {
            return;
        }
        event.setCancelled(true);
        gui.actionForSlot(event.getRawSlot()).ifPresent(action -> handleGuiAction(player, action));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(gui.title())) {
            event.setCancelled(true);
        }
    }

    private void handleGuiAction(Player player, String action) {
        String[] parts = action.split(":", 2);
        String type = parts[0].toLowerCase(Locale.ROOT);
        String argument = parts.length > 1 ? parts[1] : "";
        switch (type) {
            case "arena" -> {
                if (argument.equalsIgnoreCase("next")) {
                    String next = arenaService.cycleArena();
                    Msg.to(player, next == null ? "&cKeine Arena verfügbar." : "&aArena gewechselt zu &f" + next);
                } else if (!argument.isBlank()) {
                    selectArena(argument, player);
                }
            }
            case "reset" -> reset(argument, player);
            case "floor" -> floor(argument, player);
            case "ceiling" -> {
                if (!argument.isBlank()) {
                    String preset = argument.startsWith("ceiling_") ? argument : "ceiling_" + argument;
                    reset(preset, player);
                }
            }
            case "lava" -> lava(argument, player);
            case "light" -> light(!argument.equalsIgnoreCase("off"), player);
            case "trap" -> trap(argument, player);
            case "command" -> {
                if (!argument.isBlank()) {
                    String command = argument.replace("%player%", player.getName());
                    player.closeInventory();
                    Bukkit.dispatchCommand(player, command);
                }
            }
            case "console" -> {
                if (!argument.isBlank()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), argument);
                    Msg.to(player, "&aBefehl ausgeführt: &f" + argument);
                }
            }
            default -> Msg.to(player, "&7Keine Aktion für &f" + action);
        }
    }
}
