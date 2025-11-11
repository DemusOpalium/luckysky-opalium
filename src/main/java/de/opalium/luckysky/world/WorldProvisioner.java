package de.opalium.luckysky.world;

import com.onarandombox.multiversecore.MultiverseCore;
import com.onarandombox.multiversecore.api.MVWorldManager;
import com.onarandombox.multiversecore.api.MultiverseWorld;
import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.WorldsConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;

public final class WorldProvisioner {
    private static final String TEMPLATE_FILE = "LuckySkyBase.zip";
    private static final String BLUEPRINT_FILE = "LuckySkyBase.blueprint.yml";

    private final LuckySkyPlugin plugin;
    private final Logger logger;

    public WorldProvisioner(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public ProvisioningResult provisionLuckySky() {
        WorldsConfig.LuckyWorld config = plugin.configs().worlds().luckySky();
        Path templatesDir = plugin.getDataFolder().toPath().resolve("templates");
        Path templateZip = templatesDir.resolve(TEMPLATE_FILE);
        Path blueprintPath = templatesDir.resolve(BLUEPRINT_FILE);
        if (!Files.exists(templateZip)) {
            return ProvisioningResult.failure("Template fehlt: " + displayRelative(templateZip));
        }
        WorldBlueprint blueprint = WorldBlueprint.load(blueprintPath, config).orElse(WorldBlueprint.from(config));
        Optional<MultiverseCore> core = resolveMultiverse();
        if (core.isEmpty()) {
            return ProvisioningResult.failure("Multiverse-Core nicht geladen oder nicht verfügbar.");
        }
        try {
            return provision(templateZip, blueprint, core.get());
        } catch (IOException ex) {
            return ProvisioningResult.failure("Fehler beim Entpacken: " + ex.getMessage());
        }
    }

    private ProvisioningResult provision(Path templateZip, WorldBlueprint blueprint, MultiverseCore core)
            throws IOException {
        MVWorldManager manager = core.getMVWorldManager();
        String worldName = blueprint.worldName();

        unloadExistingWorld(manager, worldName);
        extractTemplate(templateZip, worldName);

        boolean imported = manager.importWorld(worldName, blueprint.environment(), blueprint.worldType(), null);
        if (!imported) {
            return ProvisioningResult.failure("Multiverse konnte die Welt nicht importieren: " + worldName);
        }
        if (!manager.loadWorld(worldName)) {
            return ProvisioningResult.failure("Multiverse meldet Ladefehler für: " + worldName);
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ProvisioningResult.failure("Welt nach Import nicht geladen: " + worldName);
        }
        MultiverseWorld mvWorld = manager.getMVWorld(worldName);
        applyBlueprint(world, mvWorld, blueprint, manager);
        return ProvisioningResult.success("LuckySky bereitgestellt aus Template um " + Instant.now());
    }

    private void unloadExistingWorld(MVWorldManager manager, String worldName) throws IOException {
        if (manager.isMVWorld(worldName)) {
            manager.unloadWorld(worldName);
            manager.removeWorldFromConfig(worldName);
        }
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }
        Path worldDir = Bukkit.getWorldContainer().toPath().resolve(worldName);
        if (Files.exists(worldDir)) {
            deleteRecursively(worldDir);
        }
    }

    private void extractTemplate(Path templateZip, String worldName) throws IOException {
        Path worldDir = Bukkit.getWorldContainer().toPath().resolve(worldName);
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(templateZip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String normalized = entry.getName().replace('\\', '/');
                if (normalized.startsWith(worldName + "/")) {
                    normalized = normalized.substring(worldName.length() + 1);
                }
                if (normalized.isEmpty()) {
                    continue;
                }
                Path target = worldDir.resolve(normalized).normalize();
                if (!target.startsWith(worldDir)) {
                    continue;
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    copyEntry(in, target);
                }
                in.closeEntry();
            }
        }
    }

    private void copyEntry(InputStream in, Path target) throws IOException {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void applyBlueprint(World world, MultiverseWorld mvWorld, WorldBlueprint blueprint, MVWorldManager manager) {
        WorldBlueprint.Spawn spawn = blueprint.spawn();
        Location spawnLocation = new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
        world.setSpawnLocation(spawnLocation);
        if (mvWorld != null) {
            mvWorld.setSpawnLocation(spawnLocation);
            if (!blueprint.autoLoad()) {
                logger.warning("Blueprint deaktiviert autoLoad für LuckySky – setze dennoch autoLoad=true.");
            }
            mvWorld.setAutoLoad(true);
            mvWorld.setKeepSpawnInMemory(blueprint.keepSpawnLoaded());
        }
        for (Map.Entry<String, String> entry : blueprint.gamerules().entrySet()) {
            String ruleName = entry.getKey();
            String value = entry.getValue();
            GameRule<?> gameRule = GameRule.getByName(ruleName);
            if (gameRule != null) {
                world.setGameRuleValue(ruleName, value);
            } else {
                logger.warning("Unbekannte Gamerule im Blueprint: " + ruleName);
            }
        }
        if (mvWorld != null) {
            manager.saveWorldsConfig();
        }
    }

    private Optional<MultiverseCore> resolveMultiverse() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
            return Optional.empty();
        }
        if (plugin.getServer().getPluginManager().getPlugin("Multiverse-Core") instanceof MultiverseCore core) {
            return Optional.of(core);
        }
        return Optional.empty();
    }

    private String displayRelative(Path path) {
        Path data = plugin.getDataFolder().toPath();
        try {
            return data.relativize(path).toString().replace('\\', '/');
        } catch (IllegalArgumentException ignored) {
            return path.toString();
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        try (var stream = Files.walk(directory)) {
            stream.sorted((a, b) -> Integer.compare(b.getNameCount(), a.getNameCount()))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            logger.warning("Konnte Datei nicht löschen: " + path + " – " + ex.getMessage());
                        }
                    });
        }
    }

    public record ProvisioningResult(boolean success, String message) {
        public static ProvisioningResult success(String message) {
            return new ProvisioningResult(true, message);
        }

        public static ProvisioningResult failure(String message) {
            return new ProvisioningResult(false, message);
        }
    }
}
