package de.opalium.luckysky.game;

public enum GameState {
    /**
     * Initial state when the plugin boots and no round has been prepared yet.
     */
    IDLE,

    /**
     * LuckySky is open for players to gather on the platform / in the lobby area.
     */
    LOBBY,

    /**
     * A timed round is active and the countdown is running.
     */
    COUNTDOWN,

    /**
     * A round is in progress without a countdown (free-play mode).
     */
    RUN,

    /**
     * The round has ended and rewards / end timer are being processed.
     */
    ENDING,

    /**
     * The LuckySky world is currently resetting / being wiped.
     */
    RESETTING
}
