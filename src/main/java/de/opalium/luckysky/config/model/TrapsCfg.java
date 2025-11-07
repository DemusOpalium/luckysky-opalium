package de.opalium.luckysky.config.model;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class TrapsCfg {
    private final Map<String, TrapDefinition> traps;

    private TrapsCfg(Map<String, TrapDefinition> traps) {
        this.traps = traps;
    }

    public static TrapsCfg from(YamlConfiguration yaml) {
        Map<String, TrapDefinition> traps = new LinkedHashMap<>();
        ConfigurationSection section = yaml.getConfigurationSection("traps");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection trapSection = section.getConfigurationSection(id);
                if (trapSection == null) {
                    continue;
                }
                TrapDefinition trap = TrapDefinition.fromSection(id, trapSection);
                traps.put(id, trap);
            }
        }
        return new TrapsCfg(Map.copyOf(traps));
    }

    public Map<String, TrapDefinition> traps() {
        return traps;
    }

    public record TrapDefinition(String id, String name, String command, int cooldownSeconds) {
        private static TrapDefinition fromSection(String id, ConfigurationSection section) {
            String name = section.getString("name", id);
            String command = section.getString("command", "say trap " + id);
            int cooldownSeconds = section.getInt("cooldownSeconds", 30);
            return new TrapDefinition(id, name, command, cooldownSeconds);
        }
    }
}
