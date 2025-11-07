package de.opalium.luckysky.config;

import de.opalium.luckysky.LuckySkyPlugin;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigService {
    private static final List<String> FILES = List.of(
            "messages.yml",
            "worlds.yml",
            "game.yml",
            "duels.yml",
            "traps.yml"
    );

    private final LuckySkyPlugin plugin;
    private MessagesConfig messages;
    private WorldsConfig worlds;
    private GameConfig game;
    private DuelsConfig duels;
    private TrapsConfig traps;

    public ConfigService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public ConfigService ensureDefaults() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        for (String file : FILES) {
            File target = new File(dataFolder, file);
            if (!target.exists()) {
                plugin.saveResource(file, false);
            }
        }
        return this;
    }

    public ConfigService loadAll() {
        this.messages = load("messages.yml", MessagesConfig::from);
        this.worlds = load("worlds.yml", WorldsConfig::from);
        this.game = load("game.yml", GameConfig::from);
        this.duels = load("duels.yml", DuelsConfig::from);
        this.traps = load("traps.yml", TrapsConfig::from);
        return this;
    }

    private <T> T load(String fileName, Loader<T> loader) {
        File file = new File(plugin.getDataFolder(), fileName);
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        return loader.load(configuration);
    }

    public void reloadAll() {
        loadAll();
    }

    public void saveAll() {
        save("messages.yml", messages::writeTo);
        save("worlds.yml", worlds::writeTo);
        save("game.yml", game::writeTo);
        save("duels.yml", duels::writeTo);
        save("traps.yml", traps::writeTo);
    }

    private void save(String fileName, Writer writer) {
        File file = new File(plugin.getDataFolder(), fileName);
        FileConfiguration configuration = new YamlConfiguration();
        writer.write(configuration);
        try {
            configuration.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + fileName + ": " + e.getMessage());
        }
    }

    public MessagesConfig messages() {
        return messages;
    }

    public WorldsConfig worlds() {
        return worlds;
    }

    public GameConfig game() {
        return game;
    }

    public DuelsConfig duels() {
        return duels;
    }

    public TrapsConfig traps() {
        return traps;
    }

    public void updateTraps(TrapsConfig traps) {
        this.traps = traps;
        save("traps.yml", traps::writeTo);
    }

    public void updateGame(GameConfig game) {
        this.game = game;
        save("game.yml", game::writeTo);
    }

    public void updateDuels(DuelsConfig duels) {
        this.duels = duels;
        save("duels.yml", duels::writeTo);
    }

    @FunctionalInterface
    private interface Loader<T> {
        T load(FileConfiguration configuration);
    }

    @FunctionalInterface
    private interface Writer {
        void write(FileConfiguration configuration);
    }
}
