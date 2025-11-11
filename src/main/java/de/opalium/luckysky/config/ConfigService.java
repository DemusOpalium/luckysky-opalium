package de.opalium.luckysky.config;

import de.opalium.luckysky.LuckySkyPlugin;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;

public final class ConfigService {
    private static final String MASTER_CONFIG = "config/luckysky.yml";
    private static final List<String> ROOT_FILES = List.of(
            "messages.yml",
            "duels.yml",
            "traps.yml"
    );
    private static final List<String> GUI_FILES = List.of(
            "config/gui/luckysky-admin.yml",
            "config/gui/luckysky-player.yml",
            "config/gui/duels-admin.yml",
            "config/gui/duels-player.yml"
    );
    private static final List<String> LEGACY_FILES = List.of(
            "game.yml",
            "worlds.yml",
            "admin-gui.yml",
            "player-gui.yml",
            "npcs.yml"
    );

    private final LuckySkyPlugin plugin;
    private MessagesConfig messages;
    private WorldsConfig worlds;
    private GateConfig gate;
    private GameConfig game;
    private DuelsConfig duels;
    private TrapsConfig traps;
    private NpcConfig npcs;
    private YamlConfiguration masterConfig;
    private File masterFile;

    public ConfigService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public ConfigService ensureDefaults() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File configDir = new File(dataFolder, "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        File guiDir = new File(configDir, "gui");
        if (!guiDir.exists()) {
            guiDir.mkdirs();
        }

        ensureResource(MASTER_CONFIG);
        ROOT_FILES.forEach(this::ensureResource);
        GUI_FILES.forEach(this::ensureResource);

        deprecateLegacyFiles(dataFolder);
        return this;
    }

    public ConfigService loadAll() {
        this.masterFile = new File(plugin.getDataFolder(), MASTER_CONFIG);
        this.masterConfig = YamlConfiguration.loadConfiguration(masterFile);

        this.messages = load("messages.yml", MessagesConfig::from);
        this.duels = load("duels.yml", DuelsConfig::from);
        this.traps = load("traps.yml", TrapsConfig::from);

        ConfigurationSection worldSection = masterConfig.getConfigurationSection("world");
        this.worlds = WorldsConfig.fromSection(worldSection);

        ConfigurationSection gateSection = masterConfig.getConfigurationSection("gate");
        this.gate = GateConfig.from(gateSection);

        ConfigurationSection roundSection = masterConfig.getConfigurationSection("round");
        ConfigurationSection rewardSection = masterConfig.getConfigurationSection("reward");
        this.game = GameConfig.fromSections(roundSection, rewardSection);

        ConfigurationSection npcSection = masterConfig.getConfigurationSection("npc");
        this.npcs = NpcConfig.fromSection(npcSection);
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
        save("duels.yml", duels::writeTo);
        save("traps.yml", traps::writeTo);
        saveMaster();
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

    public GateConfig gate() {
        return gate;
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

    public NpcConfig npcs() {
        return npcs;
    }

    public void updateWorlds(WorldsConfig worlds) {
        this.worlds = worlds;
        saveMaster();
    }

    public void updateGate(GateConfig gate) {
        this.gate = gate;
        saveMaster();
    }

    public void updateTraps(TrapsConfig traps) {
        this.traps = traps;
        save("traps.yml", traps::writeTo);
    }

    public void updateGame(GameConfig game) {
        this.game = game;
        saveMaster();
    }

    public void updateDuels(DuelsConfig duels) {
        this.duels = duels;
        save("duels.yml", duels::writeTo);
    }

    public void updateNpcs(NpcConfig npcs) {
        this.npcs = npcs;
        saveMaster();
    }

    private void ensureResource(String relativePath) {
        File target = new File(plugin.getDataFolder(), relativePath);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        if (!target.exists()) {
            plugin.saveResource(relativePath, false);
        }
    }

    private void deprecateLegacyFiles(File dataFolder) {
        Logger logger = plugin.getLogger();
        for (String legacy : LEGACY_FILES) {
            File file = new File(dataFolder, legacy);
            if (!file.exists()) {
                continue;
            }
            File backup = new File(dataFolder, legacy + ".legacy");
            if (backup.exists()) {
                continue;
            }
            boolean renamed = file.renameTo(backup);
            if (renamed) {
                logger.info("[LuckySky] Legacy config '" + legacy + "' verschoben nach '" + backup.getName() + "'.");
            } else {
                logger.warning("[LuckySky] Legacy config '" + legacy + "' konnte nicht verschoben werden.");
            }
        }
    }

    private void saveMaster() {
        if (masterConfig == null) {
            masterConfig = new YamlConfiguration();
        }
        writeWorldSection();
        writeGateSection();
        writeRoundSection();
        writeRewardSection();
        writeNpcSection();
        if (masterFile == null) {
            masterFile = new File(plugin.getDataFolder(), MASTER_CONFIG);
        }
        try {
            masterConfig.save(masterFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + MASTER_CONFIG + ": " + e.getMessage());
        }
    }

    private void writeWorldSection() {
        masterConfig.set("world", null);
        if (worlds == null) {
            return;
        }
        ConfigurationSection section = masterConfig.createSection("world");
        worlds.writeTo(section);
    }

    private void writeGateSection() {
        masterConfig.set("gate", null);
        if (gate == null) {
            return;
        }
        ConfigurationSection section = masterConfig.createSection("gate");
        gate.writeTo(section);
    }

    private void writeRoundSection() {
        masterConfig.set("round", null);
        if (game == null) {
            return;
        }
        ConfigurationSection section = masterConfig.createSection("round");
        game.writeRound(section);
    }

    private void writeRewardSection() {
        masterConfig.set("reward", null);
        if (game == null) {
            return;
        }
        ConfigurationSection section = masterConfig.createSection("reward");
        game.writeRewards(section);
    }

    private void writeNpcSection() {
        masterConfig.set("npc", null);
        if (npcs == null) {
            return;
        }
        ConfigurationSection section = masterConfig.createSection("npc");
        npcs.writeTo(section);
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
