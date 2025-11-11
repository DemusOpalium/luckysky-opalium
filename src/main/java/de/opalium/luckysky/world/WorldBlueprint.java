package de.opalium.luckysky.world;

import de.opalium.luckysky.config.WorldsConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public record WorldBlueprint(
        String worldName,
        Environment environment,
        WorldType worldType,
        Spawn spawn,
        Map<String, String> gamerules,
        boolean autoLoad,
        boolean keepSpawnLoaded) {

    public static WorldBlueprint from(WorldsConfig.LuckyWorld defaults) {
        WorldsConfig.Spawn spawn = defaults.spawn();
        Spawn blueprintSpawn = spawn == null
                ? new Spawn(0.0, 80.0, 0.0, 0f, 0f)
                : new Spawn(spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
        return new WorldBlueprint(
                defaults.worldName(),
                Environment.NORMAL,
                WorldType.NORMAL,
                blueprintSpawn,
                Collections.emptyMap(),
                true,
                true);
    }

    public static Optional<WorldBlueprint> load(Path path, WorldsConfig.LuckyWorld defaults) {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(path.toFile());
        String worldName = configuration.getString("world", defaults.worldName());
        Environment environment = parseEnvironment(configuration.getString("environment"))
                .orElse(Environment.NORMAL);
        WorldType worldType = parseWorldType(configuration.getString("worldType"))
                .orElse(WorldType.NORMAL);
        Spawn spawn = readSpawn(configuration.getConfigurationSection("spawn"), defaults.spawn());
        Map<String, String> gamerules = readGameRules(configuration.getConfigurationSection("gamerules"));
        ConfigurationSection mv = configuration.getConfigurationSection("multiverse");
        boolean autoLoad = mv == null || mv.getBoolean("autoLoad", true);
        boolean keepSpawnLoaded = mv == null || mv.getBoolean("keepSpawnLoaded", true);
        return Optional.of(new WorldBlueprint(worldName, environment, worldType, spawn, gamerules, autoLoad, keepSpawnLoaded));
    }

    private static Spawn readSpawn(ConfigurationSection section, WorldsConfig.Spawn fallback) {
        if (section == null) {
            if (fallback == null) {
                return new Spawn(0.0, 80.0, 0.0, 0f, 0f);
            }
            return new Spawn(fallback.x(), fallback.y(), fallback.z(), fallback.yaw(), fallback.pitch());
        }
        double x = section.getDouble("x", fallback != null ? fallback.x() : 0.0);
        double y = section.getDouble("y", fallback != null ? fallback.y() : 0.0);
        double z = section.getDouble("z", fallback != null ? fallback.z() : 0.0);
        float yaw = (float) section.getDouble("yaw", fallback != null ? fallback.yaw() : 0.0);
        float pitch = (float) section.getDouble("pitch", fallback != null ? fallback.pitch() : 0.0);
        return new Spawn(x, y, z, yaw, pitch);
    }

    private static Map<String, String> readGameRules(ConfigurationSection section) {
        if (section == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value == null) {
                continue;
            }
            map.put(key, formatGameRuleValue(value));
        }
        return map;
    }

    private static String formatGameRuleValue(Object value) {
        if (value instanceof Boolean bool) {
            return Boolean.toString(bool);
        }
        if (value instanceof Number number) {
            return String.valueOf(number.intValue());
        }
        return value.toString();
    }

    private static Optional<Environment> parseEnvironment(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Environment.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static Optional<WorldType> parseWorldType(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(WorldType.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public record Spawn(double x, double y, double z, float yaw, float pitch) {
    }
}
