package de.opalium.luckysky.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class DuelsConfig {
    private final boolean enabled;
    private final boolean requirePlugin;
    private final GuiConfig gui;
    private final Map<String, String> kitMappings;

    private DuelsConfig(boolean enabled, boolean requirePlugin, GuiConfig gui, Map<String, String> kitMappings) {
        this.enabled = enabled;
        this.requirePlugin = requirePlugin;
        this.gui = gui;
        this.kitMappings = kitMappings;
    }

    public static DuelsConfig from(YamlConfiguration yaml) {
        boolean enabled = yaml.getBoolean("enabled", false);
        boolean requirePlugin = yaml.getBoolean("requirePlugin", true);
        GuiConfig gui = GuiConfig.fromSection(yaml.getConfigurationSection("gui"));
        Map<String, String> kitMappings = readKitMappings(yaml.getConfigurationSection("kitMappings"));
        return new DuelsConfig(enabled, requirePlugin, gui, kitMappings);
    }

    private static Map<String, String> readKitMappings(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, String> mappings = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value == null || value.isBlank()) {
                continue;
            }
            mappings.put(key.toUpperCase(Locale.ROOT), value);
        }
        return Collections.unmodifiableMap(mappings);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean requirePlugin() {
        return requirePlugin;
    }

    public GuiConfig gui() {
        return gui;
    }

    public Map<String, String> kitMappings() {
        return kitMappings;
    }

    public record GuiConfig(String title, int rows, Map<Integer, GuiItem> items) {
        private static GuiConfig fromSection(ConfigurationSection section) {
            if (section == null) {
                return new GuiConfig("&8LuckySky Duels", 1, Map.of());
            }
            String title = section.getString("title", "&8LuckySky &7• &eDuell-Kits");
            int rows = Math.max(1, Math.min(6, section.getInt("rows", 3)));
            Map<Integer, GuiItem> items = new LinkedHashMap<>();
            ConfigurationSection itemsSection = section.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                    if (itemSection == null) {
                        continue;
                    }
                    int slot = itemSection.getInt("slot", -1);
                    if (slot < 0 || slot >= rows * 9) {
                        continue;
                    }
                    String material = itemSection.getString("material", "BARRIER");
                    String name = itemSection.getString("name", "&fKit");
                    List<String> lore = new ArrayList<>(itemSection.getStringList("lore"));
                    String command = itemSection.getString("command", "");
                    items.put(slot, new GuiItem(slot, material, name,
                            Collections.unmodifiableList(lore), command));
                }
            }
            return new GuiConfig(title, rows, Collections.unmodifiableMap(items));
        }
    }

    public record GuiItem(int slot, String material, String name, List<String> lore, String command) {
    }
}
