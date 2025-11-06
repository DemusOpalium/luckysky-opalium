package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
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
        Settings settings = plugin.settings();
        this.witherEnabled = settings.witherEnable;
        this.tauntsEnabled = settings.tauntEnable;
    }

    public void start() {
        stop();
        Settings settings = plugin.settings();
        witherEnabled = settings.witherEnable;
        tauntsEnabled = settings.tauntEnable;
        if (!witherEnabled) {
            return;
        }
        spawnTimer = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::spawn,
                settings.witherAfterMinutes * 60L * 20L);
        if (tauntsEnabled) {
            tauntTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::taunt,
                    settings.tauntEveryTicks, settings.tauntEveryTicks);
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
            Settings settings = plugin.settings();
            witherEnabled = settings.witherEnable;
            tauntsEnabled = settings.tauntEnable;
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
            Settings settings = plugin.settings();
            if (tauntTimer != -1) {
                Bukkit.getScheduler().cancelTask(tauntTimer);
            }
            tauntTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::taunt,
                    settings.tauntEveryTicks, settings.tauntEveryTicks);
        }
    }

    private void spawn() {
        if (!witherEnabled || plugin.game().state() != GameState.RUNNING) {
            return;
        }
        Settings settings = plugin.settings();
        World world = Worlds.require(settings.world);
        Location location = new Location(world, settings.luckyX, settings.luckyY, settings.luckyZ - 6);
        Wither wither = (Wither) world.spawnEntity(location, EntityType.WITHER);
        wither.setCustomNameVisible(true);
        wither.customName(Component.text("Abyssal Wither", NamedTextColor.DARK_PURPLE));
        Bukkit.broadcastMessage(Msg.color(settings.prefix + "&c☠ Abyssal Wither ist erwacht!"));
    }

    private void taunt() {
        if (!tauntsEnabled || plugin.game().state() != GameState.RUNNING) {
            return;
        }
        Settings settings = plugin.settings();
        World world = Worlds.require(settings.world);
        if (world.getEntitiesByClass(Wither.class).isEmpty()) {
            return;
        }
        String[] lines = settings.witherTaunts();
        if (lines.length == 0) {
            return;
        }
        String line = lines[(int) (Math.random() * lines.length)];
        Bukkit.broadcastMessage(Msg.color(settings.prefix + "&c" + line));
    }
}
