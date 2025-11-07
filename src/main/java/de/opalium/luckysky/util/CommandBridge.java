package de.opalium.luckysky.util;

import de.opalium.luckysky.LuckySkyPlugin;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Utility to dispatch commands as the console without blocking the caller thread.
 */
public class CommandBridge {
    private final LuckySkyPlugin plugin;
    private final CommandSender console;

    public CommandBridge(LuckySkyPlugin plugin) {
        this.plugin = plugin;
        this.console = plugin.getServer().getConsoleSender();
    }

    public void dispatch(String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    plugin.getServer().dispatchCommand(console, command);
                    return null;
                });
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to dispatch command: " + command, ex);
            }
        });
    }

    public void dispatchAll(Collection<String> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        commands.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).forEach(this::dispatch);
    }

    public void dispatchAll(String... commands) {
        if (commands == null || commands.length == 0) {
            return;
        }
        dispatchAll(List.of(commands));
    }
}
