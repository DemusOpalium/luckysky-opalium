package de.opalium.luckysky.npc;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.NpcConfig;
import de.opalium.luckysky.config.NpcConfig.Area;
import de.opalium.luckysky.config.NpcConfig.BlockVector;
import de.opalium.luckysky.config.NpcConfig.NpcEntry;
import de.opalium.luckysky.config.NpcConfig.Parking;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;

final class NpcCollapseTask extends BukkitRunnable {
    private final LuckySkyPlugin plugin;
    private final String id;
    private final NpcEntry entry;
    private final CitizensBridge.NpcHandle npc;
    private final World world;
    private final Location originalLocation;
    private final Runnable onFinish;
    private int phase = 0;
    private boolean completionHandled = false;
    private boolean finishScheduled = false;

    NpcCollapseTask(LuckySkyPlugin plugin, String id, NpcEntry entry, CitizensBridge.NpcHandle npc, World world,
                    Location originalLocation, Runnable onFinish) {
        this.plugin = plugin;
        this.id = id;
        this.entry = entry;
        this.npc = npc;
        this.world = world;
        this.originalLocation = originalLocation;
        this.onFinish = onFinish;
    }

    void start() {
        long delay = Math.max(1L, entry.collapseScript().phaseDelayTicks());
        runTaskTimer(plugin, 0L, delay);
    }

    void cancelTask() {
        cancel();
    }

    @Override
    public void run() {
        if (entry.collapseScript().type() == NpcConfig.CollapseScriptType.FAWE
                && entry.collapseScript().fawe().isPresent()) {
            plugin.getLogger().warning("FAWE collapse für NPC '" + id + "' ist noch nicht implementiert.");
            handleCompletion();
            cancel();
            return;
        }
        switch (phase) {
            case 0 -> removeSupports();
            case 1 -> removeWalls();
            case 2 -> removeRoof();
            default -> {
                handleCompletion();
                cancel();
                return;
            }
        }
        lowerNpc(entry.collapseScript().lowerPerPhase());
        phase++;
    }

    private void removeSupports() {
        Area area = entry.house();
        BlockVector min = area.orderedMin();
        BlockVector max = area.orderedMax();
        clearColumn(min.x(), min.z(), min.y(), max.y());
        clearColumn(min.x(), max.z(), min.y(), max.y());
        clearColumn(max.x(), min.z(), min.y(), max.y());
        clearColumn(max.x(), max.z(), min.y(), max.y());
    }

    private void clearColumn(int x, int z, int yMin, int yMax) {
        for (int y = yMin; y <= yMax; y++) {
            setToAir(x, y, z);
        }
    }

    private void removeWalls() {
        Area area = entry.house();
        BlockVector min = area.orderedMin();
        BlockVector max = area.orderedMax();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                setToAir(x, y, min.z());
                setToAir(x, y, max.z());
            }
        }
        for (int z = min.z(); z <= max.z(); z++) {
            for (int y = min.y(); y <= max.y(); y++) {
                setToAir(min.x(), y, z);
                setToAir(max.x(), y, z);
            }
        }
    }

    private void removeRoof() {
        Area area = entry.house();
        BlockVector min = area.orderedMin();
        BlockVector max = area.orderedMax();
        int y = max.y();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int z = min.z(); z <= max.z(); z++) {
                setToAir(x, y, z);
            }
        }
    }

    private void setToAir(int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != Material.AIR) {
            block.setType(Material.AIR, false);
        }
    }

    private void lowerNpc(double amount) {
        if (!npc.isSpawned()) {
            trySpawn();
            return;
        }
        Location current = npc.entity() != null ? npc.entity().getLocation().clone() : null;
        if (current == null) {
            return;
        }
        current.subtract(0.0D, amount, 0.0D);
        npc.teleport(current, TeleportCause.PLUGIN);
    }

    private void trySpawn() {
        if (!npc.isSpawned()) {
            npc.spawn(originalLocation);
        }
    }

    private void handleCompletion() {
        if (completionHandled) {
            return;
        }
        completionHandled = true;
        if (npc.isSpawned() && entry.collapseScript().despawnAfter()) {
            npc.despawn();
        }
        if (entry.collapseScript().recall()) {
            long delay = Math.max(0L, entry.collapseScript().recallDelayTicks());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!npc.isSpawned()) {
                    npc.spawn(originalLocation);
                } else {
                    npc.teleport(originalLocation, TeleportCause.PLUGIN);
                }
                entry.parking().ifPresent(parking -> scheduleParkingMove(parking));
            }, delay);
        } else {
            entry.parking().ifPresent(this::scheduleParkingMove);
        }
    }

    private void scheduleParkingMove(Parking parking) {
        long delay = Math.max(0L, parking.delayTicks());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!npc.isSpawned()) {
                return;
            }
            World targetWorld = Bukkit.getWorld(parking.world());
            if (targetWorld == null) {
                return;
            }
            Location location = new Location(targetWorld, parking.x(), parking.y(), parking.z(), parking.yaw(), parking.pitch());
            npc.teleport(location, TeleportCause.PLUGIN);
        }, delay);
    }

    private void finishOnce() {
        if (finishScheduled) {
            return;
        }
        finishScheduled = true;
        Bukkit.getScheduler().runTask(plugin, onFinish);
    }

    @Override
    public synchronized void cancel() throws IllegalStateException {
        super.cancel();
        finishOnce();
    }
}
