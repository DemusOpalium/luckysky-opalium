package de.opalium.luckysky.config.model;

import org.bukkit.configuration.file.YamlConfiguration;

public record MessagesCfg(String prefix) {
    public static MessagesCfg from(YamlConfiguration yaml) {
        String prefix = yaml.getString("prefix", "&b⛯ LuckySky: &r");
        return new MessagesCfg(prefix);
    }
}
