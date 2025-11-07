package de.opalium.luckysky.duels;

import de.opalium.luckysky.LuckySkyPlugin;
import de.opalium.luckysky.model.Settings;
import java.util.Locale;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public class DuelsService {
    private final LuckySkyPlugin plugin;
    private boolean enabled;
    private String pluginName;

    public DuelsService(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload(Settings settings) {
        Settings.DuelsSettings duels = settings.duels();
        if (duels == null) {
            enabled = false;
            pluginName = null;
            return;
        }
        enabled = duels.enabled();
        pluginName = duels.pluginName();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPluginPresent() {
        if (!enabled || pluginName == null || pluginName.isBlank()) {
            return false;
        }
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        Plugin target = pluginManager.getPlugin(pluginName);
        if (target == null) {
            // try fallback with lower-case name (common for legacy plugins)
            target = pluginManager.getPlugin(pluginName.toLowerCase(Locale.ROOT));
        }
        return target != null && target.isEnabled();
    }

    public String pluginName() {
        return pluginName;
    }
}
