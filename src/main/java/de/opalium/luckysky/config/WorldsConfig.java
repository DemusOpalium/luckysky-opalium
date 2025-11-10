package de.opalium.luckysky.config;

import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record WorldsConfig(LuckyWorld luckySky, DuelsWorld duels, Rotation rotation) {
    public static WorldsConfig from(FileConfiguration config) {
        LuckyWorld luckySky = readLuckyWorld(config.getConfigurationSection("luckySky"));
        DuelsWorld duels = readDuelsWorld(config.getConfigurationSection("duels"));
        Rotation rotation = readRotation(config.getConfigurationSection("rotation"), luckySky);
        return new WorldsConfig(luckySky, duels, rotation);
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

    private static Rotation readRotation(ConfigurationSection section, LuckyWorld luckyWorld) {
        if (section == null) {
            return new Rotation(
                    luckyWorld.worldName(),
                    luckyWorld.worldName() + "_alt",
                    "rotation/LuckySkyBase.zip",
                    false,
                    5,
                    new Border(0.0, 0.0, 400.0),
                    luckyWorld.spawn()
            );
        }
        String primary = section.getString("primary", luckyWorld.worldName());
        String secondary = section.getString("secondary", luckyWorld.worldName() + "_alt");
        String baseZip = section.getString("baseZip", section.getString("base_zip", "rotation/LuckySkyBase.zip"));
        boolean rotateWhenIdle = section.getBoolean("rotateWhenIdle", section.getBoolean("rotate_when_idle", false));
        int countdownSeconds = section.getInt("countdownSeconds", section.getInt("countdown_seconds", 5));
        Border border = readBorder(section.getConfigurationSection("border"));
        Spawn spawn = readSpawn(section.getConfigurationSection("spawn"), luckyWorld.spawn());
        return new Rotation(primary, secondary, baseZip, rotateWhenIdle, countdownSeconds, border, spawn);
    }

    private static Border readBorder(ConfigurationSection section) {
        if (section == null) {
            return new Border(0.0, 0.0, 400.0);
        }
        double centerX = section.getDouble("centerX", section.getDouble("center_x", 0.0));
        double centerZ = section.getDouble("centerZ", section.getDouble("center_z", 0.0));
        double size = section.getDouble("size", 400.0);
        return new Border(centerX, centerZ, size);
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
        writeRotation(config.createSection("rotation"), rotation);
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

    private void writeRotation(ConfigurationSection section, Rotation rotation) {
        section.set("primary", rotation.primary());
        section.set("secondary", rotation.secondary());
        section.set("baseZip", rotation.baseZip());
        section.set("rotateWhenIdle", rotation.rotateWhenIdle());
        section.set("countdownSeconds", rotation.countdownSeconds());
        ConfigurationSection border = section.createSection("border");
        border.set("centerX", rotation.border().centerX());
        border.set("centerZ", rotation.border().centerZ());
        border.set("size", rotation.border().size());
        writeSpawn(section.createSection("spawn"), rotation.spawn());
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

    public record Rotation(String primary, String secondary, String baseZip,
                           boolean rotateWhenIdle, int countdownSeconds,
                           Border border, Spawn spawn) {
    }

    public record Border(double centerX, double centerZ, double size) {
    }

    public WorldsConfig withLuckyWorld(LuckyWorld newLuckyWorld) {
        return new WorldsConfig(newLuckyWorld, duels, rotation);
    }
}
