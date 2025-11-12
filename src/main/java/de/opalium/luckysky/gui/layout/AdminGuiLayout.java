package de.opalium.luckysky.gui.layout;

import de.opalium.luckysky.LuckySkyPlugin;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Runtime representation of the admin GUI layout. Everything is loaded from
 * {@code config/gui/luckysky-admin.yml} to make the menu extensible without recompiling.
 */
public final class AdminGuiLayout {
    private final Map<String, Menu> menus;
    private final Map<String, Button> buttons;
    private final String defaultMenu;

    private AdminGuiLayout(Map<String, Menu> menus, Map<String, Button> buttons, String defaultMenu) {
        this.menus = menus;
        this.buttons = buttons;
        this.defaultMenu = defaultMenu;
    }

    public static AdminGuiLayout load(LuckySkyPlugin plugin) {
        String relativePath = "config/gui/luckysky-admin.yml";
        File file = new File(plugin.getDataFolder(), relativePath);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            plugin.saveResource(relativePath, false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        Logger logger = plugin.getLogger();

        Set<String> unknownMaterials = new HashSet<>();
        ConfigurationSection defaults = yaml.getConfigurationSection("defaults");
        Material fallbackMaterial = ButtonDisplay.resolveFallbackMaterial(defaults, logger, unknownMaterials);

        Map<String, Button> buttons = readButtons(
                yaml.getConfigurationSection("buttons"), logger, fallbackMaterial, unknownMaterials);
        Map<String, Menu> menus = readMenus(yaml.getConfigurationSection("menus"), buttons, defaults, fallbackMaterial, unknownMaterials, logger);

        String defaultMenu = yaml.getString("default-menu", yaml.getString("default_menu"));
        if (defaultMenu == null || !menus.containsKey(defaultMenu)) {
            defaultMenu = menus.keySet().stream().findFirst().orElse("main");
        }
        return new AdminGuiLayout(Collections.unmodifiableMap(menus), Collections.unmodifiableMap(buttons), defaultMenu);
    }

    public Optional<Menu> menu(String id) {
        return Optional.ofNullable(menus.get(id));
    }

    public Set<String> menuIds() {
        return menus.keySet();
    }

    public Button button(String id) {
        return buttons.get(id);
    }

    public String defaultMenu() {
        return defaultMenu;
    }

    private static Map<String, Button> readButtons(ConfigurationSection section, Logger logger,
            Material fallbackMaterial, Set<String> unknownMaterials) {
        Map<String, Button> map = new HashMap<>();
        if (section == null) {
            logger.warning("[LuckySky] luckysky-admin.yml contains no buttons section. Using empty layout.");
            return map;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection buttonSection = section.getConfigurationSection(id);
            if (buttonSection == null) {
                continue;
            }
            ButtonDisplay display = ButtonDisplay.from(buttonSection.getConfigurationSection("display"), buttonSection,
                    fallbackMaterial, logger, unknownMaterials);
            if (display == null) {
                logger.warning("[LuckySky] Hiding admin GUI button '" + id
                        + "' due to invalid or missing material configuration in luckysky-admin.yml.");
            }
            Action action = Action.from(buttonSection, logger);
            if (action == null) {
                logger.warning("[LuckySky] Skipping admin GUI button '" + id
                        + "' because its action could not be loaded from luckysky-admin.yml.");
                continue;
            }
            boolean lockWhenRunning = buttonSection.getBoolean("lock-when-running", buttonSection.getBoolean("lock_when_running"));
            boolean hideWhenLocked = buttonSection.getBoolean("hide-when-locked", buttonSection.getBoolean("hide_when_locked"));
            boolean close = buttonSection.getBoolean("close-after", buttonSection.getBoolean("close_after"));
            boolean onlyWhenRunning = buttonSection.getBoolean("only-when-running", buttonSection.getBoolean("only_when_running"));
            boolean onlyWhenIdle = buttonSection.getBoolean("only-when-idle", buttonSection.getBoolean("only_when_idle"));
            String permission = trimToNull(buttonSection.getString("permission"));
            map.put(id, new Button(id, display, action, permission, lockWhenRunning, hideWhenLocked, close,
                    onlyWhenRunning, onlyWhenIdle));
        }
        return map;
    }

    private static Map<String, Menu> readMenus(ConfigurationSection section, Map<String, Button> buttons,
            ConfigurationSection defaults, Material fallbackMaterial, Set<String> unknownMaterials, Logger logger) {
        Map<String, Menu> menus = new HashMap<>();
        ButtonDisplay defaultFiller = ButtonDisplay.defaultFiller(defaults, fallbackMaterial, logger, unknownMaterials);
        if (section == null) {
            logger.warning("[LuckySky] luckysky-admin.yml contains no menus section. Using empty layout.");
            return menus;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection menuSection = section.getConfigurationSection(id);
            if (menuSection == null) {
                continue;
            }
            String title = menuSection.getString("title", "&9LuckySky");
            int size = Math.max(9, menuSection.getInt("size", 54));
            if (size % 9 != 0) {
                size = ((size / 9) + 1) * 9;
            }
            ButtonDisplay filler = ButtonDisplay.from(menuSection.getConfigurationSection("filler"), menuSection,
                    fallbackMaterial, logger, unknownMaterials);
            if (filler == null) {
                filler = defaultFiller;
            }
            String permission = trimToNull(menuSection.getString("permission"));
            Map<Integer, String> mapping = new HashMap<>();
            ConfigurationSection buttonsSection = menuSection.getConfigurationSection("buttons");
            if (buttonsSection != null) {
                for (String slotKey : buttonsSection.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(slotKey);
                        String buttonId = buttonsSection.getString(slotKey);
                        if (buttons.containsKey(buttonId)) {
                            mapping.put(slot, buttonId);
                        } else {
                            logger.warning("[LuckySky] Menu '" + id + "' references unknown button '" + buttonId
                                    + "' in luckysky-admin.yml.");
                        }
                    } catch (NumberFormatException ex) {
                        logger.warning("[LuckySky] Invalid slot '" + slotKey + "' in menu '" + id
                                + "' (luckysky-admin.yml).");
                    }
                }
            }
            menus.put(id, new Menu(id, title, size, filler, permission, Collections.unmodifiableMap(mapping)));
        }
        return menus;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    // =====================================================================
    // === DATA CLASSES ====================================================
    // =====================================================================

    public record Menu(String id, String title, int size, ButtonDisplay filler, String permission, Map<Integer, String> slotMapping) {
    }

    public record Button(String id, ButtonDisplay display, Action action, String permission,
                          boolean lockWhenRunning, boolean hideWhenLocked, boolean closeAfterClick,
                          boolean onlyWhenRunning, boolean onlyWhenIdle) {
    }

    public sealed interface Action permits Action.BuiltinAction, Action.CommandAction, Action.OpenMenuAction {
        static Action from(ConfigurationSection section, Logger logger) {
            String type = Optional.ofNullable(section.getString("type")).orElse("builtin").toLowerCase(Locale.ROOT);
            return switch (type) {
                case "command", "commands" -> {
                    Executor executor = Executor.from(section.getString("executor"));
                    List<String> commands = new ArrayList<>();
                    for (String entry : section.getStringList("commands")) {
                        if (!entry.isBlank()) {
                            commands.add(entry);
                        }
                    }
                    if (commands.isEmpty()) {
                    logger.warning("[LuckySky] Command button without commands defined in luckysky-admin.yml at "
                                + section.getCurrentPath());
                        yield null;
                    }
                    yield new CommandAction(executor, Collections.unmodifiableList(commands));
                }
                case "open", "menu", "open-menu", "open_menu" -> {
                    String target = section.getString("target");
                    if (target == null) {
                        logger.warning("[LuckySky] open-menu button without target in luckysky-admin.yml");
                    }
                    yield new OpenMenuAction(target);
                }
                default -> {
                    String key = section.getString("key", section.getString("action", ""));
                    String argument = section.getString("argument", section.getString("value"));
                    Builtin builtin = Builtin.fromKey(key);
                    yield new BuiltinAction(builtin, argument);
                }
            };
        }

        record BuiltinAction(Builtin builtin, String argument) implements Action {
        }

        record CommandAction(Executor executor, List<String> commands) implements Action {
        }

        record OpenMenuAction(String targetMenu) implements Action {
        }
    }

    public enum Executor {
        CONSOLE,
        PLAYER,
        BOTH;

        public static Executor from(String raw) {
            if (raw == null) {
                return CONSOLE;
            }
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "PLAYER" -> PLAYER;
                case "BOTH" -> BOTH;
                default -> CONSOLE;
            };
        }
    }

    // =====================================================================
    // === BUTTON DISPLAY ==================================================
    // =====================================================================

    public record ButtonDisplay(Material material, String name, List<String> lore, boolean glow) {
        private static final ButtonDisplay DEFAULT_FILLER = new ButtonDisplay(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), false);

        public static ButtonDisplay from(ConfigurationSection section, ConfigurationSection fallback,
                Material fallbackMaterial, Logger logger, Set<String> unknownMaterials) {
            if (section == null) {
                if (fallback != null && fallback.contains("material")) {
                    return fromMaterialSection(fallback, fallbackMaterial, logger, unknownMaterials);
                }
                return null;
            }
            return fromMaterialSection(section, fallbackMaterial, logger, unknownMaterials);
        }

        private static ButtonDisplay fromMaterialSection(ConfigurationSection section, Material fallbackMaterial, Logger logger,
                Set<String> unknownMaterials) {
            String configuredName = section.getString("material", section.getString("item"));
            String materialPath;
            if (section.contains("material")) {
                materialPath = section.getCurrentPath() + ".material";
            } else if (section.contains("item")) {
                materialPath = section.getCurrentPath() + ".item";
            } else {
                materialPath = section.getCurrentPath();
            }
            Material material = resolveMaterial(configuredName, fallbackMaterial, logger, unknownMaterials, materialPath);
            if (material == null) {
                return null;
            }
            String name = section.getString("name", section.getString("title", ""));
            List<String> lore = section.getStringList("lore");
            boolean glow = section.getBoolean("glow", false);
            return new ButtonDisplay(material, name, Collections.unmodifiableList(new ArrayList<>(lore)), glow);
        }

        public static ButtonDisplay defaultFiller(ConfigurationSection defaults, Material fallbackMaterial, Logger logger,
                Set<String> unknownMaterials) {
            if (defaults != null) {
                ButtonDisplay display = from(defaults.getConfigurationSection("filler"), defaults, fallbackMaterial, logger,
                        unknownMaterials);
                if (display != null) {
                    return display;
                }
            }
            return DEFAULT_FILLER;
        }

        private static Material resolveMaterial(String rawName, Material fallbackMaterial, Logger logger,
                Set<String> unknownMaterials, String configPath) {
            if (rawName == null) {
                return fallbackMaterial;
            }

            String trimmed = rawName.trim();
            if (trimmed.isEmpty()) {
                return fallbackMaterial;
            }

            Material material = Material.matchMaterial(trimmed, true);
            if (material == null) {
                String normalised = trimmed.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
                material = Material.matchMaterial(normalised, true);
            }

            if (material == null) {
                NamespacedKey key = NamespacedKey.fromString(trimmed.toLowerCase(Locale.ROOT));
                if (key != null) {
                    material = Registry.MATERIAL.get(key);
                }
            }

            if (material != null) {
                return material;
            }

            String trackingKey = trimmed.toLowerCase(Locale.ROOT);
            if (unknownMaterials.add(trackingKey)) {
                if (configPath != null && !configPath.isEmpty()) {
                    logger.warning("[LuckySky] Unknown material '" + trimmed + "' in luckysky-admin.yml at '" + configPath
                            + "'. Button will be hidden.");
                } else {
                    logger.warning("[LuckySky] Unknown material '" + trimmed + "' in luckysky-admin.yml. Button will be hidden.");
                }
            }

            return null;
        }

        private static final String FALLBACK_MATERIAL_KEY = "fallback-material";

        public static Material resolveFallbackMaterial(ConfigurationSection defaults, Logger logger,
                Set<String> unknownMaterials) {
            Material material = Material.PAPER;
            if (defaults == null) {
                return material;
            }

            String configured = defaults.getString(FALLBACK_MATERIAL_KEY,
                    defaults.getString(FALLBACK_MATERIAL_KEY.replace('-', '_')));
            if (configured == null) {
                return material;
            }

            Material resolved = resolveMaterial(configured, material, logger, unknownMaterials,
                    defaults.getCurrentPath() + "." + FALLBACK_MATERIAL_KEY);
            return resolved != null ? resolved : material;
        }
    }

    // =====================================================================
    // === BUILTIN ACTION CATALOG ==========================================
    // =====================================================================

    public enum Builtin {
        SAVE_BASE,
        LOAD_BASE,
        LOAD_SCHEMATIC,
        PASTE_SCHEMATIC,
        TOGGLE_WITHER,
        TOGGLE_TAUNTS,
        START_DURATION,
        STOP_GAME,
        TOGGLE_SCOREBOARD,
        TOGGLE_TIMER,
        SOFT_WIPE,
        HARD_WIPE,
        BIND_ALL,
        PLACE_PLATFORM,
        PLACE_PLATFORM_EXTENDED,
        TELEPORT_TO_SPAWN,
        SAVE_CONFIG,
        CYCLE_VARIANT,
        SPAWN_WITHER_NOW,
        RELOAD_PLUGIN,
        TRIGGER_REWARD_WIN,
        TRIGGER_REWARD_FAIL,
        OPEN_PORTAL,
        CLOSE_PORTAL;

        public static Builtin fromKey(String key) {
            if (key == null) {
                return SAVE_BASE;
            }
            String normalized = key.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
            for (Builtin value : values()) {
                if (value.name().equals(normalized)) {
                    return value;
                }
            }
            return SAVE_BASE;
        }
    }
}
