package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
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
        GameConfig game = plugin.configs().game();
        int interval = Math.max(1, game.lucky().intervalTicks());
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
        GameConfig game = plugin.configs().game();
        GameConfig.Position position = game.lucky().position();
        World world = Worlds.require(plugin.configs().worlds().luckySky().worldName());
        Location location = new Location(world, position.x(), position.y(), position.z());
        if (game.lucky().requireAirAtTarget() && location.getBlock().getType() != Material.AIR) {
            return;
        }
        String command = String.format("ntdluckyblock place %s %d %d %d %s -s",
                plugin.configs().worlds().luckySky().worldName(), position.x(), position.y(), position.z(),
                game.lucky().variant());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
