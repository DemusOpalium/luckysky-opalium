package de.opalium.luckysky.round.handlers;

import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.round.RoundState;
import de.opalium.luckysky.round.RoundStateHandler;

public class LobbyStateHandler implements RoundStateHandler {
    private final GameManager gameManager;

    public LobbyStateHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void onEnter(RoundState from) {
        gameManager.lobbyStage();
    }
}
