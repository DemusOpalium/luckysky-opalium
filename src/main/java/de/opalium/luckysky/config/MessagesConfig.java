package de.opalium.luckysky.config;

import org.bukkit.configuration.file.FileConfiguration;

public record MessagesConfig(
        String prefix,
        String adminPrefix,
        String gamePrefix,
        String startBanner,
        String stopBanner,
        String duelsPrefix,
        Rotation rotation
) {
    public static MessagesConfig from(FileConfiguration config) {
        String prefix = config.getString("prefix", "§d☁ LuckySky§7 » ");
        String adminPrefix = config.getString("admin_prefix", "§d☁ §bAdmin§7 » ");
        String gamePrefix = config.getString("game_prefix", "§a[Game]§7 ");
        String startBanner = config.getString("start_banner", "§aSpiel startet … Viel Glück!");
        String stopBanner = config.getString("stop_banner", "§cSpiel gestoppt.");
        String duelsPrefix = config.getString("duels_prefix", "§5[Duels]§7 ");
        Rotation rotation = readRotation(config.getConfigurationSection("rotation"));
        return new MessagesConfig(prefix, adminPrefix, gamePrefix, startBanner, stopBanner, duelsPrefix, rotation);
    }

    public void writeTo(FileConfiguration config) {
        config.set("prefix", prefix);
        config.set("admin_prefix", adminPrefix);
        config.set("game_prefix", gamePrefix);
        config.set("start_banner", startBanner);
        config.set("stop_banner", stopBanner);
        config.set("duels_prefix", duelsPrefix);
        writeRotation(config.createSection("rotation"), rotation);
    }

    private static Rotation readRotation(org.bukkit.configuration.ConfigurationSection section) {
        if (section == null) {
            return new Rotation(
                    "§eLuckySky wechselt in §f{seconds}s§e …",
                    "§eWeltwechsel",
                    "§7Teleport in §f{seconds}s",
                    "§aTeleportiere in die neue LuckySky-Welt!",
                    "§aViel Glück!",
                    "§7Neue Welt geladen."
            );
        }
        String countdownBroadcast = section.getString("countdown_broadcast", "§eLuckySky wechselt in §f{seconds}s§e …");
        String countdownTitle = section.getString("countdown_title", "§eWeltwechsel");
        String countdownSubtitle = section.getString("countdown_subtitle", "§7Teleport in §f{seconds}s");
        String teleportBroadcast = section.getString("teleport_broadcast", "§aTeleportiere in die neue LuckySky-Welt!");
        String teleportTitle = section.getString("teleport_title", "§aViel Glück!");
        String teleportSubtitle = section.getString("teleport_subtitle", "§7Neue Welt geladen.");
        return new Rotation(countdownBroadcast, countdownTitle, countdownSubtitle,
                teleportBroadcast, teleportTitle, teleportSubtitle);
    }

    private void writeRotation(org.bukkit.configuration.ConfigurationSection section, Rotation rotation) {
        section.set("countdown_broadcast", rotation.countdownBroadcast());
        section.set("countdown_title", rotation.countdownTitle());
        section.set("countdown_subtitle", rotation.countdownSubtitle());
        section.set("teleport_broadcast", rotation.teleportBroadcast());
        section.set("teleport_title", rotation.teleportTitle());
        section.set("teleport_subtitle", rotation.teleportSubtitle());
    }

    public record Rotation(
            String countdownBroadcast,
            String countdownTitle,
            String countdownSubtitle,
            String teleportBroadcast,
            String teleportTitle,
            String teleportSubtitle
    ) {
    }
}
