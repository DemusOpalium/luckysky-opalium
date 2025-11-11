package de.opalium.luckysky.world;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.WorldsConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;

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
        MultiverseHook multiverse;
        try {
            multiverse = resolveMultiverse();
        } catch (ProvisioningException ex) {
            return ProvisioningResult.failure(ex.getMessage());
        }
        try {
            return provision(templateZip, blueprint, multiverse);
        } catch (IOException ex) {
            return ProvisioningResult.failure("Fehler beim Entpacken: " + ex.getMessage());
        } catch (ProvisioningException ex) {
            return ProvisioningResult.failure(ex.getMessage());
        }
    }

    private ProvisioningResult provision(Path templateZip, WorldBlueprint blueprint, MultiverseHook multiverse)
            throws IOException, ProvisioningException {
        String worldName = blueprint.worldName();

        unloadExistingWorld(multiverse, worldName);
        extractTemplate(templateZip, worldName);

        boolean imported = multiverse.importWorld(worldName, blueprint.environment(), blueprint.worldType());
        if (!imported) {
            return ProvisioningResult.failure("Multiverse konnte die Welt nicht importieren: " + worldName);
        }
        if (!multiverse.loadWorld(worldName)) {
            return ProvisioningResult.failure("Multiverse meldet Ladefehler für: " + worldName);
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return ProvisioningResult.failure("Welt nach Import nicht geladen: " + worldName);
        }
        Object mvWorld = multiverse.getMVWorld(worldName);
        applyBlueprint(world, mvWorld, blueprint, multiverse);
        return ProvisioningResult.success("LuckySky bereitgestellt aus Template um " + Instant.now());
    }

    private void unloadExistingWorld(MultiverseHook multiverse, String worldName)
            throws IOException, ProvisioningException {
        if (multiverse.isMVWorld(worldName)) {
            multiverse.unloadWorld(worldName);
            multiverse.removeWorldFromConfig(worldName);
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
        Files.createDirectories(worldDir);
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(templateZip))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String normalized = entry.getName().replace('\\', '/');
                if (normalized.startsWith(worldName + "/")) {
                    normalized = normalized.substring(worldName.length() + 1);
                }
                normalized = stripUnsafeSegments(normalized);
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
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    copyEntry(in, target);
                }
                in.closeEntry();
            }
        }
        normalizeExtractedWorld(worldDir);
    }

    private void copyEntry(InputStream in, Path target) throws IOException {
        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void applyBlueprint(World world, Object mvWorld, WorldBlueprint blueprint, MultiverseHook multiverse)
            throws ProvisioningException {
        WorldBlueprint.Spawn spawn = blueprint.spawn();
        Location spawnLocation = new Location(world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
        world.setSpawnLocation(spawnLocation);
        if (mvWorld != null) {
            boolean spawnApplied = multiverse.setSpawnLocation(world.getName(), mvWorld, spawnLocation);
            if (!spawnApplied) {
                logger.warning("Konnte Spawnpoint nicht auf Multiverse-Welt anwenden – nutze Bukkit-Spawn als Fallback.");
            }
            if (!blueprint.autoLoad()) {
                logger.warning("Blueprint deaktiviert autoLoad für LuckySky – setze dennoch autoLoad=true.");
            }
            boolean autoLoadApplied = multiverse.setAutoLoad(world.getName(), mvWorld, true);
            if (!autoLoadApplied) {
                logger.warning("Multiverse akzeptierte autoLoad=true nicht – bitte Konfiguration prüfen.");
            }
            boolean keepSpawnApplied = multiverse.setKeepSpawnLoaded(world.getName(), mvWorld, blueprint.keepSpawnLoaded());
            if (!keepSpawnApplied) {
                logger.warning("Multiverse akzeptierte keepSpawnLoaded nicht – bitte Konfiguration prüfen.");
            }
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
            multiverse.saveWorldsConfig();
        }
    }

    private MultiverseHook resolveMultiverse() throws ProvisioningException {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Multiverse-Core")) {
            throw new ProvisioningException("Multiverse-Core nicht geladen oder nicht verfügbar.");
        }
        return MultiverseHook.create(plugin);
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

    private String stripUnsafeSegments(String path) {
        if (path.isEmpty()) {
            return path;
        }
        String cleaned = path.replace("..", "");
        while (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned;
    }

    private void normalizeExtractedWorld(Path worldDir) throws IOException {
        Path levelDat = worldDir.resolve("level.dat");
        if (Files.exists(levelDat)) {
            return;
        }
        List<Path> children = new ArrayList<>();
        try (var stream = Files.list(worldDir)) {
            stream.forEach(children::add);
        }
        if (children.size() != 1 || !Files.isDirectory(children.get(0))) {
            return;
        }
        Path nested = children.get(0);
        if (!Files.exists(nested.resolve("level.dat"))) {
            return;
        }
        moveDirectoryContents(nested, worldDir);
        deleteRecursively(nested);
    }

    private void moveDirectoryContents(Path source, Path target) throws IOException {
        try (var stream = Files.walk(source)) {
            stream.sorted((a, b) -> Integer.compare(a.getNameCount(), b.getNameCount()))
                    .forEach(path -> {
                        if (Files.isDirectory(path)) {
                            return;
                        }
                        Path relative = source.relativize(path);
                        Path destination = target.resolve(relative);
                        try {
                            Path parent = destination.getParent();
                            if (parent != null) {
                                Files.createDirectories(parent);
                            }
                            Files.move(path, destination, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ex) {
                            throw new IllegalStateException("Konnte Datei nicht verschieben: " + path + " -> "
                                    + destination + " – " + ex.getMessage(), ex);
                        }
                    });
        } catch (IllegalStateException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            throw new IOException(ex.getMessage(), ex);
        }
    }

    private static final class MultiverseHook {
        private final Object manager;
        private final MethodWrapper isMVWorld;
        private final MethodWrapper unloadWorld;
        private final MethodWrapper removeWorldFromConfig;
        private final MethodWrapper importWorld;
        private final MethodWrapper loadWorld;
        private final MethodWrapper getMVWorld;
        private final MethodWrapper saveWorldsConfig;
        private final MethodWrapper mvSetSpawnLocation;
        private final MethodWrapper managerSetSpawnLocation;
        private final MethodWrapper mvSetAutoLoad;
        private final MethodWrapper managerSetAutoLoad;
        private final MethodWrapper mvSetKeepSpawn;
        private final MethodWrapper managerSetKeepSpawn;

        private MultiverseHook(Object manager,
                MethodWrapper isMVWorld,
                MethodWrapper unloadWorld,
                MethodWrapper removeWorldFromConfig,
                MethodWrapper importWorld,
                MethodWrapper loadWorld,
                MethodWrapper getMVWorld,
                MethodWrapper saveWorldsConfig,
                MethodWrapper mvSetSpawnLocation,
                MethodWrapper managerSetSpawnLocation,
                MethodWrapper mvSetAutoLoad,
                MethodWrapper managerSetAutoLoad,
                MethodWrapper mvSetKeepSpawn,
                MethodWrapper managerSetKeepSpawn) {
            this.manager = manager;
            this.isMVWorld = isMVWorld;
            this.unloadWorld = unloadWorld;
            this.removeWorldFromConfig = removeWorldFromConfig;
            this.importWorld = importWorld;
            this.loadWorld = loadWorld;
            this.getMVWorld = getMVWorld;
            this.saveWorldsConfig = saveWorldsConfig;
            this.mvSetSpawnLocation = mvSetSpawnLocation;
            this.managerSetSpawnLocation = managerSetSpawnLocation;
            this.mvSetAutoLoad = mvSetAutoLoad;
            this.managerSetAutoLoad = managerSetAutoLoad;
            this.mvSetKeepSpawn = mvSetKeepSpawn;
            this.managerSetKeepSpawn = managerSetKeepSpawn;
        }

        static MultiverseHook create(LuckySkyPlugin plugin) throws ProvisioningException {
            var pluginManager = plugin.getServer().getPluginManager();
            var multiversePlugin = pluginManager.getPlugin("Multiverse-Core");
            if (multiversePlugin == null) {
                throw new ProvisioningException("Multiverse-Core nicht geladen oder nicht verfügbar.");
            }
            try {
                Object core = multiversePlugin;
                MethodWrapper getMVWorldManager = MethodWrapper.of(core.getClass(), "getMVWorldManager");
                Object manager = getMVWorldManager.invoke(core);
                Class<?> managerClass = manager.getClass();

                MethodWrapper isMVWorld = MethodWrapper.of(managerClass, "isMVWorld", String.class);
                MethodWrapper unloadWorld = MethodWrapper.of(managerClass, "unloadWorld", String.class);
                MethodWrapper removeWorld = MethodWrapper.of(managerClass, "removeWorldFromConfig", String.class);
                MethodWrapper importWorld = MethodWrapper.of(managerClass, "importWorld", String.class,
                        Environment.class, WorldType.class, String.class);
                MethodWrapper loadWorld = MethodWrapper.of(managerClass, "loadWorld", String.class);
                MethodWrapper getMVWorld = MethodWrapper.of(managerClass, "getMVWorld", String.class);
                MethodWrapper saveWorldsConfig = MethodWrapper.of(managerClass, "saveWorldsConfig");

                Class<?> mvWorldClass = getMVWorld.method().getReturnType();
                MethodWrapper mvSetSpawn = MethodWrapper.optional(mvWorldClass, "setSpawnLocation", Location.class);
                MethodWrapper managerSetSpawn = MethodWrapper.optional(managerClass, "setSpawnLocation", String.class,
                        Location.class);
                MethodWrapper mvSetAutoLoad = MethodWrapper.optional(mvWorldClass, "setAutoLoad", boolean.class);
                MethodWrapper managerSetAutoLoad = MethodWrapper.optional(managerClass, "setAutoLoad", String.class,
                        boolean.class);
                MethodWrapper mvSetKeep = MethodWrapper.optional(mvWorldClass, "setKeepSpawnInMemory", boolean.class);
                MethodWrapper managerSetKeep = MethodWrapper.optional(managerClass, "setKeepSpawnInMemory",
                        String.class, boolean.class);

                return new MultiverseHook(manager, isMVWorld, unloadWorld, removeWorld, importWorld, loadWorld,
                        getMVWorld, saveWorldsConfig, mvSetSpawn, managerSetSpawn, mvSetAutoLoad, managerSetAutoLoad,
                        mvSetKeep, managerSetKeep);
            } catch (ReflectiveOperationException ex) {
                throw new ProvisioningException("Multiverse-Core API nicht kompatibel: " + ex.getMessage(), ex);
            }
        }

        boolean isMVWorld(String worldName) throws ProvisioningException {
            return MethodWrapper.asBoolean(isMVWorld.invoke(manager, worldName));
        }

        void unloadWorld(String worldName) throws ProvisioningException {
            unloadWorld.invoke(manager, worldName);
        }

        void removeWorldFromConfig(String worldName) throws ProvisioningException {
            removeWorldFromConfig.invoke(manager, worldName);
        }

        boolean importWorld(String worldName, Environment environment, WorldType worldType)
                throws ProvisioningException {
            return MethodWrapper.asBoolean(importWorld.invoke(manager, worldName, environment, worldType, null));
        }

        boolean loadWorld(String worldName) throws ProvisioningException {
            return MethodWrapper.asBoolean(loadWorld.invoke(manager, worldName));
        }

        Object getMVWorld(String worldName) throws ProvisioningException {
            return getMVWorld.invoke(manager, worldName);
        }

        void saveWorldsConfig() throws ProvisioningException {
            saveWorldsConfig.invoke(manager);
        }

        boolean setSpawnLocation(String worldName, Object mvWorld, Location location) throws ProvisioningException {
            boolean applied = false;
            if (mvSetSpawnLocation != null && mvWorld != null) {
                mvSetSpawnLocation.invoke(mvWorld, location);
                applied = true;
            }
            if (!applied && managerSetSpawnLocation != null) {
                managerSetSpawnLocation.invoke(manager, worldName, location);
                applied = true;
            }
            return applied;
        }

        boolean setAutoLoad(String worldName, Object mvWorld, boolean value) throws ProvisioningException {
            boolean applied = false;
            if (mvSetAutoLoad != null && mvWorld != null) {
                mvSetAutoLoad.invoke(mvWorld, value);
                applied = true;
            }
            if (!applied && managerSetAutoLoad != null) {
                managerSetAutoLoad.invoke(manager, worldName, value);
                applied = true;
            }
            return applied;
        }

        boolean setKeepSpawnLoaded(String worldName, Object mvWorld, boolean value) throws ProvisioningException {
            boolean applied = false;
            if (mvSetKeepSpawn != null && mvWorld != null) {
                mvSetKeepSpawn.invoke(mvWorld, value);
                applied = true;
            }
            if (!applied && managerSetKeepSpawn != null) {
                managerSetKeepSpawn.invoke(manager, worldName, value);
                applied = true;
            }
            return applied;
        }
    }

    private static final class MethodWrapper {
        private final java.lang.reflect.Method method;

        private MethodWrapper(java.lang.reflect.Method method) {
            this.method = method;
        }

        static MethodWrapper of(Class<?> type, String name, Class<?>... parameterTypes)
                throws NoSuchMethodException {
            return new MethodWrapper(type.getMethod(name, parameterTypes));
        }

        static MethodWrapper optional(Class<?> type, String name, Class<?>... parameterTypes) {
            try {
                return of(type, name, parameterTypes);
            } catch (NoSuchMethodException ex) {
                return null;
            }
        }

        Object invoke(Object target, Object... args) throws ProvisioningException {
            try {
                return method.invoke(target, args);
            } catch (ReflectiveOperationException ex) {
                throw new ProvisioningException("Multiverse-Aufruf fehlgeschlagen: " + method.getName() + " – "
                        + ex.getMessage(), ex);
            }
        }

        java.lang.reflect.Method method() {
            return method;
        }

        static boolean asBoolean(Object value) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            return true;
        }
    }

    private static final class ProvisioningException extends Exception {
        ProvisioningException(String message) {
            super(message);
        }

        ProvisioningException(String message, Throwable cause) {
            super(message, cause);
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
