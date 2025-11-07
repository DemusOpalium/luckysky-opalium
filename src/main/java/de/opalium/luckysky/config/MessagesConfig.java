package de.opalium.luckysky.config;

import org.bukkit.configuration.file.FileConfiguration;

public record MessagesConfig(
        String prefix,
        String adminPrefix,
        String gamePrefix,
        String startBanner,
        String stopBanner,
        String duelsPrefix
) {
    public static MessagesConfig from(FileConfiguration config) {
        String prefix = config.getString("prefix", "§d☁ LuckySky§7 » ");
        String adminPrefix = config.getString("admin_prefix", "§d☁ §bAdmin§7 » ");
        String gamePrefix = config.getString("game_prefix", "§a[Game]§7 ");
        String startBanner = config.getString("start_banner", "§aSpiel startet … Viel Glück!");
        String stopBanner = config.getString("stop_banner", "§cSpiel gestoppt.");
        String duelsPrefix = config.getString("duels_prefix", "§5[Duels]§7 ");
        return new MessagesConfig(prefix, adminPrefix, gamePrefix, startBanner, stopBanner, duelsPrefix);
    }

    public void writeTo(FileConfiguration config) {
        config.set("prefix", prefix);
        config.set("admin_prefix", adminPrefix);
        config.set("game_prefix", gamePrefix);
        config.set("start_banner", startBanner);
        config.set("stop_banner", stopBanner);
        config.set("duels_prefix", duelsPrefix);
    }
}
