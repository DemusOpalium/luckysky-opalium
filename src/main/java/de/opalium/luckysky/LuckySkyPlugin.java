package de.opalium.luckysky;

import de.opalium.luckysky.commands.DuelsUiCommand;
import de.opalium.luckysky.commands.LsCommand;
import de.opalium.luckysky.duels.DuelsManager;
import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.gui.AdminGui;
import de.opalium.luckysky.listeners.BossListener;
import de.opalium.luckysky.listeners.PlayerListener;
import de.opalium.luckysky.model.Settings;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckySkyPlugin extends JavaPlugin {
    private static LuckySkyPlugin instance;
    private Settings settings;
    private GameManager game;
    private AdminGui adminGui;
    private DuelsManager duels;

    public static LuckySkyPlugin get() {
        return instance;
    }

    public Settings settings() {
        return settings;
    }

    public GameManager game() {
        return game;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.settings = new Settings(getConfig());
        this.game = new GameManager(this);
        this.adminGui = new AdminGui(this);
        this.duels = new DuelsManager(this);

        PluginCommand lsCommand = getCommand("ls");
        if (lsCommand != null) {
            LsCommand executor = new LsCommand(this);
            lsCommand.setExecutor(executor);
            lsCommand.setTabCompleter(executor);
        } else {
            getLogger().severe("[LuckySky] Failed to register /ls command - entry missing in plugin.yml");
        }

        PluginCommand duelsCommand = getCommand("duelsui");
        if (duelsCommand != null) {
            duelsCommand.setExecutor(new DuelsUiCommand(this));
        } else {
            getLogger().severe("[LuckySky] Failed to register /duelsui command - entry missing in plugin.yml");
        }

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BossListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(adminGui, this);
        pm.registerEvents(duels, this);

        getLogger().info("[LuckySky] enabled.");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            game.shutdown();
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
    }

    public AdminGui adminGui() {
        return adminGui;
    }

    public DuelsManager duels() {
        return duels;
    }
}
