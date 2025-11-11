package de.opalium.luckysky.round.handlers;

import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.round.RoundState;
import de.opalium.luckysky.round.RoundStateHandler;

public class EndingStateHandler implements RoundStateHandler {
    private final GameManager gameManager;

    public EndingStateHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void onEnter(RoundState from) {
        gameManager.endingStage();
    }
}
