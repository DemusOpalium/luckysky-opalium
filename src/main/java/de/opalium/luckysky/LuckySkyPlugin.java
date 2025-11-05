package de.opalium.luckysky;

import de.opalium.luckysky.commands.LsCommand;
import de.opalium.luckysky.core.CorridorCleaner;
import de.opalium.luckysky.core.PlatformBuilder;
import de.opalium.luckysky.core.SessionManager;
import de.opalium.luckysky.core.WipeService;
import de.opalium.luckysky.core.WitherService;
import de.opalium.luckysky.util.ConfigKeys;
import de.opalium.luckysky.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.logging.Level;

public class LuckySkyPlugin extends JavaPlugin {
    private Messages messages;
    private SessionManager sessionManager;
    private PlatformBuilder platformBuilder;
    private CorridorCleaner corridorCleaner;
    private WipeService wipeService;
    private WitherService witherService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.messages = new Messages(this);
        try {
            this.messages.load();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Failed to load messages.yml", ex);
        }

        this.platformBuilder = new PlatformBuilder(this);
        this.corridorCleaner = new CorridorCleaner(this);
        this.wipeService = new WipeService(this);
        this.witherService = new WitherService(this);
        Bukkit.getPluginManager().registerEvents(this.witherService, this);
        this.sessionManager = new SessionManager(this);

        LsCommand lsCommand = new LsCommand(this);
        if (getCommand("ls") != null) {
            getCommand("ls").setExecutor(lsCommand);
            getCommand("ls").setTabCompleter(lsCommand);
        } else {
            getLogger().severe("Command 'ls' is not defined in plugin.yml");
        }

        getLogger().info("LuckySky-Opalium v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (this.sessionManager != null) {
            this.sessionManager.shutdown();
        }
        if (this.witherService != null) {
            this.witherService.shutdown();
        }
        getLogger().info("LuckySky-Opalium disabled.");
    }

    public Messages getMessages() {
        return messages;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public PlatformBuilder getPlatformBuilder() {
        return platformBuilder;
    }

    public CorridorCleaner getCorridorCleaner() {
        return corridorCleaner;
    }

    public WipeService getWipeService() {
        return wipeService;
    }

    public WitherService getWitherService() {
        return witherService;
    }

    public Optional<World> getGameWorld() {
        String worldName = getConfig().getString(ConfigKeys.WORLD);
        if (worldName == null || worldName.isEmpty()) {
            getLogger().severe("Configured world name is empty.");
            return Optional.empty();
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            getLogger().severe("LuckySky world '" + worldName + "' not loaded.");
            return Optional.empty();
        }
        return Optional.of(world);
    }

    public FileConfiguration getConfigData() {
        return getConfig();
    }
}
