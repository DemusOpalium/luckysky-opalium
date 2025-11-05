package de.opalium.luckysky.core;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.util.ConfigKeys;
import de.opalium.luckysky.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SessionManager {
    public enum Phase {
        IDLE,
        RUNNING
    }

    private final LuckySkyPlugin plugin;
    private final Messages messages;
    private Phase phase = Phase.IDLE;
    private int currentModeMinutes;
    private int remainingSeconds;
    private BukkitTask timerTask;

    public SessionManager(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
        int defaultMinutes = plugin.getConfigData().getInt(ConfigKeys.SESSION_DEFAULT_MODE, 20);
        if (!setModeMinutes(defaultMinutes)) {
            this.currentModeMinutes = defaultMinutes;
        }
    }

    public Phase getPhase() {
        return phase;
    }

    public int getCurrentModeMinutes() {
        return currentModeMinutes;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public boolean startSession() {
        if (phase == Phase.RUNNING) {
            return false;
        }
        Optional<World> optionalWorld = plugin.getGameWorld();
        if (optionalWorld.isEmpty()) {
            return false;
        }
        phase = Phase.RUNNING;
        remainingSeconds = currentModeMinutes * 60;
        plugin.getPlatformBuilder().buildBase();
        startTimer();
        return true;
    }

    public boolean stopSession() {
        if (phase == Phase.IDLE) {
            return false;
        }
        phase = Phase.IDLE;
        remainingSeconds = 0;
        cancelTimer();
        return true;
    }

    public boolean setModeMinutes(int minutes) {
        Set<Integer> allowed = new HashSet<>(plugin.getConfigData().getIntegerList(ConfigKeys.SESSION_ALLOWED_MODES));
        if (!allowed.isEmpty() && !allowed.contains(minutes)) {
            return false;
        }
        this.currentModeMinutes = minutes;
        if (phase == Phase.RUNNING) {
            remainingSeconds = minutes * 60;
        }
        return true;
    }

    public void shutdown() {
        cancelTimer();
    }

    private void startTimer() {
        cancelTimer();
        this.timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (phase != Phase.RUNNING) {
                cancelTimer();
                return;
            }
            if (remainingSeconds <= 0) {
                stopSession();
                Optional<World> optionalWorld = plugin.getGameWorld();
                optionalWorld.ifPresent(world -> {
                    String message = messages.format("session-stopped", null);
                    for (Player player : world.getPlayers()) {
                        player.sendMessage(message);
                    }
                });
                return;
            }
            remainingSeconds--;
        }, 20L, 20L);
    }

    private void cancelTimer() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
    }

    public Map<String, String> timerPlaceholder() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("minutes", String.valueOf(currentModeMinutes));
        placeholders.put("seconds", String.valueOf(remainingSeconds));
        return placeholders;
    }
}
