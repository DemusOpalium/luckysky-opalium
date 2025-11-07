package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.model.GameConfig;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class RewardsService {
    private final LuckySkyPlugin plugin;

    public RewardsService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void triggerWin(Player killer, Collection<UUID> aliveParticipants) {
        GameConfig.RewardConfig rewards = plugin.configs().game().rewards();
        List<String> commands = rewards.onBossKill();
        if (commands.isEmpty()) {
            return;
        }
        if ("killer".equalsIgnoreCase(rewards.mode()) && killer != null) {
            executeCommands(commands, killer.getName());
        } else {
            executeForParticipants(commands, aliveParticipants);
        }
    }

    public void triggerFail(Collection<UUID> allParticipants) {
        List<String> commands = plugin.configs().game().rewards().onFail();
        if (commands.isEmpty()) {
            return;
        }
        executeForParticipants(commands, allParticipants);
    }

    private void executeForParticipants(List<String> commands, Collection<UUID> participants) {
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
        for (String command : commands) {
            String parsed = command.replace("%player%", playerName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }
}
