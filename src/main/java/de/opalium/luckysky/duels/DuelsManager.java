package de.opalium.luckysky.duels;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.gui.GuiItems;
import de.opalium.luckysky.model.Settings;
import de.opalium.luckysky.util.Msg;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class DuelsManager implements Listener {
    private final LuckySkyPlugin plugin;

    private boolean enabled;
    private boolean dependencyMissing;
    private String menuTitle;
    private int menuSize;
    private ItemStack[] menuTemplate = new ItemStack[0];
    private Map<Integer, Settings.DuelsSettings.GuiItem> menuItems = Map.of();

    public DuelsManager(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        Settings settings = plugin.settings();
        Settings.DuelsSettings duels = settings.duels();
        Plugin dependency = Bukkit.getPluginManager().getPlugin("Duels");
        boolean dependencyEnabled = dependency != null && dependency.isEnabled();

        dependencyMissing = duels.requirePlugin() && !dependencyEnabled;
        enabled = duels.enabled() && !dependencyMissing;

        Settings.DuelsSettings.GuiSettings gui = duels.gui();
        menuSize = Math.max(9, Math.min(54, gui.rows() * 9));
        menuTitle = Msg.color(gui.title());

        LinkedHashMap<Integer, Settings.DuelsSettings.GuiItem> items = new LinkedHashMap<>();
        ItemStack[] template = new ItemStack[menuSize];

        for (Settings.DuelsSettings.GuiItem item : gui.items().values()) {
            int slot = item.slot();
            if (slot < 0 || slot >= menuSize) {
                continue;
            }

            Material material = Material.matchMaterial(item.material());
            if (material == null) {
                material = Material.BARRIER;
            }

            ItemStack stack = GuiItems.button(material, item.name(), item.lore(), false);
            template[slot] = stack;
            items.put(slot, item);
        }

        menuTemplate = template;
        menuItems = items;

        if (enabled && menuItems.isEmpty()) {
            enabled = false;
        }
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
                Msg.to(player, "&cLuckySky-Duels ist nicht verfügbar (Duels-Plugin fehlt).");
            } else {
                Msg.to(player, "&cDie LuckySky-Duels-GUI ist deaktiviert.");
            }
            return;
        }

        Inventory inventory = Bukkit.createInventory(player, menuSize, menuTitle);
        inventory.setContents(menuTemplate.clone());
        player.openInventory(inventory);
    }

    public boolean performMappedCommand(CommandSender sender, String variant) {
        Settings.DuelsSettings duels = plugin.settings().duels();
        String mapped = duels.kitMappings().get(variant.toUpperCase(Locale.ROOT));
        if (mapped == null) {
            Msg.to(sender, "&cUnbekannte LuckySky-Variante: &f" + variant);
            return false;
        }
        if (!enabled && !sender.hasPermission("opalium.luckysky.admin")) {
            if (dependencyMissing) {
                Msg.to(sender, "&cLuckySky-Duels ist nicht verfügbar (Duels-Plugin fehlt).");
            } else {
                Msg.to(sender, "&cDie LuckySky-Duels-GUI ist deaktiviert.");
            }
            return false;
        }

        String command = "duels kit " + mapped;
        if (sender instanceof Player player) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                player.performCommand(command);
            });
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }
        Msg.to(sender, "&aDuels-Kit &f" + mapped + " &aausgeführt.");
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!menuTitle.equals(event.getView().getTitle())) {
            return;
        }
        if (event.getRawSlot() >= menuSize) {
            return;
        }

        event.setCancelled(true);
        Settings.DuelsSettings.GuiItem item = menuItems.get(event.getRawSlot());
        if (item == null) {
            return;
        }

        String command = item.command();
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.closeInventory();
            if (command != null && !command.isBlank()) {
                player.performCommand(command);
            }
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!enabled) {
            return;
        }
        if (!menuTitle.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
    }
}
