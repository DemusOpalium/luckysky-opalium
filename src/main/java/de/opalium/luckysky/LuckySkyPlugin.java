package de.opalium.luckysky;

import de.opalium.luckysky.commands.ArenaCommand;
import de.opalium.luckysky.commands.DuelsUiCommand;
import de.opalium.luckysky.commands.LsCommand;
import de.opalium.luckysky.config.ConfigService;
import de.opalium.luckysky.duels.DuelsManager;
import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.game.ScoreboardService;
import de.opalium.luckysky.npc.NpcService;
import de.opalium.luckysky.gui.AdminGui;
import de.opalium.luckysky.gui.PlayerGui;
import de.opalium.luckysky.listeners.BossListener;
import de.opalium.luckysky.listeners.PlayerListener;
import de.opalium.luckysky.round.RoundState;
import de.opalium.luckysky.round.RoundStateMachine;
import de.opalium.luckysky.round.handlers.CountdownStateHandler;
import de.opalium.luckysky.round.handlers.EndingStateHandler;
import de.opalium.luckysky.round.handlers.LobbyStateHandler;
import de.opalium.luckysky.round.handlers.PrepareStateHandler;
import de.opalium.luckysky.round.handlers.ResetStateHandler;
import de.opalium.luckysky.round.handlers.RunStateHandler;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckySkyPlugin extends JavaPlugin {
    private static LuckySkyPlugin instance;
    private ConfigService configs;
    private GameManager game;
    private AdminGui adminGui;
    private DuelsManager duels;
    private PlayerGui playerGui;
    private ScoreboardService scoreboard;
    private NpcService npcs;
    private RoundStateMachine rounds;

    public static LuckySkyPlugin get() {
        return instance;
    }

    public ConfigService configs() {
        return configs;
    }

    public GameManager game() {
        return game;
    }

    public ScoreboardService scoreboard() {
        return scoreboard;
    }

    public RoundStateMachine rounds() {
        return rounds;
    }

    public NpcService npcs() {
        return npcs;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.configs = new ConfigService(this).ensureDefaults().loadAll();
        this.scoreboard = new ScoreboardService(this);
        this.game = new GameManager(this, scoreboard);
        this.rounds = new RoundStateMachine(this, game);
        rounds.registerHandler(RoundState.PREPARE, new PrepareStateHandler(game));
        rounds.registerHandler(RoundState.LOBBY, new LobbyStateHandler(game));
        rounds.registerHandler(RoundState.COUNTDOWN, new CountdownStateHandler(game));
        rounds.registerHandler(RoundState.RUN, new RunStateHandler(game));
        rounds.registerHandler(RoundState.ENDING, new EndingStateHandler(game));
        rounds.registerHandler(RoundState.RESET, new ResetStateHandler(game));
        game.attachRoundStateMachine(rounds);
        this.adminGui = new AdminGui(this);
        this.playerGui = new PlayerGui(this);
        this.duels = new DuelsManager(this);
        this.npcs = new NpcService(this);
        rounds.onEnable();

        registerCommand("ls", new LsCommand(this));
        registerCommand("arena", new ArenaCommand(this));
        registerCommand("duelsui", new DuelsUiCommand(this));

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BossListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(adminGui, this);
        pm.registerEvents(playerGui, this);
        pm.registerEvents(duels, this);

        scoreboard.attachAll();

        getLogger().info("[LuckySky] enabled.");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.shutdown();
        }
        if (rounds != null) {
            rounds.onDisable();
            rounds = null;
        }
        if (scoreboard != null) {
            scoreboard.shutdown();
            scoreboard = null;
        }
        if (npcs != null) {
            npcs.shutdown();
            npcs = null;
        }
        instance = null;
        getLogger().info("[LuckySky] disabled.");
    }

    public void reloadSettings() {
        reloadConfig();
        this.configs.reloadAll();
        if (scoreboard != null) {
            scoreboard.reload();
            scoreboard.attachAll();
        }
        if (game != null) {
            game.reloadSettings();
        }
        if (duels != null) {
            duels.reload();
        }
        if (npcs != null) {
            npcs.reload();
        }
        if (adminGui != null) {
            adminGui.reload();
        }
        if (playerGui != null) {
            playerGui.reload();
        }
    }

    public AdminGui adminGui() {
        return adminGui;
    }

    public PlayerGui playerGui() {
        return playerGui;
    }

    public DuelsManager duels() {
        return duels;
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            throw new IllegalStateException("Command '" + name + "' is not defined in plugin.yml");
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }
}
