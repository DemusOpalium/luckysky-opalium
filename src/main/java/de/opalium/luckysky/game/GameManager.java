package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class GameManager {
    public enum Phase { LOBBY, PHASE1, BOSS, END }

    private final LuckySkyPlugin plugin;
    private final Settings settings;

    private Phase phase = Phase.LOBBY;

    private BukkitTask tickTimer;
    private BukkitTask minuteTimer;
    private BukkitTask luckyTimer;
    private BukkitTask softWipeTimer;
    private BukkitTask tauntTimer;

    private int minutesLeft;

    public GameManager(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.settings = plugin.settings();
        this.minutesLeft = settings.minutesDefault;
    }

    public Phase phase() {
        return phase;
    }

    public void start() {
        if (phase != Phase.LOBBY && phase != Phase.END) {
            stop();
        }
        phase = Phase.PHASE1;
        minutesLeft = settings.minutesDefault;

        bindAll();
        placePlatform(settings.platformBig);
        forceload(true);

        startTickTimer();
        startLuckyTimer();
        startSoftWipeTimer();
        if (settings.tauntEnable) {
            startTauntTimer(false);
        }

        titleAll(ChatColor.GREEN + "▶ LUCKYSKY START", ChatColor.WHITE + "Lucky @ " + settings.luckyX + "," + settings.luckyY + "," + settings.luckyZ);
    }

    public void stop() {
        phase = Phase.LOBBY;
        cancelTimers();
        forceload(false);
        titleAll(ChatColor.WHITE + "■ STOP", ChatColor.GRAY + "Timer aus • Safe-Blöcke bleiben.");
    }

    public void shutdown() {
        cancelTimers();
    }

    private void startTickTimer() {
        cancel(tickTimer);
        cancel(minuteTimer);

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        tickTimer = scheduler.runTaskTimer(plugin, () -> {
            if (phase == Phase.PHASE1) {
                // jede Sekunde aktualisieren (BossBar, Scoreboard, etc.)
            }
        }, 20L, 20L);

        minuteTimer = scheduler.runTaskTimer(plugin, () -> {
            if (phase != Phase.PHASE1) {
                return;
            }
            minutesLeft--;
            if (settings.witherEnable && minutesLeft <= (settings.minutesDefault - settings.witherAfterMinutes)) {
                spawnWitherNow();
                phase = Phase.BOSS;
            }
            if (minutesLeft <= 0) {
                stop();
                // TODO: Rewards.onFail
            }
        }, 1200L, 1200L);
    }

    private void startLuckyTimer() {
        cancel(luckyTimer);
        long interval = Math.max(1L, settings.luckyInterval);
        luckyTimer = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (phase != Phase.PHASE1) {
                return;
            }
            World world = Bukkit.getWorld(settings.world);
            if (world == null) {
                return;
            }
            if (settings.luckyRequireAir && !world.getBlockAt(settings.luckyX, settings.luckyY, settings.luckyZ).isEmpty()) {
                return;
            }
            String command = String.format(
                    "ntdluckyblock place %s %d %d %d %s -s",
                    "LuckySky", settings.luckyX, settings.luckyY, settings.luckyZ, settings.luckyVariant
            );
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        }, interval, interval);
    }

    private void startSoftWipeTimer() {
        cancel(softWipeTimer);
        long interval = Math.max(1L, settings.softEveryTicks);
        softWipeTimer = plugin.getServer().getScheduler().runTaskTimer(plugin, this::softWipe, interval, interval);
    }

    private void startTauntTimer(boolean force) {
        if (!force && !settings.tauntEnable) {
            return;
        }
        cancel(tauntTimer);
        long interval = Math.max(1L, settings.tauntEveryTicks);
        tauntTimer = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (phase != Phase.PHASE1 && phase != Phase.BOSS) {
                return;
            }
            // TODO: checke Wither existiert in Reichweite; rotiere taunt lines aus config
        }, interval, interval);
    }

    private void cancelTimers() {
        cancel(tickTimer);
        cancel(minuteTimer);
        cancel(luckyTimer);
        cancel(softWipeTimer);
        cancel(tauntTimer);
    }

    private void cancel(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    public void softWipe() {
        // kill area_effect_cloud, block_display, item_display, text_display, interaction, marker, falling_block, armor_stand (Radius abhängig)
        // TODO implement kill selectors per radius
    }

    public void hardWipe() {
        // TODO: wie oben, aber größer/plus armor stands 5000
    }

    public void placePlatform(boolean big) {
        World world = Bukkit.getWorld(settings.world);
        if (world == null) {
            return;
        }
        // base aus config.platform.base.blocks setzen; wenn big==true, fill 3x3 extra
        // TODO implement block placement
    }

    public void bindAll() {
        World world = Bukkit.getWorld(settings.world);
        if (world == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            player.setBedSpawnLocation(new Location(world, settings.spawnX + 0.5, settings.spawnY, settings.spawnZ + 0.5, settings.spawnYaw, settings.spawnPitch), true);
        }
    }

    public void setDurationMinutes(int minutes) {
        minutesLeft = minutes;
    }

    public void setTaunts(boolean enabled) {
        if (enabled) {
            startTauntTimer(true);
        } else {
            cancel(tauntTimer);
        }
    }

    public void spawnWitherNow() {
        // TODO: summon wither at (0,102,0) with name/color from config; boss handling
    }

    private void titleAll(String title, String subtitle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(title, subtitle, 10, 60, 10);
        }
    }

    private void forceload(boolean add) {
        // Optional: Bukkit 1.21 hat API: Server-level? ansonsten auslassen
    }
}
