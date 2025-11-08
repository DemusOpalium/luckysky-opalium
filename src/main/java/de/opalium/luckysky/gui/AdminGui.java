package de.opalium.luckysky.gui;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class AdminGui implements Listener {
    private static final String TITLE = ChatColor.DARK_AQUA + "LuckySky Admin";
    private static final int SIZE = 27;

    private final LuckySkyPlugin plugin;

    public AdminGui(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, SIZE, TITLE);
        populate(inventory);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= SIZE) {
            return;
        }
        switch (slot) {
            case 10 -> handleStartCountdown(player);
            case 11 -> handleStopToLobby(player);
            case 12 -> handleDuration(player, 5);
            case 13 -> handleDuration(player, 20);
            case 14 -> handleDuration(player, 60);
            case 15 -> handleTauntToggle(player);
            case 16 -> handleWitherToggle(player);
            case 19 -> handleLuckyVariant(player);
            case 20 -> handleSoftWipe(player);
            case 21 -> handleHardWipe(player);
            case 22 -> handleBind(player);
            case 23 -> handlePlatform(player);
            case 24 -> handleTeleport(player);
            case 25 -> handleSave(player);
            default -> {
            }
        }
        Bukkit.getScheduler().runTask(plugin, () -> open(player));
    }

    private void populate(Inventory inventory) {
        GameConfig game = plugin.configs().game();
        TrapsConfig traps = plugin.configs().traps();
        boolean running = plugin.game().state() == de.opalium.luckysky.game.GameState.RUNNING;
        inventory.setItem(10, GuiItems.button(Material.LIME_DYE, "&aStart Countdown",
                List.of("&7Startet das Spiel und teleportiert zur Plattform."), running));
        inventory.setItem(11, GuiItems.button(Material.BARRIER, "&cStop & Lobby",
                List.of("&7Stoppt das Spiel und sendet alle zur Lobby."), false));
        inventory.setItem(12, GuiItems.button(Material.CLOCK, "&eMode 5",
                List.of("&7Setzt Dauer auf 5 Minuten."), false));
        inventory.setItem(13, GuiItems.button(Material.CLOCK, "&eMode 20",
                List.of("&7Setzt Dauer auf 20 Minuten."), false));
        inventory.setItem(14, GuiItems.button(Material.CLOCK, "&eMode 60",
                List.of("&7Setzt Dauer auf 60 Minuten."), false));
        boolean tauntsEnabled = traps.withers().taunts().enabled();
        boolean witherEnabled = traps.withers().enabled();
        inventory.setItem(15, GuiItems.button(Material.GOAT_HORN, tauntsEnabled ? "&aTaunts AN" : "&cTaunts AUS",
                List.of("&7Schaltet Wither-Taunts um."), tauntsEnabled));
        inventory.setItem(16, GuiItems.button(Material.WITHER_SKELETON_SKULL, witherEnabled ? "&aWither AN" : "&cWither AUS",
                List.of("&7Aktiviert/Deaktiviert Wither-Spawns."), witherEnabled));
        inventory.setItem(19, GuiItems.button(Material.SPONGE, "&bLucky-Variante",
                List.of("&7Aktuell: &f" + game.lucky().variant()), false));
        inventory.setItem(20, GuiItems.button(Material.FEATHER, "&bSoft-Wipe",
                List.of("&7Entfernt Effekte um Lucky."), false));
        inventory.setItem(21, GuiItems.button(Material.NETHERITE_SWORD, "&cHard-Wipe",
                List.of("&7Entfernt Entities großflächig."), false));
        inventory.setItem(22, GuiItems.button(Material.RESPAWN_ANCHOR, "&bBind",
                List.of("&7Setzt Spawn für alle."), false));
        inventory.setItem(23, GuiItems.button(Material.PRISMARINE_BRICKS, "&bPlattform",
                List.of("&7Baut Safe-Plattform."), false));
        inventory.setItem(24, GuiItems.button(Material.ENDER_PEARL, "&dTeleport",
                List.of("&7Teleportiert dich zum Spawn."), false));
        inventory.setItem(25, GuiItems.button(Material.NAME_TAG, "&aSave Config",
                List.of("&7Speichert & läd Config neu."), false));
    }

    private void handleStartCountdown(Player player) {
        plugin.game().start();
        Msg.to(player, "&aCountdown gestartet.");
    }

    private void handleStopToLobby(Player player) {
        plugin.game().stop();
        Msg.to(player, "&eGame gestoppt & Lobby.");
    }

    private void handleDuration(Player player, int minutes) {
        plugin.game().setDurationMinutes(minutes);
        Msg.to(player, "&aDauer auf " + minutes + " Minuten gesetzt.");
    }

    private void handleTauntToggle(Player player) {
        TrapsConfig traps = plugin.configs().traps();
        boolean newValue = !traps.withers().taunts().enabled();
        plugin.configs().updateTraps(traps.withTauntsEnabled(newValue));
        plugin.reloadSettings();
        plugin.game().setTauntsEnabled(newValue);
        Msg.to(player, newValue ? "&aTaunts aktiviert." : "&cTaunts deaktiviert.");
    }

    private void handleWitherToggle(Player player) {
        TrapsConfig traps = plugin.configs().traps();
        boolean newValue = !traps.withers().enabled();
        plugin.configs().updateTraps(traps.withWithersEnabled(newValue));
        plugin.reloadSettings();
        plugin.game().setWitherEnabled(newValue);
        Msg.to(player, newValue ? "&aWither aktiviert." : "&cWither deaktiviert.");
    }

    private void handleLuckyVariant(Player player) {
        GameConfig game = plugin.configs().game();
        List<String> variants = game.lucky().variantsAvailable();
        String current = game.lucky().variant();
        int index = variants.indexOf(current);
        int nextIndex = variants.isEmpty() ? 0 : (index + 1) % variants.size();
        String next = variants.isEmpty() ? current : variants.get(nextIndex);
        plugin.configs().updateGame(game.withLuckyVariant(next));
        plugin.reloadSettings();
        Msg.to(player, "&bLucky-Variante jetzt: &f" + next);
    }

    private void handleSoftWipe(Player player) {
        int removed = plugin.game().softClear();
        Msg.to(player, "&7Soft-Wipe entfernt: &f" + removed);
    }

    private void handleHardWipe(Player player) {
        int removed = plugin.game().hardClear();
        Msg.to(player, "&7Hard-Wipe entfernt: &f" + removed);
    }

    private void handleBind(Player player) {
        plugin.game().bindAll();
        Msg.to(player, "&bAlle gebunden.");
    }

    private void handlePlatform(Player player) {
        plugin.game().placePlatform();
        Msg.to(player, "&bPlattform gesetzt.");
    }

    private void handleTeleport(Player player) {
        WorldsConfig.LuckyWorld worldConfig = plugin.configs().worlds().luckySky();
        World world = Worlds.require(worldConfig.worldName());
        WorldsConfig.Spawn spawn = worldConfig.spawn();
        Location location = new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
        player.teleport(location);
        Msg.to(player, "&dTeleportiert.");
    }

    private void handleSave(Player player) {
        plugin.configs().saveAll();
        plugin.reloadSettings();
        Msg.to(player, "&aConfig gespeichert.");
    }
}
