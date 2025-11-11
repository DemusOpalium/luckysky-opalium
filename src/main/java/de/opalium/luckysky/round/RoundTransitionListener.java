package de.opalium.luckysky.round;

@FunctionalInterface
public interface RoundTransitionListener {
    void onTransition(RoundState from, RoundState to);
}
