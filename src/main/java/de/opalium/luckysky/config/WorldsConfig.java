package de.opalium.luckysky.config;

import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record WorldsConfig(LuckyWorld luckySky, DuelsWorld duels) {
    public static WorldsConfig from(FileConfiguration config) {
        LuckyWorld luckySky = readLuckyWorld(config.getConfigurationSection("luckySky"));
        DuelsWorld duels = readDuelsWorld(config.getConfigurationSection("duels"));
        return new WorldsConfig(luckySky, duels);
    }

    private static LuckyWorld readLuckyWorld(ConfigurationSection section) {
        if (section == null) {
            return new LuckyWorld(
                    "LuckySky",
                    new Spawn(0.0, 101.0, 2.0, 180f, 0f),
                    null,
                    new Lucky("§aLuckySky läuft – break the blocks!", true)
            );
        }
        String worldName = section.getString("worldName", "LuckySky");
        Spawn spawn = readSpawn(section.getConfigurationSection("spawn"), new Spawn(0.0, 101.0, 2.0, 180f, 0f));
        Spawn lobby = readOptionalSpawn(section.getConfigurationSection("lobby"));
        ConfigurationSection luckySection = section.getConfigurationSection("lucky");
        String startBanner = "§aLuckySky läuft – break the blocks!";
        boolean requireAirAtTarget = true;
        if (luckySection != null) {
            startBanner = luckySection.getString("startBanner", startBanner);
            requireAirAtTarget = luckySection.getBoolean("require_air_at_target", requireAirAtTarget);
        }
        return new LuckyWorld(worldName, spawn, lobby, new Lucky(startBanner, requireAirAtTarget));
    }

    private static DuelsWorld readDuelsWorld(ConfigurationSection section) {
        if (section == null) {
            return new DuelsWorld("duels", new Spawn(1.0, -56.0, 0.0, 0f, 0f), 24);
        }
        String worldName = section.getString("worldName", "duels");
        Spawn lobby = readSpawn(section.getConfigurationSection("lobby"), new Spawn(1.0, -56.0, 0.0, 0f, 0f));
        int protectionRadius = section.getInt("protection_radius", 24);
        return new DuelsWorld(worldName, lobby, protectionRadius);
    }

    private static Spawn readSpawn(ConfigurationSection section, Spawn def) {
        if (section == null) {
            return def;
        }
        double x = section.getDouble("x", def.x());
        double y = section.getDouble("y", def.y());
        double z = section.getDouble("z", def.z());
        float yaw = (float) section.getDouble("yaw", def.yaw());
        float pitch = (float) section.getDouble("pitch", def.pitch());
        return new Spawn(x, y, z, yaw, pitch);
    }

    private static Spawn readOptionalSpawn(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        double x = section.getDouble("x", 0.0);
        double y = section.getDouble("y", 0.0);
        double z = section.getDouble("z", 0.0);
        float yaw = (float) section.getDouble("yaw", 0.0);
        float pitch = (float) section.getDouble("pitch", 0.0);
        return new Spawn(x, y, z, yaw, pitch);
    }

    public void writeTo(FileConfiguration config) {
        writeLuckyWorld(config.createSection("luckySky"), luckySky);
        writeDuelsWorld(config.createSection("duels"), duels);
    }

    private void writeLuckyWorld(ConfigurationSection section, LuckyWorld world) {
        section.set("worldName", world.worldName());
        writeSpawn(section.createSection("spawn"), world.spawn());
        Optional.ofNullable(world.lobby())
                .ifPresent(lobby -> writeSpawn(section.createSection("lobby"), lobby));
        ConfigurationSection luckySection = section.createSection("lucky");
        luckySection.set("startBanner", world.lucky().startBanner());
        luckySection.set("require_air_at_target", world.lucky().requireAirAtTarget());
    }

    private void writeDuelsWorld(ConfigurationSection section, DuelsWorld world) {
        section.set("worldName", world.worldName());
        writeSpawn(section.createSection("lobby"), world.lobby());
        section.set("protection_radius", world.protectionRadius());
    }

    private void writeSpawn(ConfigurationSection section, Spawn spawn) {
        section.set("x", spawn.x());
        section.set("y", spawn.y());
        section.set("z", spawn.z());
        section.set("yaw", spawn.yaw());
        section.set("pitch", spawn.pitch());
    }

    public record LuckyWorld(String worldName, Spawn spawn, Spawn lobby, Lucky lucky) {
    }

    public record Lucky(String startBanner, boolean requireAirAtTarget) {
    }

    public record DuelsWorld(String worldName, Spawn lobby, int protectionRadius) {
    }

    public record Spawn(double x, double y, double z, float yaw, float pitch) {
    }
}
