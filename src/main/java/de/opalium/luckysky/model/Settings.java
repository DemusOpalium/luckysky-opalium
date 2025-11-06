package de.opalium.luckysky.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    }

    private List<PBlock> readPlatformBlocks(FileConfiguration config) {
        List<PBlock> list = new ArrayList<>();
        for (Object raw : config.getMapList("platform.base.blocks")) {
            if (!(raw instanceof java.util.Map<?, ?> map)) {
                continue;
            }
            int x = ((Number) map.getOrDefault("x", 0)).intValue();
            int y = ((Number) map.getOrDefault("y", 100)).intValue();
            int z = ((Number) map.getOrDefault("z", 0)).intValue();
            Object typeObj = map.getOrDefault("type", "PRISMARINE_BRICKS");
            String type = String.valueOf(typeObj);
            String data = map.containsKey("data") ? String.valueOf(map.get("data")) : "";
            list.add(new PBlock(x, y, z, type, data));
        }
        return Collections.unmodifiableList(list);
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
}
