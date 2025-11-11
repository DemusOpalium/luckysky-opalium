package de.opalium.luckysky.round;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameManager;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public class RoundStateMachine {
    private final LuckySkyPlugin plugin;
    private final GameManager gameManager;
    private final Map<RoundState, RoundStateHandler> handlers = new EnumMap<>(RoundState.class);
    private final List<RoundTransitionListener> listeners = new ArrayList<>();
    private final Path lockFile = Path.of("/data/round.lock");
    private boolean lockAcquired;
    private RoundState currentState = RoundState.IDLE;

    public RoundStateMachine(LuckySkyPlugin plugin, GameManager gameManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.gameManager = Objects.requireNonNull(gameManager, "gameManager");
    }

    public void onEnable() {
        ensureLock();
    }

    public void onDisable() {
        releaseLock();
    }

    public void registerHandler(RoundState state, RoundStateHandler handler) {
        handlers.put(Objects.requireNonNull(state, "state"), Objects.requireNonNull(handler, "handler"));
    }

    public void addListener(RoundTransitionListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public synchronized RoundState currentState() {
        return currentState;
    }

    public synchronized boolean requestStart() {
        if (currentState != RoundState.IDLE) {
            return false;
        }
        if (!gameManager.canStartRound()) {
            return false;
        }
        return transitionSequence(RoundState.PREPARE, RoundState.LOBBY, RoundState.COUNTDOWN, RoundState.RUN);
    }

    public synchronized boolean requestStop() {
        return switch (currentState) {
            case IDLE -> true;
            case PREPARE, LOBBY, COUNTDOWN -> abortBeforeRun();
            case RUN -> transitionSequence(RoundState.ENDING, RoundState.RESET, RoundState.IDLE);
            case ENDING -> transitionSequence(RoundState.RESET, RoundState.IDLE);
            case RESET -> transitionSequence(RoundState.IDLE);
        };
    }

    private boolean abortBeforeRun() {
        RoundState previous = currentState;
        RoundStateHandler handler = handlers.get(previous);
        if (handler != null) {
            handler.onExit(RoundState.IDLE);
        }
        RoundStateHandler resetHandler = handlers.get(RoundState.RESET);
        if (resetHandler != null) {
            resetHandler.onEnter(previous);
            resetHandler.onExit(RoundState.IDLE);
        }
        currentState = RoundState.IDLE;
        notifyListeners(previous, RoundState.IDLE);
        return true;
    }

    private boolean transitionSequence(RoundState... targets) {
        for (RoundState target : targets) {
            if (!transitionTo(target)) {
                return false;
            }
        }
        return true;
    }

    private boolean transitionTo(RoundState target) {
        RoundState expected = currentState.next();
        if (target != expected) {
            return false;
        }
        RoundStateHandler currentHandler = handlers.get(currentState);
        RoundStateHandler targetHandler = handlers.get(target);
        if (targetHandler != null && !targetHandler.canEnter(currentState)) {
            return false;
        }
        if (currentHandler != null) {
            currentHandler.onExit(target);
        }
        RoundState previous = currentState;
        currentState = target;
        if (targetHandler != null) {
            targetHandler.onEnter(previous);
        }
        notifyListeners(previous, target);
        return true;
    }

    private void notifyListeners(RoundState from, RoundState to) {
        for (RoundTransitionListener listener : listeners) {
            try {
                listener.onTransition(from, to);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Round state listener failed", ex);
            }
        }
    }

    private void ensureLock() {
        if (lockAcquired) {
            return;
        }
        try {
            Path parent = lockFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createFile(lockFile);
            lockAcquired = true;
        } catch (FileAlreadyExistsException ex) {
            throw new IllegalStateException("Round lock already acquired", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create round lock", ex);
        }
    }

    private void releaseLock() {
        if (!lockAcquired) {
            return;
        }
        try {
            Files.deleteIfExists(lockFile);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to release round lock", ex);
        } finally {
            lockAcquired = false;
        }
    }
}
