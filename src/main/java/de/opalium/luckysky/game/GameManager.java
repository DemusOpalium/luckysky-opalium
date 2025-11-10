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

    // ─────────────────────────────────────────────────────────────
    // PRESET-START / CLEANUP (NEU)
    // ─────────────────────────────────────────────────────────────

    /**
     * Startet ein Preset:
     * - setzt Laufzeit (nur runtime),
     * - plant Wither-Spawn in X Minuten,
     * - öffnet optional Portal,
     * - respektiert One-Life falls in Config aktiv (kein Config-Schreiben hier).
     */
    public void startPreset(int durationMinutes, int witherAfterMinutes, boolean oneLife, boolean openPortal) {
        if (state == GameState.RUNNING) {
            Msg.to(Bukkit.getConsoleSender(), "&ePreset ignoriert: LuckySky läuft bereits.");
            return;
        }

        // Dauer runtime setzen (persistiert nicht bewusst, um Build-Sicherheit zu wahren)
        setDurationMinutes(durationMinutes);

        // Hinweis, falls One-Life im Preset gewünscht ist, aber in Config nicht aktiv
        boolean cfgOneLife = gameConfig().lives().oneLife();
        if (oneLife && !cfgOneLife) {
            Msg.to(Bukkit.getConsoleSender(),
                    "&eHinweis: Preset fordert One-Life, aber &fconfig.yml &ehat one_life=false. (Runtime bleibt ohne Respawn)");
        }

        // Wither-Spawn planen (überschreibt ggf. Default)
        witherService.scheduleSpawn(witherAfterMinutes);

        // Portal öffnen (Multiverse-Portals)
        if (openPortal) {
            PortalService.openBackspawn();
        }

        // regulär starten (teleports, binds, services)
        start();
    }

    /**
     * Stoppt das Spiel und räumt Everything auf:
     * - Wither-Timer abbrechen,
     * - Spiel stoppen,
     * - Portal optional schließen.
     */
    public void stopAndCleanup(boolean closePortal) {
        witherService.cancelSpawn();
        stop();
        if (closePortal) {
            PortalService.closeBackspawn();
        }
    }

    // ─────────────────────────────────────────────────────────────

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
        teleportAllToPlatform();
        setAllSurvivalInWorld();
        luckyService.start();
        durationService.startDefault();
        witherService.start();
        state = GameState.RUNNING;
        WitherService.SpawnRequestResult spawnAtStart = witherService.requestSpawn(WitherService.SpawnTrigger.START);
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
        if (state != GameState.RUNNING) {
            teleportAllToLobby();
            clearParticipants();
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
        teleportAllToLobby();
        clearParticipants();
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
        }
        broadcast(messages().gamePrefix() + String.format("&bSpawnpoint gesetzt (&f%.1f, %.1f, %.1f&b).",
                spawn.x(), spawn.y(), spawn.z()));
        if (sender != null && !silent) {
            Msg.to(sender, "&aSpawnpunkte aktualisiert.");
        }
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

    public WitherService.SpawnRequestResult spawnWitherNow() {
        WitherService.SpawnRequestResult result = witherService.requestSpawn(WitherService.SpawnTrigger.MANUAL);
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
            if (state != GameState.RUNNING && !gameConfig().lives().oneLife()) {
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
            Bukkit.getScheduler().runTask(plugin, () -> player.setGameMode(GameMode.SPECTATOR));
            if (activeParticipants.isEmpty()) {
                handleAllPlayersEliminated();
            }
        }
        refreshScoreboard();
    }

    public void handleRespawn(Player player) {
        if (state != GameState.RUNNING) {
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
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                platformSpawnLocation().ifPresent(player::teleport);
            });
        }
        refreshScoreboard();
    }

    public boolean onDurationExpired() {
        if (state != GameState.RUNNING) {
            return false;
        }
        WitherService.SpawnRequestResult result = witherService.requestSpawn(WitherService.SpawnTrigger.TIMEOUT);
        if (result == WitherService.SpawnRequestResult.ACCEPTED) {
            refreshScoreboard();
            Bukkit.getScheduler().runTask(plugin, this::refreshScoreboard);
            return false;
        }
        rewardsService.triggerFail(allParticipants);
        broadcast(messages().gamePrefix() + "&eZeit abgelaufen – Spiel gestoppt.");
        stop();
        return true;
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
