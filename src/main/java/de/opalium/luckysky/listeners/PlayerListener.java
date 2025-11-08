package de.opalium.luckysky.listeners;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {
    private final LuckySkyPlugin plugin;

    public PlayerListener(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.game().state() != GameState.RUNNING) {
            return;
        }
        Player player = event.getEntity();
        if (!plugin.game().isParticipant(player)) {
            return;
        }
        plugin.game().handleParticipantDeath(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (plugin.game().oneLifeEnabled()) {
            if (plugin.game().isParticipant(player) && !plugin.game().isActiveParticipant(player)) {
                Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(org.bukkit.GameMode.SPECTATOR));
            }
        } else {
            plugin.game().handleRespawn(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.game().handleQuit(event.getPlayer());
        plugin.scoreboard().refresh();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.game().handleJoin(event.getPlayer());
        plugin.scoreboard().refresh();
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.game().handleJoin(event.getPlayer());
        plugin.scoreboard().refresh();
    }
}
