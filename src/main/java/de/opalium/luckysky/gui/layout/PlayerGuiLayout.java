package de.opalium.luckysky.gui.layout;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.util.Msg;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class PlayerGuiLayout {
    private final String title;
    private final int size;
    private final ItemDisplay filler;
    private final Map<Integer, Button> buttonsBySlot;

    private PlayerGuiLayout(String title, int size, ItemDisplay filler, Map<Integer, Button> buttonsBySlot) {
        this.title = title;
        this.size = size;
        this.filler = filler;
        this.buttonsBySlot = buttonsBySlot;
    }

    public static PlayerGuiLayout load(LuckySkyPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "player-gui.yml");
        if (!file.exists()) {
            plugin.saveResource("player-gui.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Logger logger = plugin.getLogger();

        String title = yaml.getString("title", "&bLuckySky");
        int size = Math.max(9, yaml.getInt("size", 54));
        if (size % 9 != 0) {
            size = ((size / 9) + 1) * 9;
        }
        ItemDisplay defaultFiller = new ItemDisplay(Material.LIGHT_BLUE_STAINED_GLASS_PANE, " ", List.of(), false);
        ItemDisplay filler = ItemDisplay.from(yaml.getConfigurationSection("filler"), defaultFiller, logger);
        if (filler == null) {
            filler = defaultFiller;
        }

        Map<Integer, Button> buttons = new HashMap<>();
        ConfigurationSection buttonsSection = yaml.getConfigurationSection("buttons");
        if (buttonsSection != null) {
            Set<Integer> occupiedSlots = new HashSet<>();
            for (String id : buttonsSection.getKeys(false)) {
                ConfigurationSection section = buttonsSection.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }
                int slot = section.getInt("slot", -1);
                if (slot < 0) {
                    logger.warning("[LuckySky] player-gui.yml button '" + id + "' missing slot. Skipping.");
                    continue;
                }
                if (slot >= size) {
                    logger.warning("[LuckySky] player-gui.yml button '" + id + "' uses slot " + slot
                            + " outside of inventory size " + size + ". Skipping.");
                    continue;
                }
                if (!occupiedSlots.add(slot)) {
                    logger.warning("[LuckySky] player-gui.yml duplicate button slot " + slot + " (" + id + "). Skipping.");
                    continue;
                }
                Action action = Action.from(section.getString("action"));
                if (action == null) {
                    logger.warning("[LuckySky] player-gui.yml button '" + id + "' has unknown action. Skipping.");
                    continue;
                }
                ItemDisplay display = ItemDisplay.from(section.getConfigurationSection("display"), null, logger);
                if (display == null) {
                    logger.warning("[LuckySky] player-gui.yml button '" + id + "' missing display. Skipping.");
                    continue;
                }
                String argument = section.getString("argument", "");
                Map<String, String> placeholders = readPlaceholders(section.getConfigurationSection("placeholders"));
                List<String> messages = section.getStringList("messages");
                buttons.put(slot, new Button(id, action, argument, slot, display,
                        Collections.unmodifiableMap(placeholders), List.copyOf(messages)));
            }
        }

        return new PlayerGuiLayout(title, size, filler, Collections.unmodifiableMap(buttons));
    }

    private static Map<String, String> readPlaceholders(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>();
        for (String key : section.getKeys(false)) {
            String value = section.getString(key);
            if (value != null) {
                map.put(key, value);
            }
        }
        return map;
    }

    public String title() {
        return title;
    }

    public int size() {
        return size;
    }

    public ItemDisplay filler() {
        return filler;
    }

    public List<Button> buttons() {
        return new ArrayList<>(buttonsBySlot.values());
    }

    public Optional<Button> buttonAt(int slot) {
        return Optional.ofNullable(buttonsBySlot.get(slot));
    }

    public enum Action {
        JOIN("join", "beitreten"),
        LEAVE("leave", "verlassen"),
        START("start", "start_vote", "start-vote"),
        LOBBY("lobby", "spawn", "teleport"),
        RULES("rules", "info", "message"),
        SCOREBOARD("scoreboard", "scoreboard_toggle", "toggle_scoreboard", "scoreboard-toggle");

        private final Set<String> keys;

        Action(String... aliases) {
            Set<String> set = new HashSet<>();
            for (String alias : aliases) {
                set.add(normalize(alias));
            }
            this.keys = Set.copyOf(set);
        }

        private static String normalize(String value) {
            return value.toLowerCase(Locale.ROOT).replace('-', '_');
        }

        public static Action from(String raw) {
            if (raw == null) {
                return null;
            }
            String normalized = normalize(raw);
            for (Action action : values()) {
                if (action.keys.contains(normalized)) {
                    return action;
                }
            }
            return null;
        }
    }

    public record Button(String id, Action action, String argument, int slot,
            ItemDisplay display, Map<String, String> placeholders, List<String> messages) {
    }

    public record ItemDisplay(Material material, String name, List<String> lore, boolean glow) {
        private static final ItemFlag[] BASE_FLAGS = {
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE
        };

        static ItemDisplay from(ConfigurationSection section, ItemDisplay fallback, Logger logger) {
            if (section == null) {
                return fallback;
            }
            String materialName = section.getString("material");
            Material material = null;
            if (materialName != null) {
                material = Material.matchMaterial(materialName, true);
                if (material == null) {
                    logger.warning("[LuckySky] Unknown material '" + materialName
                            + "' in player-gui.yml. Using fallback.");
                }
            }
            if (material == null) {
                if (fallback == null) {
                    return null;
                }
                material = fallback.material;
            }
            String name = section.contains("name") ? section.getString("name")
                    : fallback != null ? fallback.name : null;
            List<String> lore;
            if (section.isList("lore")) {
                lore = List.copyOf(section.getStringList("lore"));
            } else if (fallback != null) {
                lore = fallback.lore;
            } else {
                lore = List.of();
            }
            boolean glow = section.getBoolean("glow", fallback != null && fallback.glow);
            return new ItemDisplay(material, name, lore, glow);
        }

        public ItemStack render(Map<String, String> placeholders) {
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return item;
            }
            if (name != null && !name.isBlank()) {
                meta.setDisplayName(Msg.color(apply(name, placeholders)));
            }
            if (!lore.isEmpty()) {
                List<String> rendered = new ArrayList<>(lore.size());
                for (String line : lore) {
                    rendered.add(Msg.color(apply(line, placeholders)));
                }
                meta.setLore(rendered);
            }
            meta.addItemFlags(BASE_FLAGS);
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            }
            item.setItemMeta(meta);
            return item;
        }

        private static String apply(String text, Map<String, String> placeholders) {
            String result = text;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            return result;
        }
    }
}
