package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.model.GameConfig;
import de.opalium.luckysky.config.model.WorldsCfg;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;

public class WitherService {
    private final LuckySkyPlugin plugin;
    private int spawnTimer = -1;
    private int tauntTimer = -1;
    private boolean witherEnabled;
    private boolean tauntsEnabled;

    public WitherService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        GameConfig.WitherConfig withers = plugin.configs().game().withers();
        this.witherEnabled = withers.enabled();
        this.tauntsEnabled = withers.taunts().enabled();
    }

    public void start() {
        stop();
        GameConfig.WitherConfig withers = plugin.configs().game().withers();
        witherEnabled = withers.enabled();
        tauntsEnabled = withers.taunts().enabled();
        if (!witherEnabled) {
            return;
        }
        spawnTimer = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::spawn,
                withers.spawnAfterMinutes() * 60L * 20L);
        if (tauntsEnabled) {
            tauntTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::taunt,
                    withers.taunts().everyTicks(), withers.taunts().everyTicks());
        }
    }

    public void stop() {
        if (spawnTimer != -1) {
            Bukkit.getScheduler().cancelTask(spawnTimer);
        }
        if (tauntTimer != -1) {
            Bukkit.getScheduler().cancelTask(tauntTimer);
        }
        spawnTimer = -1;
        tauntTimer = -1;
    }

    public void reload() {
        if (plugin.game().state() == GameState.RUNNING) {
            start();
        } else {
            GameConfig.WitherConfig withers = plugin.configs().game().withers();
            witherEnabled = withers.enabled();
            tauntsEnabled = withers.taunts().enabled();
        }
    }

    public void spawnNow() {
        stop();
        spawn();
        if (tauntsEnabled && plugin.game().state() == GameState.RUNNING) {
            setTauntsEnabled(true);
        }
    }

    public void setWitherEnabled(boolean enabled) {
        this.witherEnabled = enabled;
        if (!enabled) {
            stop();
        } else if (plugin.game().state() == GameState.RUNNING) {
            start();
        }
    }

    public void setTauntsEnabled(boolean enabled) {
        this.tauntsEnabled = enabled;
        if (!enabled && tauntTimer != -1) {
            Bukkit.getScheduler().cancelTask(tauntTimer);
            tauntTimer = -1;
        } else if (enabled && plugin.game().state() == GameState.RUNNING) {
            if (tauntTimer != -1) {
                Bukkit.getScheduler().cancelTask(tauntTimer);
            }
            tauntTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::taunt,
                    plugin.configs().game().withers().taunts().everyTicks(),
                    plugin.configs().game().withers().taunts().everyTicks());
        }
    }

    private void spawn() {
        if (!witherEnabled || plugin.game().state() != GameState.RUNNING) {
            return;
        }
        WorldsCfg.WorldCfg worldCfg = plugin.configs().worlds().primary();
        WorldsCfg.Lucky lucky = worldCfg.lucky();
        World world = Worlds.require(worldCfg.worldName());
        Location location = new Location(world, lucky.x(), lucky.y(), lucky.z() - 6);
        Wither wither = (Wither) world.spawnEntity(location, EntityType.WITHER);
        wither.setCustomNameVisible(true);
        wither.customName(Component.text("Abyssal Wither", NamedTextColor.DARK_PURPLE));
        Bukkit.broadcastMessage(Msg.color(plugin.configs().messages().prefix() + "&c☠ Abyssal Wither ist erwacht!"));
    }

    private void taunt() {
        if (!tauntsEnabled || plugin.game().state() != GameState.RUNNING) {
            return;
        }
        WorldsCfg.WorldCfg worldCfg = plugin.configs().worlds().primary();
        World world = Worlds.require(worldCfg.worldName());
        if (world.getEntitiesByClass(Wither.class).isEmpty()) {
            return;
        }
        java.util.List<String> lines = plugin.configs().game().withers().taunts().lines();
        if (lines.isEmpty()) {
            return;
        }
        String line = lines.get((int) (Math.random() * lines.size()));
        Bukkit.broadcastMessage(Msg.color(plugin.configs().messages().prefix() + "&c" + line));
    }
}
