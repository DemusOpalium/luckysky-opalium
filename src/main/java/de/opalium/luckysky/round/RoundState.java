package de.opalium.luckysky.round;

public enum RoundState {
    IDLE,
    PREPARE,
    LOBBY,
    COUNTDOWN,
    RUN,
    ENDING,
    RESET;

    public RoundState next() {
        return switch (this) {
            case IDLE -> PREPARE;
            case PREPARE -> LOBBY;
            case LOBBY -> COUNTDOWN;
            case COUNTDOWN -> RUN;
            case RUN -> ENDING;
            case ENDING -> RESET;
            case RESET -> IDLE;
        };
    }
}
