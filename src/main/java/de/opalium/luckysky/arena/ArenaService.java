package de.opalium.luckysky.arena;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
import de.opalium.luckysky.util.Msg;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class ArenaService {
    public enum OperationType {
        BUILD("build"),
        CLEAR("clear"),
        LIGHT("light"),
        CROWN("crown"),
        FLOOR("floor"),
        CEILING("ceiling");

        private final String key;

        OperationType(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }

        public static OperationType fromKey(String key) {
            for (OperationType type : values()) {
                if (type.key.equalsIgnoreCase(key)) {
                    return type;
                }
            }
            return null;
        }
    }

    public record BlockChange(int x, int y, int z, Material type, String data) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("x", x);
            map.put("y", y);
            map.put("z", z);
            map.put("type", type.name());
            if (data != null && !data.isEmpty()) {
                map.put("data", data);
            }
            return map;
        }
    }

    public static class ArenaOperation {
        private final List<BlockChange> blockChanges;

        public ArenaOperation(List<BlockChange> blockChanges) {
            this.blockChanges = Collections.unmodifiableList(new ArrayList<>(blockChanges));
        }

        public List<BlockChange> blocks() {
            return blockChanges;
        }

        public boolean isEmpty() {
            return blockChanges.isEmpty();
        }
    }

    public static class ArenaVariant {
        private final String id;
        private final String worldName;
        private final int originX;
        private final int originY;
        private final int originZ;
        private final Map<OperationType, ArenaOperation> operations;

        public ArenaVariant(String id, String worldName, int originX, int originY, int originZ,
                             Map<OperationType, ArenaOperation> operations) {
            this.id = id;
            this.worldName = worldName;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.operations = Collections.unmodifiableMap(new EnumMap<>(operations));
        }

        public String id() {
            return id;
        }

        public String worldName() {
            return worldName;
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

        public Location resolveOrigin() {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                throw new IllegalStateException("World not loaded: " + worldName);
            }
            return new Location(world, originX, originY, originZ);
        }

        public Optional<ArenaOperation> operation(OperationType type) {
            return Optional.ofNullable(operations.get(type));
        }

        public Map<OperationType, ArenaOperation> operations() {
            return operations;
        }
    }

    public static class ArenaDefinition {
        private final String id;
        private final String name;
        private final Map<String, ArenaVariant> variants;

        public ArenaDefinition(String id, String name, Map<String, ArenaVariant> variants) {
            this.id = id;
            this.name = name;
            this.variants = Collections.unmodifiableMap(new HashMap<>(variants));
        }

        public String id() {
            return id;
        }

        public String name() {
            return name;
        }

        public Map<String, ArenaVariant> variants() {
            return variants;
        }

        public ArenaVariant requireVariant(String variantId) {
            ArenaVariant variant = variants.get(variantId);
            if (variant == null) {
                throw new IllegalArgumentException("Unknown variant '" + variantId + "' for arena '" + id + "'");
            }
            return variant;
        }
    }

    private final LuckySkyPlugin plugin;
    private final File file;
    private final Map<String, ArenaDefinition> arenas = new ConcurrentHashMap<>();

    public ArenaService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
        ensureDefaults();
        reload();
    }

    private void ensureDefaults() {
        if (!file.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
    }

    public void reload() {
        arenas.clear();
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Failed to load arenas.yml: " + e.getMessage());
            return;
        }
        ConfigurationSection root = config.getConfigurationSection("arenas");
        if (root == null) {
            plugin.getLogger().warning("No arenas defined in arenas.yml");
            return;
        }
        for (String arenaId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(arenaId);
            if (section == null) {
                continue;
            }
            String name = section.getString("name", arenaId);
            ConfigurationSection variantsSection = section.getConfigurationSection("variants");
            if (variantsSection == null) {
                continue;
            }
            Map<String, ArenaVariant> variantMap = new HashMap<>();
            for (String variantId : variantsSection.getKeys(false)) {
                ArenaVariant variant = parseVariant(variantId, variantsSection.getConfigurationSection(variantId));
                if (variant != null) {
                    variantMap.put(variantId, variant);
                }
            }
            arenas.put(arenaId, new ArenaDefinition(arenaId, name, variantMap));
        }
    }

    private ArenaVariant parseVariant(String variantId, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String world = section.getString("world");
        if (world == null || world.isEmpty()) {
            plugin.getLogger().warning("Variant '" + variantId + "' is missing world entry");
            return null;
        }
        ConfigurationSection origin = section.getConfigurationSection("origin");
        int originX = origin != null ? origin.getInt("x") : section.getInt("origin.x");
        int originY = origin != null ? origin.getInt("y") : section.getInt("origin.y");
        int originZ = origin != null ? origin.getInt("z") : section.getInt("origin.z");
        ConfigurationSection operationsSection = section.getConfigurationSection("operations");
        Map<OperationType, ArenaOperation> operations = new EnumMap<>(OperationType.class);
        if (operationsSection != null) {
            for (String opKey : operationsSection.getKeys(false)) {
                OperationType type = OperationType.fromKey(opKey);
                if (type == null) {
                    plugin.getLogger().warning("Unknown operation '" + opKey + "' in variant '" + variantId + "'");
                    continue;
                }
                ArenaOperation operation = parseOperation(operationsSection.get(opKey));
                if (operation != null) {
                    operations.put(type, operation);
                }
            }
        }
        return new ArenaVariant(variantId, world, originX, originY, originZ, operations);
    }

    private ArenaOperation parseOperation(Object raw) {
        if (raw instanceof ConfigurationSection section) {
            List<?> list = section.getList("blocks");
            if (list == null) {
                return null;
            }
            return new ArenaOperation(parseBlockChanges(list));
        }
        if (raw instanceof List<?> list) {
            return new ArenaOperation(parseBlockChanges(list));
        }
        return null;
    }

    private List<BlockChange> parseBlockChanges(List<?> list) {
        List<BlockChange> result = new ArrayList<>();
        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> map)) {
                continue;
            }
            Material material = Material.matchMaterial(String.valueOf(map.getOrDefault("type", "AIR")));
            if (material == null) {
                continue;
            }
            int x = getInt(map, "x");
            int y = getInt(map, "y");
            int z = getInt(map, "z");
            String data = map.containsKey("data") ? Objects.toString(map.get("data"), "") : "";
            result.add(new BlockChange(x, y, z, material, data));
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

    public Set<String> arenaIds() {
        return Collections.unmodifiableSet(arenas.keySet());
    }

    public Optional<ArenaDefinition> arena(String id) {
        return Optional.ofNullable(arenas.get(id));
    }

    public void buildDefaultArena() {
        applyDefault(OperationType.BUILD);
    }

    public void clearDefaultArena() {
        applyDefault(OperationType.CLEAR);
    }

    public void lightDefaultArena() {
        applyDefault(OperationType.LIGHT);
    }

    public void crownDefaultArena() {
        applyDefault(OperationType.CROWN);
    }

    public void floorDefaultArena() {
        applyDefault(OperationType.FLOOR);
    }

    public void ceilingDefaultArena() {
        applyDefault(OperationType.CEILING);
    }

    private void applyDefault(OperationType type) {
        Settings settings = plugin.settings();
        String arenaId = settings.arenaDefaultId();
        String variantId = settings.arenaDefaultVariant();
        if (arenaId == null || variantId == null) {
            plugin.getLogger().warning("No default arena configured for operation " + type);
            return;
        }
        apply(arenaId, variantId, type);
    }

    public void apply(String arenaId, String variantId, OperationType type) {
        ArenaDefinition definition = arenas.get(arenaId);
        if (definition == null) {
            Msg.to(Bukkit.getConsoleSender(), "&cUnknown arena '&f" + arenaId + "&c'.");
            return;
        }
        ArenaVariant variant = definition.variants().get(variantId);
        if (variant == null) {
            Msg.to(Bukkit.getConsoleSender(), "&cUnknown variant '&f" + variantId + "&c' for arena '&f" + arenaId + "&c'.");
            return;
        }
        ArenaOperation operation = variant.operation(type).orElse(null);
        if (operation == null) {
            Msg.to(Bukkit.getConsoleSender(), "&eNo operation '&f" + type.key() + "&e' configured for variant '&f" + variantId + "&e'.");
            return;
        }
        runBatchedOperation(variant, operation);
    }

    private void runBatchedOperation(ArenaVariant variant, ArenaOperation operation) {
        if (operation.isEmpty()) {
            return;
        }
        World world = Bukkit.getWorld(variant.worldName());
        if (world == null) {
            plugin.getLogger().warning("World not loaded for arena variant " + variant.id());
            return;
        }
        ArrayDeque<BlockChange> queue = new ArrayDeque<>(operation.blocks());
        new BukkitRunnable() {
            @Override
            public void run() {
                int processed = 0;
                while (!queue.isEmpty() && processed < 200) {
                    BlockChange change = queue.poll();
                    int x = variant.originX() + change.x();
                    int y = variant.originY() + change.y();
                    int z = variant.originZ() + change.z();
                    BlockData data = null;
                    if (change.data() != null && !change.data().isEmpty()) {
                        try {
                            data = Bukkit.createBlockData(change.data());
                        } catch (IllegalArgumentException ex) {
                            plugin.getLogger().warning("Invalid block data '" + change.data() + "' for arena operation at "
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

    public List<String> variantsForArena(String arenaId) {
        ArenaDefinition definition = arenas.get(arenaId);
        if (definition == null) {
            return List.of();
        }
        return new ArrayList<>(definition.variants().keySet());
    }

    public void updateVariantOrigin(String arenaId, String variantId, Location origin) {
        ArenaDefinition definition = arenas.get(arenaId);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown arena: " + arenaId);
        }
        ArenaVariant variant = definition.variants().get(variantId);
        if (variant == null) {
            throw new IllegalArgumentException("Unknown variant: " + variantId);
        }
        if (origin.getWorld() == null) {
            throw new IllegalArgumentException("Origin location has no world");
        }
        Map<OperationType, ArenaOperation> operations = new EnumMap<>(variant.operations());
        ArenaVariant updated = new ArenaVariant(variant.id(), origin.getWorld().getName(), origin.getBlockX(),
                origin.getBlockY(), origin.getBlockZ(), operations);
        Map<String, ArenaVariant> newVariants = new HashMap<>(definition.variants());
        newVariants.put(variantId, updated);
        arenas.put(arenaId, new ArenaDefinition(definition.id(), definition.name(), newVariants));
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection root = yaml.createSection("arenas");
        List<String> arenaIds = new ArrayList<>(arenas.keySet());
        arenaIds.sort(String::compareToIgnoreCase);
        for (String arenaId : arenaIds) {
            ArenaDefinition definition = arenas.get(arenaId);
            if (definition == null) {
                continue;
            }
            ConfigurationSection arenaSection = root.createSection(definition.id());
            arenaSection.set("name", definition.name());
            ConfigurationSection variantsSection = arenaSection.createSection("variants");
            List<String> variants = new ArrayList<>(definition.variants().keySet());
            variants.sort(String::compareToIgnoreCase);
            for (String variantId : variants) {
                ArenaVariant variant = definition.variants().get(variantId);
                if (variant == null) {
                    continue;
                }
                ConfigurationSection variantSection = variantsSection.createSection(variant.id());
                variantSection.set("world", variant.worldName());
                ConfigurationSection origin = variantSection.createSection("origin");
                origin.set("x", variant.originX());
                origin.set("y", variant.originY());
                origin.set("z", variant.originZ());
                ConfigurationSection operationsSection = variantSection.createSection("operations");
                List<OperationType> operationTypes = new ArrayList<>(variant.operations().keySet());
                operationTypes.sort((a, b) -> a.key().compareToIgnoreCase(b.key()));
                for (OperationType type : operationTypes) {
                    ArenaOperation op = variant.operations().get(type);
                    if (op == null) {
                        continue;
                    }
                    List<Map<String, Object>> list = op.blocks().stream()
                            .map(BlockChange::toMap)
                            .collect(Collectors.toList());
                    operationsSection.set(type.key(), list);
                }
            }
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas.yml: " + e.getMessage());
        }
    }

    public Map<String, ArenaDefinition> arenas() {
        return Collections.unmodifiableMap(arenas);
    }

    public Optional<ArenaVariant> defaultVariant() {
        Settings settings = plugin.settings();
        return arena(settings.arenaDefaultId())
                .map(def -> def.variants().get(settings.arenaDefaultVariant()));
    }

    public void applyWithFeedback(String arenaId, String variantId, OperationType type) {
        Msg.to(Bukkit.getConsoleSender(), "&7Arena &f" + arenaId + " &7variant &f" + variantId
                + " &7operation &f" + type.key().toUpperCase(Locale.ROOT));
        apply(arenaId, variantId, type);
    }
}
