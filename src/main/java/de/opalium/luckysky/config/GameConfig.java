package de.opalium.luckysky.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

public record GameConfig(
        Durations durations,
        Lucky lucky,
        Platform platform,
        Rig rig,
        Wipes wipes,
        Rewards rewards,
        Lives lives,
        Scoreboard scoreboard
) {
    public static GameConfig from(FileConfiguration config) {
        Durations durations = readDurations(config.getConfigurationSection("durations"));
        Lucky lucky = readLucky(config.getConfigurationSection("lucky"));
        Platform platform = readPlatform(config.getConfigurationSection("platform"));
        Rig rig = readRig(config.getConfigurationSection("rig"));
        Wipes wipes = readWipes(config.getConfigurationSection("wipes"));
        Rewards rewards = readRewards(config.getConfigurationSection("rewards"));
        Lives lives = new Lives(config.getBoolean("lives.one_life", false));
        Scoreboard scoreboard = readScoreboard(config.getConfigurationSection("scoreboard"));
        return new GameConfig(durations, lucky, platform, rig, wipes, rewards, lives, scoreboard);
    }

    private static Durations readDurations(ConfigurationSection section) {
        if (section == null) {
            return new Durations(60, List.of(30, 45, 60));
        }
        int minutesDefault = section.getInt("minutesDefault", section.getInt("minutes_default", 60));
        List<Integer> presets = section.getIntegerList("presets");
        if (presets.isEmpty()) {
            presets = List.of(minutesDefault);
        }
        return new Durations(minutesDefault, Collections.unmodifiableList(new ArrayList<>(presets)));
    }

    private static Lucky readLucky(ConfigurationSection section) {
        if (section == null) {
            return new Lucky(new Position(0, 200, 0), 160, true, "RANDOM", List.of("RANDOM"));
        }
        ConfigurationSection positionSection = section.getConfigurationSection("position");
        Position position = new Position(
                positionSection != null ? positionSection.getInt("x", 0) : section.getInt("pos.x", 0),
                positionSection != null ? positionSection.getInt("y", 200) : section.getInt("pos.y", 200),
                positionSection != null ? positionSection.getInt("z", 0) : section.getInt("pos.z", 0));
        int interval = section.getInt("interval_ticks", section.getInt("intervalTicks", 160));
        boolean requireAir = section.getBoolean("require_air_at_target", true);
        String variant = section.getString("variant", "RANDOM");
        List<String> variants = section.getStringList("variants_available");
        if (variants.isEmpty()) {
            variants = List.of(variant);
        }
        return new Lucky(position, interval, requireAir, variant, Collections.unmodifiableList(new ArrayList<>(variants)));
    }

    private static Platform readPlatform(ConfigurationSection section) {
        if (section == null) {
            return new Platform(true, List.of());
        }
        boolean big = section.getBoolean("big_3x3", true);
        List<Block> blocks = new ArrayList<>();
        for (Object raw : section.getMapList("base.blocks")) {
            if (raw instanceof Map<?, ?> map) {
                int x = getInt(map, "x", 0);
                int y = getInt(map, "y", 200);
                int z = getInt(map, "z", 0);
                Object materialObj = map.get("material");
                if (materialObj == null) {
                    materialObj = map.get("type");
                }
                String material = materialObj != null ? String.valueOf(materialObj) : "PRISMARINE_BRICKS";
                String data = map.containsKey("data") ? String.valueOf(map.get("data")) : "";
                blocks.add(new Block(x, y, z, material, data));
            }
        }
        return new Platform(big, Collections.unmodifiableList(blocks));
    }

    private static Rig readRig(ConfigurationSection section) {
        if (section == null) {
            return new Rig(201, true);
        }
        int baseHeight = section.getInt("base_height", 201);
        boolean corridorClear = section.getBoolean("corridor_clear", true);
        return new Rig(baseHeight, corridorClear);
    }

    private static Wipes readWipes(ConfigurationSection section) {
        if (section == null) {
            return new Wipes(3600, 30, 60, 100);
        }
        int softEveryTicks = section.getInt("entity_soft_every_ticks", 3600);
        int softRadius = section.getInt("soft_radius", section.getInt("radius", 30));
        int hardRadius = section.getInt("hard_radius", section.getInt("hardwipe.radius", 60));
        int armorstandRadius = section.getInt("armorstand_radius", section.getInt("hardwipe.armorstand_radius", 100));
        return new Wipes(softEveryTicks, softRadius, hardRadius, armorstandRadius);
    }

    private static Rewards readRewards(ConfigurationSection section) {
        if (section == null) {
            return new Rewards("balanced", List.of(), List.of());
        }
        String mode = section.getString("mode", "balanced");
        List<String> onBoss = section.getStringList("on_boss_kill.commands");
        List<String> onFail = section.getStringList("on_fail.commands");
        return new Rewards(mode,
                Collections.unmodifiableList(new ArrayList<>(onBoss)),
                Collections.unmodifiableList(new ArrayList<>(onFail)));
    }

    private static Scoreboard readScoreboard(ConfigurationSection section) {
        if (section == null) {
            return new Scoreboard(false, "&bLuckySky", List.of(
                    "&7Status: &f{state}",
                    "&7Timer: &f{timer}",
                    "&7Spieler: &f{players}",
                    "&7Wither: &f{wither}"
            ));
        }
        boolean enabled = section.getBoolean("enabled", section.getBoolean("active", true));
        String title = section.getString("title", "&bLuckySky");
        List<String> lines = section.getStringList("lines");
        if (lines.isEmpty()) {
            lines = List.of(
                    "&7Status: &f{state}",
                    "&7Timer: &f{timer}",
                    "&7Spieler: &f{players}",
                    "&7Wither: &f{wither}"
            );
        }
        return new Scoreboard(enabled, title,
                Collections.unmodifiableList(new ArrayList<>(lines)));
    }

    private static int getInt(Map<?, ?> map, String key, int def) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    public void writeTo(FileConfiguration config) {
        ConfigurationSection durationsSection = config.createSection("durations");
        durationsSection.set("minutesDefault", durations.minutesDefault());
        durationsSection.set("presets", durations.presets());

        ConfigurationSection luckySection = config.createSection("lucky");
        ConfigurationSection position = luckySection.createSection("position");
        position.set("x", lucky.position().x());
        position.set("y", lucky.position().y());
        position.set("z", lucky.position().z());
        luckySection.set("interval_ticks", lucky.intervalTicks());
        luckySection.set("require_air_at_target", lucky.requireAirAtTarget());
        luckySection.set("variant", lucky.variant());
        luckySection.set("variants_available", lucky.variantsAvailable());

        ConfigurationSection platformSection = config.createSection("platform");
        platformSection.set("big_3x3", platform.bigPlatform());
        List<Map<String, Object>> blocks = new ArrayList<>();
        for (Block block : platform.baseBlocks()) {
            blocks.add(Map.of(
                    "x", block.x(),
                    "y", block.y(),
                    "z", block.z(),
                    "material", block.material(),
                    "data", block.data()));
        }
        platformSection.set("base.blocks", blocks);

        ConfigurationSection rigSection = config.createSection("rig");
        rigSection.set("base_height", rig.baseHeight());
        rigSection.set("corridor_clear", rig.corridorClear());

        ConfigurationSection wipesSection = config.createSection("wipes");
        wipesSection.set("entity_soft_every_ticks", wipes.entitySoftEveryTicks());
        wipesSection.set("soft_radius", wipes.softRadius());
        wipesSection.set("hard_radius", wipes.hardRadius());
        wipesSection.set("armorstand_radius", wipes.armorstandRadius());

        ConfigurationSection rewardsSection = config.createSection("rewards");
        rewardsSection.set("mode", rewards.mode());
        rewardsSection.set("on_boss_kill.commands", rewards.onBossKill());
        rewardsSection.set("on_fail.commands", rewards.onFail());

        ConfigurationSection livesSection = config.createSection("lives");
        livesSection.set("one_life", lives.oneLife());

        ConfigurationSection scoreboardSection = config.createSection("scoreboard");
        scoreboardSection.set("enabled", scoreboard.enabled());
        scoreboardSection.set("title", scoreboard.title());
        scoreboardSection.set("lines", scoreboard.lines());
    }

    public GameConfig withLuckyVariant(String variant) {
        Lucky updated = new Lucky(lucky.position(), lucky.intervalTicks(), lucky.requireAirAtTarget(), variant,
                lucky.variantsAvailable());
        return new GameConfig(durations, updated, platform, rig, wipes, rewards, lives, scoreboard);
    }

    public record Durations(int minutesDefault, List<Integer> presets) {
    }

    public record Lucky(Position position, int intervalTicks, boolean requireAirAtTarget, String variant,
                        List<String> variantsAvailable) {
    }

    public record Position(int x, int y, int z) {
        public Vector toVector() {
            return new Vector(x, y, z);
        }
    }

    public record Platform(boolean bigPlatform, List<Block> baseBlocks) {
    }

    public record Block(int x, int y, int z, String material, String data) {
    }

    public record Rig(int baseHeight, boolean corridorClear) {
    }

    public record Wipes(int entitySoftEveryTicks, int softRadius, int hardRadius, int armorstandRadius) {
    }

    public record Rewards(String mode, List<String> onBossKill, List<String> onFail) {
    }

    public record Lives(boolean oneLife) {
    }

    public record Scoreboard(boolean enabled, String title, List<String> lines) {
    }
}
