package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.GameConfig.WitherSpawnMode;
import de.opalium.luckysky.config.MessagesConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.util.List;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;

public class WitherService {
    private final LuckySkyPlugin plugin;
    private int spawnTimer = -1;
    private int tauntTimer = -1;
    private boolean witherEnabled;
    private boolean tauntsEnabled;

    public WitherService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        TrapsConfig traps = traps();
        this.witherEnabled = traps.withers().enabled();
        this.tauntsEnabled = traps.withers().taunts().enabled();
    }

    // ─────────────────────────────────────────────────────────────
    // LEBENSZYKLUS
    // ─────────────────────────────────────────────────────────────
    public void start() {
        stop(); // Timer säubern
        TrapsConfig traps = traps();
        witherEnabled = traps.withers().enabled();
        tauntsEnabled = traps.withers().taunts().enabled();
        if (!witherEnabled) return;

        // einheitlich über scheduleSpawn(...)
        if (witherConfig().spawnMode().usesSchedule()) {
            scheduleSpawn(traps.withers().spawnAfterMinutes());
        }

        if (tauntsEnabled) {
            tauntTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin, this::taunt,
                traps.withers().taunts().everyTicks(),
                traps.withers().taunts().everyTicks()
            );
        }
    }

    public void stop() {
        cancelSpawn();
        if (tauntTimer != -1) {
            Bukkit.getScheduler().cancelTask(tauntTimer);
            tauntTimer = -1;
        }
    }

    public void reload() {
        if (plugin.game().state() == GameState.RUNNING) {
            start();
        } else {
            TrapsConfig traps = traps();
            witherEnabled = traps.withers().enabled();
            tauntsEnabled = traps.withers().taunts().enabled();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // NEU: EXPLIZITE PLANUNG / ABBRUCH
    // ─────────────────────────────────────────────────────────────
    /** Plant den Wither-Spawn in X Minuten (überschreibt Config-Verzögerung). */
    public void scheduleSpawn(int minutes) {
        cancelSpawn();
        if (!witherEnabled || plugin.game().state() != GameState.RUNNING) return;
        if (!witherConfig().spawnMode().usesSchedule()) {
            return;
        }
        if (minutes <= 0) {
            // sofort spawnen (gleiches Verhalten wie spawnNow, aber ohne Taunt-Neuaufbau)
            Bukkit.getScheduler().runTask(plugin, () -> spawn());
            return;
        }
        long delay = minutes * 60L * 20L;
        spawnTimer = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> spawn(), delay);
    }

    /** Bricht geplanten Wither-Spawn ab. */
    public void cancelSpawn() {
        if (spawnTimer != -1) {
            Bukkit.getScheduler().cancelTask(spawnTimer);
            spawnTimer = -1;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // STEUERUNGEN
    // ─────────────────────────────────────────────────────────────
    public void spawnNow() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            cancelSpawn();
            boolean resumeTaunts = tauntTimer != -1;
            if (tauntTimer != -1) {
                Bukkit.getScheduler().cancelTask(tauntTimer);
                tauntTimer = -1;
            }
            boolean spawned = spawn();
            if ((spawned || resumeTaunts) && tauntsEnabled && plugin.game().state() == GameState.RUNNING) {
                setTauntsEnabled(true);
            }
        });
    }

    public void setWitherEnabled(boolean enabled) {
        this.witherEnabled = enabled;
        if (!enabled) {
            stop();
        } else if (plugin.game().state() == GameState.RUNNING) {
            start();
        }
    }

    public void setTauntsEnabled(boolean enabled) {
        this.tauntsEnabled = enabled;
        if (!enabled && tauntTimer != -1) {
            Bukkit.getScheduler().cancelTask(tauntTimer);
            tauntTimer = -1;
        } else if (enabled && plugin.game().state() == GameState.RUNNING) {
            TrapsConfig traps = traps();
            if (tauntTimer != -1) Bukkit.getScheduler().cancelTask(tauntTimer);
            tauntTimer = Bukkit.getScheduler().scheduleSyncRepeatingTask(
                plugin, this::taunt,
                traps.withers().taunts().everyTicks(),
                traps.withers().taunts().everyTicks()
            );
        }
    }

    // ─────────────────────────────────────────────────────────────
    // INTERN
    // ─────────────────────────────────────────────────────────────
    private boolean spawn() {
        Logger logger = plugin.getLogger();
        if (!witherEnabled) {
            logger.info("[LuckySky] Wither-Spawn abgebrochen: deaktiviert.");
            return false;
        }
        if (plugin.game().state() != GameState.RUNNING) {
            logger.info("[LuckySky] Wither-Spawn abgebrochen: Spiel läuft nicht.");
            return false;
        }

        GameConfig.Wither settings = witherConfig();
        GameConfig.Position base = plugin.configs().game().lucky().position();
        World world = ensureWorldLoaded();
        if (world == null) {
            return false;
        }
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            logger.info("[LuckySky] Wither-Spawn abgebrochen: Schwierigkeit PEACEFUL in " + world.getName() + ".");
            return false;
        }
        Boolean doMobSpawning = world.getGameRuleValue(GameRule.DO_MOB_SPAWNING);
        if (Boolean.FALSE.equals(doMobSpawning)) {
            logger.info("[LuckySky] Wither-Spawn abgebrochen: GameRule doMobSpawning ist deaktiviert in " + world.getName() + ".");
            return false;
        }

        if (settings.singleBoss()) {
            List<Wither> existing = world.getEntitiesByClass(Wither.class);
            if (!existing.isEmpty()) {
                existing.forEach(Wither::remove);
                logger.info("[LuckySky] " + existing.size() + " vorhandene(r) Wither entfernt (singleBoss=true).");
            }
        }

        Location location = new Location(world,
                base.x() + settings.offset().x(),
                settings.spawnY(),
                base.z() + settings.offset().z());

        Wither wither = (Wither) world.spawnEntity(location, EntityType.WITHER);
        wither.setCustomNameVisible(true);
        wither.customName(Component.text("Abyssal Wither", NamedTextColor.DARK_PURPLE));
        Bukkit.broadcastMessage(Msg.color(messages().prefix() + "&c☠ Abyssal Wither ist erwacht!"));
        return true;
    }

    private void taunt() {
        if (!tauntsEnabled || plugin.game().state() != GameState.RUNNING) return;
        World world = Worlds.require(worldConfig().worldName());
        if (world.getEntitiesByClass(Wither.class).isEmpty()) return;

        List<String> taunts = traps().withers().taunts().lines();
        if (taunts.isEmpty()) return;

        String line = taunts.get((int) (Math.random() * taunts.size()));
        Bukkit.broadcastMessage(Msg.color(messages().prefix() + "&c" + line));
    }

    private TrapsConfig traps() { return plugin.configs().traps(); }
    private WorldsConfig.LuckyWorld worldConfig() { return plugin.configs().worlds().luckySky(); }
    private MessagesConfig messages() { return plugin.configs().messages(); }

    private World ensureWorldLoaded() {
        WorldsConfig.LuckyWorld config = worldConfig();
        String worldName = config.worldName();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.createWorld(new WorldCreator(worldName));
        }
        if (world == null) {
            plugin.getLogger().info("[LuckySky] Wither-Spawn abgebrochen: Welt '" + worldName + "' nicht geladen.");
        }
        return world;
    }

    private GameConfig.Wither witherConfig() {
        return plugin.configs().game().wither();
    }

    public enum SpawnTrigger {
        MANUAL,
        START,
        TIMEOUT
    }

    public enum SpawnRequestResult {
        ACCEPTED,
        GAME_NOT_RUNNING,
        WITHER_DISABLED,
        SKIPPED_BY_MODE
    }

    public SpawnRequestResult requestSpawn(SpawnTrigger trigger) {
        if (!witherEnabled) {
            return SpawnRequestResult.WITHER_DISABLED;
        }
        if (plugin.game().state() != GameState.RUNNING) {
            return SpawnRequestResult.GAME_NOT_RUNNING;
        }
        if (!shouldTrigger(trigger)) {
            return SpawnRequestResult.SKIPPED_BY_MODE;
        }
        spawnNow();
        return SpawnRequestResult.ACCEPTED;
    }

    private boolean shouldTrigger(SpawnTrigger trigger) {
        WitherSpawnMode mode = witherConfig().spawnMode();
        return switch (trigger) {
            case MANUAL -> true;
            case START -> mode.spawnOnStart();
            case TIMEOUT -> mode.spawnOnTimeout();
        };
    }
}
