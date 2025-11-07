package de.opalium.luckysky.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

public class Settings {
    public record PBlock(int x, int y, int z, String type, String data) {
    }

    public final String world;
    public final int spawnX;
    public final int spawnY;
    public final int spawnZ;
    public final float spawnYaw;
    public final float spawnPitch;

    public final int luckyX;
    public final int luckyY;
    public final int luckyZ;
    public final int luckyInterval;
    public final boolean luckyRequireAir;
    public final String luckyVariant;
    private final List<String> luckyVariants;

    public final boolean platformBig;
    private final List<PBlock> platformBlocks;

    public final int minutesDefault;
    public final int[] presets;

    public final boolean witherEnable;
    public final int witherAfterMinutes;
    public final boolean tauntEnable;
    public final int tauntEveryTicks;
    private final String[] witherTaunts;

    public final int softEveryTicks;
    public final int wipeRadius;
    public final int hardRadius;
    public final int armorstandRadius;

    public final String prefix;

    private final List<String> rewardsBossCommands;
    private final List<String> rewardsFailCommands;
    private final String rewardMode;

    private final boolean oneLife;

    private final DuelsSettings duelsSettings;

    public Settings(FileConfiguration config) {
        world = config.getString("world", "LuckySky");
        spawnX = config.getInt("spawn.x");
        spawnY = config.getInt("spawn.y");
        spawnZ = config.getInt("spawn.z");
        spawnYaw = (float) config.getDouble("spawn.yaw");
        spawnPitch = (float) config.getDouble("spawn.pitch");

        luckyX = config.getInt("lucky.pos.x");
        luckyY = config.getInt("lucky.pos.y");
        luckyZ = config.getInt("lucky.pos.z");
        luckyInterval = config.getInt("lucky.interval_ticks", 160);
        luckyRequireAir = config.getBoolean("lucky.require_air_at_target", true);
        luckyVariant = config.getString("lucky.variant", "RANDOM");
        List<String> variants = config.getStringList("lucky.variants_available");
        if (variants.isEmpty()) {
            variants.add(luckyVariant);
        }
        luckyVariants = Collections.unmodifiableList(new ArrayList<>(variants));

        platformBig = config.getBoolean("platform.big_3x3", true);
        platformBlocks = readPlatformBlocks(config);

        minutesDefault = config.getInt("durations.minutes_default", 60);
        presets = toIntArray(config.getIntegerList("durations.presets"));

        witherEnable = config.getBoolean("withers.enable", true);
        witherAfterMinutes = config.getInt("withers.spawn_after_minutes", 60);
        tauntEnable = config.getBoolean("withers.taunts.enable", true);
        tauntEveryTicks = config.getInt("withers.taunts.every_ticks", 1200);
        List<String> taunts = config.getStringList("withers.taunts.lines");
        witherTaunts = taunts.toArray(new String[0]);

        softEveryTicks = config.getInt("wipes.entity_soft_every_ticks", 3600);
        wipeRadius = config.getInt("wipes.radius", 300);
        hardRadius = config.getInt("wipes.hardwipe.radius", 1500);
        armorstandRadius = config.getInt("wipes.hardwipe.armorstand_radius", 5000);

        ConfigurationSection rewards = config.getConfigurationSection("rewards");
        if (rewards != null) {
            rewardMode = rewards.getString("mode", "all");
            rewardsBossCommands = Collections.unmodifiableList(rewards.getStringList("on_boss_kill.commands"));
            rewardsFailCommands = Collections.unmodifiableList(rewards.getStringList("on_fail.commands"));
        } else {
            rewardMode = "all";
            rewardsBossCommands = List.of();
            rewardsFailCommands = List.of();
        }

        oneLife = config.getBoolean("lives.one_life", false);

        prefix = config.getString("messages.prefix", "&b⛯ LuckySky: &r");

        ConfigurationSection duels = config.getConfigurationSection("duels");
        if (duels != null) {
            duelsSettings = new DuelsSettings(duels);
        } else {
            duelsSettings = DuelsSettings.disabled();
        }
    }

    private List<PBlock> readPlatformBlocks(FileConfiguration config) {
        List<PBlock> list = new ArrayList<>();
        for (Object raw : config.getMapList("platform.base.blocks")) {
            if (!(raw instanceof Map<?, ?> map)) {
                continue;
            }
            int x = getInt(map, "x", 0);
            int y = getInt(map, "y", 100);
            int z = getInt(map, "z", 0);
            String type = getString(map, "type", "PRISMARINE_BRICKS");
            String data = map.containsKey("data") ? String.valueOf(map.get("data")) : "";
            list.add(new PBlock(x, y, z, type, data));
        }
        return Collections.unmodifiableList(list);
    }

    private int getInt(Map<?, ?> map, String key, int def) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
                // fall through to default
            }
        }
        return def;
    }

    private String getString(Map<?, ?> map, String key, String def) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : def;
    }

    private int[] toIntArray(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    public List<PBlock> getPlatformBlocks() {
        return platformBlocks;
    }

    public int platformY() {
        return platformBlocks.isEmpty() ? 100 : platformBlocks.get(0).y();
    }

    public Vector luckyCenter() {
        return new Vector(luckyX, luckyY, luckyZ);
    }

    public String[] witherTaunts() {
        return witherTaunts;
    }

    public List<String> rewardsBossCommands() {
        return rewardsBossCommands;
    }

    public List<String> rewardsFailCommands() {
        return rewardsFailCommands;
    }

    public String rewardMode() {
        return rewardMode;
    }

    public boolean oneLife() {
        return oneLife;
    }

    public List<String> luckyVariants() {
        return luckyVariants;
    }

    public DuelsSettings duels() {
        return duelsSettings;
    }

    public static final class DuelsSettings {
        private final boolean enabled;
        private final boolean requirePlugin;
        private final GuiSettings gui;
        private final Map<String, String> kitMappings;

        private DuelsSettings(boolean enabled, boolean requirePlugin, GuiSettings gui, Map<String, String> kitMappings) {
            this.enabled = enabled;
            this.requirePlugin = requirePlugin;
            this.gui = gui;
            this.kitMappings = kitMappings;
        }

        public DuelsSettings(ConfigurationSection section) {
            this(
                    section.getBoolean("enabled", false),
                    section.getBoolean("requirePlugin", true),
                    GuiSettings.fromSection(section.getConfigurationSection("gui")),
                    readKitMappings(section.getConfigurationSection("kitMappings"))
            );
        }

        public static DuelsSettings disabled() {
            return new DuelsSettings(false, true, GuiSettings.empty(), Map.of());
        }

        public boolean enabled() {
            return enabled;
        }

        public boolean requirePlugin() {
            return requirePlugin;
        }

        public GuiSettings gui() {
            return gui;
        }

        public Map<String, String> kitMappings() {
            return kitMappings;
        }

        private static Map<String, String> readKitMappings(ConfigurationSection section) {
            if (section == null) {
                return Map.of();
            }
            Map<String, String> map = new LinkedHashMap<>();
            for (String key : section.getKeys(false)) {
                String value = section.getString(key);
                if (value != null && !value.isBlank()) {
                    map.put(key.toUpperCase(Locale.ROOT), value);
                }
            }
            return Collections.unmodifiableMap(map);
        }

        public static final class GuiSettings {
            private final String title;
            private final int rows;
            private final Map<Integer, GuiItem> items;

            private GuiSettings(String title, int rows, Map<Integer, GuiItem> items) {
                this.title = title;
                this.rows = rows;
                this.items = items;
            }

            public static GuiSettings fromSection(ConfigurationSection section) {
                if (section == null) {
                    return empty();
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
                        List<String> lore = itemSection.getStringList("lore");
                        String command = itemSection.getString("command", "");
                        items.put(slot, new GuiItem(slot, material, name,
                                Collections.unmodifiableList(new ArrayList<>(lore)), command));
                    }
                }
                return new GuiSettings(title, rows, Collections.unmodifiableMap(items));
            }

            public static GuiSettings empty() {
                return new GuiSettings("&8LuckySky Duels", 1, Map.of());
            }

            public String title() {
                return title;
            }

            public int rows() {
                return rows;
            }

            public Map<Integer, GuiItem> items() {
                return items;
            }
        }

        public static final class GuiItem {
            private final int slot;
            private final String material;
            private final String name;
            private final List<String> lore;
            private final String command;

            public GuiItem(int slot, String material, String name, List<String> lore, String command) {
                this.slot = slot;
                this.material = material;
                this.name = name;
                this.lore = lore;
                this.command = command;
            }

            public int slot() {
                return slot;
            }

            public String material() {
                return material;
            }

            public String name() {
                return name;
            }

            public List<String> lore() {
                return lore;
            }

            public String command() {
                return command;
            }
        }
    }
}
