package de.opalium.luckysky.duels;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.gui.GuiItems;
import de.opalium.luckysky.util.Msg;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

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
        File file = new File(plugin.getDataFolder(), "config/gui/duels-admin.yml");
        if (!file.exists()) {
            plugin.saveResource("config/gui/duels-admin.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Logger logger = plugin.getLogger();

        this.size = Math.max(9, Math.min(54, yaml.getInt("size", 27)));
        this.title = Msg.color(yaml.getString("title", "&8LuckySky Duels"));
        ItemStack[] temp = new ItemStack[size];
        Map<Integer, String> map = new LinkedHashMap<>();
        ConfigurationSection itemsSection = yaml.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection section = itemsSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                int slot = section.getInt("slot", -1);
                if (slot < 0 || slot >= size) {
                    logger.warning("[LuckySky] Duels-Admin-GUI: Slot " + slot + " für '" + key + "' ist ungültig.");
                    continue;
                }
                String materialName = section.getString("material", "BARRIER");
                Material material = Material.matchMaterial(materialName, true);
                if (material == null) {
                    logger.warning("[LuckySky] Duels-Admin-GUI: Unbekanntes Material '" + materialName + "' für '" + key + "'.");
                    material = Material.BARRIER;
                }
                String name = Msg.color(section.getString("name", ""));
                List<String> lore = section.getStringList("lore");
                List<String> coloredLore = lore.stream().map(Msg::color).toList();
                boolean glow = section.getBoolean("glow", false);
                ItemStack stack = GuiItems.button(material, name, coloredLore, glow);
                temp[slot] = stack;
                map.put(slot, section.getString("action", key));
            }
        }
        this.template = temp;
        this.actions = map;
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
