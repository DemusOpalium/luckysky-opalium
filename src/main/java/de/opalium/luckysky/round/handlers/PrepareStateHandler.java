package de.opalium.luckysky.round.handlers;

import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.round.RoundState;
import de.opalium.luckysky.round.RoundStateHandler;

public class PrepareStateHandler implements RoundStateHandler {
    private final GameManager gameManager;

    public PrepareStateHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void onEnter(RoundState from) {
        gameManager.prepareRoundStage();
    }

    @Override
    public void onExit(RoundState to) {
        gameManager.onLeavePrepareStage();
    }
}
