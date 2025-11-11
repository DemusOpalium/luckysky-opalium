package de.opalium.luckysky.round.handlers;

import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.round.RoundState;
import de.opalium.luckysky.round.RoundStateHandler;

public class CountdownStateHandler implements RoundStateHandler {
    private final GameManager gameManager;

    public CountdownStateHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void onEnter(RoundState from) {
        gameManager.countdownStage();
    }
}
