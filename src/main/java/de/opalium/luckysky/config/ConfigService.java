package de.opalium.luckysky.config;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.model.ArenaCfg;
import de.opalium.luckysky.config.model.DuelsConfig;
import de.opalium.luckysky.config.model.GameConfig;
import de.opalium.luckysky.config.model.LevelCfg;
import de.opalium.luckysky.config.model.MessagesCfg;
import de.opalium.luckysky.config.model.TrapsCfg;
import de.opalium.luckysky.config.model.WorldsCfg;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigService {
    private static final String CONFIG_FILE = "config.yml";
    private static final String MESSAGES_FILE = "messages.yml";
    private static final String DUELS_FILE = "duels.yml";
    private static final String WORLDS_FILE = "worlds.yml";
    private static final String TRAPS_FILE = "traps.yml";
    private static final String ARENAS_FOLDER = "arenas";
    private static final String LEVELS_FOLDER = "levels";

    private final LuckySkyPlugin plugin;
    private final Logger logger;

    private YamlConfiguration configYaml;
    private YamlConfiguration messagesYaml;
    private YamlConfiguration duelsYaml;
    private YamlConfiguration worldsYaml;
    private YamlConfiguration trapsYaml;

    private GameConfig gameConfig;
    private MessagesCfg messagesCfg;
    private DuelsConfig duelsConfig;
    private WorldsCfg worldsCfg;
    private TrapsCfg trapsCfg;
    private Map<String, ArenaCfg> arenas;
    private Map<String, LevelCfg> levels;

    public ConfigService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void load() {
        ensureDefaults();
        reload();
    }

    public void reload() {
        this.configYaml = loadYaml(CONFIG_FILE);
        this.messagesYaml = loadYaml(MESSAGES_FILE);
        this.duelsYaml = loadYaml(DUELS_FILE);
        this.worldsYaml = loadYaml(WORLDS_FILE);
        this.trapsYaml = loadYaml(TRAPS_FILE);

        this.gameConfig = GameConfig.from(configYaml);
        this.messagesCfg = MessagesCfg.from(messagesYaml);
        this.duelsConfig = DuelsConfig.from(duelsYaml);
        this.worldsCfg = WorldsCfg.from(worldsYaml, logger);
        this.trapsCfg = TrapsCfg.from(trapsYaml);
        this.arenas = loadArenas(ARENAS_FOLDER, file -> ArenaCfg.from(file.getName().replaceFirst("\\.yml$", ""),
                YamlConfiguration.loadConfiguration(file), logger));
        this.levels = loadLevels(LEVELS_FOLDER, file -> LevelCfg.from(file.getName().replaceFirst("\\.yml$", ""),
                YamlConfiguration.loadConfiguration(file)));
    }

    public void saveAll() {
        saveYaml(configYaml, CONFIG_FILE);
        saveYaml(messagesYaml, MESSAGES_FILE);
        saveYaml(duelsYaml, DUELS_FILE);
        saveYaml(worldsYaml, WORLDS_FILE);
        saveYaml(trapsYaml, TRAPS_FILE);
    }

    public GameConfig game() {
        return gameConfig;
    }

    public MessagesCfg messages() {
        return messagesCfg;
    }

    public DuelsConfig duels() {
        return duelsConfig;
    }

    public WorldsCfg worlds() {
        return worldsCfg;
    }

    public TrapsCfg traps() {
        return trapsCfg;
    }

    public Map<String, ArenaCfg> arenas() {
        return arenas;
    }

    public Map<String, LevelCfg> levels() {
        return levels;
    }

    public void setTauntsEnabled(boolean enabled) {
        configYaml.set("withers.taunts.enable", enabled);
        saveYaml(configYaml, CONFIG_FILE);
    }

    public void setWitherEnabled(boolean enabled) {
        configYaml.set("withers.enable", enabled);
        saveYaml(configYaml, CONFIG_FILE);
    }

    public void setLuckyVariant(String variant) {
        if (worldsCfg == null) {
            return;
        }
        if (!worldsCfg.primary().lucky().variants().contains(variant)) {
            logger.warning("Lucky variant '" + variant + "' is not listed for primary world; applying anyway.");
        }
        String path = String.format("worlds.%s.lucky.variant", worldsCfg.primaryId());
        worldsYaml.set(path, variant);
        saveYaml(worldsYaml, WORLDS_FILE);
    }

    private void ensureDefaults() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            logger.warning("Failed to create plugin data folder.");
        }
        saveDefault(CONFIG_FILE);
        saveDefault(MESSAGES_FILE);
        saveDefault(DUELS_FILE);
        saveDefault(WORLDS_FILE);
        saveDefault(TRAPS_FILE);
        saveDefault(ARENAS_FOLDER + "/default.yml");
        saveDefault(LEVELS_FOLDER + "/default.yml");
    }

    private void saveDefault(String resource) {
        File target = new File(plugin.getDataFolder(), resource);
        if (!target.exists()) {
            plugin.saveResource(resource, false);
        }
    }

    private YamlConfiguration loadYaml(String resource) {
        File file = new File(plugin.getDataFolder(), resource);
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        return yaml;
    }

    private Map<String, ArenaCfg> loadArenas(String folder, ArenaLoader loader) {
        File dir = new File(plugin.getDataFolder(), folder);
        Map<String, ArenaCfg> map = new LinkedHashMap<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    try {
                        ArenaCfg value = loader.load(file);
                        map.put(value.id(), value);
                    } catch (Exception ex) {
                        logger.warning("Failed to load " + folder + " file '" + file.getName() + "': " + ex.getMessage());
                    }
                }
            }
        }
        return Map.copyOf(map);
    }

    private Map<String, LevelCfg> loadLevels(String folder, LevelLoader loader) {
        File dir = new File(plugin.getDataFolder(), folder);
        Map<String, LevelCfg> map = new LinkedHashMap<>();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    try {
                        LevelCfg value = loader.load(file);
                        map.put(value.id(), value);
                    } catch (Exception ex) {
                        logger.warning("Failed to load " + folder + " file '" + file.getName() + "': " + ex.getMessage());
                    }
                }
            }
        }
        return Map.copyOf(map);
    }

    private void saveYaml(YamlConfiguration yaml, String resource) {
        if (yaml == null) {
            return;
        }
        File file = new File(plugin.getDataFolder(), resource);
        try {
            yaml.save(file);
        } catch (IOException ex) {
            logger.severe("Failed to save " + resource + ": " + ex.getMessage());
        }
    }

    @FunctionalInterface
    private interface ArenaLoader {
        ArenaCfg load(File file) throws Exception;
    }

    @FunctionalInterface
    private interface LevelLoader {
        LevelCfg load(File file) throws Exception;
    }
}
