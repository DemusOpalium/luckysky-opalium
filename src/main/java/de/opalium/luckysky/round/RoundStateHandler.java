package de.opalium.luckysky.round;

public interface RoundStateHandler {
    default boolean canEnter(RoundState from) {
        return true;
    }

    default void onEnter(RoundState from) {
    }

    default void onExit(RoundState to) {
    }
}
