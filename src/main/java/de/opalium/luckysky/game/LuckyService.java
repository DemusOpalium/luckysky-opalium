package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
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
        Settings settings = plugin.settings();
        int interval = Math.max(1, settings.luckyInterval);
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
        Settings settings = plugin.settings();
        World world = Worlds.require(settings.world);
        Location location = new Location(world, settings.luckyX, settings.luckyY, settings.luckyZ);
        if (settings.luckyRequireAir && location.getBlock().getType() != Material.AIR) {
            return;
        }
        String command = String.format("ntdluckyblock place %s %d %d %d %s -s",
                settings.world, settings.luckyX, settings.luckyY, settings.luckyZ, settings.luckyVariant);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
