package de.opalium.luckysky.duels;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.DuelsConfig;
import de.opalium.luckysky.gui.GuiItems;
import de.opalium.luckysky.util.Msg;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DuelsGui {
    private final LuckySkyPlugin plugin;
    private String title = Msg.color("&8LuckySky Duels");
    private int size = 27;
    private ItemStack[] template = new ItemStack[0];
    private Map<Integer, String> actions = Map.of();

    public DuelsGui(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        DuelsConfig.Gui gui = plugin.configs().duels().gui();
        this.size = Math.max(9, Math.min(54, gui.size()));
        this.title = Msg.color(guiTitle(gui));
        ItemStack[] temp = new ItemStack[size];
        Map<Integer, String> map = new LinkedHashMap<>();
        for (Map.Entry<Integer, DuelsConfig.GuiItem> entry : gui.items().entrySet()) {
            Integer slot = entry.getKey();
            DuelsConfig.GuiItem item = entry.getValue();
            if (slot < 0 || slot >= size) {
                continue;
            }
            Material material = Material.matchMaterial(item.material().toUpperCase());
            if (material == null) {
                material = Material.BARRIER;
            }
            List<String> lore = item.lore() == null ? List.of() : item.lore();
            ItemStack stack = GuiItems.button(material, item.name(), lore, false);
            temp[slot] = stack;
            map.put(slot, item.action());
        }
        this.template = temp;
        this.actions = map;
    }

    private String guiTitle(DuelsConfig.Gui gui) {
        return "&8LuckySky Duels";
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(player, size, title);
        ItemStack[] contents = template.clone();
        inventory.setContents(contents);
        player.openInventory(inventory);
    }

    public Optional<String> actionForSlot(int slot) {
        return Optional.ofNullable(actions.get(slot));
    }

    public String title() {
        return title;
    }

    public int size() {
        return size;
    }
}
