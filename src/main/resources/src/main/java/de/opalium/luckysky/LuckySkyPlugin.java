package de.opalium.luckysky;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class LuckySkyPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("LuckySky-Opalium enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("LuckySky-Opalium disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("ls")) return false;

        if (!sender.hasPermission("luckysky.admin")) {
            sender.sendMessage(ChatColor.RED + "Keine Rechte.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "⛯ LuckySky: /ls start • stop • reset • clean • rig_on • rig_off • plat • plat+ • corridor • sign • bind • mode5 • mode20 • mode60 • wither • taunt_on • taunt_off");
            return true;
        }

        // Platzhalter: hier bauen wir später die echte Logik (Rig, Safe-Platform, Wipes, etc.)
        String sub = args[0].toLowerCase();

        switch (sub) {
            case "start" -> sender.sendMessage(ok("Start (Phase=1) – TODO implement"));
            case "stop" -> sender.sendMessage(ok("Stop (Phase=0) – TODO implement"));
            case "reset", "clean" -> sender.sendMessage(ok("Field clear / Hard wipe – TODO implement"));
            case "rig_on" -> sender.sendMessage(ok("Build console rig – TODO implement"));
            case "rig_off" -> sender.sendMessage(ok("Remove console rig – TODO implement"));
            case "plat" -> sender.sendMessage(ok("Safe platform 3+1 – TODO implement"));
            case "plat+" -> sender.sendMessage(ok("Safe platform 3×3 – TODO implement"));
            case "corridor" -> sender.sendMessage(ok("Corridor clear – TODO implement"));
            case "sign" -> sender.sendMessage(ok("Warp sign + setwarp – TODO implement"));
            case "bind" -> sender.sendMessage(ok("Bind all spawnpoints – TODO implement"));
            case "mode5", "mode20", "mode60" -> sender.sendMessage(ok("Set duration – TODO implement"));
            case "wither" -> sender.sendMessage(ok("Summon Wither – TODO implement"));
            case "taunt_on", "taunt_off" -> sender.sendMessage(ok("Wither taunts toggle – TODO implement"));
            default -> sender.sendMessage(ChatColor.GRAY + "Unbekanntes Subcommand. /ls für Hilfe.");
        }
        return true;
    }

    private String ok(String msg) {
        return ChatColor.GREEN + "OK: " + ChatColor.WHITE + msg;
    }
}
