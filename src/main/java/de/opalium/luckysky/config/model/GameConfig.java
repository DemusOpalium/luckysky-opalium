package de.opalium.luckysky.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class GameConfig {
    private final DurationConfig durations;
    private final WitherConfig withers;
    private final WipeConfig wipes;
    private final RewardConfig rewards;
    private final LivesConfig lives;

    private GameConfig(DurationConfig durations, WitherConfig withers, WipeConfig wipes,
            RewardConfig rewards, LivesConfig lives) {
        this.durations = durations;
        this.withers = withers;
        this.wipes = wipes;
        this.rewards = rewards;
        this.lives = lives;
    }

    public static GameConfig from(YamlConfiguration yaml) {
        DurationConfig durations = DurationConfig.fromSection(yaml.getConfigurationSection("durations"));
        WitherConfig withers = WitherConfig.fromSection(yaml.getConfigurationSection("withers"));
        WipeConfig wipes = WipeConfig.fromSection(yaml.getConfigurationSection("wipes"));
        RewardConfig rewards = RewardConfig.fromSection(yaml.getConfigurationSection("rewards"));
        LivesConfig lives = LivesConfig.fromSection(yaml.getConfigurationSection("lives"));
        return new GameConfig(durations, withers, wipes, rewards, lives);
    }

    public DurationConfig durations() {
        return durations;
    }

    public WitherConfig withers() {
        return withers;
    }

    public WipeConfig wipes() {
        return wipes;
    }

    public RewardConfig rewards() {
        return rewards;
    }

    public LivesConfig lives() {
        return lives;
    }

    public record DurationConfig(int minutesDefault, List<Integer> presets) {
        private static DurationConfig fromSection(ConfigurationSection section) {
            if (section == null) {
                return new DurationConfig(60, List.of(5, 20, 60));
            }
            int minutesDefault = section.getInt("minutes_default", 60);
            List<Integer> presets = new ArrayList<>(section.getIntegerList("presets"));
            if (presets.isEmpty()) {
                presets.addAll(List.of(5, 20, 60));
            }
            return new DurationConfig(minutesDefault, Collections.unmodifiableList(presets));
        }
    }

    public record WitherConfig(boolean enabled, int spawnAfterMinutes, TauntConfig taunts) {
        private static WitherConfig fromSection(ConfigurationSection section) {
            if (section == null) {
                return new WitherConfig(true, 60, TauntConfig.defaultConfig());
            }
            boolean enabled = section.getBoolean("enable", true);
            int spawnAfterMinutes = section.getInt("spawn_after_minutes", 60);
            TauntConfig taunts = TauntConfig.fromSection(section.getConfigurationSection("taunts"));
            return new WitherConfig(enabled, spawnAfterMinutes, taunts);
        }
    }

    public record TauntConfig(boolean enabled, int everyTicks, List<String> lines) {
        private static TauntConfig fromSection(ConfigurationSection section) {
            if (section == null) {
                return defaultConfig();
            }
            boolean enabled = section.getBoolean("enable", true);
            int everyTicks = section.getInt("every_ticks", 1200);
            List<String> lines = new ArrayList<>(section.getStringList("lines"));
            if (lines.isEmpty()) {
                lines.add("[WITHER] Ich bin die Leere zwischen euren Sternen…");
                lines.add("[WITHER] Eure Hoffnung zerbricht wie Glas.");
                lines.add("[WITHER] Atmet Staub, Sterbliche.");
            }
            return new TauntConfig(enabled, Collections.unmodifiableList(lines), everyTicks);
        }

        private static TauntConfig defaultConfig() {
            return new TauntConfig(true, 1200, List.of(
                    "[WITHER] Ich bin die Leere zwischen euren Sternen…",
                    "[WITHER] Eure Hoffnung zerbricht wie Glas.",
                    "[WITHER] Atmet Staub, Sterbliche."
            ));
        }

        private TauntConfig(boolean enabled, List<String> lines, int everyTicks) {
            this(enabled, everyTicks, lines);
        }
    }

    public record WipeConfig(int entitySoftEveryTicks, int radius, HardWipeConfig hardwipe) {
        private static WipeConfig fromSection(ConfigurationSection section) {
            if (section == null) {
                return new WipeConfig(3600, 300, new HardWipeConfig(1500, 5000));
            }
            int entitySoftEveryTicks = section.getInt("entity_soft_every_ticks", 3600);
            int radius = section.getInt("radius", 300);
            HardWipeConfig hardwipe = HardWipeConfig.fromSection(section.getConfigurationSection("hardwipe"));
            return new WipeConfig(entitySoftEveryTicks, radius, hardwipe);
        }
    }

    public record HardWipeConfig(int radius, int armorstandRadius) {
        private static HardWipeConfig fromSection(ConfigurationSection section) {
            if (section == null) {
                return new HardWipeConfig(1500, 5000);
            }
            int radius = section.getInt("radius", 1500);
            int armorstandRadius = section.getInt("armorstand_radius", 5000);
            return new HardWipeConfig(radius, armorstandRadius);
        }
    }

    public record RewardConfig(String mode, List<String> onBossKill, List<String> onFail) {
        private static RewardConfig fromSection(ConfigurationSection section) {
            if (section == null) {
                return new RewardConfig("all", List.of(), List.of());
            }
            String mode = section.getString("mode", "all");
            List<String> onBossKill = Collections.unmodifiableList(new ArrayList<>(
                    section.getStringList("on_boss_kill.commands")));
            List<String> onFail = Collections.unmodifiableList(new ArrayList<>(
                    section.getStringList("on_fail.commands")));
            return new RewardConfig(mode, onBossKill, onFail);
        }
    }

    public record LivesConfig(boolean oneLife) {
        private static LivesConfig fromSection(ConfigurationSection section) {
            if (section == null) {
                return new LivesConfig(false);
            }
            return new LivesConfig(section.getBoolean("one_life", false));
        }
    }
}
