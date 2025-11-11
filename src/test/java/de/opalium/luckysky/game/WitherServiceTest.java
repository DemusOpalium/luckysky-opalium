package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.ConfigService;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.MessagesConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WitherServiceTest {
    private LuckySkyPlugin plugin;
    private GameManager gameManager;
    private TestableWitherService service;

    @BeforeEach
    void setUp() {
        plugin = mock(LuckySkyPlugin.class);
        gameManager = mock(GameManager.class);
        ConfigService configs = mock(ConfigService.class);

        when(plugin.game()).thenReturn(gameManager);
        when(plugin.configs()).thenReturn(configs);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        TrapsConfig trapsConfig = new TrapsConfig(
                new TrapsConfig.Withers(true, 1, true, 5, new TrapsConfig.Taunts(false, 1200, List.of("taunt"))),
                new TrapsConfig.Effects(true)
        );
        WorldsConfig worldsConfig = new WorldsConfig(
                new WorldsConfig.LuckyWorld(
                        "LuckySky",
                        new WorldsConfig.Spawn(0.0, 0.0, 0.0, 0f, 0f),
                        null,
                        new WorldsConfig.Lucky("banner", true)
                ),
                new WorldsConfig.DuelsWorld("duels", new WorldsConfig.Spawn(0.0, 0.0, 0.0, 0f, 0f), 10)
        );
        GameConfig gameConfig = new GameConfig(
                new GameConfig.Durations(60, List.of(60), false, "&dLuckySky", false, "&eTimer: &f{time}"),
                new GameConfig.Lucky(new GameConfig.Position(10, 64, 10), 160, true, "RANDOM", List.of("RANDOM")),
                new GameConfig.Platform(true, List.of()),
                new GameConfig.Rig(200, true),
                new GameConfig.Wipes(100, 10, 20, 30),
                new GameConfig.Rewards("balanced", List.of(), List.of()),
                new GameConfig.Lives(false),
                new GameConfig.Scoreboard(false, "Title", List.of("line")),
                new GameConfig.Wither(GameConfig.WitherSpawnMode.ALL),
                new GameConfig.Spawns(false, false, "")
        );
        MessagesConfig messagesConfig = new MessagesConfig("&d☁ LuckySky&7 » ", "", "", "", "", "");

        when(configs.traps()).thenReturn(trapsConfig);
        when(configs.worlds()).thenReturn(worldsConfig);
        when(configs.game()).thenReturn(gameConfig);
        when(configs.messages()).thenReturn(messagesConfig);

        service = new TestableWitherService(plugin);
        service.setShouldTrigger(true);
        when(gameManager.state()).thenReturn(GameState.RUN);
    }

    @Test
    void requestSpawnReturnsGameNotRunningWhenGameIsNotRunning() {
        when(gameManager.state()).thenReturn(GameState.IDLE);

        WitherService.SpawnRequestResult result = service.requestSpawn(WitherService.SpawnTrigger.MANUAL);

        assertEquals(WitherService.SpawnRequestResult.GAME_NOT_RUNNING, result);
    }

    @Test
    void requestSpawnReturnsSkippedByModeWhenTriggerRejected() {
        service.setShouldTrigger(false);

        WitherService.SpawnRequestResult result = service.requestSpawn(WitherService.SpawnTrigger.MANUAL);

        assertEquals(WitherService.SpawnRequestResult.SKIPPED_BY_MODE, result);
    }

    @Test
    void requestSpawnReturnsFailedWhenWorldDifficultyPeaceful() {
        World world = mock(World.class);
        when(world.getDifficulty()).thenReturn(Difficulty.PEACEFUL);
        when(world.getName()).thenReturn("LuckySky");

        try (MockedStatic<Worlds> worlds = mockStatic(Worlds.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            worlds.when(() -> Worlds.require("LuckySky")).thenReturn(world);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);

            WitherService.SpawnRequestResult result = service.requestSpawn(WitherService.SpawnTrigger.MANUAL);

            assertEquals(WitherService.SpawnRequestResult.FAILED, result);
            verify(world, never()).spawnEntity(any(Location.class), eq(EntityType.WITHER));
        }
    }

    @Test
    void requestSpawnReturnsFailedWhenMobSpawningDisabled() {
        World world = mock(World.class);
        when(world.getDifficulty()).thenReturn(Difficulty.HARD);
        when(world.getGameRuleValue(GameRule.DO_MOB_SPAWNING)).thenReturn(Boolean.FALSE);
        when(world.getName()).thenReturn("LuckySky");

        try (MockedStatic<Worlds> worlds = mockStatic(Worlds.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            worlds.when(() -> Worlds.require("LuckySky")).thenReturn(world);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);

            WitherService.SpawnRequestResult result = service.requestSpawn(WitherService.SpawnTrigger.MANUAL);

            assertEquals(WitherService.SpawnRequestResult.FAILED, result);
            verify(world, never()).spawnEntity(any(Location.class), eq(EntityType.WITHER));
        }
    }

    @Test
    void requestSpawnReturnsAcceptedWhenSpawnSucceeds() {
        World world = mock(World.class);
        Wither wither = mock(Wither.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        when(world.getDifficulty()).thenReturn(Difficulty.HARD);
        when(world.getGameRuleValue(GameRule.DO_MOB_SPAWNING)).thenReturn(Boolean.TRUE);
        when(world.getName()).thenReturn("LuckySky");
        when(world.getEntitiesByClass(Wither.class)).thenReturn(List.of());
        when(world.spawnEntity(any(Location.class), eq(EntityType.WITHER))).thenReturn(wither);
        when(scheduler.scheduleSyncRepeatingTask(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(1);

        try (MockedStatic<Worlds> worlds = mockStatic(Worlds.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Msg> msg = mockStatic(Msg.class)) {
            worlds.when(() -> Worlds.require("LuckySky")).thenReturn(world);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.broadcastMessage(anyString())).thenReturn(1);
            msg.when(() -> Msg.color(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

            WitherService.SpawnRequestResult result = service.requestSpawn(WitherService.SpawnTrigger.MANUAL);

            assertEquals(WitherService.SpawnRequestResult.ACCEPTED, result);
            verify(world).spawnEntity(any(Location.class), eq(EntityType.WITHER));
        }
    }

    @Test
    void requestSpawnRemovesExistingWithersBeforeSpawning() {
        World world = mock(World.class);
        Wither existing = mock(Wither.class);
        Wither spawned = mock(Wither.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);

        when(world.getDifficulty()).thenReturn(Difficulty.HARD);
        when(world.getGameRuleValue(GameRule.DO_MOB_SPAWNING)).thenReturn(Boolean.TRUE);
        when(world.getName()).thenReturn("LuckySky");
        when(world.getEntitiesByClass(Wither.class)).thenReturn(List.of(existing));
        when(world.spawnEntity(any(Location.class), eq(EntityType.WITHER))).thenReturn(spawned);
        when(scheduler.scheduleSyncRepeatingTask(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(1);

        try (MockedStatic<Worlds> worlds = mockStatic(Worlds.class);
             MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<Msg> msg = mockStatic(Msg.class)) {
            worlds.when(() -> Worlds.require("LuckySky")).thenReturn(world);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.broadcastMessage(anyString())).thenReturn(1);
            msg.when(() -> Msg.color(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

            WitherService.SpawnRequestResult result = service.requestSpawn(WitherService.SpawnTrigger.MANUAL);

            assertEquals(WitherService.SpawnRequestResult.ACCEPTED, result);

            InOrder inOrder = inOrder(existing, world);
            inOrder.verify(existing).remove();
            inOrder.verify(world).spawnEntity(any(Location.class), eq(EntityType.WITHER));
        }
    }

    private static final class TestableWitherService extends WitherService {
        private boolean shouldTrigger;

        private TestableWitherService(LuckySkyPlugin plugin) {
            super(plugin);
        }

        void setShouldTrigger(boolean value) {
            this.shouldTrigger = value;
        }

        @Override
        boolean shouldTrigger(SpawnTrigger trigger) {
            return shouldTrigger;
        }
    }
}
