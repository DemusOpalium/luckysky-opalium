package de.opalium.luckysky.util;

import de.opalium.luckysky.LuckySkyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Messages {
    private final LuckySkyPlugin plugin;
    private FileConfiguration configuration;

    public Messages(LuckySkyPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() throws IOException, InvalidConfigurationException {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.configuration = YamlConfiguration.loadConfiguration(file);
    }

    public String getRaw(String key) {
        if (configuration == null) {
            return key;
        }
        return configuration.getString(key, key);
    }

    public String format(String key, Map<String, String> placeholders) {
        String message = ChatColor.translateAlternateColorCodes('&', getRaw(key));
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return message;
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(format(key, null));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(format(key, placeholders));
    }
}
