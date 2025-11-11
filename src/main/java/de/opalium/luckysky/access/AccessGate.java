package de.opalium.luckysky.access;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameState;
import de.opalium.luckysky.game.StateMachine;
import de.opalium.luckysky.util.Msg;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Ensures that only whitelisted players may enter the LuckySky world when the game is in an
 * appropriate state. The gate reacts to teleports as well as fresh joins and reroutes the players
 * back to the configured lobby spawn when they are not allowed to enter.
 */
public class AccessGate implements Listener {
    private static final Set<GameState> OPEN_STATES = EnumSet.of(GameState.LOBBY, GameState.COUNTDOWN, GameState.RUN);
    private static final String BYPASS_PERMISSION = "opalium.luckysky.access.bypass";

    private final LuckySkyPlugin plugin;
    private final StateMachine stateMachine;

    public AccessGate(LuckySkyPlugin plugin, StateMachine stateMachine) {
        this.plugin = plugin;
        this.stateMachine = stateMachine;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (!isTargetLuckyWorld(to.getWorld())) {
            return;
        }
        Player player = event.getPlayer();
        if (isAllowed(player)) {
            return;
        }
        event.setCancelled(true);
        deny(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        handlePostTeleport(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> handlePostTeleport(event.getPlayer()));
    }

    private void handlePostTeleport(Player player) {
        if (isAllowed(player)) {
            return;
        }
        if (!isTargetLuckyWorld(player.getWorld())) {
            return;
        }
        teleportToLobby(player);
        deny(player);
    }

    private boolean isAllowed(Player player) {
        if (player.hasPermission(BYPASS_PERMISSION) || player.hasPermission("opalium.luckysky.admin")) {
            return true;
        }
        GameState state = stateMachine.state();
        if (!OPEN_STATES.contains(state)) {
            return false;
        }
        if (state == GameState.LOBBY) {
            return true;
        }
        UUID id = player.getUniqueId();
        return stateMachine.isWhitelisted(id);
    }

    private void deny(Player player) {
        Msg.to(player, plugin.configs().messages().gamePrefix() + "&cDu kannst LuckySky gerade nicht betreten.");
    }

    private void teleportToLobby(Player player) {
        plugin.game().lobbySpawnLocation().ifPresent(player::teleport);
    }

    private boolean isTargetLuckyWorld(World world) {
        if (world == null) {
            return false;
        }
        String luckyName = plugin.configs().worlds().luckySky().worldName();
        return world.getName().equalsIgnoreCase(luckyName);
    }
}
