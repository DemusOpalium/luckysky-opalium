package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
import de.opalium.luckysky.util.Msg;
import org.bukkit.Bukkit;

public class DurationService {
    private final LuckySkyPlugin plugin;
    private int ticksRemaining;
    private int taskId = -1;

    public DurationService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void startDefault() {
        startMinutes(plugin.settings().minutesDefault);
    }

    public void startMinutes(int minutes) {
        startTicks(minutes * 60 * 20);
    }

    private void startTicks(int ticks) {
        stop();
        ticksRemaining = Math.max(ticks, 20);
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (plugin.game().state() != GameState.RUNNING) {
                return;
            }
            ticksRemaining -= 20;
            if (ticksRemaining <= 0) {
                plugin.game().onDurationExpired();
                Msg.to(Bukkit.getConsoleSender(), "&eZeit abgelaufen – Spiel gestoppt.");
                stop();
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void reload() {
        if (taskId != -1) {
            startTicks(ticksRemaining);
        }
    }
}
