package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.MessagesConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GameManager {
    private final LuckySkyPlugin plugin;
    private final PlatformService platformService;
    private final WipeService wipeService;
    private final LuckyService luckyService;
    private final CountdownService countdownService;
    private final WitherService witherService;
    private final RewardService rewardService;
    private final RespawnService respawnService;
    private final ScoreboardService scoreboardService;
    private final StateMachine stateMachine;
    private int scheduledDurationMinutes;

    private final Set<UUID> activeParticipants = new HashSet<>();
    private final Set<UUID> allParticipants = new HashSet<>();
    private final Set<UUID> disconnectedParticipants = new HashSet<>();

    public GameManager(LuckySkyPlugin plugin, ScoreboardService scoreboardService) {
        this.plugin = plugin;
        this.scoreboardService = scoreboardService;
        this.stateMachine = new StateMachine(plugin);
        this.platformService = new PlatformService(plugin);
        this.wipeService = new WipeService(plugin);
        this.luckyService = new LuckyService(plugin);
        this.witherService = new WitherService(plugin);
        this.countdownService = new CountdownService(plugin, scoreboardService);
        this.rewardService = new RewardService(plugin, this, stateMachine, scoreboardService);
        this.respawnService = new RespawnService(plugin, this);
        this.scheduledDurationMinutes = plugin.configs().game().durations().minutesDefault();
    }

    public void shutdown() {
        luckyService.stop();
        countdownService.stop();
        rewardService.cancelEndTimer();
        witherService.stop();
        stateMachine.setState(GameState.IDLE);
        refreshScoreboard();
    }

    public GameState state() {
        return stateMachine.state();
    }

    public StateMachine stateMachine() {
        return stateMachine;
    }

    public CountdownService countdown() {
        return countdownService;
    }

    public RespawnService respawn() {
        return respawnService;
    }

    public RewardService rewards() {
        return rewardService;
    }

    // ─────────────────────────────────────────────────────────────
    // PRESET-START / CLEANUP
    // ─────────────────────────────────────────────────────────────
    public void startPreset(int durationMinutes, int witherAfterMinutes, boolean oneLife, boolean openPortal) {
        if (isRoundActive()) {
            Msg.to(Bukkit.getConsoleSender(), "&ePreset ignoriert: LuckySky läuft bereits.");
            return;
        }
        setDurationMinutes(durationMinutes);

        boolean cfgOneLife = gameConfig().lives().oneLife();
        if (oneLife && !cfgOneLife) {
            Msg.to(Bukkit.getConsoleSender(),
                    "&eHinweis: Preset fordert One-Life, aber &fconfig.yml &ehat one_life=false. (Runtime bleibt ohne Respawn)");
        }

        witherService.scheduleSpawn(witherAfterMinutes);

        if (openPortal) {
            PortalService.openBackspawn();
        }

        start();
    }

    public void stopAndCleanup(boolean closePortal) {
        witherService.cancelSpawn();
        stop();
        if (closePortal) {
            PortalService.closeBackspawn();
        }
    }

    // ─────────────────────────────────────────────────────────────

    public void start() {
        GameState current = stateMachine.state();
        if (isRoundActive() || current == GameState.ENDING || current == GameState.RESETTING) {
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
        stateMachine.clearWhitelist();
        platformService.placeBase();
        bindAll();
        teleportAllToPlatform();
        setAllSurvivalInWorld();
        luckyService.start();
        int minutes = scheduledDurationMinutes > 0
                ? scheduledDurationMinutes
                : plugin.configs().game().durations().minutesDefault();
        countdownService.startMinutes(minutes);
        witherService.start();
        rewardService.cancelEndTimer();
        stateMachine.setState(GameState.COUNTDOWN);

        WitherService.SpawnRequestResult spawnAtStart =
                witherService.requestSpawn(WitherService.SpawnTrigger.START);
        refreshScoreboard();
        if (spawnAtStart == WitherService.SpawnRequestResult.ACCEPTED) {
            Bukkit.getScheduler().runTask(plugin, this::refreshScoreboard);
        }

        broadcast(messages().gamePrefix() + worldConfig().lucky().startBanner());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!platformService.isBaseIntact()) {
                platformService.placeBase();
                Msg.to(Bukkit.getConsoleSender(), messages().adminPrefix() + "Plattformkern wiederhergestellt.");
            }
        }, 100L);
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> Msg.to(Bukkit.getConsoleSender(), messages().adminPrefix() + "Game is running."), 1L);
    }

    public void stop() {
        GameState previous = stateMachine.state();
        luckyService.stop();
        countdownService.stop();
        rewardService.cancelEndTimer();
        witherService.stop();
        stateMachine.setState(GameState.LOBBY);
        teleportAllToLobby();
        clearParticipants();
        scheduledDurationMinutes = plugin.configs().game().durations().minutesDefault();
        refreshScoreboard();
        if (previous == GameState.COUNTDOWN || previous == GameState.RUN || previous == GameState.ENDING) {
            broadcast(messages().gamePrefix() + plugin.configs().messages().stopBanner());
        }
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
        bindAllInternal(null, true);
    }

    public void bindAll(CommandSender sender) {
        bindAllInternal(sender, false);
    }

    private void bindAllInternal(CommandSender sender, boolean silent) {
        GameConfig.Spawns spawns = gameConfig().spawns();
        if (!spawns.allowBinding()) {
            if (!silent) {
                if (sender != null) {
                    Msg.to(sender, spawns.warning());
                }
                Msg.to(Bukkit.getConsoleSender(), spawns.warning());
            }
            return;
        }
        Optional<Location> respawnOpt = platformSpawnLocation();
        if (respawnOpt.isEmpty()) {
            Msg.to(Bukkit.getConsoleSender(), messages().adminPrefix() + "Kein LuckySky-Spawn definiert.");
            return;
        }
        Location respawn = respawnOpt.get();
        World world = respawn.getWorld();
        if (world == null) {
            return;
        }
        WorldsConfig.Spawn spawn = worldConfig().spawn();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            player.setBedSpawnLocation(respawn, true);
            UUID id = player.getUniqueId();
            activeParticipants.add(id);
            allParticipants.add(id);
            disconnectedParticipants.remove(id);
            stateMachine.whitelistPlayer(id);
        }
        broadcast(messages().gamePrefix() + String.format("&bSpawnpoint gesetzt (&f%.1f, %.1f, %.1f&b).",
                spawn.x(), spawn.y(), spawn.z()));
        if (sender != null && !silent) {
            Msg.to(sender, "&aSpawnpunkte aktualisiert.");
        }
        refreshScoreboard();
    }

    public void setDurationMinutes(int minutes) {
        scheduledDurationMinutes = Math.max(1, minutes);
        if (isRoundActive()) {
            countdownService.startMinutes(scheduledDurationMinutes);
        }
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

    public WitherService.SpawnRequestResult spawnWitherNow() {
        WitherService.SpawnRequestResult result =
                witherService.requestSpawn(WitherService.SpawnTrigger.MANUAL);
        if (result == WitherService.SpawnRequestResult.ACCEPTED) {
            refreshScoreboard();
            Bukkit.getScheduler().runTask(plugin, this::refreshScoreboard);
        }
        return result;
    }

    public void setAllSurvivalInWorld() {
        World world = Worlds.require(worldConfig().worldName());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(world)) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    public void teleportAllToPlatform() {
        Optional<Location> locationOpt = platformSpawnLocation();
        if (locationOpt.isEmpty()) {
            return;
        }
        Location location = locationOpt.get();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            player.teleport(location);
        }
    }

    public void teleportAllToLobby() {
        Optional<Location> lobbyOpt = lobbySpawnLocation();
        if (lobbyOpt.isEmpty()) {
            Msg.to(Bukkit.getConsoleSender(), messages().adminPrefix() + "Kein LuckySky-Lobby-Spawn definiert.");
            return;
        }
        Location lobby = lobbyOpt.get();
        World world = lobby.getWorld();
        if (world == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().equalsIgnoreCase(worldConfig().worldName())) {
                continue;
            }
            player.teleport(lobby);
            if (gameConfig().spawns().allowLobbyOverride()) {
                player.setBedSpawnLocation(lobby, true);
            }
            if (!isRoundActive() && !gameConfig().lives().oneLife()) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    public void teleportPlayerToLobby(Player player) {
        Optional<Location> lobbyOpt = lobbySpawnLocation();
        if (lobbyOpt.isEmpty()) {
            return;
        }
        Location location = lobbyOpt.get();
        player.teleport(location);
        if (gameConfig().spawns().allowLobbyOverride()) {
            player.setBedSpawnLocation(location, true);
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
            if (activeParticipants.isEmpty()) {
                handleAllPlayersEliminated();
            }
        }
        refreshScoreboard();
    }

    public void handleRespawn(Player player) {
        if (!isRoundActive()) {
            return;
        }
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
        if (isRoundActive()) {
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
        if (!isRoundActive()) {
            disconnectedParticipants.remove(id);
            return;
        }
        World world = Worlds.require(worldConfig().worldName());
        if (!player.getWorld().equals(world)) {
            return;
        }
        disconnectedParticipants.remove(id);
        activeParticipants.add(id);
        stateMachine.whitelistPlayer(id);
        if (isRoundActive()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                platformSpawnLocation().ifPresent(player::teleport);
            });
        }
        refreshScoreboard();
    }

    public boolean onDurationExpired() {
        if (!isRoundActive()) {
            return false;
        }
        WitherService.SpawnRequestResult result =
                witherService.requestSpawn(WitherService.SpawnTrigger.TIMEOUT);
        if (result == WitherService.SpawnRequestResult.ACCEPTED) {
            refreshScoreboard();
            Bukkit.getScheduler().runTask(plugin, this::refreshScoreboard);
            return false;
        }
        prepareEndPhase();
        rewardService.triggerFail(allParticipants);
        broadcast(messages().gamePrefix() + "&eZeit abgelaufen – Spiel gestoppt.");
        return true;
    }

    public void handleWitherKill(Player killer) {
        if (!isRoundActive()) {
            return;
        }
        prepareEndPhase();
        rewardService.triggerWin(killer, activeParticipants);
        broadcast(messages().gamePrefix() + "&aWither besiegt! GG!");
    }

    public void reloadSettings() {
        luckyService.reload();
        countdownService.reload();
        witherService.reload();
        if (!isRoundActive()) {
            scheduledDurationMinutes = plugin.configs().game().durations().minutesDefault();
        }
        refreshScoreboard();
    }

    private void handleAllPlayersEliminated() {
        prepareEndPhase();
        rewardService.triggerFail(allParticipants);
        broadcast(messages().gamePrefix() + "&cAlle Spieler ausgeschieden – Spiel beendet.");
    }

    public Set<UUID> activeParticipants() {
        return Collections.unmodifiableSet(activeParticipants);
    }

    public Set<UUID> allParticipants() {
        return Collections.unmodifiableSet(allParticipants);
    }

    public void triggerRewardsWin(Player killer) {
        prepareEndPhase();
        rewardService.triggerWin(killer, activeParticipants);
        refreshScoreboard();
    }

    public void triggerRewardsFail() {
        prepareEndPhase();
        rewardService.triggerFail(allParticipants);
        refreshScoreboard();
    }

    public boolean oneLifeEnabled() {
        return gameConfig().lives().oneLife();
    }

    public Optional<Location> platformSpawnLocation() {
        return locationForSpawn(worldConfig().spawn());
    }

    public Optional<Location> lobbySpawnLocation() {
        return locationForSpawn(worldConfig().lobby());
    }

    private World ensureWorldLoaded() {
        return Worlds.require(worldConfig().worldName());
    }

    private Optional<Location> locationForSpawn(WorldsConfig.Spawn spawn) {
        if (spawn == null) {
            return Optional.empty();
        }
        World world = Worlds.require(worldConfig().worldName());
        return Optional.of(new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()));
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

    private void clearParticipants() {
        activeParticipants.clear();
        allParticipants.clear();
        disconnectedParticipants.clear();
        stateMachine.clearWhitelist();
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

    private boolean isRoundActive() {
        GameState current = stateMachine.state();
        return current == GameState.COUNTDOWN || current == GameState.RUN;
    }

    private void prepareEndPhase() {
        luckyService.stop();
        countdownService.stop();
        witherService.stop();
    }
}
