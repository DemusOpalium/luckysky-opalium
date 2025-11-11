package de.opalium.luckysky.game;

import de.opalium.luckysky.LuckySkyPlugin;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Central state registry for the LuckySky game flow. Besides tracking the current {@link GameState}
 * it also keeps an access whitelist that is evaluated by the {@code AccessGate} so that only
 * approved players may enter the LuckySky world while sensitive operations (reset, wipe, ...)
 * are running.
 */
public class StateMachine {
    private final LuckySkyPlugin plugin;
    private final Set<UUID> accessWhitelist = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Consumer<GameState>> listeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private volatile GameState state = GameState.LOBBY;

    public StateMachine(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public GameState state() {
        return state;
    }

    public void setState(GameState newState) {
        if (newState == null) {
            throw new IllegalArgumentException("GameState may not be null");
        }
        GameState previous = this.state;
        if (previous == newState) {
            return;
        }
        this.state = newState;
        listeners.forEach(listener -> listener.accept(newState));
        if (plugin.scoreboard() != null) {
            plugin.scoreboard().refresh();
        }
    }

    public void onStateChange(Consumer<GameState> listener) {
        listeners.add(listener);
    }

    public void removeListener(Consumer<GameState> listener) {
        listeners.remove(listener);
    }

    public void whitelistPlayer(UUID playerId) {
        if (playerId != null) {
            accessWhitelist.add(playerId);
        }
    }

    public void whitelistPlayers(Collection<UUID> playerIds) {
        if (playerIds == null) {
            return;
        }
        for (UUID id : playerIds) {
            whitelistPlayer(id);
        }
    }

    public void removeFromWhitelist(UUID playerId) {
        if (playerId != null) {
            accessWhitelist.remove(playerId);
        }
    }

    public void clearWhitelist() {
        accessWhitelist.clear();
    }

    public boolean isWhitelisted(UUID playerId) {
        return playerId != null && accessWhitelist.contains(playerId);
    }

    public Set<UUID> accessWhitelist() {
        return Collections.unmodifiableSet(accessWhitelist);
    }
}
