package de.opalium.luckysky.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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

    private final DuelsSettings duels;

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

        duels = readDuels(config.getConfigurationSection("duels"));
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
        return duels;
    }

    private DuelsSettings readDuels(ConfigurationSection section) {
        if (section == null) {
            return new DuelsSettings(false, "Duels", new DuelsMenuSettings("&8Duels", 27,
                    null, Collections.emptyMap()));
        }
        boolean enabled = section.getBoolean("enabled", false);
        String pluginName = section.getString("plugin", "Duels");
        DuelsMenuSettings menu = readDuelsMenu(section.getConfigurationSection("menu"));
        return new DuelsSettings(enabled, pluginName, menu);
    }

    private DuelsMenuSettings readDuelsMenu(ConfigurationSection section) {
        if (section == null) {
            return new DuelsMenuSettings("&8Duels", 27, null, Collections.emptyMap());
        }
        String title = section.getString("title", "&8Duels");
        int size = section.getInt("size", 27);
        DuelsMenuItem fill = readMenuItem(section.getConfigurationSection("fill"));
        Map<Integer, DuelsMenuItem> items = new LinkedHashMap<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    DuelsMenuItem item = readMenuItem(itemsSection.getConfigurationSection(key));
                    if (item != null) {
                        items.put(slot, item);
                    }
                } catch (NumberFormatException ignored) {
                    // ignore invalid keys
                }
            }
        }
        return new DuelsMenuSettings(title, size, fill, Collections.unmodifiableMap(items));
    }

    private DuelsMenuItem readMenuItem(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String material = section.getString("material", "AIR");
        String name = section.getString("name", "");
        List<String> lore = section.getStringList("lore");
        List<String> commands = section.getStringList("commands");
        boolean close = section.getBoolean("close", false);
        return new DuelsMenuItem(material, name, Collections.unmodifiableList(new ArrayList<>(lore)),
                Collections.unmodifiableList(new ArrayList<>(commands)), close);
    }

    public record DuelsSettings(boolean enabled, String pluginName, DuelsMenuSettings menu) {
    }

    public record DuelsMenuSettings(String title, int size, DuelsMenuItem fillItem,
                                    Map<Integer, DuelsMenuItem> items) {
    }

    public record DuelsMenuItem(String material, String name, List<String> lore,
                                List<String> commands, boolean close) {
    }
}
