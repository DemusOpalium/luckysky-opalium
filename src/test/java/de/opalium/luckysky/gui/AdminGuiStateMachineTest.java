package de.opalium.luckysky.gui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.round.RoundStateMachine;
import de.opalium.luckysky.util.Msg;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AdminGuiStateMachineTest {
    private LuckySkyPlugin plugin;
    private GameManager game;
    private RoundStateMachine rounds;
    private Player player;

    @BeforeEach
    void setup() {
        plugin = mock(LuckySkyPlugin.class);
        game = mock(GameManager.class);
        rounds = mock(RoundStateMachine.class);
        player = mock(Player.class);

        when(plugin.game()).thenReturn(game);
        when(plugin.rounds()).thenReturn(rounds);
    }

    @Test
    void startWithDurationUsesStateMachine() throws Exception {
        when(rounds.requestStart()).thenReturn(true);
        AdminGui gui = new AdminGui(plugin) {
            @Override
            public void reload() {
                // no layout needed for tests
            }
        };

        Method method = AdminGui.class.getDeclaredMethod("startWithDuration", Player.class, int.class);
        method.setAccessible(true);

        try (MockedStatic<Msg> msg = org.mockito.Mockito.mockStatic(Msg.class)) {
            method.invoke(gui, player, 5);
        }

        verify(game).setDurationMinutes(5);
        verify(rounds).requestStart();
    }

    @Test
    void stopGameUsesStateMachine() throws Exception {
        AdminGui gui = new AdminGui(plugin) {
            @Override
            public void reload() {
                // no layout needed for tests
            }
        };

        Method method = AdminGui.class.getDeclaredMethod("stopGame", Player.class);
        method.setAccessible(true);

        try (MockedStatic<Msg> msg = org.mockito.Mockito.mockStatic(Msg.class)) {
            method.invoke(gui, player);
        }

        verify(rounds).requestStop();
    }
}
