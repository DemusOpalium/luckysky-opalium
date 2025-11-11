package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class RewardService {
    private static final int END_TIMER_SECONDS = 60;

    private final LuckySkyPlugin plugin;
    private final GameManager gameManager;
    private final StateMachine stateMachine;
    private final ScoreboardService scoreboardService;

    private int endTaskId = -1;
    private int endTicksRemaining;

    public RewardService(LuckySkyPlugin plugin, GameManager gameManager, StateMachine stateMachine,
            ScoreboardService scoreboardService) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.stateMachine = stateMachine;
        this.scoreboardService = scoreboardService;
    }

    public void triggerWin(Player killer, Collection<UUID> aliveParticipants) {
        GameConfig.Rewards rewards = plugin.configs().game().rewards();
        executeCommands(rewards.onBossKill(), rewards.mode(), killer, aliveParticipants);
        startEndTimer();
    }

    public void triggerFail(Collection<UUID> allParticipants) {
        GameConfig.Rewards rewards = plugin.configs().game().rewards();
        executeForParticipants(rewards.onFail(), allParticipants);
        startEndTimer();
    }

    public void cancelEndTimer() {
        if (endTaskId != -1) {
            Bukkit.getScheduler().cancelTask(endTaskId);
            endTaskId = -1;
        }
        if (scoreboardService != null) {
            scoreboardService.onTimerStop();
        }
    }

    private void startEndTimer() {
        cancelEndTimer();
        stateMachine.setState(GameState.ENDING);
        endTicksRemaining = END_TIMER_SECONDS * 20;
        if (scoreboardService != null) {
            scoreboardService.onTimerStart(endTicksRemaining);
        }
        endTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            endTicksRemaining -= 20;
            if (scoreboardService != null) {
                scoreboardService.onTimerTick(Math.max(endTicksRemaining, 0));
            }
            if (endTicksRemaining <= 0) {
                completeEndTimer();
            }
        }, 20L, 20L);
    }

    private void completeEndTimer() {
        cancelEndTimer();
        Bukkit.getScheduler().runTask(plugin, () -> {
            gameManager.stop();
            stateMachine.setState(GameState.LOBBY);
        });
    }

    private void executeCommands(List<String> commands, String mode, Player killer,
            Collection<UUID> aliveParticipants) {
        if (commands.isEmpty()) {
            return;
        }
        if ("killer".equalsIgnoreCase(mode) && killer != null) {
            executeCommands(commands, killer.getName());
        } else {
            executeForParticipants(commands, aliveParticipants);
        }
    }

    private void executeForParticipants(List<String> commands, Collection<UUID> participants) {
        if (commands.isEmpty() || participants == null) {
            return;
        }
        for (UUID uuid : participants) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            String name = offline.getName();
            if (name == null) {
                continue;
            }
            executeCommands(commands, name);
        }
    }

    private void executeCommands(List<String> commands, String playerName) {
        if (playerName == null) {
            return;
        }
        for (String command : commands) {
            String parsed = command.replace("%player%", playerName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }
}
