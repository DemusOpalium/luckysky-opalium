package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.config.WorldsConfig.BaseLayer;
import de.opalium.luckysky.config.WorldsConfig.Reset;
import de.opalium.luckysky.config.WorldsConfig.ResetArea;
import de.opalium.luckysky.util.Worlds;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitTask;

public class BlockResetService {
    private static final int CHUNKS_PER_TICK = 1;

    private final LuckySkyPlugin plugin;
    private BukkitTask currentTask;

    public BlockResetService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public ResetResult resetArea(Runnable onComplete) {
        if (currentTask != null) {
            return ResetResult.ALREADY_RUNNING;
        }
        WorldsConfig.LuckyWorld config = plugin.configs().worlds().luckySky();
        Reset reset = config.reset();
        if (reset == null || reset.area() == null) {
            return ResetResult.NO_CONFIGURATION;
        }
        ResetArea area = reset.area();
        int minX = Math.min(area.min().x(), area.max().x());
        int maxX = Math.max(area.min().x(), area.max().x());
        int minY = Math.min(area.min().y(), area.max().y());
        int maxY = Math.max(area.min().y(), area.max().y());
        int minZ = Math.min(area.min().z(), area.max().z());
        int maxZ = Math.max(area.min().z(), area.max().z());
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return ResetResult.NO_CONFIGURATION;
        }

        World world = Worlds.require(config.worldName());
        int worldMinY = world.getMinHeight();
        int worldMaxY = world.getMaxHeight() - 1;
        minY = Math.max(minY, worldMinY);
        maxY = Math.min(maxY, worldMaxY);
        if (minY > maxY) {
            return ResetResult.NO_CONFIGURATION;
        }

        Map<Integer, Material> baseLayers = resolveBaseLayers(reset.baseLayers());
        Runnable completion = () -> {
            currentTask = null;
            if (onComplete != null) {
                Bukkit.getScheduler().runTask(plugin, onComplete);
            }
        };

        ResetTask task = new ResetTask(world, minX, maxX, minY, maxY, minZ, maxZ, reset.removeLava(), baseLayers,
                completion);
        currentTask = task.start();
        return ResetResult.STARTED;
    }

    public boolean isRunning() {
        return currentTask != null;
    }

    public void shutdown() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }

    private Map<Integer, Material> resolveBaseLayers(List<BaseLayer> layers) {
        Map<Integer, Material> resolved = new HashMap<>();
        Logger logger = plugin.getLogger();
        for (BaseLayer layer : layers) {
            if (layer == null || layer.material() == null) {
                continue;
            }
            String materialName = layer.material();
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
            }
            if (material == null) {
                logger.warning("Unknown material in reset base layer: " + materialName);
                continue;
            }
            resolved.put(layer.y(), material);
        }
        return resolved;
    }

    public enum ResetResult {
        STARTED,
        ALREADY_RUNNING,
        NO_CONFIGURATION
    }

    private final class ResetTask implements Runnable {
        private final World world;
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;
        private final boolean removeLava;
        private final Map<Integer, Material> baseLayers;
        private final Runnable completion;
        private final Deque<ChunkCoordinate> chunks;
        private BukkitTask task;

        ResetTask(World world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
                boolean removeLava, Map<Integer, Material> baseLayers, Runnable completion) {
            this.world = world;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.removeLava = removeLava;
            this.baseLayers = baseLayers;
            this.completion = completion;
            this.chunks = buildChunkQueue();
        }

        private Deque<ChunkCoordinate> buildChunkQueue() {
            Deque<ChunkCoordinate> queue = new ArrayDeque<>();
            int minChunkX = Math.floorDiv(minX, 16);
            int maxChunkX = Math.floorDiv(maxX, 16);
            int minChunkZ = Math.floorDiv(minZ, 16);
            int maxChunkZ = Math.floorDiv(maxZ, 16);
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    queue.addLast(new ChunkCoordinate(chunkX, chunkZ));
                }
            }
            return queue;
        }

        BukkitTask start() {
            if (chunks.isEmpty()) {
                completion.run();
                return null;
            }
            this.task = Bukkit.getScheduler().runTaskTimer(plugin, this, 1L, 1L);
            return this.task;
        }

        @Override
        public void run() {
            int processed = 0;
            while (processed < CHUNKS_PER_TICK && !chunks.isEmpty()) {
                ChunkCoordinate coordinate = chunks.removeFirst();
                processChunk(coordinate.x(), coordinate.z());
                processed++;
            }
            if (chunks.isEmpty()) {
                if (task != null) {
                    task.cancel();
                }
                completion.run();
            }
        }

        private void processChunk(int chunkX, int chunkZ) {
            world.loadChunk(chunkX, chunkZ);
            int baseX = chunkX << 4;
            int baseZ = chunkZ << 4;
            int startX = Math.max(minX, baseX);
            int endX = Math.min(maxX, baseX + 15);
            int startZ = Math.max(minZ, baseZ);
            int endZ = Math.min(maxZ, baseZ + 15);
            for (int x = startX; x <= endX; x++) {
                for (int z = startZ; z <= endZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        Block block = world.getBlockAt(x, y, z);
                        Material target = baseLayers.get(y);
                        if (target != null) {
                            if (block.getType() != target) {
                                block.setType(target, false);
                            }
                            continue;
                        }
                        Material current = block.getType();
                        if (!removeLava && current == Material.LAVA) {
                            continue;
                        }
                        if (current != Material.AIR) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }
    }

    private record ChunkCoordinate(int x, int z) {
    }
}
