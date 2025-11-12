package de.opalium.luckysky.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record NpcConfig(Map<String, NpcEntry> npcs) {
    public static NpcConfig from(FileConfiguration config) {
        return fromSection(config.getConfigurationSection("npcs"));
    }

    public static NpcConfig fromSection(ConfigurationSection section) {
        if (section == null) {
            return new NpcConfig(Collections.emptyMap());
        }
        ConfigurationSection root = section;
        ConfigurationSection nested = section.getConfigurationSection("npcs");
        if (nested != null && !nested.getKeys(false).isEmpty()) {
            root = nested;
        }
        Map<String, NpcEntry> entries = new HashMap<>();
        for (String key : root.getKeys(false)) {
            ConfigurationSection entrySection = root.getConfigurationSection(key);
            if (entrySection == null) {
                continue;
            }
            NpcEntry entry = readEntry(key, entrySection);
            if (entry != null) {
                entries.put(key, entry);
            }
        }
        return new NpcConfig(Collections.unmodifiableMap(entries));
    }

    public Optional<NpcEntry> get(String id) {
        return Optional.ofNullable(npcs.get(id));
    }

    public void writeTo(FileConfiguration config) {
        ConfigurationSection root = config.createSection("npcs");
        writeTo(root);
    }

    public void writeTo(ConfigurationSection root) {
        for (Map.Entry<String, NpcEntry> entry : npcs.entrySet()) {
            ConfigurationSection section = root.createSection(entry.getKey());
            NpcEntry npc = entry.getValue();
            section.set("npc_id", npc.npcId());
            section.set("world", npc.world());

            ConfigurationSection houseSection = section.createSection("house");
            ConfigurationSection areaSection = houseSection.createSection("area");
            writeVector(areaSection.createSection("min"), npc.house().min());
            writeVector(areaSection.createSection("max"), npc.house().max());

            ConfigurationSection scriptSection = section.createSection("collapse_script");
            scriptSection.set("type", npc.collapseScript().type().name());
            scriptSection.set("phase_delay_ticks", npc.collapseScript().phaseDelayTicks());
            scriptSection.set("lower_per_phase", npc.collapseScript().lowerPerPhase());
            scriptSection.set("despawn_after", npc.collapseScript().despawnAfter());
            scriptSection.set("recall", npc.collapseScript().recall());
            scriptSection.set("recall_delay_ticks", npc.collapseScript().recallDelayTicks());
            npc.collapseScript().fawe().ifPresent(fawe -> {
                ConfigurationSection faweSection = scriptSection.createSection("schematics");
                faweSection.set("hut_open", fawe.hutOpen());
                faweSection.set("hut_closed", fawe.hutClosed());
                faweSection.set("paste_delay_ticks", fawe.pasteDelayTicks());
            });

            npc.parking().ifPresent(parking -> {
                ConfigurationSection parkingSection = section.createSection("parking");
                parkingSection.set("delay_ticks", parking.delayTicks());
                ConfigurationSection loc = parkingSection.createSection("location");
                loc.set("world", parking.world());
                loc.set("x", parking.x());
                loc.set("y", parking.y());
                loc.set("z", parking.z());
                loc.set("yaw", parking.yaw());
                loc.set("pitch", parking.pitch());
            });
        }
    }

    private static NpcEntry readEntry(String id, ConfigurationSection section) {
        int npcId = section.getInt("npc_id", -1);
        String world = section.getString("world", "LuckySky");
        ConfigurationSection house = section.getConfigurationSection("house");
        if (house == null) {
            return null;
        }
        ConfigurationSection areaSection = house.getConfigurationSection("area");
        if (areaSection == null) {
            return null;
        }
        BlockVector min = readVector(areaSection.getConfigurationSection("min"), new BlockVector(0, 0, 0));
        BlockVector max = readVector(areaSection.getConfigurationSection("max"), new BlockVector(0, 0, 0));
        Area area = new Area(min, max);

        CollapseScript script = readCollapseScript(section.getConfigurationSection("collapse_script"));
        Optional<Parking> parking = readParking(section.getConfigurationSection("parking"));

        return new NpcEntry(id, npcId, world, area, script, parking);
    }

    private static CollapseScript readCollapseScript(ConfigurationSection section) {
        if (section == null) {
            return new CollapseScript(
                    CollapseScriptType.SEQUENTIAL,
                    40L,
                    0.35D,
                    true,
                    true,
                    60L,
                    Optional.empty()
            );
        }
        String typeRaw = section.getString("type", "SEQUENTIAL");
        CollapseScriptType type;
        try {
            type = CollapseScriptType.valueOf(typeRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            type = CollapseScriptType.SEQUENTIAL;
        }
        long phaseDelay = section.getLong("phase_delay_ticks", 40L);
        double lowerPerPhase = section.getDouble("lower_per_phase", 0.35D);
        boolean despawnAfter = section.getBoolean("despawn_after", true);
        boolean recall = section.getBoolean("recall", true);
        long recallDelay = section.getLong("recall_delay_ticks", 60L);

        Optional<FaweSettings> fawe = Optional.empty();
        ConfigurationSection faweSection = section.getConfigurationSection("schematics");
        if (faweSection != null) {
            String open = faweSection.getString("hut_open");
            String closed = faweSection.getString("hut_closed");
            long pasteDelay = faweSection.getLong("paste_delay_ticks", 0L);
            if (open != null && closed != null) {
                fawe = Optional.of(new FaweSettings(open, closed, pasteDelay));
            }
        }

        return new CollapseScript(type, phaseDelay, lowerPerPhase, despawnAfter, recall, recallDelay, fawe);
    }

    private static Optional<Parking> readParking(ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }
        long delay = section.getLong("delay_ticks", 0L);
        ConfigurationSection loc = section.getConfigurationSection("location");
        if (loc == null) {
            return Optional.empty();
        }
        String world = loc.getString("world", "LuckySky");
        double x = loc.getDouble("x", 0.5D);
        double y = loc.getDouble("y", 100.0D);
        double z = loc.getDouble("z", 0.5D);
        float yaw = (float) loc.getDouble("yaw", 0.0D);
        float pitch = (float) loc.getDouble("pitch", 0.0D);
        return Optional.of(new Parking(delay, world, x, y, z, yaw, pitch));
    }

    private static BlockVector readVector(ConfigurationSection section, BlockVector def) {
        if (section == null) {
            return def;
        }
        int x = section.getInt("x", def.x());
        int y = section.getInt("y", def.y());
        int z = section.getInt("z", def.z());
        return new BlockVector(x, y, z);
    }

    private static void writeVector(ConfigurationSection section, BlockVector vector) {
        section.set("x", vector.x());
        section.set("y", vector.y());
        section.set("z", vector.z());
    }

    public record NpcEntry(String id, int npcId, String world, Area house, CollapseScript collapseScript,
                           Optional<Parking> parking) {
    }

    public record Area(BlockVector min, BlockVector max) {
        public BlockVector orderedMin() {
            return new BlockVector(
                    Math.min(min.x(), max.x()),
                    Math.min(min.y(), max.y()),
                    Math.min(min.z(), max.z()));
        }

        public BlockVector orderedMax() {
            return new BlockVector(
                    Math.max(min.x(), max.x()),
                    Math.max(min.y(), max.y()),
                    Math.max(min.z(), max.z()));
        }
    }

    public record BlockVector(int x, int y, int z) {
    }

    public enum CollapseScriptType {
        SEQUENTIAL,
        FAWE
    }

    public record CollapseScript(CollapseScriptType type, long phaseDelayTicks, double lowerPerPhase,
                                 boolean despawnAfter, boolean recall, long recallDelayTicks,
                                 Optional<FaweSettings> fawe) {
    }

    public record FaweSettings(String hutOpen, String hutClosed, long pasteDelayTicks) {
    }

    public record Parking(long delayTicks, String world, double x, double y, double z, float yaw, float pitch) {
    }
}
