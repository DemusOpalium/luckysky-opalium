package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.MessagesConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.util.List;
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
        TrapsConfig traps = traps();
        this.witherEnabled = traps.withers().enabled();
        this.tauntsEnabled = traps.withers().taunts().enabled();
    }

    public void start() {
        stop();
        TrapsConfig traps = traps();
        witherEnabled = traps.withers().enabled();
        tauntsEnabled = traps.withers().taunts().enabled();
        if (!witherEnabled) {
            return;
        }
        spawnTimer = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::spawn,
                traps.withers().spawnAfterMinutes() * 60L * 20L);
        if (tauntsEnabled) {
            tauntTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::taunt,
                    traps.withers().taunts().everyTicks(), traps.withers().taunts().everyTicks());
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
            TrapsConfig traps = traps();
            witherEnabled = traps.withers().enabled();
            tauntsEnabled = traps.withers().taunts().enabled();
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
            TrapsConfig traps = traps();
            if (tauntTimer != -1) {
                Bukkit.getScheduler().cancelTask(tauntTimer);
            }
            tauntTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::taunt,
                    traps.withers().taunts().everyTicks(), traps.withers().taunts().everyTicks());
        }
    }

    private void spawn() {
        if (!witherEnabled || plugin.game().state() != GameState.RUNNING) {
            return;
        }
        GameConfig.Position position = plugin.configs().game().lucky().position();
        World world = Worlds.require(worldConfig().worldName());
        Location location = new Location(world, position.x(), position.y(), position.z() - 6);
        Wither wither = (Wither) world.spawnEntity(location, EntityType.WITHER);
        wither.setCustomNameVisible(true);
        wither.customName(Component.text("Abyssal Wither", NamedTextColor.DARK_PURPLE));
        Bukkit.broadcastMessage(Msg.color(messages().prefix() + "&c☠ Abyssal Wither ist erwacht!"));
    }

    private void taunt() {
        if (!tauntsEnabled || plugin.game().state() != GameState.RUNNING) {
            return;
        }
        World world = Worlds.require(worldConfig().worldName());
        if (world.getEntitiesByClass(Wither.class).isEmpty()) {
            return;
        }
        List<String> taunts = traps().withers().taunts().lines();
        if (taunts.isEmpty()) {
            return;
        }
        String line = taunts.get((int) (Math.random() * taunts.size()));
        Bukkit.broadcastMessage(Msg.color(messages().prefix() + "&c" + line));
    }

    private TrapsConfig traps() {
        return plugin.configs().traps();
    }

    private WorldsConfig.LuckyWorld worldConfig() {
        return plugin.configs().worlds().luckySky();
    }

    private MessagesConfig messages() {
        return plugin.configs().messages();
    }
}
