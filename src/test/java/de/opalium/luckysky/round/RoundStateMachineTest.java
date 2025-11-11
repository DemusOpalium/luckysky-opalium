package de.opalium.luckysky.round;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoundStateMachineTest {
    private LuckySkyPlugin plugin;
    private GameManager game;
    private RoundStateMachine machine;

    @BeforeEach
    void setup() {
        plugin = mock(LuckySkyPlugin.class);
        game = mock(GameManager.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("RoundStateMachineTest"));
        machine = new RoundStateMachine(plugin, game);
    }

    @AfterEach
    void cleanupLock() throws Exception {
        machine.onDisable();
        Files.deleteIfExists(Path.of("/data/round.lock"));
    }

    @Test
    void requestStartRunsThroughSequence() {
        when(game.canStartRound()).thenReturn(true);
        List<RoundState> entered = new ArrayList<>();
        machine.registerHandler(RoundState.PREPARE, new RecordingHandler(entered, RoundState.PREPARE));
        machine.registerHandler(RoundState.LOBBY, new RecordingHandler(entered, RoundState.LOBBY));
        machine.registerHandler(RoundState.COUNTDOWN, new RecordingHandler(entered, RoundState.COUNTDOWN));
        machine.registerHandler(RoundState.RUN, new RecordingHandler(entered, RoundState.RUN));

        boolean started = machine.requestStart();

        assertTrue(started);
        assertEquals(List.of(RoundState.PREPARE, RoundState.LOBBY, RoundState.COUNTDOWN, RoundState.RUN), entered);
        assertEquals(RoundState.RUN, machine.currentState());
    }

    @Test
    void requestStopFromRunGoesThroughEndingAndReset() {
        when(game.canStartRound()).thenReturn(true);
        List<RoundState> entered = new ArrayList<>();
        machine.registerHandler(RoundState.PREPARE, new RecordingHandler(entered, RoundState.PREPARE));
        machine.registerHandler(RoundState.LOBBY, new RecordingHandler(entered, RoundState.LOBBY));
        machine.registerHandler(RoundState.COUNTDOWN, new RecordingHandler(entered, RoundState.COUNTDOWN));
        machine.registerHandler(RoundState.RUN, new RecordingHandler(entered, RoundState.RUN));
        machine.registerHandler(RoundState.ENDING, new RecordingHandler(entered, RoundState.ENDING));
        machine.registerHandler(RoundState.RESET, new RecordingHandler(entered, RoundState.RESET));

        assertTrue(machine.requestStart());
        entered.clear();

        boolean stopped = machine.requestStop();

        assertTrue(stopped);
        assertEquals(List.of(RoundState.ENDING, RoundState.RESET), entered);
        assertEquals(RoundState.IDLE, machine.currentState());
    }

    @Test
    void lockFileCreatedOnEnableAndRemovedOnDisable() throws Exception {
        when(game.canStartRound()).thenReturn(false);
        machine.onEnable();
        assertTrue(Files.exists(Path.of("/data/round.lock")));
        machine.onDisable();
        assertTrue(Files.notExists(Path.of("/data/round.lock")));
    }

    private static final class RecordingHandler implements RoundStateHandler {
        private final List<RoundState> order;
        private final RoundState state;

        private RecordingHandler(List<RoundState> order, RoundState state) {
            this.order = order;
            this.state = state;
        }

        @Override
        public void onEnter(RoundState from) {
            order.add(state);
        }
    }
}
