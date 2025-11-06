package de.opalium.luckysky.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

public class Settings {
    public record Vec3(int x, int y, int z) {
    }

    public record CleanSpec(int yFrom, int yTo, int radius) {
    }

    public record PlatformSpec(Vec3 center, int halfSize, CleanSpec clean) {
    }

    public record WarpSignSpec(Vec3 position, boolean protectArea) {
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

    private final PlatformSpec platform;
    private final WarpSignSpec warpSign;

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

    public Settings(FileConfiguration config) {
        ConfigurationSection root = config.getConfigurationSection("luckysky");
        String defaultWorld = config.getString("world", "LuckySky");
        world = root != null ? root.getString("world", defaultWorld) : defaultWorld;
        platform = readPlatform(root);
        warpSign = readWarpSign(root, platform.center());
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
    }

    private PlatformSpec readPlatform(ConfigurationSection root) {
        int defaultX = 0;
        int defaultY = 101;
        int defaultZ = 5;
        int defaultHalfSize = 10;
        int defaultCleanFrom = 96;
        int defaultCleanTo = 110;
        int defaultCleanRadius = 30;

        if (root == null) {
            return new PlatformSpec(new Vec3(defaultX, defaultY, defaultZ), defaultHalfSize,
                    new CleanSpec(defaultCleanFrom, defaultCleanTo, defaultCleanRadius));
        }

        ConfigurationSection platformSection = root.getConfigurationSection("platform");
        int centerX = defaultX;
        int centerY = defaultY;
        int centerZ = defaultZ;
        int halfSize = defaultHalfSize;
        int cleanFrom = defaultCleanFrom;
        int cleanTo = defaultCleanTo;
        int cleanRadius = defaultCleanRadius;
        if (platformSection != null) {
            centerX = platformSection.getInt("center.x", centerX);
            centerY = platformSection.getInt("center.y", centerY);
            centerZ = platformSection.getInt("center.z", centerZ);
            halfSize = platformSection.getInt("halfSize", halfSize);
            ConfigurationSection clean = platformSection.getConfigurationSection("clean");
            if (clean != null) {
                cleanFrom = clean.getInt("y_from", cleanFrom);
                cleanTo = clean.getInt("y_to", cleanTo);
                cleanRadius = clean.getInt("radius", cleanRadius);
            }
        }
        return new PlatformSpec(new Vec3(centerX, centerY, centerZ), halfSize,
                new CleanSpec(cleanFrom, cleanTo, cleanRadius));
    }

    private WarpSignSpec readWarpSign(ConfigurationSection root, Vec3 defaultCenter) {
        Vec3 position = defaultCenter;
        boolean protect = true;
        if (root != null) {
            ConfigurationSection warp = root.getConfigurationSection("warp_sign");
            if (warp != null) {
                int x = warp.getInt("pos.x", position.x());
                int y = warp.getInt("pos.y", position.y());
                int z = warp.getInt("pos.z", position.z());
                position = new Vec3(x, y, z);
                protect = warp.getBoolean("protect_3x3x3", protect);
            }
        }
        return new WarpSignSpec(position, protect);
    }

    private int[] toIntArray(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    public PlatformSpec platform() {
        return platform;
    }

    public WarpSignSpec warpSign() {
        return warpSign;
    }

    public int platformHalfSize() {
        return platform.halfSize();
    }

    public Vec3 platformCenter() {
        return platform.center();
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
}
