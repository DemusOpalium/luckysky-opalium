package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.util.Msg;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

public class CountdownService {
    private static final int MIN_TICKS = 20;

    private final LuckySkyPlugin plugin;
    private final ScoreboardService scoreboardService;

    private boolean bossbarEnabled;
    private String bossbarTitle;
    private boolean actionbarEnabled;
    private String actionbarFormat;

    private BossBar bossBar;
    private int taskId = -1;
    private int ticksRemaining;
    private int totalTicks;

    private int lastAnnouncedSecond = -1;

    public CountdownService(LuckySkyPlugin plugin, ScoreboardService scoreboardService) {
        this.plugin = plugin;
        this.scoreboardService = scoreboardService;
        reload();
    }

    public void reload() {
        GameConfig.Durations durations = plugin.configs().game().durations();
        this.bossbarEnabled = durations.bossbarEnabled();
        this.bossbarTitle = durations.bossbarTitle();
        this.actionbarEnabled = durations.actionbarEnabled();
        this.actionbarFormat = durations.actionbarFormat();
        if (taskId != -1) {
            rebuildBossBar();
        }
    }

    public void startDefault() {
        startMinutes(plugin.configs().game().durations().minutesDefault());
    }

    public void startMinutes(int minutes) {
        startTicks(minutes * 60 * 20);
    }

    public void startTicks(int ticks) {
        stop();
        totalTicks = Math.max(MIN_TICKS, ticks);
        ticksRemaining = totalTicks;
        lastAnnouncedSecond = totalTicks / 20;
        if (scoreboardService != null) {
            scoreboardService.onTimerStart(ticksRemaining);
        }
        rebuildBossBar();
        if (bossBar != null) {
            bossBar.setProgress(1.0);
            bossBar.setVisible(true);
            Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
        }
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        ticksRemaining = 0;
        if (scoreboardService != null) {
            scoreboardService.onTimerStop();
        }
        if (bossBar != null) {
            bossBar.setVisible(false);
            bossBar.removeAll();
        }
    }

    public List<Integer> presets() {
        return plugin.configs().game().durations().presets();
    }

    public boolean isRunning() {
        return taskId != -1;
    }

    private void tick() {
        if (ticksRemaining <= 0) {
            return;
        }
        ticksRemaining--;
        updateBossBar();
        updateActionbar();
        if (ticksRemaining % 20 == 0 || ticksRemaining < 20) {
            if (scoreboardService != null) {
                scoreboardService.onTimerTick(Math.max(ticksRemaining, 0));
            }
        }
        if (ticksRemaining <= 0) {
            finishCountdown();
        }
    }

    private void finishCountdown() {
        stop();
        Bukkit.getScheduler().runTask(plugin, () -> plugin.game().onDurationExpired());
    }

    private void rebuildBossBar() {
        if (!bossbarEnabled) {
            if (bossBar != null) {
                bossBar.removeAll();
                bossBar.setVisible(false);
                bossBar = null;
            }
            return;
        }
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(Msg.color(bossbarTitle), BarColor.PURPLE, BarStyle.SOLID);
        } else {
            bossBar.setTitle(Msg.color(bossbarTitle));
        }
        if (taskId != -1) {
            bossBar.setVisible(true);
            Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
        }
    }

    private void updateBossBar() {
        if (bossBar == null || totalTicks <= 0) {
            return;
        }
        double progress = Math.max(0.0, Math.min(1.0, ticksRemaining / (double) totalTicks));
        bossBar.setProgress(progress);
    }

    private void updateActionbar() {
        if (!actionbarEnabled) {
            return;
        }
        int currentSecond = Math.max(0, ticksRemaining / 20);
        if (currentSecond == lastAnnouncedSecond && ticksRemaining % 20 != 0) {
            return;
        }
        lastAnnouncedSecond = currentSecond;
        String formatted = actionbarFormat.replace("{time}", formatTime(ticksRemaining));
        String colored = Msg.color(formatted);
        BaseComponent[] components = TextComponent.fromLegacyText(colored);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
        }
    }

    private String formatTime(int ticks) {
        int totalSeconds = Math.max(0, ticks / 20);
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }
}
