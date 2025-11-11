package de.opalium.luckysky.listeners;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.game.GameState;
import de.opalium.luckysky.gui.PlayerGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class PlayerListener implements Listener {
    private final LuckySkyPlugin plugin;

    public PlayerListener(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        GameManager game = plugin.game();
        if (game == null) {
            return;
        }
        GameState state = game.state();
        if (state != GameState.COUNTDOWN && state != GameState.RUN) {
            return;
        }
        game.respawn().handleDeath(event);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        GameManager game = plugin.game();
        if (game == null) {
            return;
        }
        game.respawn().handleRespawn(event);
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

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!event.getRightClicked().hasMetadata("NPC")) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("luckysky.gui.players")) {
            return;
        }
        PlayerGui gui = plugin.playerGui();
        if (gui == null) {
            return;
        }
        event.setCancelled(true);
        gui.open(player);
    }
}
