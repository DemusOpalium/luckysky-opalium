package de.opalium.luckysky.duels;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.DuelsConfig;
import de.opalium.luckysky.config.DuelsConfig.Arena;
import de.opalium.luckysky.config.DuelsConfig.FloorPreset;
import de.opalium.luckysky.config.DuelsConfig.Hazards;
import de.opalium.luckysky.config.DuelsConfig.LightRaster;
import de.opalium.luckysky.config.DuelsConfig.Region;
import de.opalium.luckysky.config.DuelsConfig.ResetPreset;
import de.opalium.luckysky.config.DuelsConfig.ResetStep;
import de.opalium.luckysky.config.DuelsConfig.Vector3i;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

public class ArenaService {
    private final LuckySkyPlugin plugin;

    public ArenaService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
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

    public boolean applyHazardPreset(String presetId, CommandSender sender) {
        Arena arena = activeArena();
        if (arena == null) {
            Msg.to(sender, "&cKeine aktive Arena ausgewählt.");
            return false;
        }
        Hazards hazards = arena.hazards();
        World world = Worlds.require(arena.world());
        if (presetId.equalsIgnoreCase("clear")) {
            Region region = new Region(new Vector3i(arena.bounds().min().x(), hazards.layerY(), arena.bounds().min().z()),
                    new Vector3i(arena.bounds().max().x(), hazards.layerY(), arena.bounds().max().z()));
            fillRegion(world, region.min(), region.max(), Material.AIR, null);
            Msg.to(sender, "&aAlle Lava-Presets entfernt.");
            return true;
        }
        Region region = hazards.presets().get(presetId);
        if (region == null) {
            Msg.to(sender, "&cUnbekanntes Lava-Preset: &f" + presetId);
            return false;
        }
        Vector3i min = new Vector3i(region.min().x(), hazards.layerY(), region.min().z());
        Vector3i max = new Vector3i(region.max().x(), hazards.layerY(), region.max().z());
        fillRegion(world, min, max, Material.LAVA, null);
        Msg.to(sender, "&aLava-Preset &f" + presetId + " &aaktiviert.");
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
            for (ResetStep step : steps) {
                applyStep(world, arena, step);
            }
            plugin.getLogger().info("[ArenaService] " + context + " abgeschlossen für " + arena.id());
        });
    }

    private void applyStep(World world, Arena arena, ResetStep step) {
        String type = step.type().toLowerCase(Locale.ROOT);
        Map<String, Object> params = new LinkedHashMap<>(step.parameters());
        switch (type) {
            case "fill" -> {
                Vector3i from = parseVector(params.get("from"), arena.bounds().min());
                Vector3i to = parseVector(params.get("to"), arena.bounds().max());
                Material material = parseMaterial(step.material()).orElse(Material.AIR);
                fillRegion(world, from, to, material, null);
            }
            case "fill_replace" -> {
                Vector3i from = parseVector(params.get("from"), arena.bounds().min());
                Vector3i to = parseVector(params.get("to"), arena.bounds().max());
                Material find = parseMaterial(step.find()).orElse(Material.AIR);
                Material with = parseMaterial(step.with()).orElse(Material.AIR);
                fillRegion(world, from, to, with, find);
            }
            case "slice_fill" -> applySliceFill(world, arena, params, step.material());
            case "light_raster" -> {
                boolean enabled = Boolean.parseBoolean(String.valueOf(params.getOrDefault("enabled", "true")));
                applyLightRaster(world, arena, enabled);
            }
            case "clone" -> applyClone(world, params);
            default -> plugin.getLogger().warning("[ArenaService] Unbekannter Schritt: " + type);
        }
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
        if (min == null || max == null) {
            return;
        }
        int minX = Math.min(min.x(), max.x());
        int maxX = Math.max(min.x(), max.x());
        int minY = Math.min(min.y(), max.y());
        int maxY = Math.max(min.y(), max.y());
        int minZ = Math.min(min.z(), max.z());
        int maxZ = Math.max(min.z(), max.z());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (required != null && block.getType() != required) {
                        continue;
                    }
                    block.setType(material, false);
                }
            }
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
}
