package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.model.WorldsCfg;
import de.opalium.luckysky.util.Worlds;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class LuckyService {
    private final LuckySkyPlugin plugin;
    private int taskId = -1;

    public LuckyService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        WorldsCfg.Lucky lucky = plugin.configs().worlds().primary().lucky();
        int interval = Math.max(1, lucky.intervalTicks());
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick,
                interval, interval);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void reload() {
        if (taskId != -1) {
            start();
        }
    }

    private void tick() {
        if (plugin.game().state() != GameState.RUNNING) {
            return;
        }
        WorldsCfg.WorldCfg worldCfg = plugin.configs().worlds().primary();
        WorldsCfg.Lucky lucky = worldCfg.lucky();
        World world = Worlds.require(worldCfg.worldName());
        Location location = new Location(world, lucky.x(), lucky.y(), lucky.z());
        if (lucky.requireAirAtTarget() && location.getBlock().getType() != Material.AIR) {
            return;
        }
        String command = String.format("ntdluckyblock place %s %d %d %d %s -s",
                worldCfg.worldName(), lucky.x(), lucky.y(), lucky.z(), lucky.variant());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
