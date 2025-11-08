package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.MessagesConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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
    private final ScoreboardService scoreboardService;

    private GameState state = GameState.IDLE;

    private final Set<UUID> activeParticipants = new HashSet<>();
    private final Set<UUID> allParticipants = new HashSet<>();
    private final Set<UUID> disconnectedParticipants = new HashSet<>();

    public GameManager(LuckySkyPlugin plugin, ScoreboardService scoreboardService) {
        this.plugin = plugin;
        this.scoreboardService = scoreboardService;
        this.platformService = new PlatformService(plugin);
        this.wipeService = new WipeService(plugin);
        this.luckyService = new LuckyService(plugin);
        this.durationService = new DurationService(plugin, scoreboardService);
        this.witherService = new WitherService(plugin);
        this.rewardsService = new RewardsService(plugin);
    }

    public void shutdown() {
        luckyService.stop();
        durationService.stop();
        witherService.stop();
        refreshScoreboard();
    }

    public GameState state() {
        return state;
    }

    public void start() {
        if (state == GameState.RUNNING) {
            Msg.to(Bukkit.getConsoleSender(), "&cLuckySky läuft bereits.");
            return;
        }
        World world = ensureWorldLoaded();
        GameConfig game = gameConfig();
        GameConfig.Position position = game.lucky().position();
        if (game.lucky().requireAirAtTarget()
                && world.getBlockAt(position.x(), position.y(), position.z()).getType() != Material.AIR) {
            Msg.to(Bukkit.getConsoleSender(), "&cLucky-Locus ist blockiert. Entferne Block bei "
                    + position.x() + ", " + position.y() + ", " + position.z() + ".");
            return;
        }
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
        refreshScoreboard();
        broadcast(messages().gamePrefix() + worldConfig().lucky().startBanner());
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> Msg.to(Bukkit.getConsoleSender(), messages().adminPrefix() + "Game is running."), 1L);
    }

    public void stop() {
        if (state != GameState.RUNNING) {
            state = GameState.STOPPED;
            refreshScoreboard();
            return;
        }
        luckyService.stop();
        durationService.stop();
        witherService.stop();
        state = GameState.STOPPED;
        refreshScoreboard();
        broadcast(messages().gamePrefix() + plugin.configs().messages().stopBanner());
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
        WorldsConfig.Spawn spawn = worldConfig().spawn();
        World world = Worlds.require(worldConfig().worldName());
        Location respawn = new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
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
        broadcast(messages().gamePrefix() + String.format("&bSpawnpoint gesetzt (&f%.1f, %.1f, %.1f&b).",
                spawn.x(), spawn.y(), spawn.z()));
        refreshScoreboard();
    }

    public void setDurationMinutes(int minutes) {
        durationService.startMinutes(minutes);
        refreshScoreboard();
    }

    public void setTauntsEnabled(boolean enabled) {
        witherService.setTauntsEnabled(enabled);
        refreshScoreboard();
    }

    public void setWitherEnabled(boolean enabled) {
        witherService.setWitherEnabled(enabled);
        refreshScoreboard();
    }

    public void spawnWitherNow() {
        witherService.spawnNow();
        refreshScoreboard();
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
        if (gameConfig().lives().oneLife()) {
            Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
            if (activeParticipants.isEmpty()) {
                handleAllPlayersEliminated();
            }
        }
        refreshScoreboard();
    }

    public void handleRespawn(Player player) {
        if (!isParticipant(player)) {
            return;
        }
        if (gameConfig().lives().oneLife()) {
            return;
        }
        activeParticipants.add(player.getUniqueId());
        disconnectedParticipants.remove(player.getUniqueId());
        refreshScoreboard();
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
        refreshScoreboard();
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
        refreshScoreboard();
    }

    public void onDurationExpired() {
        if (state != GameState.RUNNING) {
            return;
        }
        rewardsService.triggerFail(allParticipants);
        broadcast(messages().gamePrefix() + "&eZeit abgelaufen – Spiel gestoppt.");
        stop();
    }

    public void handleWitherKill(Player killer) {
        if (state != GameState.RUNNING) {
            return;
        }
        rewardsService.triggerWin(killer, activeParticipants);
        broadcast(messages().gamePrefix() + "&aWither besiegt! GG!");
        stop();
    }

    public void reloadSettings() {
        luckyService.reload();
        durationService.reload();
        witherService.reload();
        refreshScoreboard();
    }

    private void handleAllPlayersEliminated() {
        rewardsService.triggerFail(allParticipants);
        broadcast(messages().gamePrefix() + "&cAlle Spieler ausgeschieden – Spiel beendet.");
        stop();
    }

    public Set<UUID> activeParticipants() {
        return Collections.unmodifiableSet(activeParticipants);
    }

    public Set<UUID> allParticipants() {
        return Collections.unmodifiableSet(allParticipants);
    }

    public void triggerRewardsWin(Player killer) {
        rewardsService.triggerWin(killer, activeParticipants);
        refreshScoreboard();
    }

    public void triggerRewardsFail() {
        rewardsService.triggerFail(allParticipants);
        refreshScoreboard();
    }

    public boolean oneLifeEnabled() {
        return gameConfig().lives().oneLife();
    }

    private World ensureWorldLoaded() {
        return Worlds.require(worldConfig().worldName());
    }

    private GameConfig gameConfig() {
        return plugin.configs().game();
    }

    private WorldsConfig.LuckyWorld worldConfig() {
        return plugin.configs().worlds().luckySky();
    }

    private MessagesConfig messages() {
        return plugin.configs().messages();
    }

    private void broadcast(String message) {
        String colored = Msg.color(messages().prefix() + message);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(colored));
        Bukkit.getConsoleSender().sendMessage(colored);
    }

    private void refreshScoreboard() {
        if (scoreboardService != null) {
            scoreboardService.refresh();
        }
    }
}
