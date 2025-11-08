package de.opalium.luckysky.listeners;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameManager;
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
        GameManager game = plugin.game();
        boolean running = game.state() == GameState.RUNNING;
        boolean oneLife = game.oneLifeEnabled();
        boolean eliminated = game.isParticipant(player) && !game.isActiveParticipant(player);

        if (running) {
            game.platformSpawnLocation().ifPresent(event::setRespawnLocation);
            if (oneLife && eliminated) {
                Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(org.bukkit.GameMode.SPECTATOR));
                return;
            }
            game.handleRespawn(player);
            Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(org.bukkit.GameMode.SURVIVAL));
        } else {
            game.lobbySpawnLocation().ifPresent(event::setRespawnLocation);
            Bukkit.getScheduler().runTask(plugin, () -> {
                game.teleportPlayerToLobby(player);
                if (oneLife && eliminated) {
                    player.setGameMode(org.bukkit.GameMode.SPECTATOR);
                } else if (!oneLife) {
                    player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                }
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.game().handleQuit(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.game().handleJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.game().handleJoin(event.getPlayer());
    }
}
