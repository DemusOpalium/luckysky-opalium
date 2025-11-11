package de.opalium.luckysky.commands;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.game.GameManager;
import de.opalium.luckysky.round.RoundStateMachine;
import de.opalium.luckysky.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class LsCommandStateMachineTest {
    @Test
    void startCommandDelegatesToStateMachine() {
        LuckySkyPlugin plugin = mock(LuckySkyPlugin.class);
        GameManager game = mock(GameManager.class);
        RoundStateMachine rounds = mock(RoundStateMachine.class);
        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);

        when(plugin.game()).thenReturn(game);
        when(plugin.rounds()).thenReturn(rounds);
        when(sender.hasPermission(anyString())).thenReturn(true);
        when(rounds.requestStart()).thenReturn(true);

        LsCommand lsCommand = new LsCommand(plugin);

        try (MockedStatic<Msg> msg = org.mockito.Mockito.mockStatic(Msg.class)) {
            boolean handled = lsCommand.onCommand(sender, command, "ls", new String[] {"start"});
            assertTrue(handled);
        }

        verify(rounds).requestStart();
    }

    @Test
    void stopCommandDelegatesToStateMachine() {
        LuckySkyPlugin plugin = mock(LuckySkyPlugin.class);
        GameManager game = mock(GameManager.class);
        RoundStateMachine rounds = mock(RoundStateMachine.class);
        CommandSender sender = mock(CommandSender.class);
        Command command = mock(Command.class);

        when(plugin.game()).thenReturn(game);
        when(plugin.rounds()).thenReturn(rounds);
        when(sender.hasPermission(anyString())).thenReturn(true);

        LsCommand lsCommand = new LsCommand(plugin);

        try (MockedStatic<Msg> msg = org.mockito.Mockito.mockStatic(Msg.class)) {
            boolean handled = lsCommand.onCommand(sender, command, "ls", new String[] {"stop"});
            assertTrue(handled);
        }

        verify(rounds).requestStop();
    }
}
