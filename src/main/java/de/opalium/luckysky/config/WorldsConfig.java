package de.opalium.luckysky.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            return new LuckyWorld("LuckySky", new Spawn(0.0, 101.0, 2.0, 180f, 0f),
                    new Lucky("§aLuckySky läuft – break the blocks!", true), defaultReset());
        }
        String worldName = section.getString("worldName", "LuckySky");
        Spawn spawn = readSpawn(section.getConfigurationSection("spawn"), new Spawn(0.0, 101.0, 2.0, 180f, 0f));
        ConfigurationSection luckySection = section.getConfigurationSection("lucky");
        String startBanner = "§aLuckySky läuft – break the blocks!";
        boolean requireAirAtTarget = true;
        if (luckySection != null) {
            startBanner = luckySection.getString("startBanner", startBanner);
            requireAirAtTarget = luckySection.getBoolean("require_air_at_target", requireAirAtTarget);
        }
        Reset reset = readReset(section.getConfigurationSection("reset"));
        return new LuckyWorld(worldName, spawn, new Lucky(startBanner, requireAirAtTarget), reset);
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

    public void writeTo(FileConfiguration config) {
        writeLuckyWorld(config.createSection("luckySky"), luckySky);
        writeDuelsWorld(config.createSection("duels"), duels);
    }

    private void writeLuckyWorld(ConfigurationSection section, LuckyWorld world) {
        section.set("worldName", world.worldName());
        writeSpawn(section.createSection("spawn"), world.spawn());
        ConfigurationSection luckySection = section.createSection("lucky");
        luckySection.set("startBanner", world.lucky().startBanner());
        luckySection.set("require_air_at_target", world.lucky().requireAirAtTarget());
        writeReset(section.createSection("reset"), world.reset());
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

    private static Reset readReset(ConfigurationSection section) {
        Reset defaults = defaultReset();
        if (section == null) {
            return defaults;
        }
        ResetArea area = readResetArea(section.getConfigurationSection("area"), defaults.area());
        boolean removeLava = section.getBoolean("remove_lava", defaults.removeLava());
        List<Map<?, ?>> rawLayers = section.getMapList("base_layers");
        List<BaseLayer> layers = new ArrayList<>();
        for (Map<?, ?> entry : rawLayers) {
            Object materialObj = entry.get("material");
            if (materialObj == null) {
                continue;
            }
            Object yObj = entry.get("y");
            int y = yObj instanceof Number number ? number.intValue() : defaults.area().min().y();
            String material = materialObj.toString();
            layers.add(new BaseLayer(y, material));
        }
        return new Reset(area, removeLava, layers);
    }

    private static ResetArea readResetArea(ConfigurationSection section, ResetArea defaults) {
        if (section == null) {
            return defaults;
        }
        ResetPoint min = readResetPoint(section.getConfigurationSection("min"), defaults.min());
        ResetPoint max = readResetPoint(section.getConfigurationSection("max"), defaults.max());
        return new ResetArea(min, max);
    }

    private static ResetPoint readResetPoint(ConfigurationSection section, ResetPoint defaults) {
        if (section == null) {
            return defaults;
        }
        int x = section.getInt("x", defaults.x());
        int y = section.getInt("y", defaults.y());
        int z = section.getInt("z", defaults.z());
        return new ResetPoint(x, y, z);
    }

    private static void writeReset(ConfigurationSection section, Reset reset) {
        writeResetArea(section.createSection("area"), reset.area());
        section.set("remove_lava", reset.removeLava());
        List<Map<String, Object>> layers = new ArrayList<>();
        for (BaseLayer layer : reset.baseLayers()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("y", layer.y());
            entry.put("material", layer.material());
            layers.add(entry);
        }
        section.set("base_layers", layers);
    }

    private static void writeResetArea(ConfigurationSection section, ResetArea area) {
        writeResetPoint(section.createSection("min"), area.min());
        writeResetPoint(section.createSection("max"), area.max());
    }

    private static void writeResetPoint(ConfigurationSection section, ResetPoint point) {
        section.set("x", point.x());
        section.set("y", point.y());
        section.set("z", point.z());
    }

    private static Reset defaultReset() {
        return new Reset(
                new ResetArea(new ResetPoint(-64, -120, -64), new ResetPoint(64, 320, 64)),
                true,
                List.of(new BaseLayer(100, "STONE"))
        );
    }

    public record LuckyWorld(String worldName, Spawn spawn, Lucky lucky, Reset reset) {
        public LuckyWorld {
            if (reset == null) {
                reset = defaultReset();
            }
        }

        public String startBanner() {
            return lucky.startBanner();
        }
    }

    public record Lucky(String startBanner, boolean requireAirAtTarget) {
    }

    public record DuelsWorld(String worldName, Spawn lobby, int protectionRadius) {
    }

    public record Spawn(double x, double y, double z, float yaw, float pitch) {
    }

    public record Reset(ResetArea area, boolean removeLava, List<BaseLayer> baseLayers) {
        public Reset {
            baseLayers = baseLayers == null ? List.of() : List.copyOf(baseLayers);
        }
    }

    public record ResetArea(ResetPoint min, ResetPoint max) {
    }

    public record ResetPoint(int x, int y, int z) {
    }

    public record BaseLayer(int y, String material) {
    }
}
