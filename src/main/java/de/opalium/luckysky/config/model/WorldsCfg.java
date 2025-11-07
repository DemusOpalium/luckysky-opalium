package de.opalium.luckysky.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class WorldsCfg {
    private final String primaryId;
    private final Map<String, WorldCfg> worlds;

    private WorldsCfg(String primaryId, Map<String, WorldCfg> worlds) {
        this.primaryId = primaryId;
        this.worlds = worlds;
    }

    public static WorldsCfg from(YamlConfiguration yaml, Logger logger) {
        String primaryId = yaml.getString("primary");
        Map<String, WorldCfg> worlds = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("worlds");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection worldSection = section.getConfigurationSection(id);
                if (worldSection == null) {
                    continue;
                }
                try {
                    WorldCfg world = WorldCfg.fromSection(id, worldSection, logger);
                    worlds.put(id, world);
                } catch (IllegalArgumentException ex) {
                    logger.warning("Failed to load world config '" + id + "': " + ex.getMessage());
                }
            }
        }
        if (worlds.isEmpty()) {
            logger.warning("No world definitions found in worlds.yml; falling back to built-in defaults.");
            WorldCfg fallback = WorldCfg.defaultWorld();
            worlds.put("default", fallback);
        }
        if (primaryId == null || !worlds.containsKey(primaryId)) {
            primaryId = worlds.keySet().iterator().next();
        }
        return new WorldsCfg(primaryId, Collections.unmodifiableMap(worlds));
    }

    public String primaryId() {
        return primaryId;
    }

    public WorldCfg primary() {
        return worlds.get(primaryId);
    }

    public Map<String, WorldCfg> all() {
        return worlds;
    }

    public WorldCfg require(String id) {
        WorldCfg cfg = worlds.get(id);
        if (cfg == null) {
            throw new IllegalArgumentException("Unknown world config: " + id);
        }
        return cfg;
    }

    public record WorldCfg(String id, String worldName, Spawn spawn, Lucky lucky, Platform platform) {
        private static WorldCfg fromSection(String id, ConfigurationSection section, Logger logger) {
            String worldName = section.getString("worldName", id);
            Spawn spawn = Spawn.fromSection(section.getConfigurationSection("spawn"));
            Lucky lucky = Lucky.fromSection(section.getConfigurationSection("lucky"));
            Platform platform = Platform.fromSection(section.getConfigurationSection("platform"), logger);
            return new WorldCfg(id, worldName, spawn, lucky, platform);
        }

        private static WorldCfg defaultWorld() {
            return new WorldCfg("default", "LuckySky",
                    new Spawn(0, 101, 2, 180.0f, 0.0f),
                    new Lucky(0, 102, 6, 160, true, "RANDOM", List.of("RANDOM", "CLASSIC", "CHAOS")),
                    Platform.defaultPlatform());
        }
    }

    public record Spawn(int x, int y, int z, float yaw, float pitch) {
        private static Spawn fromSection(ConfigurationSection section) {
            if (section == null) {
                return new Spawn(0, 101, 2, 180.0f, 0.0f);
            }
            int x = section.getInt("x", 0);
            int y = section.getInt("y", 101);
            int z = section.getInt("z", 2);
            float yaw = (float) section.getDouble("yaw", 180.0);
            float pitch = (float) section.getDouble("pitch", 0.0);
            return new Spawn(x, y, z, yaw, pitch);
        }
    }

    public record Lucky(int x, int y, int z, int intervalTicks, boolean requireAirAtTarget,
                        String variant, List<String> variants) {
        private static Lucky fromSection(ConfigurationSection section) {
            if (section == null) {
                return new Lucky(0, 102, 6, 160, true, "RANDOM", List.of("RANDOM"));
            }
            int x = section.getInt("x", 0);
            int y = section.getInt("y", 102);
            int z = section.getInt("z", 6);
            int intervalTicks = section.getInt("interval_ticks", 160);
            boolean requireAirAtTarget = section.getBoolean("require_air_at_target", true);
            String variant = section.getString("variant", "RANDOM");
            List<String> variants = new ArrayList<>(section.getStringList("variants_available"));
            if (variants.isEmpty()) {
                variants.add(variant);
            }
            return new Lucky(x, y, z, intervalTicks, requireAirAtTarget, variant,
                    Collections.unmodifiableList(variants));
        }
    }

    public record Platform(boolean big3x3, List<Block> baseBlocks) {
        private static Platform fromSection(ConfigurationSection section, Logger logger) {
            boolean big3x3 = false;
            List<Block> blocks = new ArrayList<>();
            if (section != null) {
                big3x3 = section.getBoolean("big_3x3", true);
                ConfigurationSection base = section.getConfigurationSection("base");
                if (base != null) {
                    for (Map<?, ?> raw : base.getMapList("blocks")) {
                        try {
                            blocks.add(Block.fromMap(raw));
                        } catch (IllegalArgumentException ex) {
                            logger.warning("Invalid platform block definition: " + ex.getMessage());
                        }
                    }
                }
            }
            if (blocks.isEmpty()) {
                blocks.add(new Block(0, 100, -1, "PRISMARINE_STAIRS", "facing=south,half=bottom,shape=straight"));
                blocks.add(new Block(0, 100, 0, "PRISMARINE_STAIRS", "facing=south,half=bottom,shape=straight"));
                blocks.add(new Block(0, 100, 1, "PRISMARINE_BRICKS", ""));
                blocks.add(new Block(0, 100, 2, "PRISMARINE_BRICKS", ""));
            }
            return new Platform(big3x3, Collections.unmodifiableList(blocks));
        }

        private static Platform defaultPlatform() {
            return new Platform(true, List.of(
                    new Block(0, 100, -1, "PRISMARINE_STAIRS", "facing=south,half=bottom,shape=straight"),
                    new Block(0, 100, 0, "PRISMARINE_STAIRS", "facing=south,half=bottom,shape=straight"),
                    new Block(0, 100, 1, "PRISMARINE_BRICKS", ""),
                    new Block(0, 100, 2, "PRISMARINE_BRICKS", "")
            ));
        }
    }

    public record Block(int x, int y, int z, String type, String data) {
        private static Block fromMap(Map<?, ?> map) {
            if (map == null) {
                throw new IllegalArgumentException("entry missing values");
            }
            int x = getInt(map, "x", 0);
            int y = getInt(map, "y", 100);
            int z = getInt(map, "z", 0);
            String type = getString(map, "type", "PRISMARINE_BRICKS");
            String data = getString(map, "data", "");
            return new Block(x, y, z, type, data);
        }

        private static int getInt(Map<?, ?> map, String key, int def) {
            Object value = map.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String s) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException ignored) {
                }
            }
            return def;
        }

        private static String getString(Map<?, ?> map, String key, String def) {
            Object value = map.get(key);
            return value != null ? String.valueOf(value) : def;
        }
    }
}
