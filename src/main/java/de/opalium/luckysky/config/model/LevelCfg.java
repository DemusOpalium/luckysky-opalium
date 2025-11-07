package de.opalium.luckysky.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;

public final class LevelCfg {
    private final String id;
    private final String name;
    private final int unlockAt;
    private final List<String> rewards;

    private LevelCfg(String id, String name, int unlockAt, List<String> rewards) {
        this.id = id;
        this.name = name;
        this.unlockAt = unlockAt;
        this.rewards = rewards;
    }

    public static LevelCfg from(String id, YamlConfiguration yaml) {
        String resolvedId = yaml.getString("id", id);
        String name = yaml.getString("name", resolvedId);
        int unlockAt = yaml.getInt("unlockAt", 0);
        List<String> rewards = Collections.unmodifiableList(new ArrayList<>(yaml.getStringList("rewards")));
        return new LevelCfg(resolvedId, name, unlockAt, rewards);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int unlockAt() {
        return unlockAt;
    }

    public List<String> rewards() {
        return rewards;
    }
}
