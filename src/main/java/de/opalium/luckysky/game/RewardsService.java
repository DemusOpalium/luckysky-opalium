package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
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
        Settings settings = plugin.settings();
        List<String> commands = settings.rewardsBossCommands();
        if (commands.isEmpty()) {
            return;
        }
        if ("killer".equalsIgnoreCase(settings.rewardMode()) && killer != null) {
            executeCommands(commands, killer.getName());
        } else {
            executeForParticipants(commands, aliveParticipants);
        }
    }

    public void triggerFail(Collection<UUID> allParticipants) {
        Settings settings = plugin.settings();
        List<String> commands = settings.rewardsFailCommands();
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
