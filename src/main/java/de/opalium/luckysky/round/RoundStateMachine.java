package de.opalium.luckysky.round;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.world.WorldProvisioner;
import de.opalium.luckysky.world.WorldProvisioner.ProvisioningResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Logger;

public final class RoundStateMachine {
    private final LuckySkyPlugin plugin;
    private final WorldProvisioner provisioner;
    private final Logger logger;
    private final Path lockFile;

    private RoundState state = RoundState.IDLE;

    public RoundStateMachine(LuckySkyPlugin plugin, WorldProvisioner provisioner) {
        this.plugin = plugin;
        this.provisioner = provisioner;
        this.logger = plugin.getLogger();
        this.lockFile = plugin.getDataFolder().toPath().resolve("round.lock");
    }

    public synchronized RoundState state() {
        return state;
    }

    public synchronized boolean prepareLuckySky() {
        if (state == RoundState.RUNNING) {
            logger.warning("PREPARE abgebrochen: LuckySky läuft noch. Bitte zuerst stoppen.");
            return false;
        }
        state = RoundState.PREPARING;
        ProvisioningResult result = provisioner.provisionLuckySky();
        if (!result.success()) {
            logger.severe("LuckySky PREPARE fehlgeschlagen: " + result.message());
            state = RoundState.IDLE;
            return false;
        }
        logger.info(result.message());
        deleteLockQuietly();
        state = RoundState.PREPARED;
        return true;
    }

    public synchronized boolean resetLuckySky() {
        if (state == RoundState.RUNNING) {
            logger.warning("RESET abgebrochen: LuckySky läuft noch. Bitte zuerst stoppen.");
            return false;
        }
        state = RoundState.RESETTING;
        String worldName = plugin.configs().worlds().luckySky().worldName();
        if (plugin.getServer().getWorld(worldName) != null) {
            plugin.game().teleportAllToLobby();
        }
        ProvisioningResult result = provisioner.provisionLuckySky();
        if (!result.success()) {
            logger.severe("LuckySky RESET fehlgeschlagen: " + result.message());
            state = RoundState.AWAITING_RESET;
            return false;
        }
        logger.info(result.message());
        deleteLockQuietly();
        state = RoundState.PREPARED;
        return true;
    }

    public synchronized void onRoundStarted() {
        state = RoundState.RUNNING;
        createLock();
    }

    public synchronized void onRoundStopped() {
        if (state == RoundState.RUNNING) {
            state = RoundState.AWAITING_RESET;
        }
    }

    public synchronized void recoverIfLocked() {
        if (!Files.exists(lockFile)) {
            return;
        }
        logger.warning("LuckySky-Run-Lock entdeckt (" + lockFile.getFileName()
                + "). Server wurde vermutlich zwischen RUN und RESET beendet. Starte automatischen RESET...");
        boolean success = resetLuckySky();
        if (!success) {
            logger.severe("Automatischer LuckySky-Reset nach Crash fehlgeschlagen. Bitte Template prüfen und manuell eingreifen.");
        }
    }

    public synchronized void shutdown() {
        deleteLockQuietly();
        state = RoundState.IDLE;
    }

    private void createLock() {
        try {
            Files.createDirectories(lockFile.getParent());
            Files.writeString(lockFile, "locked " + Instant.now());
        } catch (IOException ex) {
            logger.warning("Konnte LuckySky-Lock nicht schreiben: " + ex.getMessage());
        }
    }

    private void deleteLockQuietly() {
        try {
            Files.deleteIfExists(lockFile);
        } catch (IOException ex) {
            logger.warning("Konnte LuckySky-Lock nicht entfernen: " + ex.getMessage());
        }
    }
}
