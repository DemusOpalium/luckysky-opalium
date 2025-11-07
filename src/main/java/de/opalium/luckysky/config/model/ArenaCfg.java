package de.opalium.luckysky.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ArenaCfg {
    private final String id;
    private final String displayName;
    private final String world;
    private final int safeRadius;
    private final List<String> description;

    private ArenaCfg(String id, String displayName, String world, int safeRadius, List<String> description) {
        this.id = id;
        this.displayName = displayName;
        this.world = world;
        this.safeRadius = safeRadius;
        this.description = description;
    }

    public static ArenaCfg from(String id, YamlConfiguration yaml, Logger logger) {
        String resolvedId = yaml.getString("id", id);
        String displayName = yaml.getString("displayName", resolvedId);
        String world = yaml.getString("world", "LuckySky");
        int safeRadius = yaml.getInt("safeRadius", 128);
        List<String> description = Collections.unmodifiableList(new ArrayList<>(yaml.getStringList("description")));
        return new ArenaCfg(resolvedId, displayName, world, safeRadius, description);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String world() {
        return world;
    }

    public int safeRadius() {
        return safeRadius;
    }

    public List<String> description() {
        return description;
    }
}
