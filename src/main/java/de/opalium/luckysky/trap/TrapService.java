package de.opalium.luckysky.trap;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.arena.ArenaService;
import de.opalium.luckysky.util.Msg;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class TrapService {
    public static class TrapStage {
        private final List<ArenaService.BlockChange> blocks;

        public TrapStage(List<ArenaService.BlockChange> blocks) {
            this.blocks = Collections.unmodifiableList(new ArrayList<>(blocks));
        }

        public List<ArenaService.BlockChange> blocks() {
            return blocks;
        }

        public boolean isEmpty() {
            return blocks.isEmpty();
        }
    }

    public static class TrapRotation {
        private final String id;
        private final String name;
        private final String world;
        private final int originX;
        private final int originY;
        private final int originZ;
        private final List<TrapStage> stages;
        private final TrapStage clearStage;

        public TrapRotation(String id, String name, String world, int originX, int originY, int originZ,
                             List<TrapStage> stages, TrapStage clearStage) {
            this.id = id;
            this.name = name;
            this.world = world;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.stages = Collections.unmodifiableList(new ArrayList<>(stages));
            this.clearStage = clearStage;
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public String world() {
            return world;
        }

        public int originX() {
            return originX;
        }

        public int originY() {
            return originY;
        }

        public int originZ() {
            return originZ;
        }

        public List<TrapStage> stages() {
            return stages;
        }

        public Optional<TrapStage> clearStage() {
            return Optional.ofNullable(clearStage);
        }
    }

    private final LuckySkyPlugin plugin;
    private final File file;
    private final Map<String, TrapRotation> rotations = new ConcurrentHashMap<>();

    private TrapRotation activeRotation;
    private int stageIndex = -1;

    public TrapService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "traps.yml");
        ensureDefaults();
        reload();
    }

    private void ensureDefaults() {
        if (!file.exists()) {
            plugin.saveResource("traps.yml", false);
        }
    }

    public void reload() {
        rotations.clear();
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Failed to load traps.yml: " + e.getMessage());
            return;
        }
        ConfigurationSection root = config.getConfigurationSection("rotations");
        if (root == null) {
            plugin.getLogger().warning("No trap rotations configured");
            return;
        }
        for (String rotationId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(rotationId);
            if (section == null) {
                continue;
            }
            String name = section.getString("name", rotationId);
            String world = section.getString("world");
            ConfigurationSection origin = section.getConfigurationSection("origin");
            int originX = origin != null ? origin.getInt("x") : section.getInt("origin.x");
            int originY = origin != null ? origin.getInt("y") : section.getInt("origin.y");
            int originZ = origin != null ? origin.getInt("z") : section.getInt("origin.z");
            List<TrapStage> stages = parseStages(section.getMapList("stages"), section.getConfigurationSection("stages"));
            TrapStage clearStage = parseStage(section.get("clear"));
            rotations.put(rotationId, new TrapRotation(rotationId, name, world, originX, originY, originZ, stages, clearStage));
        }
    }

    private List<TrapStage> parseStages(List<Map<?, ?>> list, ConfigurationSection section) {
        List<TrapStage> stages = new ArrayList<>();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                TrapStage stage = parseStage(section.get(key));
                if (stage != null) {
                    stages.add(stage);
                }
            }
            return stages;
        }
        if (list != null) {
            for (Object obj : list) {
                TrapStage stage = parseStage(obj);
                if (stage != null) {
                    stages.add(stage);
                }
            }
        }
        return stages;
    }

    private TrapStage parseStage(Object raw) {
        if (raw instanceof ConfigurationSection section) {
            List<?> blocks = section.getList("blocks");
            if (blocks == null) {
                return null;
            }
            return new TrapStage(parseBlockChanges(blocks));
        }
        if (raw instanceof Map<?, ?> map) {
            Object blocks = map.get("blocks");
            if (blocks instanceof List<?> list) {
                return new TrapStage(parseBlockChanges(list));
            }
        }
        if (raw instanceof List<?> list) {
            return new TrapStage(parseBlockChanges(list));
        }
        return null;
    }

    private List<ArenaService.BlockChange> parseBlockChanges(List<?> list) {
        List<ArenaService.BlockChange> result = new ArrayList<>();
        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> map)) {
                continue;
            }
            Material type = Material.matchMaterial(String.valueOf(map.getOrDefault("type", "AIR")));
            if (type == null) {
                continue;
            }
            int x = getInt(map, "x");
            int y = getInt(map, "y");
            int z = getInt(map, "z");
            String data = map.containsKey("data") ? String.valueOf(map.get("data")) : "";
            result.add(new ArenaService.BlockChange(x, y, z, type, data));
        }
        return result;
    }

    private int getInt(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public Map<String, TrapRotation> rotations() {
        return Collections.unmodifiableMap(rotations);
    }

    public Optional<TrapRotation> rotation(String id) {
        return Optional.ofNullable(rotations.get(id));
    }

    public boolean enableDefault() {
        String rotationId = plugin.settings().trapDefaultRotation();
        if (rotationId == null || rotationId.isEmpty()) {
            plugin.getLogger().warning("No default trap rotation configured");
            return false;
        }
        return enable(rotationId);
    }

    public boolean enable(String rotationId) {
        TrapRotation rotation = rotations.get(rotationId);
        if (rotation == null) {
            Msg.to(Bukkit.getConsoleSender(), "&cUnknown trap rotation '&f" + rotationId + "&c'.");
            return false;
        }
        activeRotation = rotation;
        stageIndex = -1;
        Msg.to(Bukkit.getConsoleSender(), "&aTrap rotation '&f" + rotation.name() + "&a' enabled.");
        cycle();
        return true;
    }

    public boolean disable() {
        if (activeRotation == null) {
            return false;
        }
        applyStage(activeRotation.clearStage().orElse(null));
        Msg.to(Bukkit.getConsoleSender(), "&cTrap rotation disabled.");
        activeRotation = null;
        stageIndex = -1;
        return true;
    }

    public boolean cycle() {
        if (activeRotation == null) {
            Msg.to(Bukkit.getConsoleSender(), "&cNo active trap rotation to cycle.");
            return false;
        }
        if (activeRotation.stages().isEmpty()) {
            Msg.to(Bukkit.getConsoleSender(), "&eTrap rotation '&f" + activeRotation.name() + "&e' has no stages.");
            return false;
        }
        stageIndex = (stageIndex + 1) % activeRotation.stages().size();
        TrapStage stage = activeRotation.stages().get(stageIndex);
        applyStage(stage);
        Msg.to(Bukkit.getConsoleSender(), "&7Trap stage &f" + (stageIndex + 1) + "&7/&f" + activeRotation.stages().size() + " &7applied.");
        return true;
    }

    public boolean clear() {
        if (activeRotation == null) {
            Msg.to(Bukkit.getConsoleSender(), "&cNo active rotation to clear.");
            return false;
        }
        applyStage(activeRotation.clearStage().orElse(null));
        Msg.to(Bukkit.getConsoleSender(), "&7Trap rotation cleared.");
        return true;
    }

    private void applyStage(TrapStage stage) {
        if (stage == null || stage.isEmpty() || activeRotation == null) {
            return;
        }
        World world = Bukkit.getWorld(activeRotation.world());
        if (world == null) {
            plugin.getLogger().warning("World not loaded for trap rotation " + activeRotation.id());
            return;
        }
        ArrayDeque<ArenaService.BlockChange> queue = new ArrayDeque<>(stage.blocks());
        new BukkitRunnable() {
            @Override
            public void run() {
                int processed = 0;
                while (!queue.isEmpty() && processed < 200) {
                    ArenaService.BlockChange change = queue.poll();
                    int x = activeRotation.originX() + change.x();
                    int y = activeRotation.originY() + change.y();
                    int z = activeRotation.originZ() + change.z();
                    BlockData data = null;
                    if (change.data() != null && !change.data().isEmpty()) {
                        try {
                            data = Bukkit.createBlockData(change.data());
                        } catch (IllegalArgumentException ex) {
                            plugin.getLogger().warning("Invalid block data '" + change.data() + "' for trap rotation at "
                                    + x + "," + y + "," + z);
                        }
                    }
                    if (data != null) {
                        world.getBlockAt(x, y, z).setBlockData(data, false);
                    } else {
                        world.getBlockAt(x, y, z).setType(change.type(), false);
                    }
                    processed++;
                }
                if (queue.isEmpty()) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
