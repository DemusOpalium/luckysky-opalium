package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class GameManager {
    private final LuckySkyPlugin plugin;
    private final PlatformService platformService;
    private final WipeService wipeService;
    private final LuckyService luckyService;
    private final DurationService durationService;
    private final WitherService witherService;
    private final RewardsService rewardsService;

    private GameState state = GameState.IDLE;

    private final Set<UUID> activeParticipants = new HashSet<>();
    private final Set<UUID> allParticipants = new HashSet<>();

    public GameManager(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.platformService = new PlatformService(plugin);
        this.wipeService = new WipeService(plugin);
        this.luckyService = new LuckyService(plugin);
        this.durationService = new DurationService(plugin);
        this.witherService = new WitherService(plugin);
        this.rewardsService = new RewardsService(plugin);
    }

    public void shutdown() {
        luckyService.stop();
        durationService.stop();
        witherService.stop();
    }

    public GameState state() {
        return state;
    }

    public void start() {
        if (state == GameState.RUNNING) {
            return;
        }
        ensureWorldLoaded();
        activeParticipants.clear();
        allParticipants.clear();
        platformService.placeBase();
        bindAll();
        setAllSurvivalInWorld();
        luckyService.start();
        durationService.startDefault();
        witherService.start();
        state = GameState.RUNNING;
        broadcast("&a▶ LUCKYSKY START &7(Lucky @ " + plugin.settings().luckyX + "," + plugin.settings().luckyY + "," + plugin.settings().luckyZ + ")");
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> Msg.to(Bukkit.getConsoleSender(), "&7Game is running."), 1L);
    }

    public void stop() {
        if (state != GameState.RUNNING) {
            state = GameState.STOPPED;
            return;
        }
        luckyService.stop();
        durationService.stop();
        witherService.stop();
        state = GameState.STOPPED;
        broadcast("&f■ STOP &7(Timer aus • Safe-Blöcke bleiben)");
    }

    public void placePlatform() {
        ensureWorldLoaded();
        platformService.placeBase();
        Msg.to(Bukkit.getConsoleSender(), "&bSafe-Plattform gesetzt.");
    }

    public void placePlatformExtended() {
        ensureWorldLoaded();
        platformService.placeExtended();
        Msg.to(Bukkit.getConsoleSender(), "&bSafe-Plattform erweitert.");
    }

    public int softClear() {
        return wipeService.fieldClearSoft();
    }

    public int hardClear() {
        return wipeService.hardWipe();
    }

    public void bindAll() {
        Settings settings = plugin.settings();
        World world = Worlds.require(settings.world);
        Location respawn = new Location(world, settings.spawnX + 0.5, settings.spawnY, settings.spawnZ + 0.5, settings.spawnYaw, settings.spawnPitch);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            player.setBedSpawnLocation(respawn, true);
            UUID id = player.getUniqueId();
            activeParticipants.add(id);
            allParticipants.add(id);
        }
        broadcast("&bSpawnpoint für alle = (&f" + settings.spawnX + "," + settings.spawnY + "," + settings.spawnZ + "&b).");
    }

    public void setDurationMinutes(int minutes) {
        durationService.startMinutes(minutes);
    }

    public void setTauntsEnabled(boolean enabled) {
        witherService.setTauntsEnabled(enabled);
    }

    public void setWitherEnabled(boolean enabled) {
        witherService.setWitherEnabled(enabled);
    }

    public void spawnWitherNow() {
        witherService.spawnNow();
    }

    public void setAllSurvivalInWorld() {
        World world = Worlds.require(plugin.settings().world);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(world)) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    public boolean isParticipant(Player player) {
        return allParticipants.contains(player.getUniqueId());
    }

    public boolean isActiveParticipant(Player player) {
        return activeParticipants.contains(player.getUniqueId());
    }

    public void handleParticipantDeath(Player player) {
        if (!isActiveParticipant(player)) {
            return;
        }
        UUID id = player.getUniqueId();
        activeParticipants.remove(id);
        if (plugin.settings().oneLife()) {
            Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
            if (activeParticipants.isEmpty()) {
                rewardsService.triggerFail(allParticipants);
                broadcast("&cAlle Spieler ausgeschieden – Spiel beendet.");
                stop();
            }
        }
    }

    public void handleRespawn(Player player) {
        if (!isParticipant(player)) {
            return;
        }
        if (plugin.settings().oneLife()) {
            return;
        }
        activeParticipants.add(player.getUniqueId());
    }

    public void onDurationExpired() {
        if (state != GameState.RUNNING) {
            return;
        }
        rewardsService.triggerFail(allParticipants);
        broadcast("&eZeit abgelaufen – Spiel gestoppt.");
        stop();
    }

    public void handleWitherKill(Player killer) {
        if (state != GameState.RUNNING) {
            return;
        }
        rewardsService.triggerWin(killer, activeParticipants);
        broadcast("&aWither besiegt! GG!");
        stop();
    }

    public void reloadSettings() {
        luckyService.reload();
        durationService.reload();
        witherService.reload();
    }

    public Set<UUID> activeParticipants() {
        return Collections.unmodifiableSet(activeParticipants);
    }

    public Set<UUID> allParticipants() {
        return Collections.unmodifiableSet(allParticipants);
    }

    public boolean oneLifeEnabled() {
        return plugin.settings().oneLife();
    }

    private void ensureWorldLoaded() {
        Worlds.require(plugin.settings().world);
    }

    private void broadcast(String message) {
        String colored = Msg.color(plugin.settings().prefix + message);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(colored));
        Bukkit.getConsoleSender().sendMessage(colored);
    }
}
