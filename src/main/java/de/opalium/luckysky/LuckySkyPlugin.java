package de.opalium.luckysky;

import de.opalium.luckysky.commands.ArenaCommand;
import de.opalium.luckysky.commands.DuelsUiCommand;
import de.opalium.luckysky.commands.LsCommand;
import de.opalium.luckysky.config.ConfigService;
import de.opalium.luckysky.duels.DuelsManager;
import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.game.ScoreboardService;
import de.opalium.luckysky.gui.AdminGui;
import de.opalium.luckysky.listeners.BossListener;
import de.opalium.luckysky.listeners.PlayerListener;
import de.opalium.luckysky.listeners.NpcRightClickListener;
import de.opalium.luckysky.npc.NpcDepot;
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
    private ScoreboardService scoreboard;
    private NpcDepot npcDepot;

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

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.configs = new ConfigService(this).ensureDefaults().loadAll();
        this.scoreboard = new ScoreboardService(this);
        this.game = new GameManager(this, scoreboard);
        this.adminGui = new AdminGui(this);
        this.duels = new DuelsManager(this);
        this.npcDepot = new NpcDepot(this);
        this.npcDepot.enable();

        registerCommand("ls", new LsCommand(this));
        registerCommand("arena", new ArenaCommand(this));
        registerCommand("duelsui", new DuelsUiCommand(this));

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BossListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(adminGui, this);
        pm.registerEvents(duels, this);
        if (npcDepot.isAvailable()) {
            pm.registerEvents(new NpcRightClickListener(this), this);
        }

        getLogger().info("[LuckySky] enabled.");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.shutdown();
        }
        if (scoreboard != null) {
            scoreboard.shutdown();
            scoreboard = null;
        }
        if (npcDepot != null) {
            npcDepot.shutdown();
            npcDepot = null;
        }
        instance = null;
        getLogger().info("[LuckySky] disabled.");
    }

    public void reloadSettings() {
        reloadConfig();
        this.configs.reloadAll();
        if (scoreboard != null) {
            scoreboard.reload();
        }
        if (game != null) {
            game.reloadSettings();
        }
        if (duels != null) {
            duels.reload();
        }
        if (adminGui != null) {
            adminGui.reload();
        }
        if (npcDepot != null && npcDepot.isAvailable()) {
            npcDepot.reload();
        }
    }

    public AdminGui adminGui() {
        return adminGui;
    }

    public DuelsManager duels() {
        return duels;
    }

    public NpcDepot npcDepot() {
        return npcDepot;
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
