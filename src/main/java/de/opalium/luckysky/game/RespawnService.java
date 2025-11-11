package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handles all respawn related duties for LuckySky. Beds and respawn anchors are ignored – players
 * are always routed to the configured platform / lobby locations. In one-life mode the service also
 * puts eliminated players into spectator mode after their death.
 */
public class RespawnService {
    private final LuckySkyPlugin plugin;
    private final GameManager gameManager;

    public RespawnService(LuckySkyPlugin plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    public void handleDeath(PlayerDeathEvent event) {
        if (!gameManager.isParticipant(event.getEntity())) {
            return;
        }
        gameManager.handleParticipantDeath(event.getEntity());
        if (plugin.configs().game().lives().oneLife()) {
            Bukkit.getScheduler().runTask(plugin, () -> event.getEntity().setGameMode(GameMode.SPECTATOR));
        }
    }

    public void handleRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameState state = gameManager.state();
        boolean oneLife = gameManager.oneLifeEnabled();
        boolean eliminated = gameManager.isParticipant(player) && !gameManager.isActiveParticipant(player);

        if (state == GameState.COUNTDOWN || state == GameState.RUN) {
            Optional<Location> respawn = gameManager.platformSpawnLocation();
            respawn.ifPresent(event::setRespawnLocation);
            if (oneLife && eliminated) {
                Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
                return;
            }
            gameManager.handleRespawn(player);
            Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SURVIVAL));
            respawn.ifPresent(location -> Bukkit.getScheduler().runTask(plugin, () -> player.teleport(location)));
        } else {
            Optional<Location> lobby = gameManager.lobbySpawnLocation();
            lobby.ifPresent(event::setRespawnLocation);
            Bukkit.getScheduler().runTask(plugin, () -> {
                lobby.ifPresent(player::teleport);
                if (oneLife && eliminated) {
                    player.setGameMode(GameMode.SPECTATOR);
                } else if (!oneLife) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
            });
        }
    }
}
