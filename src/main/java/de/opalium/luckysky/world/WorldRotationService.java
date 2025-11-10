package de.opalium.luckysky.world;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.MessagesConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.game.GameState;
import de.opalium.luckysky.game.PlatformService;
import de.opalium.luckysky.util.Msg;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WorldRotationService {
    private final LuckySkyPlugin plugin;
    private final PlatformService platformService;
    private final Path stateFile;
    private final AtomicBoolean rotationInProgress = new AtomicBoolean(false);

    private volatile String activeWorld;

    public WorldRotationService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.platformService = new PlatformService(plugin);
        this.stateFile = plugin.getDataFolder().toPath().resolve("rotation-state.txt");
        this.activeWorld = loadActiveWorld();
        applyActiveWorldToConfig();
        ensureWorldLoaded(activeWorld);
    }

    public String currentWorldName() {
        return activeWorld;
    }

    public void reload() {
        applyActiveWorldToConfig();
    }

    public void rotateWorlds(RotationTrigger trigger) {
        if (rotationInProgress.get()) {
            return;
        }
        WorldsConfig worlds = plugin.configs().worlds();
        WorldsConfig.Rotation rotationConfig = worlds.rotation();
        if (trigger == RotationTrigger.IDLE && !rotationConfig.rotateWhenIdle()) {
            return;
        }
        if (isGameRunning()) {
            plugin.getLogger().info("Skipping world rotation: game is running.");
            return;
        }

        rotationInProgress.set(true);
        String previousWorld = activeWorld;
        String nextWorld = determineNextWorld(rotationConfig);

        CompletableFuture<Void> preparation = CompletableFuture.runAsync(() -> {
            try {
                prepareWorld(nextWorld, rotationConfig);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });

        startCountdown(rotationConfig.countdownSeconds(), previousWorld, () ->
                preparation.whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        rotationInProgress.set(false);
                        plugin.getLogger().log(Level.SEVERE, "Could not prepare next world", throwable);
                        return;
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> performSwitch(previousWorld, nextWorld, rotationConfig));
                }));
    }

    private void performSwitch(String previousWorld, String nextWorld, WorldsConfig.Rotation rotationConfig) {
        try {
            World newWorld = ensureWorldLoaded(nextWorld);
            applyWorldSettings(newWorld, rotationConfig);
            List<Player> teleported = teleportPlayers(previousWorld, newWorld, rotationConfig);
            updateActiveWorld(nextWorld);
            broadcastTeleport(teleported);
            unloadWorld(previousWorld);
            CompletableFuture.runAsync(() -> {
                try {
                    prepareWorld(previousWorld, rotationConfig);
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE, "Could not restore inactive world", e);
                }
            });
        } finally {
            rotationInProgress.set(false);
        }
    }

    private void startCountdown(int seconds, String sourceWorld, Runnable onComplete) {
        int countdownSeconds = Math.max(0, seconds);
        if (countdownSeconds == 0) {
            onComplete.run();
            return;
        }
        MessagesConfig.Rotation messages = plugin.configs().messages().rotation();
        new BukkitRunnable() {
            private int remaining = countdownSeconds;

            @Override
            public void run() {
                if (remaining <= 0) {
                    cancel();
                    onComplete.run();
                    return;
                }
                broadcastFormatted(messages.countdownBroadcast(), remaining);
                sendTitle(sourceWorld, messages.countdownTitle(), messages.countdownSubtitle(), remaining);
                remaining--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void broadcastTeleport(List<Player> players) {
        MessagesConfig.Rotation messages = plugin.configs().messages().rotation();
        broadcastFormatted(messages.teleportBroadcast(), 0);
        String title = formatCountdown(messages.teleportTitle(), 0);
        String subtitle = formatCountdown(messages.teleportSubtitle(), 0);
        for (Player player : players) {
            player.sendTitle(Msg.color(title), Msg.color(subtitle), 10, 60, 10);
        }
    }

    private void broadcastFormatted(String template, int seconds) {
        String message = formatCountdown(template, seconds);
        String colored = Msg.color(plugin.configs().messages().prefix() + message);
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(colored));
        Bukkit.getConsoleSender().sendMessage(colored);
    }

    private void sendTitle(String worldName, String titleTemplate, String subtitleTemplate, int seconds) {
        String title = Msg.color(formatCountdown(titleTemplate, seconds));
        String subtitle = Msg.color(formatCountdown(subtitleTemplate, seconds));
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getWorld().getName().equalsIgnoreCase(worldName))
                .forEach(player -> player.sendTitle(title, subtitle, 0, 30, 5));
    }

    private String formatCountdown(String template, int seconds) {
        return template.replace("{seconds}", Integer.toString(seconds));
    }

    private World ensureWorldLoaded(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            return world;
        }
        WorldCreator creator = new WorldCreator(worldName);
        return Bukkit.createWorld(creator);
    }

    private void applyWorldSettings(World world, WorldsConfig.Rotation rotationConfig) {
        WorldsConfig.Border borderConfig = rotationConfig.border();
        WorldBorder border = world.getWorldBorder();
        border.setCenter(borderConfig.centerX(), borderConfig.centerZ());
        border.setSize(borderConfig.size());

        platformService.placeBase(world);
        platformService.placeExtended(world);

        WorldsConfig.Spawn spawn = rotationConfig.spawn();
        world.setSpawnLocation((int) Math.round(spawn.x()), (int) Math.round(spawn.y()), (int) Math.round(spawn.z()));
    }

    private List<Player> teleportPlayers(String previousWorld, World newWorld, WorldsConfig.Rotation rotationConfig) {
        List<Player> teleported = new ArrayList<>();
        WorldsConfig.Spawn spawn = rotationConfig.spawn();
        Location target = new Location(newWorld, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getName().equalsIgnoreCase(previousWorld)) {
                continue;
            }
            player.teleport(target);
            player.setBedSpawnLocation(target, true);
            teleported.add(player);
        }
        return teleported;
    }

    private void unloadWorld(String name) {
        World world = Bukkit.getWorld(name);
        if (world != null) {
            Bukkit.unloadWorld(world, true);
        }
    }

    private void updateActiveWorld(String worldName) {
        this.activeWorld = worldName;
        saveState(worldName);
        WorldsConfig config = plugin.configs().worlds();
        WorldsConfig.LuckyWorld old = config.luckySky();
        if (!old.worldName().equals(worldName)) {
            WorldsConfig.LuckyWorld updated = new WorldsConfig.LuckyWorld(worldName, old.spawn(), old.lobby(), old.lucky());
            plugin.configs().updateWorlds(config.withLuckyWorld(updated));
        }
    }

    private String determineNextWorld(WorldsConfig.Rotation rotationConfig) {
        String primary = rotationConfig.primary();
        String secondary = rotationConfig.secondary();
        if (activeWorld.equalsIgnoreCase(primary)) {
            return secondary;
        }
        if (activeWorld.equalsIgnoreCase(secondary)) {
            return primary;
        }
        return primary;
    }

    private void applyActiveWorldToConfig() {
        WorldsConfig config = plugin.configs().worlds();
        WorldsConfig.LuckyWorld lucky = config.luckySky();
        if (!lucky.worldName().equals(activeWorld)) {
            WorldsConfig.LuckyWorld updated = new WorldsConfig.LuckyWorld(activeWorld, lucky.spawn(), lucky.lobby(), lucky.lucky());
            plugin.configs().updateWorlds(config.withLuckyWorld(updated));
        }
    }

    private String loadActiveWorld() {
        try {
            if (Files.exists(stateFile)) {
                String value = Files.readString(stateFile, StandardCharsets.UTF_8).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read rotation state", e);
        }
        return plugin.configs().worlds().rotation().primary();
    }

    private void saveState(String worldName) {
        try {
            Files.createDirectories(stateFile.getParent());
            Files.writeString(stateFile, worldName, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not write rotation state", e);
        }
    }

    private void prepareWorld(String worldName, WorldsConfig.Rotation rotationConfig) throws IOException {
        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath();
        Path target = container.resolve(worldName);
        deleteDirectory(target);
        Path tempDir = Files.createTempDirectory(plugin.getDataFolder().toPath(), "rotation-");
        try {
            unzip(resolveBaseZip(rotationConfig.baseZip()), tempDir);
            Path contentRoot = resolveContentRoot(tempDir, worldName);
            copyDirectory(contentRoot, target);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private Path resolveBaseZip(String configured) {
        Path path = Path.of(configured);
        if (!path.isAbsolute()) {
            path = plugin.getDataFolder().toPath().resolve(path);
        }
        return path.normalize();
    }

    private Path resolveContentRoot(Path tempDir, String worldName) throws IOException {
        Path direct = tempDir.resolve(worldName);
        if (Files.isDirectory(direct)) {
            return direct;
        }
        try (var stream = Files.list(tempDir)) {
            List<Path> entries = stream.toList();
            if (entries.size() == 1 && Files.isDirectory(entries.get(0))) {
                return entries.get(0);
            }
        }
        return tempDir;
    }

    private void unzip(Path zipFile, Path destination) throws IOException {
        if (!Files.exists(zipFile)) {
            throw new IOException("Base zip not found: " + zipFile);
        }
        try (InputStream in = Files.newInputStream(zipFile);
             ZipInputStream zipInputStream = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    Path dir = destination.resolve(entry.getName());
                    Files.createDirectories(dir);
                } else {
                    Path file = destination.resolve(entry.getName());
                    Path parent = file.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    try (OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                        zipInputStream.transferTo(out);
                    }
                }
            }
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            throw new IOException("Source directory missing: " + source);
        }
        try (var stream = Files.walk(source)) {
            for (Path path : stream.toList()) {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative.toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Path parent = destination.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            List<Path> paths = stream.sorted((a, b) -> b.compareTo(a)).toList();
            for (Path entry : paths) {
                Files.deleteIfExists(entry);
            }
        }
    }

    private boolean isGameRunning() {
        if (plugin.game() == null) {
            return false;
        }
        return plugin.game().state() == GameState.RUNNING;
    }

    public enum RotationTrigger {
        GAME_END,
        IDLE
    }
}
