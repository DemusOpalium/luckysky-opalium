package de.opalium.luckysky.core;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.util.ConfigKeys;
import de.opalium.luckysky.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WitherService implements Listener {
    private final LuckySkyPlugin plugin;
    private final Messages messages;
    private UUID witherId;
    private BukkitTask tauntTask;
    private boolean tauntsEnabled;

    public WitherService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
        this.tauntsEnabled = plugin.getConfigData().getBoolean(ConfigKeys.WITHER_TAUNTS_ENABLED, true);
    }

    public boolean spawnWither() {
        if (!plugin.getConfigData().getBoolean(ConfigKeys.WITHER_ENABLED, true)) {
            return false;
        }
        if (isWitherAlive()) {
            return false;
        }
        Optional<World> optionalWorld = plugin.getGameWorld();
        if (optionalWorld.isEmpty()) {
            return false;
        }
        World world = optionalWorld.get();
        Location spawnLocation = readSpawnLocation(world);
        Wither wither = (Wither) world.spawnEntity(spawnLocation, EntityType.WITHER);
        this.witherId = wither.getUniqueId();
        if (tauntsEnabled) {
            startTaunts();
        }
        return true;
    }

    public boolean isWitherAlive() {
        return getWither().map(Entity::isValid).orElse(false);
    }

    public void despawnWither() {
        getWither().ifPresent(entity -> {
            entity.remove();
        });
        this.witherId = null;
        cancelTaunts();
    }

    public boolean setTauntsEnabled(boolean enabled) {
        this.tauntsEnabled = enabled;
        if (!enabled) {
            cancelTaunts();
            return true;
        }
        if (isWitherAlive()) {
            startTaunts();
            return true;
        }
        return false;
    }

    public boolean isTauntsEnabled() {
        return tauntsEnabled;
    }

    public void shutdown() {
        cancelTaunts();
        this.witherId = null;
    }

    @EventHandler
    public void onWitherDeath(EntityDeathEvent event) {
        if (event.getEntity().getType() == EntityType.WITHER && witherId != null && event.getEntity().getUniqueId().equals(witherId)) {
            witherId = null;
            cancelTaunts();
        }
    }

    private void startTaunts() {
        cancelTaunts();
        int intervalSeconds = plugin.getConfigData().getInt(ConfigKeys.WITHER_TAUNTS_INTERVAL, 20);
        this.tauntTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isWitherAlive()) {
                cancelTaunts();
                return;
            }
            Optional<World> optionalWorld = plugin.getGameWorld();
            if (optionalWorld.isEmpty()) {
                return;
            }
            World world = optionalWorld.get();
            List<Player> players = world.getPlayers();
            if (players.isEmpty()) {
                return;
            }
            String title = messages.format("wither-taunt-title", null);
            String subtitle = messages.format("wither-taunt-subtitle", null);
            String actionbar = messages.format("wither-taunt-actionbar", null);
            boolean useTitle = plugin.getConfigData().getBoolean(ConfigKeys.WITHER_TAUNTS_TITLES, true);
            boolean useActionbar = plugin.getConfigData().getBoolean(ConfigKeys.WITHER_TAUNTS_ACTIONBAR, true);
            boolean useSounds = plugin.getConfigData().getBoolean(ConfigKeys.WITHER_TAUNTS_SOUNDS, true);
            for (Player player : players) {
                if (useTitle) {
                    player.sendTitle(title, subtitle, 5, 40, 5);
                }
                if (useActionbar) {
                    player.sendActionBar(actionbar);
                }
                if (useSounds) {
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 1.0f);
                }
            }
        }, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    private void cancelTaunts() {
        if (tauntTask != null) {
            tauntTask.cancel();
            tauntTask = null;
        }
    }

    private Optional<Wither> getWither() {
        if (witherId == null) {
            return Optional.empty();
        }
        Entity entity = Bukkit.getEntity(witherId);
        if (entity instanceof Wither wither) {
            return Optional.of(wither);
        }
        return Optional.empty();
    }

    private Location readSpawnLocation(World world) {
        ConfigurationSection section = plugin.getConfigData().getConfigurationSection(ConfigKeys.WITHER_SPAWN);
        double x = section != null ? section.getDouble("x", 0) : 0;
        double y = section != null ? section.getDouble("y", world.getHighestBlockYAt(world.getSpawnLocation())) : world.getHighestBlockYAt(world.getSpawnLocation());
        double z = section != null ? section.getDouble("z", 0) : 0;
        return new Location(world, x, y, z);
    }
}
