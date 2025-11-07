package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.model.WorldsCfg;
import de.opalium.luckysky.config.model.WorldsCfg.WorldCfg;
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
    private final Set<UUID> disconnectedParticipants = new HashSet<>();

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
        disconnectedParticipants.clear();
        platformService.placeBase();
        bindAll();
        setAllSurvivalInWorld();
        luckyService.start();
        durationService.startDefault();
        witherService.start();
        state = GameState.RUNNING;
        WorldsCfg.Lucky lucky = worldConfig().lucky();
        broadcast("&a▶ LUCKYSKY START &7(Lucky @ " + lucky.x() + "," + lucky.y() + "," + lucky.z() + ")");
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
        platformService.placeBase();
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
        WorldCfg worldCfg = worldConfig();
        WorldsCfg.Spawn spawn = worldCfg.spawn();
        World world = Worlds.require(worldCfg.worldName());
        Location respawn = new Location(world, spawn.x() + 0.5, spawn.y(), spawn.z() + 0.5, spawn.yaw(), spawn.pitch());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            player.setBedSpawnLocation(respawn, true);
            UUID id = player.getUniqueId();
            activeParticipants.add(id);
            allParticipants.add(id);
            disconnectedParticipants.remove(id);
        }
        broadcast("&bSpawnpoint für alle = (&f" + spawn.x() + "," + spawn.y() + "," + spawn.z() + "&b).");
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
        World world = Worlds.require(worldConfig().worldName());
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
        disconnectedParticipants.remove(id);
        if (plugin.configs().game().lives().oneLife()) {
            Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
            if (activeParticipants.isEmpty()) {
                handleAllPlayersEliminated();
            }
        }
    }

    public void handleRespawn(Player player) {
        if (!isParticipant(player)) {
            return;
        }
        if (plugin.configs().game().lives().oneLife()) {
            return;
        }
        activeParticipants.add(player.getUniqueId());
        disconnectedParticipants.remove(player.getUniqueId());
    }

    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        if (!allParticipants.contains(id)) {
            return;
        }
        boolean wasActive = activeParticipants.remove(id);
        if (!wasActive) {
            disconnectedParticipants.remove(id);
            return;
        }
        if (state == GameState.RUNNING) {
            disconnectedParticipants.add(id);
            if (activeParticipants.isEmpty()) {
                handleAllPlayersEliminated();
            }
        }
    }

    public void handleJoin(Player player) {
        UUID id = player.getUniqueId();
        if (!disconnectedParticipants.contains(id)) {
            return;
        }
        if (state != GameState.RUNNING) {
            disconnectedParticipants.remove(id);
            return;
        }
        World world = Worlds.require(worldConfig().worldName());
        if (!player.getWorld().equals(world)) {
            return;
        }
        disconnectedParticipants.remove(id);
        activeParticipants.add(id);
        if (state == GameState.RUNNING) {
            Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SURVIVAL));
        }
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

    private void handleAllPlayersEliminated() {
        rewardsService.triggerFail(allParticipants);
        broadcast("&cAlle Spieler ausgeschieden – Spiel beendet.");
        stop();
    }

    public Set<UUID> activeParticipants() {
        return Collections.unmodifiableSet(activeParticipants);
    }

    public Set<UUID> allParticipants() {
        return Collections.unmodifiableSet(allParticipants);
    }

    public boolean oneLifeEnabled() {
        return plugin.configs().game().lives().oneLife();
    }

    private void ensureWorldLoaded() {
        Worlds.require(worldConfig().worldName());
    }

    private void broadcast(String message) {
        String colored = Msg.color(plugin.configs().messages().prefix() + message);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(colored));
        Bukkit.getConsoleSender().sendMessage(colored);
    }

    private WorldCfg worldConfig() {
        return plugin.configs().worlds().primary();
    }

}
