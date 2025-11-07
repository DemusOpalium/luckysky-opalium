package de.opalium.luckysky.duels;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
import de.opalium.luckysky.util.CommandBridge;
import de.opalium.luckysky.util.Msg;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class DuelsMenu implements Listener {
    private final LuckySkyPlugin plugin;
    private final CommandBridge bridge;
    private final DuelsService service;

    private boolean enabled;
    private String title;
    private int size;
    private ItemStack filler;
    private Map<Integer, MenuButton> buttons = Collections.emptyMap();

    public DuelsMenu(LuckySkyPlugin plugin, CommandBridge bridge, DuelsService service) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.service = service;
    }

    public void reload(Settings settings) {
        Settings.DuelsSettings duels = settings.duels();
        if (duels == null) {
            enabled = false;
            title = "";
            size = 27;
            filler = null;
            buttons = Collections.emptyMap();
            return;
        }

        enabled = duels.enabled();
        Settings.DuelsMenuSettings menu = duels.menu();
        if (menu == null) {
            title = Msg.color("&8Duels");
            size = 27;
            filler = null;
            buttons = Collections.emptyMap();
            return;
        }

        title = Msg.color(menu.title());
        size = normalize(menu.size());
        filler = toItem(menu.fillItem());
        Map<Integer, MenuButton> map = new HashMap<>();
        for (Map.Entry<Integer, Settings.DuelsMenuItem> entry : menu.items().entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= size) {
                plugin.getLogger().warning("Duels menu item slot out of range: " + slot);
                continue;
            }
            ItemStack item = toItem(entry.getValue());
            if (item == null) {
                continue;
            }
            List<String> commands = new ArrayList<>(entry.getValue().commands());
            map.put(slot, new MenuButton(item, commands, entry.getValue().close()));
        }
        buttons = Collections.unmodifiableMap(map);
    }

    public void open(Player player) {
        if (!enabled) {
            Msg.to(player, "&cDas Duels-Menü ist derzeit deaktiviert.");
            return;
        }
        if (!service.isPluginPresent()) {
            Msg.to(player, "&cDuels-Integration nicht verfügbar (Plugin fehlt).");
            return;
        }
        Inventory inventory = Bukkit.createInventory(player, size, title);
        if (filler != null) {
            ItemStack fill = filler.clone();
            for (int slot = 0; slot < size; slot++) {
                inventory.setItem(slot, fill.clone());
            }
        }
        for (Map.Entry<Integer, MenuButton> entry : buttons.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().display().clone());
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!enabled) {
            return;
        }
        if (!Objects.equals(title, event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= size) {
            return;
        }
        MenuButton button = buttons.get(slot);
        if (button == null) {
            return;
        }
        if (!service.isPluginPresent()) {
            Msg.to(player, "&cDuels-Integration nicht verfügbar (Plugin fehlt).");
            Bukkit.getScheduler().runTask(plugin, player::closeInventory);
            return;
        }
        if (button.commands().isEmpty()) {
            return;
        }
        for (String command : button.commands()) {
            String parsed = command.replace("{player}", player.getName());
            bridge.dispatch(parsed);
        }
        if (button.close()) {
            Bukkit.getScheduler().runTask(plugin, player::closeInventory);
        }
    }

    private int normalize(int requested) {
        int rows = Math.max(1, Math.min(6, (requested + 8) / 9));
        return rows * 9;
    }

    private ItemStack toItem(Settings.DuelsMenuItem item) {
        if (item == null) {
            return null;
        }
        Material material = Material.matchMaterial(item.material());
        if (material == null) {
            plugin.getLogger().log(Level.WARNING, "Unknown material in duels menu: {0}", item.material());
            return null;
        }
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (item.name() != null && !item.name().isBlank()) {
                meta.setDisplayName(Msg.color(item.name()));
            }
            if (!item.lore().isEmpty()) {
                List<String> lore = new ArrayList<>(item.lore().size());
                for (String line : item.lore()) {
                    lore.add(Msg.color(line));
                }
                meta.setLore(lore);
            }
            meta.addItemFlags(ItemFlag.values());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private record MenuButton(ItemStack display, List<String> commands, boolean close) {
    }
}
