package de.opalium.luckysky.model;

import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public class Settings {
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

    public final boolean platformBig;

    public final int minutesDefault;
    public final int[] presets;

    public final boolean witherEnable;
    public final int witherAfterMinutes;
    public final boolean tauntEnable;
    public final int tauntEveryTicks;

    public final int softEveryTicks;
    public final int wipeRadius;
    public final int hardRadius;
    public final int armorstandRadius;

    public final String prefix;

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

        platformBig = config.getBoolean("platform.big_3x3", true);

        minutesDefault = config.getInt("durations.minutes_default", 60);
        presets = toIntArray(config.getIntegerList("durations.presets"));

        witherEnable = config.getBoolean("withers.enable", true);
        witherAfterMinutes = config.getInt("withers.spawn_after_minutes", 60);
        tauntEnable = config.getBoolean("withers.taunts.enable", true);
        tauntEveryTicks = config.getInt("withers.taunts.every_ticks", 1200);

        softEveryTicks = config.getInt("wipes.entity_soft_every_ticks", 3600);
        wipeRadius = config.getInt("wipes.radius", 300);
        hardRadius = config.getInt("wipes.hardwipe.radius", 1500);
        armorstandRadius = config.getInt("wipes.hardwipe.armorstand_radius", 5000);

        prefix = config.getString("messages.prefix", "LuckySky: ");
    }

    private int[] toIntArray(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).toArray();
    }
}
