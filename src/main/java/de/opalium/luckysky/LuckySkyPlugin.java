package de.opalium.luckysky;

import de.opalium.luckysky.commands.DuelsUiCommand;
import de.opalium.luckysky.commands.ArenaCommand;
import de.opalium.luckysky.commands.LsCommand;
import de.opalium.luckysky.duels.DuelsManager;
import de.opalium.luckysky.arena.ArenaService;
import de.opalium.luckysky.gui.editor.ArenaEditorGui;
import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.gui.AdminGui;
import de.opalium.luckysky.listeners.BossListener;
import de.opalium.luckysky.listeners.PlayerListener;
import de.opalium.luckysky.model.Settings;
import de.opalium.luckysky.trap.TrapService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckySkyPlugin extends JavaPlugin {
    private static LuckySkyPlugin instance;
    private Settings settings;
    private GameManager game;
    private AdminGui adminGui;
    private DuelsManager duels;
    private ArenaService arenaService;
    private TrapService trapService;
    private ArenaEditorGui arenaEditorGui;

    public static LuckySkyPlugin get() {
        return instance;
    }

    public Settings settings() {
        return settings;
    }

    public GameManager game() {
        return game;
    }

    public ArenaService arenaService() {
        return arenaService;
    }

    public TrapService trapService() {
        return trapService;
    }

    public ArenaEditorGui arenaEditorGui() {
        return arenaEditorGui;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.settings = new Settings(getConfig());
        this.arenaService = new ArenaService(this);
        this.trapService = new TrapService(this);
        this.game = new GameManager(this, arenaService, trapService);
        this.adminGui = new AdminGui(this);
        this.arenaEditorGui = new ArenaEditorGui(this, arenaService);
        this.duels = new DuelsManager(this);

        PluginCommand lsCommand = getCommand("ls");
        if (lsCommand != null) {
            LsCommand executor = new LsCommand(this);
            lsCommand.setExecutor(executor);
            lsCommand.setTabCompleter(executor);
        } else {
            getLogger().severe("[LuckySky] Failed to register /ls command - entry missing in plugin.yml");
        }

        PluginCommand arenaCommand = getCommand("arena");
        if (arenaCommand != null) {
            ArenaCommand executor = new ArenaCommand(this);
            arenaCommand.setExecutor(executor);
            arenaCommand.setTabCompleter(executor);
        } else {
            getLogger().severe("[LuckySky] Failed to register /arena command - entry missing in plugin.yml");
        }

        PluginCommand duelsCommand = getCommand("duelsui");
        if (duelsCommand != null) {
            DuelsUiCommand executor = new DuelsUiCommand(this);
            duelsCommand.setExecutor(executor);
            duelsCommand.setTabCompleter(executor);
        } else {
            getLogger().warning("[LuckySky] Failed to register /duelsui command - entry missing in plugin.yml");
        }

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BossListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(adminGui, this);
        pm.registerEvents(arenaEditorGui, this);
        pm.registerEvents(duels, this);

        getLogger().info("[LuckySky] enabled.");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.shutdown();
        }
        if (trapService != null) {
            trapService.disable();
        }
        if (arenaService != null) {
            arenaService.save();
        }
        instance = null;
        getLogger().info("[LuckySky] disabled.");
    }

    public void reloadSettings() {
        reloadConfig();
        this.settings = new Settings(getConfig());
        if (game != null) {
            game.reloadSettings();
        }
        if (duels != null) {
            duels.reload();
        }
        if (arenaService != null) {
            arenaService.reload();
        }
        if (trapService != null) {
            trapService.reload();
        }
    }

    public AdminGui adminGui() {
        return adminGui;
    }

    public DuelsManager duels() {
        return duels;
    }
}
