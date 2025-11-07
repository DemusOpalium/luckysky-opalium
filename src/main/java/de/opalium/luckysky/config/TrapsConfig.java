package de.opalium.luckysky.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record TrapsConfig(Withers withers, Effects effects) {
    public static TrapsConfig from(FileConfiguration config) {
        Withers withers = readWithers(config.getConfigurationSection("withers"));
        Effects effects = readEffects(config.getConfigurationSection("effects"));
        return new TrapsConfig(withers, effects);
    }

    private static Withers readWithers(ConfigurationSection section) {
        if (section == null) {
            return new Withers(true, 1, true, 60, new Taunts(true, 1200, List.of(
                    "Niemand entkommt dem Abyssal Wither!",
                    "Spürt die Leere!"
            )));
        }
        boolean enabled = section.getBoolean("enabled", section.getBoolean("enable", true));
        int maxParallel = section.getInt("max_parallel", 1);
        boolean announce = section.getBoolean("announce", true);
        int spawnAfterMinutes = section.getInt("spawn_after_minutes", 60);
        Taunts taunts = readTaunts(section.getConfigurationSection("taunts"));
        return new Withers(enabled, maxParallel, announce, spawnAfterMinutes, taunts);
    }

    private static Taunts readTaunts(ConfigurationSection section) {
        if (section == null) {
            return new Taunts(true, 1200, List.of(
                    "Niemand entkommt dem Abyssal Wither!",
                    "Spürt die Leere!"
            ));
        }
        boolean enabled = section.getBoolean("enable", section.getBoolean("enabled", true));
        int everyTicks = section.getInt("every_ticks", 1200);
        List<String> lines = section.getStringList("lines");
        return new Taunts(enabled, everyTicks, Collections.unmodifiableList(new ArrayList<>(lines)));
    }

    private static Effects readEffects(ConfigurationSection section) {
        if (section == null) {
            return new Effects(true);
        }
        boolean tauntsEnabled = section.getBoolean("taunts_enabled", true);
        return new Effects(tauntsEnabled);
    }

    public void writeTo(FileConfiguration config) {
        ConfigurationSection withersSection = config.createSection("withers");
        withersSection.set("enabled", withers.enabled());
        withersSection.set("max_parallel", withers.maxParallel());
        withersSection.set("announce", withers.announce());
        withersSection.set("spawn_after_minutes", withers.spawnAfterMinutes());
        ConfigurationSection tauntsSection = withersSection.createSection("taunts");
        tauntsSection.set("enabled", withers.taunts().enabled());
        tauntsSection.set("every_ticks", withers.taunts().everyTicks());
        tauntsSection.set("lines", withers.taunts().lines());

        ConfigurationSection effectsSection = config.createSection("effects");
        effectsSection.set("taunts_enabled", effects.tauntsEnabled());
    }

    public TrapsConfig withTauntsEnabled(boolean enabled) {
        Taunts taunts = new Taunts(enabled, withers.taunts().everyTicks(), withers.taunts().lines());
        return new TrapsConfig(new Withers(withers.enabled(), withers.maxParallel(), withers.announce(),
                withers.spawnAfterMinutes(), taunts), new Effects(enabled));
    }

    public TrapsConfig withWithersEnabled(boolean enabled) {
        return new TrapsConfig(new Withers(enabled, withers.maxParallel(), withers.announce(),
                withers.spawnAfterMinutes(), withers.taunts()), effects);
    }

    public Withers withers() {
        return withers;
    }

    public Effects effects() {
        return effects;
    }

    public record Withers(boolean enabled, int maxParallel, boolean announce, int spawnAfterMinutes, Taunts taunts) {
    }

    public record Taunts(boolean enabled, int everyTicks, List<String> lines) {
    }

    public record Effects(boolean tauntsEnabled) {
    }
}
