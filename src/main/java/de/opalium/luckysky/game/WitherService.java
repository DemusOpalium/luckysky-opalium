package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.config.GameConfig;
import de.opalium.luckysky.config.MessagesConfig;
import de.opalium.luckysky.config.TrapsConfig;
import de.opalium.luckysky.config.WorldsConfig;
import de.opalium.luckysky.util.Msg;
import de.opalium.luckysky.util.Worlds;
import java.util.List;
import java.util.concurrent.ExecutionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
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
        scheduleSpawn(traps.withers().spawnAfterMinutes());

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
    // PLANUNG / ABBRUCH
    // ─────────────────────────────────────────────────────────────
    /** Plant den Wither-Spawn in X Minuten (überschreibt Config-Verzögerung). */
    public void scheduleSpawn(int minutes) {
        cancelSpawn();
        if (!witherEnabled || plugin.game().state() != GameState.RUNNING) return;
        if (minutes <= 0) {
            Bukkit.getScheduler().runTask(plugin, this::spawn);
            return;
        }
        long delay = minutes * 60L * 20L;
        spawnTimer = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this::spawn, delay);
    }

    /** Bricht geplanten Wither-Spawn ab. */
    public void cancelSpawn() {
        if (spawnTimer != -1) {
            Bukkit.getScheduler().cancelTask(spawnTimer);
            spawnTimer = -1;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // ÖFFENTLICHE STEUERUNGEN
    // ─────────────────────────────────────────────────────────────
    /** Löst eine Spawn-Anfrage aus und liefert Ergebnisstatus. */
    public SpawnRequestResult requestSpawn(SpawnTrigger trigger) {
        if (!witherEnabled) {
            return SpawnRequestResult.WITHER_DISABLED;
        }
        if (plugin.game().state() != GameState.RUNNING) {
            return SpawnRequestResult.GAME_NOT_RUNNING;
        }
        // Falls später Wither-Spawn-Mode in der Config ausgewertet wird,
        // kann hier selektiv SKIPPED_BY_MODE zurückgegeben werden.
        if (!shouldTrigger(trigger)) {
            return SpawnRequestResult.SKIPPED_BY_MODE;
        }
        boolean ok = spawnNow();
        return ok ? SpawnRequestResult.ACCEPTED : SpawnRequestResult.FAILED;
    }

    /** Sofort spawnen, thread-sicher. */
    public boolean spawnNow() {
        // Timer stoppen, um Doppelspawns zu vermeiden
        stop();

        boolean spawned;
        if (Bukkit.isPrimaryThread()) {
            spawned = spawn();
        } else {
            try {
                spawned = Bukkit.getScheduler().callSyncMethod(plugin, this::spawn).get();
            } catch (InterruptedException | ExecutionException e) {
                plugin.getLogger().warning("[LuckySky] Sync-Spawn fehlgeschlagen: " + e.getMessage());
                spawned = false;
            }
        }

        // Taunts nur reaktivieren, wenn wirklich gespawnt wurde und Game läuft
        if (spawned && tauntsEnabled && plugin.game().state() == GameState.RUNNING) {
            setTauntsEnabled(true);
        }
        return spawned;
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
        if (!witherEnabled || plugin.game().state() != GameState.RUNNING) {
            return false;
        }

        World world = Worlds.require(worldConfig().worldName());

        // Guards gegen Fehlkonfig
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            plugin.getLogger().info("[LuckySky] Wither-Spawn abgebrochen: Difficulty=PEACEFUL in " + world.getName());
            return false;
        }
        Boolean doMobSpawning = world.getGameRuleValue(GameRule.DO_MOB_SPAWNING);
        if (Boolean.FALSE.equals(doMobSpawning)) {
            plugin.getLogger().info("[LuckySky] Wither-Spawn abgebrochen: doMobSpawning=false in " + world.getName());
            return false;
        }

        GameConfig.Position pos = plugin.configs().game().lucky().position();
        Location location = new Location(world, pos.x(), pos.y(), pos.z() - 6);

        try {
            Wither wither = (Wither) world.spawnEntity(location, EntityType.WITHER);
            wither.setCustomNameVisible(true);
            wither.customName(Component.text("Abyssal Wither", NamedTextColor.DARK_PURPLE));
            Bukkit.broadcastMessage(Msg.color(messages().prefix() + "&c☠ Abyssal Wither ist erwacht!"));
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("[LuckySky] Wither-Spawn Exception: " + t.getMessage());
            return false;
        }
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

    private boolean shouldTrigger(SpawnTrigger trigger) {
        // Platzhalter: aktuell keine Mode-Logik → immer true,
        // außer MANUAL ist explizit angefragt, dann auch true.
        // Später ggf. über GameConfig.Wither-Mode selektieren.
        return true;
    }

    private TrapsConfig traps() { return plugin.configs().traps(); }
    private WorldsConfig.LuckyWorld worldConfig() { return plugin.configs().worlds().luckySky(); }
    private MessagesConfig messages() { return plugin.configs().messages(); }

    // ─────────────────────────────────────────────────────────────
    // ENUMS
    // ─────────────────────────────────────────────────────────────
    public enum SpawnTrigger {
        MANUAL,
        START,
        TIMEOUT
    }

    public enum SpawnRequestResult {
        ACCEPTED,
        GAME_NOT_RUNNING,
        WITHER_DISABLED,
        SKIPPED_BY_MODE,
        FAILED
    }
}
