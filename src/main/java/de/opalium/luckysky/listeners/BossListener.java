package de.opalium.luckysky.listeners;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameState;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class BossListener implements Listener {
    private final LuckySkyPlugin plugin;

    public BossListener(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntityType() != EntityType.WITHER) {
            return;
        }
        GameState state = plugin.game().state();
        if (state != GameState.COUNTDOWN && state != GameState.RUN) {
            return;
        }
        plugin.game().handleWitherKill(event.getEntity().getKiller());
    }
}
