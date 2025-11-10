package de.opalium.luckysky.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record DuelsConfig(
        String activeArena,
        Map<String, Arena> arenas,
        Gui gui,
        Scoreboard scoreboard
) {
    public static DuelsConfig from(FileConfiguration config) {
        String activeArena = config.getString("active_arena", "default");
        Map<String, Arena> arenas = readArenas(config.getConfigurationSection("arenas"));
        Gui gui = readGui(config.getConfigurationSection("gui"));
        Scoreboard scoreboard = readScoreboard(config.getConfigurationSection("scoreboard"));
        if (!arenas.containsKey(activeArena) && !arenas.isEmpty()) {
            activeArena = arenas.keySet().iterator().next();
        }
        return new DuelsConfig(activeArena, Collections.unmodifiableMap(arenas), gui, scoreboard);
    }

    private static Map<String, Arena> readArenas(ConfigurationSection section) {
        Map<String, Arena> arenas = new LinkedHashMap<>();
        if (section == null) {
            return arenas;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection arenaSection = section.getConfigurationSection(id);
            if (arenaSection == null) {
                continue;
            }
            arenas.put(id, readArena(id, arenaSection));
        }
        return arenas;
    }

    private static Arena readArena(String id, ConfigurationSection section) {
        String world = section.getString("world", "duels");
        Region bounds = readRegion(section.getConfigurationSection("bounds"), new Vector3i(-32, 115, -32), new Vector3i(32, 150, 32));
        Walls walls = readWalls(section.getConfigurationSection("walls"));
        Ceiling ceiling = readCeiling(section.getConfigurationSection("ceiling"));
        Floor floor = readFloor(section.getConfigurationSection("floor"));
        Hazards hazards = readHazards(section.getConfigurationSection("hazards"));
        Spawns spawns = readSpawns(section.getConfigurationSection("spawns"));
        Rules rules = readRules(section.getConfigurationSection("rules"));
        Map<String, ResetPreset> resets = readResets(section.getConfigurationSection("resets"));
        Map<String, ResetPreset> traps = readResets(section.getConfigurationSection("traps"));
        return new Arena(id, world, bounds, walls, ceiling, floor, hazards, spawns, rules, resets, traps);
    }

    private static Walls readWalls(ConfigurationSection section) {
        if (section == null) {
            return new Walls(new Region(new Vector3i(-48, -57, -48), new Vector3i(48, -40, 48)), 1,
                    "reinforced_deepslate", "polished_deepslate");
        }
        Region inner = readRegion(section.getConfigurationSection("inner_min"), section.getConfigurationSection("inner_max"));
        if (inner == null) {
            inner = readRegion(section.getConfigurationSection("inner"));
        }
        if (inner == null) {
            inner = new Region(new Vector3i(-48, -57, -48), new Vector3i(48, -40, 48));
        }
        int thickness = section.getInt("thickness", 1);
        String coreMaterial = section.getString("core_material", "reinforced_deepslate");
        String innerMaterial = section.getString("inner_material", "polished_deepslate");
        return new Walls(inner, thickness, coreMaterial, innerMaterial);
    }

    private static Ceiling readCeiling(ConfigurationSection section) {
        if (section == null) {
            return new Ceiling(-39, List.of(-31, -30), "purple_stained_glass",
                    "bedrock", new LightRaster(true, -40, 12, "sea_lantern", true));
        }
        int yGlass = section.getInt("y_glass", -39);
        List<Integer> crown = section.getIntegerList("y_crown");
        if (crown.isEmpty()) {
            crown = List.of(-31, -30);
        }
        String glassMaterial = section.getString("glass_material", "purple_stained_glass");
        String crownMaterial = section.getString("crown_material", "bedrock");
        LightRaster raster = readLightRaster(section.getConfigurationSection("light_raster"));
        return new Ceiling(yGlass, crown, glassMaterial, crownMaterial, raster);
    }

    private static LightRaster readLightRaster(ConfigurationSection section) {
        if (section == null) {
            return new LightRaster(false, -40, 12, "sea_lantern", false);
        }
        boolean enabled = section.getBoolean("enabled", true);
        int y = section.getInt("y", -40);
        int step = section.getInt("step", 12);
        String material = section.getString("material", "sea_lantern");
        boolean extraCorners = section.getBoolean("extra_corners", false);
        return new LightRaster(enabled, y, step, material, extraCorners);
    }

    private static Floor readFloor(ConfigurationSection section) {
        if (section == null) {
            return new Floor(-57, "deepslate_tiles", Collections.emptyMap());
        }
        int y = section.getInt("y", -57);
        String defaultMaterial = section.getString("default_material", "deepslate_tiles");
        Map<String, FloorPreset> presets = new LinkedHashMap<>();
        ConfigurationSection presetSection = section.getConfigurationSection("presets");
        if (presetSection != null) {
            for (String id : presetSection.getKeys(false)) {
                List<ResetStep> steps = readResetSteps(presetSection.getMapList(id + ".layers"));
                presets.put(id, new FloorPreset(id, steps));
            }
        }
        return new Floor(y, defaultMaterial, Collections.unmodifiableMap(presets));
    }

    private static Hazards readHazards(ConfigurationSection section) {
        if (section == null) {
            return new Hazards(-56, Map.of());
        }
        int layerY = section.getInt("layer_y", -56);
        Map<String, HazardPreset> presets = new LinkedHashMap<>();
        ConfigurationSection presetSection = section.getConfigurationSection("presets");
        if (presetSection != null) {
            for (String id : presetSection.getKeys(false)) {
                ConfigurationSection hazardSection = presetSection.getConfigurationSection(id);
                if (hazardSection == null) {
                    continue;
                }
                Region region = readRegion(hazardSection.getConfigurationSection("region"));
                if (region == null) {
                    region = readRegion(hazardSection);
                }
                if (region == null) {
                    continue;
                }
                List<HazardLayer> layers = readHazardLayers(hazardSection, layerY);
                presets.put(id, new HazardPreset(id, region, Collections.unmodifiableList(layers)));
            }
        }
        return new Hazards(layerY, Collections.unmodifiableMap(presets));
    }

    private static List<HazardLayer> readHazardLayers(ConfigurationSection section, int defaultY) {
        List<HazardLayer> layers = new ArrayList<>();
        List<Map<?, ?>> rawLayers = section.getMapList("layers");
        if (rawLayers.isEmpty()) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("y", section.getInt("y", defaultY));
            fallback.put("offset", section.contains("offset") ? section.getInt("offset") : 0);
            fallback.put("thickness", section.getInt("thickness", 1));
            fallback.put("material", section.getString("material", "LAVA"));
            rawLayers = List.of(fallback);
        }
        for (Map<?, ?> raw : rawLayers) {
            if (!(raw instanceof Map<?, ?> map)) {
                continue;
            }
            int base = defaultY;
            if (map.containsKey("offset")) {
                base += parseInt(map.get("offset"), 0);
            }
            int y = map.containsKey("y") ? parseInt(map.get("y"), base) : base;
            int thickness = Math.max(1, parseInt(map.get("thickness"), 1));
            Object mat = map.get("material");
            String material = mat != null ? String.valueOf(mat) : String.valueOf(section.getString("material", "LAVA"));
            layers.add(new HazardLayer(y, thickness, material));
        }
        return layers;
    }

    private static int parseInt(Object value, int def) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private static Spawns readSpawns(ConfigurationSection section) {
        if (section == null) {
            return new Spawns(new Point(0.5, 120.0, 0.5, 0f, 0f),
                    new Point(-24.5, -57.0, 0.5, 90f, 0f),
                    new Point(24.5, -57.0, 0.5, -90f, 0f));
        }
        Point lobby = readPoint(section.getConfigurationSection("lobby"),
                new Point(0.5, 120.0, 0.5, 0f, 0f));
        Point p1 = readPoint(section.getConfigurationSection("p1"),
                new Point(-24.5, -57.0, 0.5, 90f, 0f));
        Point p2 = readPoint(section.getConfigurationSection("p2"),
                new Point(24.5, -57.0, 0.5, -90f, 0f));
        return new Spawns(lobby, p1, p2);
    }

    private static Rules readRules(ConfigurationSection section) {
        if (section == null) {
            return new Rules(false, 48, true, true);
        }
        boolean keepInventory = section.getBoolean("keep_inventory", false);
        int buildProtectRadius = section.getInt("build_protect_radius", 48);
        boolean allowCrystals = section.getBoolean("allow_crystals", true);
        boolean scoreboardEnabled = section.getBoolean("scoreboard_enabled", true);
        return new Rules(keepInventory, buildProtectRadius, allowCrystals, scoreboardEnabled);
    }

    private static Map<String, ResetPreset> readResets(ConfigurationSection section) {
        Map<String, ResetPreset> presets = new LinkedHashMap<>();
        if (section == null) {
            return presets;
        }
        for (String id : section.getKeys(false)) {
            List<Map<?, ?>> rawSteps = section.getMapList(id + ".steps");
            List<ResetStep> steps = readResetSteps(rawSteps);
            presets.put(id, new ResetPreset(id, steps));
        }
        return Collections.unmodifiableMap(presets);
    }

    private static List<ResetStep> readResetSteps(List<Map<?, ?>> rawSteps) {
        List<ResetStep> steps = new ArrayList<>();
        for (Map<?, ?> raw : rawSteps) {
            Object typeObj = raw.get("type");
            String type = typeObj != null ? String.valueOf(typeObj) : "fill";
            Map<String, Object> params = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                params.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            steps.add(new ResetStep(type, Collections.unmodifiableMap(params)));
        }
        return Collections.unmodifiableList(steps);
    }

    private static Gui readGui(ConfigurationSection section) {
        if (section == null) {
            return new Gui(27, new GuiSlots(11, 15, 13), Map.of());
        }
        int size = Math.max(9, Math.min(54, section.getInt("size", 27)));
        GuiSlots slots = readGuiSlots(section.getConfigurationSection("slots"));
        Map<Integer, GuiItem> items = new LinkedHashMap<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection == null) {
                    continue;
                }
                int slot = slots.byName(key);
                if (slot == -1) {
                    slot = itemSection.getInt("slot", -1);
                }
                if (slot < 0) {
                    continue;
                }
                String material = itemSection.getString("material", "BARRIER");
                String name = itemSection.getString("name", "");
                List<String> lore = itemSection.getStringList("lore");
                String action = itemSection.getString("action", key);
                items.put(slot, new GuiItem(slot, material, name, lore, action));
            }
        }
        return new Gui(size, slots, Collections.unmodifiableMap(items));
    }

    private static GuiSlots readGuiSlots(ConfigurationSection section) {
        if (section == null) {
            return new GuiSlots(11, 15, 13);
        }
        int start = section.getInt("start", 11);
        int stop = section.getInt("stop", 15);
        int refill = section.getInt("refill", 13);
        Map<String, Integer> named = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            if (!Set.of("start", "stop", "refill").contains(key)) {
                named.put(key, section.getInt(key));
            }
        }
        return new GuiSlots(start, stop, refill, Collections.unmodifiableMap(named));
    }

    private static Scoreboard readScoreboard(ConfigurationSection section) {
        if (section == null) {
            return new Scoreboard(true);
        }
        boolean enabled = section.getBoolean("enabled", true);
        return new Scoreboard(enabled);
    }

    private static Region readRegion(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        ConfigurationSection minSection = section.getConfigurationSection("min");
        ConfigurationSection maxSection = section.getConfigurationSection("max");
        return readRegion(minSection, maxSection);
    }

    private static Region readRegion(ConfigurationSection minSection, ConfigurationSection maxSection) {
        if (minSection == null || maxSection == null) {
            return null;
        }
        return new Region(readVector(minSection, new Vector3i(-32, 115, -32)),
                readVector(maxSection, new Vector3i(32, 150, 32)));
    }

    private static Region readRegion(ConfigurationSection section, Vector3i defaultMin, Vector3i defaultMax) {
        if (section == null) {
            return new Region(defaultMin, defaultMax);
        }
        Vector3i min = readVector(section.getConfigurationSection("min"), defaultMin);
        Vector3i max = readVector(section.getConfigurationSection("max"), defaultMax);
        return new Region(min, max);
    }

    private static Vector3i readVector(ConfigurationSection section, Vector3i def) {
        if (section == null) {
            return def;
        }
        int x = section.getInt("x", def.x());
        int y = section.getInt("y", def.y());
        int z = section.getInt("z", def.z());
        return new Vector3i(x, y, z);
    }

    private static Point readPoint(ConfigurationSection section, Point def) {
        if (section == null) {
            return def;
        }
        double x = section.getDouble("x", def.x());
        double y = section.getDouble("y", def.y());
        double z = section.getDouble("z", def.z());
        float yaw = (float) section.getDouble("yaw", def.yaw());
        float pitch = (float) section.getDouble("pitch", def.pitch());
        return new Point(x, y, z, yaw, pitch);
    }

    public void writeTo(FileConfiguration config) {
        config.set("active_arena", activeArena);
        ConfigurationSection arenasSection = config.createSection("arenas");
        for (Arena arena : arenas.values()) {
            ConfigurationSection section = arenasSection.createSection(arena.id());
            section.set("world", arena.world());
            writeRegion(section.createSection("bounds"), arena.bounds());
            writeWalls(section.createSection("walls"), arena.walls());
            writeCeiling(section.createSection("ceiling"), arena.ceiling());
            writeFloor(section.createSection("floor"), arena.floor());
            writeHazards(section.createSection("hazards"), arena.hazards());
            writeSpawns(section.createSection("spawns"), arena.spawns());
            writeRules(section.createSection("rules"), arena.rules());
            writeResets(section.createSection("resets"), arena.resets());
            writeResets(section.createSection("traps"), arena.traps());
        }
        writeGui(config.createSection("gui"), gui);
        ConfigurationSection scoreboardSection = config.createSection("scoreboard");
        scoreboardSection.set("enabled", scoreboard.enabled());
    }

    private void writeRegion(ConfigurationSection section, Region region) {
        writeVector(section.createSection("min"), region.min());
        writeVector(section.createSection("max"), region.max());
    }

    private void writeWalls(ConfigurationSection section, Walls walls) {
        writeRegion(section.createSection("inner"), walls.inner());
        section.set("thickness", walls.thickness());
        section.set("core_material", walls.coreMaterial());
        section.set("inner_material", walls.innerMaterial());
    }

    private void writeCeiling(ConfigurationSection section, Ceiling ceiling) {
        section.set("y_glass", ceiling.yGlass());
        section.set("y_crown", ceiling.yCrown());
        section.set("glass_material", ceiling.glassMaterial());
        section.set("crown_material", ceiling.crownMaterial());
        ConfigurationSection rasterSection = section.createSection("light_raster");
        rasterSection.set("enabled", ceiling.lightRaster().enabled());
        rasterSection.set("y", ceiling.lightRaster().y());
        rasterSection.set("step", ceiling.lightRaster().step());
        rasterSection.set("material", ceiling.lightRaster().material());
        rasterSection.set("extra_corners", ceiling.lightRaster().extraCorners());
    }

    private void writeFloor(ConfigurationSection section, Floor floor) {
        section.set("y", floor.y());
        section.set("default_material", floor.defaultMaterial());
        ConfigurationSection presetsSection = section.createSection("presets");
        for (FloorPreset preset : floor.presets().values()) {
            ConfigurationSection presetSection = presetsSection.createSection(preset.id());
            List<Map<String, Object>> layers = new ArrayList<>();
            for (ResetStep step : preset.steps()) {
                layers.add(new LinkedHashMap<>(step.parameters()));
            }
            presetSection.set("layers", layers);
        }
    }

    private void writeHazards(ConfigurationSection section, Hazards hazards) {
        section.set("layer_y", hazards.layerY());
        ConfigurationSection presetsSection = section.createSection("presets");
        for (Map.Entry<String, HazardPreset> entry : hazards.presets().entrySet()) {
            ConfigurationSection presetSection = presetsSection.createSection(entry.getKey());
            writeRegion(presetSection.createSection("region"), entry.getValue().region());
            List<Map<String, Object>> rawLayers = new ArrayList<>();
            for (HazardLayer layer : entry.getValue().layers()) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("y", layer.y());
                map.put("thickness", layer.thickness());
                map.put("material", layer.material());
                rawLayers.add(map);
            }
            presetSection.set("layers", rawLayers);
        }
    }

    private void writeSpawns(ConfigurationSection section, Spawns spawns) {
        writePoint(section.createSection("lobby"), spawns.lobby());
        writePoint(section.createSection("p1"), spawns.p1());
        writePoint(section.createSection("p2"), spawns.p2());
    }

    private void writeRules(ConfigurationSection section, Rules rules) {
        section.set("keep_inventory", rules.keepInventory());
        section.set("build_protect_radius", rules.buildProtectRadius());
        section.set("allow_crystals", rules.allowCrystals());
        section.set("scoreboard_enabled", rules.scoreboardEnabled());
    }

    private void writeResets(ConfigurationSection section, Map<String, ResetPreset> presets) {
        for (Map.Entry<String, ResetPreset> entry : presets.entrySet()) {
            ConfigurationSection presetSection = section.createSection(entry.getKey());
            List<Map<String, Object>> steps = new ArrayList<>();
            for (ResetStep step : entry.getValue().steps()) {
                steps.add(new LinkedHashMap<>(step.parameters()));
            }
            presetSection.set("steps", steps);
        }
    }

    private void writeGui(ConfigurationSection section, Gui gui) {
        section.set("size", gui.size());
        ConfigurationSection slotsSection = section.createSection("slots");
        slotsSection.set("start", gui.slots().start());
        slotsSection.set("stop", gui.slots().stop());
        slotsSection.set("refill", gui.slots().refill());
        for (Map.Entry<String, Integer> entry : gui.slots().named().entrySet()) {
            slotsSection.set(entry.getKey(), entry.getValue());
        }
        ConfigurationSection itemsSection = section.createSection("items");
        for (GuiItem item : gui.items().values()) {
            ConfigurationSection itemSection = itemsSection.createSection(item.action());
            itemSection.set("slot", item.slot());
            itemSection.set("material", item.material());
            itemSection.set("name", item.name());
            itemSection.set("lore", item.lore());
            itemSection.set("action", item.action());
        }
    }

    private void writeVector(ConfigurationSection section, Vector3i vector) {
        section.set("x", vector.x());
        section.set("y", vector.y());
        section.set("z", vector.z());
    }

    private void writePoint(ConfigurationSection section, Point point) {
        section.set("x", point.x());
        section.set("y", point.y());
        section.set("z", point.z());
        section.set("yaw", point.yaw());
        section.set("pitch", point.pitch());
    }

    public record Arena(String id, String world, Region bounds, Walls walls, Ceiling ceiling, Floor floor,
                        Hazards hazards, Spawns spawns, Rules rules, Map<String, ResetPreset> resets,
                        Map<String, ResetPreset> traps) {
    }

    public record Region(Vector3i min, Vector3i max) {
    }

    public record Vector3i(int x, int y, int z) {
    }

    public record Walls(Region inner, int thickness, String coreMaterial, String innerMaterial) {
    }

    public record Ceiling(int yGlass, List<Integer> yCrown, String glassMaterial, String crownMaterial,
                          LightRaster lightRaster) {
    }

    public record LightRaster(boolean enabled, int y, int step, String material, boolean extraCorners) {
    }

    public record Floor(int y, String defaultMaterial, Map<String, FloorPreset> presets) {
    }

    public record FloorPreset(String id, List<ResetStep> steps) {
    }

    public record Hazards(int layerY, Map<String, HazardPreset> presets) {
    }

    public record HazardPreset(String id, Region region, List<HazardLayer> layers) {
    }

    public record HazardLayer(int y, int thickness, String material) {
    }

    public record Spawns(Point lobby, Point p1, Point p2) {
    }

    public record Point(double x, double y, double z, float yaw, float pitch) {
    }

    public record Rules(boolean keepInventory, int buildProtectRadius, boolean allowCrystals,
                        boolean scoreboardEnabled) {
    }

    public record ResetPreset(String id, List<ResetStep> steps) {
    }

    public record ResetStep(String type, Map<String, Object> parameters) {
        public String material() {
            return Objects.toString(parameters.get("material"), null);
        }

        public String find() {
            return Objects.toString(parameters.get("find"), null);
        }

        public String with() {
            return Objects.toString(parameters.get("with"), null);
        }
    }

    public record Gui(int size, GuiSlots slots, Map<Integer, GuiItem> items) {
    }

    public record GuiSlots(int start, int stop, int refill, Map<String, Integer> named) {
        public GuiSlots(int start, int stop, int refill) {
            this(start, stop, refill, Map.of());
        }

        public int byName(String key) {
            if (key.equalsIgnoreCase("start")) {
                return start;
            }
            if (key.equalsIgnoreCase("stop")) {
                return stop;
            }
            if (key.equalsIgnoreCase("refill")) {
                return refill;
            }
            return named.getOrDefault(key, -1);
        }
    }

    public record GuiItem(int slot, String material, String name, List<String> lore, String action) {
    }

    public record Scoreboard(boolean enabled) {
    }
}
