package de.opalium.luckysky.duels;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.DuelsConfig;
import de.opalium.luckysky.config.DuelsConfig.Arena;
import de.opalium.luckysky.config.DuelsConfig.FloorPreset;
import de.opalium.luckysky.config.DuelsConfig.HazardLayer;
import de.opalium.luckysky.config.DuelsConfig.HazardPreset;
import de.opalium.luckysky.config.DuelsConfig.Hazards;
import de.opalium.luckysky.config.DuelsConfig.LightRaster;
import de.opalium.luckysky.config.DuelsConfig.Region;
import de.opalium.luckysky.config.DuelsConfig.ResetPreset;
import de.opalium.luckysky.config.DuelsConfig.ResetStep;
import de.opalium.luckysky.config.DuelsConfig.Vector3i;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitTask;

public class ArenaService {
    private final LuckySkyPlugin plugin;
    private final BlockFiller blockFiller;

    public ArenaService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.blockFiller = FaweBlockFiller.create(plugin.getLogger()).orElseGet(BukkitBlockFiller::new);
    }

    public DuelsConfig config() {
        return plugin.configs().duels();
    }

    public Arena activeArena() {
        DuelsConfig config = config();
        return config.arenas().get(config.activeArena());
    }

    public List<String> arenaIds() {
        return new ArrayList<>(config().arenas().keySet());
    }

    public boolean selectArena(String id) {
        DuelsConfig cfg = config();
        if (!cfg.arenas().containsKey(id)) {
            return false;
        }
        DuelsConfig updated = new DuelsConfig(id, cfg.arenas(), cfg.gui(), cfg.scoreboard());
        plugin.configs().updateDuels(updated);
        return true;
    }

    public String cycleArena() {
        List<String> ids = arenaIds();
        if (ids.isEmpty()) {
            return null;
        }
        String current = config().activeArena();
        int index = ids.indexOf(current);
        int nextIndex = (index + 1) % ids.size();
        String next = ids.get(nextIndex);
        selectArena(next);
        return next;
    }

    public boolean applyReset(String presetId, CommandSender sender) {
        Arena arena = activeArena();
        if (arena == null) {
            Msg.to(sender, "&cKeine aktive Arena ausgewählt.");
            return false;
        }
        ResetPreset preset = arena.resets().get(presetId);
        if (preset == null) {
            Msg.to(sender, "&cUnbekanntes Reset-Preset: &f" + presetId);
            return false;
        }
        executeSteps(arena, preset.steps(), "Reset " + presetId);
        Msg.to(sender, "&aReset &f" + presetId + " &aangewendet.");
        return true;
    }

    public boolean applyFloorPreset(String presetId, CommandSender sender) {
        Arena arena = activeArena();
        if (arena == null) {
            Msg.to(sender, "&cKeine aktive Arena ausgewählt.");
            return false;
        }
        FloorPreset preset = arena.floor().presets().get(presetId);
        if (preset == null) {
            Msg.to(sender, "&cUnbekanntes Boden-Preset: &f" + presetId);
            return false;
        }
        executeSteps(arena, preset.steps(), "Floor " + presetId);
        Msg.to(sender, "&aBoden-Preset &f" + presetId + " &aangewendet.");
        return true;
    }

    public boolean applyTrapPreset(String presetId, CommandSender sender) {
        Arena arena = activeArena();
        if (arena == null) {
            Msg.to(sender, "&cKeine aktive Arena ausgewählt.");
            return false;
        }
        ResetPreset preset = arena.traps().get(presetId);
        if (preset == null) {
            Msg.to(sender, "&cUnbekanntes Fallen-Preset: &f" + presetId);
            return false;
        }
        executeSteps(arena, preset.steps(), "Trap " + presetId);
        Msg.to(sender, "&aFalle &f" + presetId + " &aaktiviert.");
        return true;
    }

    public boolean applyHazardPreset(String presetId, CommandSender sender) {
        Arena arena = activeArena();
        if (arena == null) {
            Msg.to(sender, "&cKeine aktive Arena ausgewählt.");
            return false;
        }
        Hazards hazards = arena.hazards();
        World world = Worlds.require(arena.world());
        if (presetId.equalsIgnoreCase("clear")) {
            clearHazards(world, arena, hazards);
            Msg.to(sender, "&aAlle Gefahren entfernt.");
            return true;
        }
        HazardPreset preset = hazards.presets().get(presetId);
        if (preset == null) {
            Msg.to(sender, "&cUnbekanntes Gefahren-Preset: &f" + presetId);
            return false;
        }
        for (HazardLayer layer : preset.layers()) {
            Optional<Material> materialOpt = parseMaterial(layer.material());
            if (materialOpt.isEmpty()) {
                plugin.getLogger().warning("[ArenaService] Unbekanntes Material für Hazard '" + presetId + "': " + layer.material());
                continue;
            }
            int minY = layer.y();
            int maxY = layer.y() + Math.max(0, layer.thickness() - 1);
            Vector3i min = new Vector3i(preset.region().min().x(), minY, preset.region().min().z());
            Vector3i max = new Vector3i(preset.region().max().x(), maxY, preset.region().max().z());
            fillRegion(world, min, max, materialOpt.get(), null);
        }
        Msg.to(sender, "&aGefahren-Preset &f" + presetId + " &aaktiviert.");
        return true;
    }

    public void setLight(boolean enabled, CommandSender sender) {
        Arena arena = activeArena();
        if (arena == null) {
            Msg.to(sender, "&cKeine aktive Arena ausgewählt.");
            return;
        }
        applyLightRaster(Worlds.require(arena.world()), arena, enabled);
        Msg.to(sender, enabled ? "&aLicht-Raster aktiviert." : "&cLicht-Raster deaktiviert.");
    }

    private void executeSteps(Arena arena, List<ResetStep> steps, String context) {
        World world = Worlds.require(arena.world());
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getLogger().info("[ArenaService] Beginne " + context + " für " + arena.id());
            runStepQueue(world, arena, steps, 0, context);
        });
    }

    private void runStepQueue(World world, Arena arena, List<ResetStep> steps, int index, String context) {
        if (index >= steps.size()) {
            plugin.getLogger().info("[ArenaService] " + context + " abgeschlossen für " + arena.id());
            return;
        }
        ResetStep step = steps.get(index);
        long delay = Math.max(1L, applyStep(world, arena, step));
        Bukkit.getScheduler().runTaskLater(plugin, () -> runStepQueue(world, arena, steps, index + 1, context), delay);
    }

    private long applyStep(World world, Arena arena, ResetStep step) {
        String type = step.type().toLowerCase(Locale.ROOT);
        Map<String, Object> params = new LinkedHashMap<>(step.parameters());
        switch (type) {
            case "fill" -> {
                Vector3i from = parseVector(params.get("from"), arena.bounds().min());
                Vector3i to = parseVector(params.get("to"), arena.bounds().max());
                Material material = parseMaterial(step.material()).orElse(Material.AIR);
                fillRegion(world, from, to, material, null);
                return 1L;
            }
            case "fill_replace" -> {
                Vector3i from = parseVector(params.get("from"), arena.bounds().min());
                Vector3i to = parseVector(params.get("to"), arena.bounds().max());
                Material find = parseMaterial(step.find()).orElse(Material.AIR);
                Material with = parseMaterial(step.with()).orElse(Material.AIR);
                fillRegion(world, from, to, with, find);
                return 1L;
            }
            case "slice_fill" -> {
                applySliceFill(world, arena, params, step.material());
                return 1L;
            }
            case "light_raster" -> {
                boolean enabled = Boolean.parseBoolean(String.valueOf(params.getOrDefault("enabled", "true")));
                applyLightRaster(world, arena, enabled);
                return 1L;
            }
            case "clone" -> {
                applyClone(world, params);
                return 1L;
            }
            case "block_remove" -> {
                applyBlockRemove(world, arena, params, step);
                return 1L;
            }
            case "delay", "wait", "pause" -> {
                int ticks = Math.max(1, toInt(params.get("ticks"), 20));
                return ticks;
            }
            case "particles" -> {
                return applyParticles(world, arena, params);
            }
            default -> {
                plugin.getLogger().warning("[ArenaService] Unbekannter Schritt: " + type);
                return 1L;
            }
        }
    }

    private void clearHazards(World world, Arena arena, Hazards hazards) {
        Region fallback = arena.walls().inner();
        int minX = fallback.min().x();
        int maxX = fallback.max().x();
        int minZ = fallback.min().z();
        int maxZ = fallback.max().z();
        int minY = hazards.layerY();
        int maxY = hazards.layerY();
        if (!hazards.presets().isEmpty()) {
            minX = Integer.MAX_VALUE;
            minZ = Integer.MAX_VALUE;
            maxX = Integer.MIN_VALUE;
            maxZ = Integer.MIN_VALUE;
            minY = Integer.MAX_VALUE;
            maxY = Integer.MIN_VALUE;
            for (HazardPreset preset : hazards.presets().values()) {
                minX = Math.min(minX, preset.region().min().x());
                minZ = Math.min(minZ, preset.region().min().z());
                maxX = Math.max(maxX, preset.region().max().x());
                maxZ = Math.max(maxZ, preset.region().max().z());
                for (HazardLayer layer : preset.layers()) {
                    minY = Math.min(minY, layer.y());
                    maxY = Math.max(maxY, layer.y() + Math.max(0, layer.thickness() - 1));
                }
            }
            if (minX == Integer.MAX_VALUE) {
                minX = fallback.min().x();
                maxX = fallback.max().x();
                minZ = fallback.min().z();
                maxZ = fallback.max().z();
            }
            if (minY == Integer.MAX_VALUE) {
                minY = hazards.layerY();
                maxY = hazards.layerY();
            }
        }
        Vector3i min = new Vector3i(minX, minY, minZ);
        Vector3i max = new Vector3i(maxX, maxY, maxZ);
        fillRegion(world, min, max, Material.AIR, null);
    }

    private void applyBlockRemove(World world, Arena arena, Map<String, Object> params, ResetStep step) {
        Material required = parseMaterial(step.find()).orElse(null);
        Vector3i from = parseVector(params.get("from"), null);
        Vector3i to = parseVector(params.get("to"), null);
        if (from != null && to != null) {
            fillRegion(world, from, to, Material.AIR, required);
        }
        Object regionsRaw = params.get("regions");
        if (regionsRaw instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> regionMap) {
                    Vector3i rFrom = parseVector(regionMap.get("from"), arena.bounds().min());
                    Vector3i rTo = parseVector(regionMap.get("to"), arena.bounds().max());
                    fillRegion(world, rFrom, rTo, Material.AIR, required);
                }
            }
        }
        for (Vector3i point : parseVectorList(params.get("points"))) {
            removeBlock(world, point, required);
        }
    }

    private long applyParticles(World world, Arena arena, Map<String, Object> params) {
        Particle particle = parseParticle(String.valueOf(params.getOrDefault("particle", "SMOKE_NORMAL")))
                .orElse(Particle.SMOKE_NORMAL);
        Vector3i position = parseVector(params.getOrDefault("position", params.get("pos")), arena.bounds().min());
        double[] baseOffset = parseOffset(params.get("offset"));
        double offsetX = toDouble(params.get("offset_x"), baseOffset[0]);
        double offsetY = toDouble(params.get("offset_y"), baseOffset[1]);
        double offsetZ = toDouble(params.get("offset_z"), baseOffset[2]);
        int count = Math.max(1, toInt(params.get("count"), 20));
        double speed = toDouble(params.get("speed"), 0.01d);
        int duration = Math.max(0, toInt(params.get("duration"), 40));
        int interval = Math.max(1, toInt(params.get("interval"), 5));
        int delay = Math.max(0, toInt(params.get("delay"), 0));
        Runnable spawn = () -> world.spawnParticle(particle,
                position.x() + 0.5,
                position.y() + 0.5,
                position.z() + 0.5,
                count,
                offsetX,
                offsetY,
                offsetZ,
                speed);
        if (duration <= 0) {
            if (delay <= 0) {
                spawn.run();
            } else {
                Bukkit.getScheduler().runTaskLater(plugin, spawn, delay);
            }
            return Math.max(1, delay);
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, spawn, delay, interval);
        Bukkit.getScheduler().runTaskLater(plugin, task::cancel, delay + duration);
        return Math.max(1, delay + duration);
    }

    private void applyClone(World world, Map<String, Object> params) {
        Vector3i from = parseVector(params.get("from"), null);
        Vector3i to = parseVector(params.get("to"), null);
        Vector3i target = parseVector(params.get("target"), null);
        if (from == null || to == null || target == null) {
            plugin.getLogger().warning("[ArenaService] Clone-Schritt ohne gültige Parameter.");
            return;
        }
        int dx = to.x() - from.x();
        int dy = to.y() - from.y();
        int dz = to.z() - from.z();
        for (int x = 0; x <= dx; x++) {
            for (int y = 0; y <= dy; y++) {
                for (int z = 0; z <= dz; z++) {
                    Block source = world.getBlockAt(from.x() + x, from.y() + y, from.z() + z);
                    Block dest = world.getBlockAt(target.x() + x, target.y() + y, target.z() + z);
                    dest.setBlockData(source.getBlockData(), false);
                }
            }
        }
    }

    private void applySliceFill(World world, Arena arena, Map<String, Object> params, String materialName) {
        int fromY = toInt(params.get("from_y"), arena.bounds().min().y());
        int toY = toInt(params.get("to_y"), arena.bounds().max().y());
        Coordinate fromExpr = parseCoordinate(params.get("from"));
        Coordinate toExpr = parseCoordinate(params.get("to"));
        Material material = parseMaterial(materialName).orElse(Material.AIR);
        for (int y = fromY; y <= toY; y++) {
            Vector3i from = resolveCoordinate(fromExpr, y, arena.bounds().min());
            Vector3i to = resolveCoordinate(toExpr, y, arena.bounds().max());
            fillRegion(world, from, to, material, null);
        }
    }

    private void applyLightRaster(World world, Arena arena, boolean enabled) {
        LightRaster raster = arena.ceiling().lightRaster();
        if (!raster.enabled() && enabled) {
            // even if config disables by default, respect request to enable
        }
        Region region = arena.walls().inner();
        int minX = region.min().x();
        int maxX = region.max().x();
        int minZ = region.min().z();
        int maxZ = region.max().z();
        int y = raster.y();
        Material material = parseMaterial(raster.material()).orElse(Material.SEA_LANTERN);
        for (int x = minX; x <= maxX; x++) {
            if ((x - minX) % raster.step() != 0 && !(raster.extraCorners() && (x == minX || x == maxX))) {
                continue;
            }
            for (int z = minZ; z <= maxZ; z++) {
                if ((z - minZ) % raster.step() != 0 && !(raster.extraCorners() && (z == minZ || z == maxZ))) {
                    continue;
                }
                Block block = world.getBlockAt(x, y, z);
                if (enabled) {
                    block.setType(material, false);
                } else if (block.getType() == material) {
                    block.setType(Material.AIR, false);
                }
            }
        }
    }

    private void fillRegion(World world, Vector3i min, Vector3i max, Material material, Material required) {
        if (min == null || max == null || material == null) {
            return;
        }
        int minX = Math.min(min.x(), max.x());
        int maxX = Math.max(min.x(), max.x());
        int minY = Math.min(min.y(), max.y());
        int maxY = Math.max(min.y(), max.y());
        int minZ = Math.min(min.z(), max.z());
        int maxZ = Math.max(min.z(), max.z());
        Vector3i actualMin = new Vector3i(minX, minY, minZ);
        Vector3i actualMax = new Vector3i(maxX, maxY, maxZ);
        if (required == null) {
            blockFiller.fill(world, actualMin, actualMax, material);
        } else {
            blockFiller.fillReplacing(world, actualMin, actualMax, material, required);
        }
    }

    private List<Vector3i> parseVectorList(Object raw) {
        List<Vector3i> list = new ArrayList<>();
        if (raw instanceof List<?> rawList) {
            for (Object entry : rawList) {
                Vector3i vector = parseVector(entry, null);
                if (vector != null) {
                    list.add(vector);
                }
            }
        }
        return list;
    }

    private void removeBlock(World world, Vector3i point, Material required) {
        if (point == null) {
            return;
        }
        Block block = world.getBlockAt(point.x(), point.y(), point.z());
        if (required != null && block.getType() != required) {
            return;
        }
        block.setType(Material.AIR, false);
    }

    private double[] parseOffset(Object raw) {
        double[] result = new double[] {0d, 0d, 0d};
        if (raw instanceof Number number) {
            double value = number.doubleValue();
            return new double[] {value, value, value};
        }
        if (raw instanceof List<?> list && list.size() >= 3) {
            result[0] = toDouble(list.get(0), 0d);
            result[1] = toDouble(list.get(1), 0d);
            result[2] = toDouble(list.get(2), 0d);
            return result;
        }
        if (raw instanceof Map<?, ?> map) {
            result[0] = toDouble(map.get("x"), 0d);
            result[1] = toDouble(map.get("y"), 0d);
            result[2] = toDouble(map.get("z"), 0d);
        }
        return result;
    }

    private double toDouble(Object value, double def) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    private Optional<Particle> parseParticle(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Particle.valueOf(name.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().log(Level.WARNING, "[ArenaService] Unbekannter Partikeltyp: " + name, ex);
            return Optional.empty();
        }
    }

    private Vector3i parseVector(Object raw, Vector3i def) {
        if (raw instanceof Vector3i vector) {
            return vector;
        }
        if (raw instanceof List<?> list && list.size() >= 3) {
            int x = toInt(list.get(0), def != null ? def.x() : 0);
            int y = toInt(list.get(1), def != null ? def.y() : 0);
            int z = toInt(list.get(2), def != null ? def.z() : 0);
            return new Vector3i(x, y, z);
        }
        if (raw instanceof Map<?, ?> map) {
            int x = toInt(map.get("x"), def != null ? def.x() : 0);
            int y = toInt(map.get("y"), def != null ? def.y() : 0);
            int z = toInt(map.get("z"), def != null ? def.z() : 0);
            return new Vector3i(x, y, z);
        }
        return def;
    }

    private int toInt(Object value, int def) {
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

    private Optional<Material> parseMaterial(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return Optional.ofNullable(material);
    }

    private Coordinate parseCoordinate(Object raw) {
        if (raw instanceof List<?> list && list.size() >= 3) {
            return new Coordinate(parseNullable(list.get(0)), parseNullable(list.get(1)), parseNullable(list.get(2)));
        }
        return new Coordinate(parseNullable(raw), null, null);
    }

    private Integer parseNullable(Object value) {
        if (value == null) {
            return null;
        }
        String str = String.valueOf(value);
        if ("*".equals(str)) {
            return null;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Vector3i resolveCoordinate(Coordinate coordinate, int currentY, Vector3i fallback) {
        int x = coordinate.x != null ? coordinate.x : fallback.x();
        int y = coordinate.y != null ? coordinate.y : currentY;
        int z = coordinate.z != null ? coordinate.z : fallback.z();
        return new Vector3i(x, y, z);
    }

    private record Coordinate(Integer x, Integer y, Integer z) {
    }

    private interface BlockFiller {
        void fill(World world, Vector3i min, Vector3i max, Material material);

        void fillReplacing(World world, Vector3i min, Vector3i max, Material material, Material required);
    }

    private static class BukkitBlockFiller implements BlockFiller {
        @Override
        public void fill(World world, Vector3i min, Vector3i max, Material material) {
            for (int x = min.x(); x <= max.x(); x++) {
                for (int y = min.y(); y <= max.y(); y++) {
                    for (int z = min.z(); z <= max.z(); z++) {
                        world.getBlockAt(x, y, z).setType(material, false);
                    }
                }
            }
        }

        @Override
        public void fillReplacing(World world, Vector3i min, Vector3i max, Material material, Material required) {
            for (int x = min.x(); x <= max.x(); x++) {
                for (int y = min.y(); y <= max.y(); y++) {
                    for (int z = min.z(); z <= max.z(); z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (required != null && block.getType() != required) {
                            continue;
                        }
                        block.setType(material, false);
                    }
                }
            }
        }
    }

    private static class FaweBlockFiller extends BukkitBlockFiller {
        private final Logger logger;
        private final Method adaptWorld;
        private final Method adaptBlockData;
        private final Method getEditSessionBuilder;
        private final Method buildMethod;
        private final Method fastModeMethod;
        private final Method blockVectorAt;
        private final Constructor<?> cuboidConstructor;
        private final Method setBlocks;
        private final Method closeMethod;
        private final Class<?> regionClass;

        private FaweBlockFiller(Logger logger) throws ReflectiveOperationException {
            this.logger = logger;
            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Class<?> worldClass = Class.forName("com.sk89q.worldedit.world.World");
            Class<?> faweApi = Class.forName("com.fastasyncworldedit.core.FaweAPI");
            this.adaptWorld = bukkitAdapter.getMethod("adapt", World.class);
            this.adaptBlockData = bukkitAdapter.getMethod("adapt", org.bukkit.block.data.BlockData.class);
            this.getEditSessionBuilder = faweApi.getMethod("getEditSessionBuilder", worldClass);
            Class<?> builderClass = getEditSessionBuilder.getReturnType();
            Method fastMode;
            try {
                fastMode = builderClass.getMethod("fastMode", boolean.class);
            } catch (NoSuchMethodException ignored) {
                try {
                    fastMode = builderClass.getMethod("fastmode", boolean.class);
                } catch (NoSuchMethodException ignoredAgain) {
                    fastMode = null;
                }
            }
            this.fastModeMethod = fastMode;
            this.buildMethod = builderClass.getMethod("build");
            Class<?> editSessionClass = Class.forName("com.sk89q.worldedit.EditSession");
            this.regionClass = Class.forName("com.sk89q.worldedit.regions.Region");
            Class<?> cuboidRegionClass = Class.forName("com.sk89q.worldedit.regions.CuboidRegion");
            Class<?> blockVectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            this.blockVectorAt = blockVectorClass.getMethod("at", int.class, int.class, int.class);
            this.cuboidConstructor = cuboidRegionClass.getConstructor(blockVectorClass, blockVectorClass);
            Class<?> patternClass = Class.forName("com.sk89q.worldedit.function.pattern.Pattern");
            this.setBlocks = editSessionClass.getMethod("setBlocks", regionClass, patternClass);
            this.closeMethod = editSessionClass.getMethod("close");
        }

        public static Optional<BlockFiller> create(Logger logger) {
            if (!Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit")) {
                return Optional.empty();
            }
            try {
                logger.info("[ArenaService] FAWE erkannt – schnelle Füllvorgänge aktiviert.");
                return Optional.of(new FaweBlockFiller(logger));
            } catch (ReflectiveOperationException ex) {
                logger.log(Level.WARNING, "[ArenaService] Konnte FAWE-Hook nicht initialisieren.", ex);
                return Optional.empty();
            }
        }

        @Override
        public void fill(World world, Vector3i min, Vector3i max, Material material) {
            Object editSession = null;
            try {
                Object adaptedWorld = adaptWorld.invoke(null, world);
                Object builder = getEditSessionBuilder.invoke(null, adaptedWorld);
                if (fastModeMethod != null) {
                    fastModeMethod.invoke(builder, true);
                }
                editSession = buildMethod.invoke(builder);
                Object minVec = blockVectorAt.invoke(null, min.x(), min.y(), min.z());
                Object maxVec = blockVectorAt.invoke(null, max.x(), max.y(), max.z());
                Object region = cuboidConstructor.newInstance(minVec, maxVec);
                Object blockState = adaptBlockData.invoke(null, material.createBlockData());
                setBlocks.invoke(editSession, region, blockState);
            } catch (InvocationTargetException | IllegalAccessException | InstantiationException ex) {
                logger.log(Level.WARNING, "[ArenaService] FAWE fill fehlgeschlagen, fallback wird verwendet.", ex);
                super.fill(world, min, max, material);
            } finally {
                if (editSession != null) {
                    try {
                        closeMethod.invoke(editSession);
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
        }

        @Override
        public void fillReplacing(World world, Vector3i min, Vector3i max, Material material, Material required) {
            if (required == null) {
                fill(world, min, max, material);
                return;
            }
            super.fillReplacing(world, min, max, material, required);
        }
    }
}
