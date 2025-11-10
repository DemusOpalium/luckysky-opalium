package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.util.Msg;
import org.bukkit.Bukkit;

public class DurationService {
    private final LuckySkyPlugin plugin;
    private int ticksRemaining;
    private int taskId = -1;
    private final ScoreboardService scoreboardService;

    public DurationService(LuckySkyPlugin plugin, ScoreboardService scoreboardService) {
        this.plugin = plugin;
        this.scoreboardService = scoreboardService;
    }

    public void startDefault() {
        startMinutes(plugin.configs().game().durations().minutesDefault());
    }

    public void startMinutes(int minutes) {
        startTicks(minutes * 60 * 20);
    }

    private void startTicks(int ticks) {
        stop();
        ticksRemaining = Math.max(ticks, 20);
        if (scoreboardService != null) {
            scoreboardService.onTimerStart(ticksRemaining);
        }
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (plugin.game().state() != GameState.RUNNING) {
                return;
            }
            ticksRemaining -= 20;
            if (scoreboardService != null) {
                scoreboardService.onTimerTick(Math.max(ticksRemaining, 0));
            }
            if (ticksRemaining <= 0) {
                boolean stopped = plugin.game().onDurationExpired();
                if (stopped) {
                    Msg.to(Bukkit.getConsoleSender(), "&eZeit abgelaufen – Spiel gestoppt.");
                }
                stop();
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (scoreboardService != null) {
            scoreboardService.onTimerStop();
        }
    }

    public void reload() {
        if (taskId != -1) {
            startTicks(ticksRemaining);
        } else if (scoreboardService != null) {
            scoreboardService.onTimerStop();
        }
    }
}
